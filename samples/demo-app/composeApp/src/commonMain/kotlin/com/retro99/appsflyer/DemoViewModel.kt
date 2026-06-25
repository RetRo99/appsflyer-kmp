package com.retro99.appsflyer.sample

import com.retro99.appsflyer.AdRevenueData
import com.retro99.appsflyer.AfEmailCryptType
import com.retro99.appsflyer.AfLogLevel
import com.retro99.appsflyer.AfMediationNetwork
import com.retro99.appsflyer.AfPurchaseType
import com.retro99.appsflyer.AppsFlyer
import com.retro99.appsflyer.CampaignData
import com.retro99.appsflyer.DeepLinkResult
import com.retro99.appsflyer.InviteLinkParams
import com.retro99.appsflyer.LogEventResult
import com.retro99.appsflyer.PurchaseDetails
import com.retro99.appsflyer.PurchaseValidationResult
import com.retro99.appsflyer.StartResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LogLevel { INFO, SUCCESS, ERROR, DEEPLINK }

enum class ButtonPlatform { ANDROID, IOS, BOTH }

enum class ParamKey(val label: String, val default: String) {
    EVENT_NAME("Event", "test_event"),
    APP_ID("App ID", "app123"),
    CAMPAIGN("Campaign", "campaign1"),
    CHANNEL("Channel", "email"),
    CUSTOMER_USER_ID("User ID", "user-123"),
    DEEP_LINK_URL("DeepLink URL", "https://example.onelink.me/deeplink"),
    CURRENCY_CODE("Currency", "USD"),
    REVENUE("Revenue", "1.50"),
    EMAIL("Email", "test@test.com"),
    PHONE_NUMBER("Phone", "+1234567890"),
    PRODUCT_ID("Product ID", "com.example.product"),
    TRANSACTION_ID("Transaction", "txn_001"),
    PARTNER_ID("Partner", "partner1"),
    MEDIA_SOURCE("Media Src", "admob"),
    LATITUDE("Latitude", "37.7749"),
    LONGITUDE("Longitude", "-122.4194"),
}

enum class Section(val title: String) {
    SESSION("Session & Attribution"),
    EVENTS("Events"),
    USER_IDENTITY("User Identity"),
    DEEP_LINKING("Deep Linking"),
    PRIVACY("Privacy & Filters"),
    CONFIGURATION("Configuration"),
    ANDROID_ONLY("Android-only"),
    IOS_ONLY("iOS-only"),
    CROSS_PROMO("Cross-Promotion & Invites"),
    PARTNER_PLUGIN("Partner & Plugin"),
}

data class LogEntry(
    val timestamp: String,
    val message: String,
    val level: LogLevel,
)

data class DemoButton(
    val label: String,
    val platform: ButtonPlatform,
    val onClick: () -> Unit,
)

data class DemoSection(
    val section: Section,
    val buttons: List<DemoButton>,
)

data class DemoUiState(
    val logs: List<LogEntry> = emptyList(),
    val params: Map<ParamKey, String> = ParamKey.entries.associateWith { it.default },
    val collapsedSections: Set<Section> = emptySet(),
)

class DemoViewModel : ViewModel() {

    private val client = AppsFlyer.client

    private val _uiState = MutableStateFlow(DemoUiState())
    val uiState: StateFlow<DemoUiState> = _uiState.asStateFlow()

    val isAndroid: Boolean get() = currentPlatform == Platform.ANDROID

    init {
        client.setCustomerUserId("demo-user-001")
        observeDeepLinks()
        observeStartResult()
        observeConversionData()
        client.start()
    }

    fun updateParam(key: ParamKey, value: String) {
        _uiState.update { it.copy(params = it.params + (key to value)) }
    }

    fun toggleSection(section: Section) {
        _uiState.update { state ->
            val collapsed = if (section in state.collapsedSections) {
                state.collapsedSections - section
            } else {
                state.collapsedSections + section
            }
            state.copy(collapsedSections = collapsed)
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun isButtonEnabled(platform: ButtonPlatform): Boolean =
        platform == ButtonPlatform.BOTH || platform.name == currentPlatform.name

    fun runSection(section: DemoSection) {
        section.buttons
            .filter { isButtonEnabled(it.platform) }
            .forEach { it.onClick() }
    }

    fun exportLogs(): String =
        _uiState.value.logs.joinToString("\n") { "${it.timestamp} [${it.level}] ${it.message}" }

    private fun log(message: String, level: LogLevel = LogLevel.INFO) {
        val entry = LogEntry(
            timestamp = formatTimestamp(nowMillis()),
            message = message,
            level = level,
        )
        _uiState.update { it.copy(logs = it.logs + entry) }
    }

    private fun runSuspend(
        level: LogLevel = LogLevel.INFO,
        block: suspend () -> String,
    ) {
        viewModelScope.launch {
            try {
                log(block(), level)
            } catch (e: Exception) {
                log("Error: ${e.message}", LogLevel.ERROR)
            }
        }
    }

    private fun observeStartResult() {
        viewModelScope.launch {
            val result = client.getStartResult()
            when (result) {
                is StartResult.Success -> log("Start: Success", LogLevel.SUCCESS)
                is StartResult.Error -> log("Start: Error(${result.code}, ${result.message})", LogLevel.ERROR)
            }
        }
    }

    private fun observeConversionData() {
        viewModelScope.launch {
            val data = client.getConversionData()
            when (data) {
                is CampaignData.Success ->
                    log("Conversion: ${data.status} | source=${data.mediaSource} campaign=${data.campaign}")
                is CampaignData.Error -> log("Conversion Error: ${data.message}", LogLevel.ERROR)
            }
        }
    }

    private fun observeDeepLinks() {
        viewModelScope.launch {
            client.deepLink.collect { result ->
                when (result) {
                    is DeepLinkResult.Found ->
                        log("DeepLink: value=${result.deepLinkValue} deferred=${result.isDeferred}", LogLevel.DEEPLINK)
                    is DeepLinkResult.NotFound -> log("DeepLink: Not Found", LogLevel.DEEPLINK)
                    is DeepLinkResult.Error -> log("DeepLink Error: ${result.message}", LogLevel.DEEPLINK)
                }
            }
        }
    }

    val sections: List<DemoSection> get() = build(p())

    private fun p(): Map<ParamKey, String> = _uiState.value.params

    private fun build(lp: Map<ParamKey, String>): List<DemoSection> = listOf(
        DemoSection(
            section = Section.SESSION,
            buttons = listOf(
                DemoButton("start()", ButtonPlatform.BOTH) {
                    client.start()
                    log("start() called")
                },
                DemoButton("getStartResult()", ButtonPlatform.BOTH) {
                    runSuspend {
                        when (val r = client.getStartResult()) {
                            is StartResult.Success -> "getStartResult: Success"
                            is StartResult.Error -> "getStartResult: Error(${r.code}, ${r.message})"
                        }
                    }
                },
                DemoButton("getConversionData()", ButtonPlatform.BOTH) {
                    runSuspend {
                        when (val d = client.getConversionData()) {
                            is CampaignData.Success -> "getConversionData: ${d.status} source=${d.mediaSource} campaign=${d.campaign}"
                            is CampaignData.Error -> "getConversionData: Error ${d.message}"
                        }
                    }
                },
                DemoButton("getAppsFlyerUID()", ButtonPlatform.BOTH) {
                    log("getAppsFlyerUID: ${client.getAppsFlyerUID()}")
                },
                DemoButton("getSdkVersion()", ButtonPlatform.BOTH) {
                    log("getSdkVersion: ${client.getSdkVersion()}")
                },
                DemoButton("stop(true)", ButtonPlatform.BOTH) {
                    client.stop(stop = true)
                    log("stop(true) called")
                },
                DemoButton("stop(false)", ButtonPlatform.BOTH) {
                    client.stop(stop = false)
                    log("stop(false) called")
                },
                DemoButton("isStopped", ButtonPlatform.BOTH) {
                    log("isStopped: ${client.isStopped}")
                },
            ),
        ),
        DemoSection(
            section = Section.EVENTS,
            buttons = listOf(
                DemoButton("logEvent()", ButtonPlatform.BOTH) {
                    client.logEvent(
                        lp[ParamKey.EVENT_NAME] ?: ParamKey.EVENT_NAME.default,
                        mapOf("key" to "value", "count" to 1),
                    )
                    log("logEvent: ${lp[ParamKey.EVENT_NAME]}")
                },
                DemoButton("logEventForResult()", ButtonPlatform.BOTH) {
                    runSuspend {
                        when (val r = client.logEventForResult(
                            lp[ParamKey.EVENT_NAME] ?: ParamKey.EVENT_NAME.default,
                            mapOf("param" to "val"),
                        )) {
                            is LogEventResult.Success -> "logEventForResult: Success"
                            is LogEventResult.Error -> "logEventForResult: Error(${r.code}, ${r.message})"
                        }
                    }
                },
                DemoButton("logAdRevenue()", ButtonPlatform.BOTH) {
                    client.logAdRevenue(
                        AdRevenueData(
                            monetizationNetwork = lp[ParamKey.MEDIA_SOURCE] ?: ParamKey.MEDIA_SOURCE.default,
                            mediationNetwork = AfMediationNetwork.GOOGLE_ADMOB,
                            currency = lp[ParamKey.CURRENCY_CODE] ?: ParamKey.CURRENCY_CODE.default,
                            revenue = (lp[ParamKey.REVENUE] ?: ParamKey.REVENUE.default).toDoubleOrNull() ?: 0.0,
                            additionalParameters = mapOf("country" to "US"),
                        ),
                    )
                    log("logAdRevenue: ${lp[ParamKey.REVENUE]} ${lp[ParamKey.CURRENCY_CODE]}")
                },
                DemoButton("logLocation()", ButtonPlatform.BOTH) {
                    client.logLocation(
                        latitude = (lp[ParamKey.LATITUDE] ?: ParamKey.LATITUDE.default).toDoubleOrNull() ?: 0.0,
                        longitude = (lp[ParamKey.LONGITUDE] ?: ParamKey.LONGITUDE.default).toDoubleOrNull() ?: 0.0,
                    )
                    log("logLocation: ${lp[ParamKey.LATITUDE]}, ${lp[ParamKey.LONGITUDE]}")
                },
                DemoButton("validateAndLogInAppPurchase()", ButtonPlatform.BOTH) {
                    runSuspend(LogLevel.SUCCESS) {
                        when (val r = client.validateAndLogInAppPurchase(
                            PurchaseDetails(
                                productId = lp[ParamKey.PRODUCT_ID] ?: ParamKey.PRODUCT_ID.default,
                                transactionId = lp[ParamKey.TRANSACTION_ID] ?: ParamKey.TRANSACTION_ID.default,
                                purchaseType = AfPurchaseType.ONE_TIME_PURCHASE,
                            ),
                        )) {
                            is PurchaseValidationResult.Success -> "validateAndLogInAppPurchase: Success ${r.result}"
                            is PurchaseValidationResult.Error -> "validateAndLogInAppPurchase: Error ${r.message}"
                        }
                    }
                },
            ),
        ),
        DemoSection(
            section = Section.USER_IDENTITY,
            buttons = listOf(
                DemoButton("setCustomerUserId()", ButtonPlatform.BOTH) {
                    client.setCustomerUserId(lp[ParamKey.CUSTOMER_USER_ID] ?: ParamKey.CUSTOMER_USER_ID.default)
                    log("setCustomerUserId: ${lp[ParamKey.CUSTOMER_USER_ID]}")
                },
                DemoButton("setCustomerUserId(null)", ButtonPlatform.BOTH) {
                    client.setCustomerUserId(null)
                    log("setCustomerUserId: null")
                },
                DemoButton("setCustomerIdAndLogSession()", ButtonPlatform.BOTH) {
                    client.setCustomerIdAndLogSession(lp[ParamKey.CUSTOMER_USER_ID] ?: ParamKey.CUSTOMER_USER_ID.default)
                    log("setCustomerIdAndLogSession: ${lp[ParamKey.CUSTOMER_USER_ID]}")
                },
                DemoButton("setUserEmails()", ButtonPlatform.BOTH) {
                    client.setUserEmails(
                        emails = listOf(lp[ParamKey.EMAIL] ?: ParamKey.EMAIL.default),
                        cryptType = AfEmailCryptType.SHA256,
                    )
                    log("setUserEmails: [${lp[ParamKey.EMAIL]}] SHA256")
                },
                DemoButton("setPhoneNumber()", ButtonPlatform.BOTH) {
                    client.setPhoneNumber(lp[ParamKey.PHONE_NUMBER] ?: ParamKey.PHONE_NUMBER.default)
                    log("setPhoneNumber: ${lp[ParamKey.PHONE_NUMBER]}")
                },
                DemoButton("setPhoneNumber(null)", ButtonPlatform.BOTH) {
                    client.setPhoneNumber(null)
                    log("setPhoneNumber: null")
                },
            ),
        ),
        DemoSection(
            section = Section.DEEP_LINKING,
            buttons = listOf(
                DemoButton("setDeepLinkTimeout(5)", ButtonPlatform.BOTH) {
                    client.setDeepLinkTimeout(5)
                    log("setDeepLinkTimeout: 5")
                },
                DemoButton("performOnAppAttribution()", ButtonPlatform.BOTH) {
                    client.performOnAppAttribution(lp[ParamKey.DEEP_LINK_URL] ?: ParamKey.DEEP_LINK_URL.default)
                    log("performOnAppAttribution: ${lp[ParamKey.DEEP_LINK_URL]}")
                },
                DemoButton("setOneLinkCustomDomain()", ButtonPlatform.BOTH) {
                    client.setOneLinkCustomDomain(listOf("mydomain.onelink.me"))
                    log("setOneLinkCustomDomain: [mydomain.onelink.me]")
                },
                DemoButton("appendParametersToDeepLinkingURL()", ButtonPlatform.BOTH) {
                    client.appendParametersToDeepLinkingURL(
                        contains = "example",
                        parameters = mapOf("param" to "value"),
                    )
                    log("appendParametersToDeepLinkingURL: example")
                },
                DemoButton("setResolveDeepLinkURLs()", ButtonPlatform.BOTH) {
                    client.setResolveDeepLinkURLs(listOf("https://redirect.example.com"))
                    log("setResolveDeepLinkURLs: [https://redirect.example.com]")
                },
                DemoButton("enableFacebookDeferredApplinks(true)", ButtonPlatform.ANDROID) {
                    client.enableFacebookDeferredApplinks(true)
                    log("enableFacebookDeferredApplinks: true")
                },
                DemoButton("addPushNotificationDeepLinkPath()", ButtonPlatform.BOTH) {
                    client.addPushNotificationDeepLinkPath(listOf("af", "deep_link"))
                    log("addPushNotificationDeepLinkPath: [af, deep_link]")
                },
                DemoButton("handlePushNotification()", ButtonPlatform.IOS) {
                    client.handlePushNotification(mapOf("alert" to "test", "af" to "deep_link"))
                    log("handlePushNotification: {alert=test, af=deep_link}")
                },
            ),
        ),
        DemoSection(
            section = Section.PRIVACY,
            buttons = listOf(
                DemoButton("setAnonymizeUser(true)", ButtonPlatform.BOTH) {
                    client.setAnonymizeUser(true)
                    log("setAnonymizeUser: true")
                },
                DemoButton("setAnonymizeUser(false)", ButtonPlatform.BOTH) {
                    client.setAnonymizeUser(false)
                    log("setAnonymizeUser: false")
                },
                DemoButton("setSharingFilterPartners()", ButtonPlatform.BOTH) {
                    client.setSharingFilterPartners(setOf(lp[ParamKey.PARTNER_ID] ?: ParamKey.PARTNER_ID.default, "partner2"))
                    log("setSharingFilterPartners: [${lp[ParamKey.PARTNER_ID]}, partner2]")
                },
                DemoButton("setSharingFilterForAllPartners()", ButtonPlatform.BOTH) {
                    client.setSharingFilterForAllPartners()
                    log("setSharingFilterForAllPartners: called")
                },
                DemoButton("setDisableAdvertisingIdentifier(true)", ButtonPlatform.BOTH) {
                    client.setDisableAdvertisingIdentifier(true)
                    log("setDisableAdvertisingIdentifier: true")
                },
                DemoButton("setDisableSKAdNetwork(true)", ButtonPlatform.IOS) {
                    client.setDisableSKAdNetwork(true)
                    log("setDisableSKAdNetwork: true")
                },
                DemoButton("setDisableIDFVCollection(true)", ButtonPlatform.IOS) {
                    client.setDisableIDFVCollection(true)
                    log("setDisableIDFVCollection: true")
                },
                DemoButton("setDisableCollectASA(true)", ButtonPlatform.IOS) {
                    client.setDisableCollectASA(true)
                    log("setDisableCollectASA: true")
                },
                DemoButton("setDisableAppleAdsAttribution(true)", ButtonPlatform.IOS) {
                    client.setDisableAppleAdsAttribution(true)
                    log("setDisableAppleAdsAttribution: true")
                },
                DemoButton("waitForATTUserAuthorization(60)", ButtonPlatform.IOS) {
                    client.waitForATTUserAuthorization(60.0)
                    log("waitForATTUserAuthorization: 60s")
                },
            ),
        ),
        DemoSection(
            section = Section.CONFIGURATION,
            buttons = listOf(
                DemoButton("setCurrencyCode()", ButtonPlatform.BOTH) {
                    client.setCurrencyCode(lp[ParamKey.CURRENCY_CODE] ?: ParamKey.CURRENCY_CODE.default)
                    log("setCurrencyCode: ${lp[ParamKey.CURRENCY_CODE]}")
                },
                DemoButton("setAdditionalData()", ButtonPlatform.BOTH) {
                    client.setAdditionalData(mapOf("custom" to "data", "num" to 42))
                    log("setAdditionalData: {custom=data, num=42}")
                },
                DemoButton("setMinTimeBetweenSessions(30)", ButtonPlatform.BOTH) {
                    client.setMinTimeBetweenSessions(30)
                    log("setMinTimeBetweenSessions: 30")
                },
                DemoButton("setLogLevel(DEBUG)", ButtonPlatform.BOTH) {
                    client.setLogLevel(AfLogLevel.DEBUG)
                    log("setLogLevel: DEBUG")
                },
                DemoButton("setLogLevel(NONE)", ButtonPlatform.BOTH) {
                    client.setLogLevel(AfLogLevel.NONE)
                    log("setLogLevel: NONE")
                },
                DemoButton("setShouldCollectDeviceName(true)", ButtonPlatform.IOS) {
                    client.setShouldCollectDeviceName(true)
                    log("setShouldCollectDeviceName: true")
                },
                DemoButton("setUseReceiptValidationSandbox(true)", ButtonPlatform.IOS) {
                    client.setUseReceiptValidationSandbox(true)
                    log("setUseReceiptValidationSandbox: true")
                },
                DemoButton("setUseUninstallSandbox(true)", ButtonPlatform.IOS) {
                    client.setUseUninstallSandbox(true)
                    log("setUseUninstallSandbox: true")
                },
                DemoButton("setCurrentDeviceLanguage(\"en\")", ButtonPlatform.IOS) {
                    client.setCurrentDeviceLanguage("en")
                    log("setCurrentDeviceLanguage: en")
                },
                DemoButton("remoteDebuggingCall()", ButtonPlatform.IOS) {
                    client.remoteDebuggingCall("test_data")
                    log("remoteDebuggingCall: test_data")
                },
                DemoButton("setHost()", ButtonPlatform.BOTH) {
                    client.setHost("prefix", "example.com")
                    log("setHost: prefix, example.com")
                },
                DemoButton("getHostName()", ButtonPlatform.BOTH) {
                    log("getHostName: ${client.getHostName()}")
                },
                DemoButton("getHostPrefix()", ButtonPlatform.BOTH) {
                    log("getHostPrefix: ${client.getHostPrefix()}")
                },
                DemoButton("setExtension(\"ext\")", ButtonPlatform.ANDROID) {
                    client.setExtension("ext")
                    log("setExtension: ext")
                },
                DemoButton("setInstallId()", ButtonPlatform.BOTH) {
                    client.setInstallId("install-123")
                    log("setInstallId: install-123")
                },
            ),
        ),
        DemoSection(
            section = Section.ANDROID_ONLY,
            buttons = listOf(
                DemoButton("setIsUpdate(true)", ButtonPlatform.ANDROID) {
                    client.setIsUpdate(true)
                    log("setIsUpdate: true")
                },
                DemoButton("setCollectIMEI(true)", ButtonPlatform.ANDROID) {
                    client.setCollectIMEI(true)
                    log("setCollectIMEI: true")
                },
                DemoButton("setCollectOaid(true)", ButtonPlatform.ANDROID) {
                    client.setCollectOaid(true)
                    log("setCollectOaid: true")
                },
                DemoButton("setImeiData()", ButtonPlatform.ANDROID) {
                    client.setImeiData("imei_value")
                    log("setImeiData: imei_value")
                },
                DemoButton("setOaidData()", ButtonPlatform.ANDROID) {
                    client.setOaidData("oaid_value")
                    log("setOaidData: oaid_value")
                },
                DemoButton("setAndroidIdData()", ButtonPlatform.ANDROID) {
                    client.setAndroidIdData("android_id_value")
                    log("setAndroidIdData: android_id_value")
                },
                DemoButton("disableAppSetId()", ButtonPlatform.ANDROID) {
                    client.disableAppSetId()
                    log("disableAppSetId: called")
                },
                DemoButton("setDisableNetworkData(true)", ButtonPlatform.ANDROID) {
                    client.setDisableNetworkData(true)
                    log("setDisableNetworkData: true")
                },
                DemoButton("waitForCustomerUserId(true)", ButtonPlatform.ANDROID) {
                    client.waitForCustomerUserId(true)
                    log("waitForCustomerUserId: true")
                },
                DemoButton("setPreinstallAttribution()", ButtonPlatform.ANDROID) {
                    client.setPreinstallAttribution("source", "campaign", "site123")
                    log("setPreinstallAttribution: source, campaign, site123")
                },
                DemoButton("setOutOfStore()", ButtonPlatform.ANDROID) {
                    client.setOutOfStore("play_store")
                    log("setOutOfStore: play_store")
                },
                DemoButton("isPreInstalledApp()", ButtonPlatform.ANDROID) {
                    log("isPreInstalledApp: ${client.isPreInstalledApp()}")
                },
                DemoButton("getAttributionId()", ButtonPlatform.ANDROID) {
                    log("getAttributionId: ${client.getAttributionId()}")
                },
                DemoButton("getOutOfStore()", ButtonPlatform.ANDROID) {
                    log("getOutOfStore: ${client.getOutOfStore()}")
                },
                DemoButton("logSession()", ButtonPlatform.ANDROID) {
                    client.logSession()
                    log("logSession: called")
                },
                DemoButton("onPause()", ButtonPlatform.ANDROID) {
                    client.onPause()
                    log("onPause: called")
                },
                DemoButton("registerUninstall()", ButtonPlatform.ANDROID) {
                    client.registerUninstall("fake-token-123")
                    log("registerUninstall: fake-token-123")
                },
            ),
        ),
        DemoSection(
            section = Section.IOS_ONLY,
            buttons = listOf(
                DemoButton("isSessionReady()", ButtonPlatform.IOS) {
                    log("isSessionReady: ${client.isSessionReady()}")
                },
                DemoButton("unregisterSessionReadyListener()", ButtonPlatform.IOS) {
                    client.unregisterSessionReadyListener()
                    log("unregisterSessionReadyListener: called")
                },
            ),
        ),
        DemoSection(
            section = Section.CROSS_PROMO,
            buttons = listOf(
                DemoButton("logCrossPromoteImpression()", ButtonPlatform.BOTH) {
                    client.logCrossPromoteImpression(
                        lp[ParamKey.APP_ID] ?: ParamKey.APP_ID.default,
                        lp[ParamKey.CAMPAIGN] ?: ParamKey.CAMPAIGN.default,
                    )
                    log("logCrossPromoteImpression: ${lp[ParamKey.APP_ID]}, ${lp[ParamKey.CAMPAIGN]}")
                },
                DemoButton("logAndOpenStore()", ButtonPlatform.BOTH) {
                    client.logAndOpenStore(
                        lp[ParamKey.APP_ID] ?: ParamKey.APP_ID.default,
                        lp[ParamKey.CAMPAIGN] ?: ParamKey.CAMPAIGN.default,
                    )
                    log("logAndOpenStore: ${lp[ParamKey.APP_ID]}, ${lp[ParamKey.CAMPAIGN]}")
                },
                DemoButton("logInvite()", ButtonPlatform.BOTH) {
                    client.logInvite(
                        lp[ParamKey.CHANNEL] ?: ParamKey.CHANNEL.default,
                        mapOf("ref" to "user1"),
                    )
                    log("logInvite: ${lp[ParamKey.CHANNEL]}, {ref=user1}")
                },
                DemoButton("generateInviteUrl()", ButtonPlatform.BOTH) {
                    runSuspend {
                        val url = client.generateInviteUrl(
                            InviteLinkParams(
                                channel = lp[ParamKey.CHANNEL],
                                campaign = lp[ParamKey.CAMPAIGN],
                                referrerName = "Demo User",
                            ),
                        )
                        "generateInviteUrl: $url"
                    }
                },
                DemoButton("setAppInviteOneLink()", ButtonPlatform.BOTH) {
                    client.setAppInviteOneLink("oneLink123")
                    log("setAppInviteOneLink: oneLink123")
                },
            ),
        ),
        DemoSection(
            section = Section.PARTNER_PLUGIN,
            buttons = listOf(
                DemoButton("setPartnerData()", ButtonPlatform.BOTH) {
                    client.setPartnerData(
                        lp[ParamKey.PARTNER_ID] ?: ParamKey.PARTNER_ID.default,
                        mapOf("key" to "value"),
                    )
                    log("setPartnerData: ${lp[ParamKey.PARTNER_ID]}, {key=value}")
                },
                DemoButton("setPluginInfo()", ButtonPlatform.BOTH) {
                    client.setPluginInfo("kotlin", "1.0.0")
                    log("setPluginInfo: kotlin 1.0.0")
                },
            ),
        ),
    )
}
