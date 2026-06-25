package com.retro99.appsflyer

import android.content.Context
import com.appsflyer.AFAdRevenueData
import com.appsflyer.AFPurchaseDetails
import com.appsflyer.AFPurchaseType
import com.appsflyer.AFLogger
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerInAppPurchaseValidationCallback
import com.appsflyer.AppsFlyerLib
import com.appsflyer.AppsFlyerProperties
import com.appsflyer.MediationNetwork
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.appsflyer.deeplink.DeepLink
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult as AfDeepLinkResult
import com.appsflyer.deeplink.DeepLinkResult.Status
import java.lang.ref.WeakReference

internal class AndroidAppsFlyerSdk(
    context: Context,
) : AppsFlyerSdk {

    private val appContext: Context = context.applicationContext
    private var startContextRef: WeakReference<Context>? = WeakReference(context)

    private val lib: AppsFlyerLib = AppsFlyerLib.getInstance()
    private var deepLinkCallback: ((DeepLinkResult) -> Unit)? = null
    private var deepLinkTimeoutMs: Long? = null

    override fun configure(
        config: AppsFlyerConfig,
        onConversion: (Map<String, Any?>) -> Unit,
        onConversionError: (String) -> Unit,
        onDeepLink: (DeepLinkResult) -> Unit,
        onStart: (StartResult) -> Unit,
    ) {
        val ctx = startContextRef?.get() ?: appContext
        startContextRef = null
        config.logLevel?.let { level -> lib.setLogLevel(level.toAndroidLogLevel()) }
            ?: lib.setDebugLog(config.isDebug)
        lib.setCollectAndroidID(config.collectAndroidId)
        lib.anonymizeUser(config.anonymizeUser)
        lib.setSharingFilterForPartners(*config.sharingFilterPartners.toTypedArray())
        lib.init(
            config.devKey,
            object : AppsFlyerConversionListener {
                override fun onConversionDataSuccess(data: Map<String, Any>) = onConversion(data)

                override fun onConversionDataFail(errorMessage: String) = onConversionError(errorMessage)

                override fun onAppOpenAttribution(attributionData: Map<String, String>) {
                    // No-op: unified deep link API (subscribeForDeepLink) handles this.
                }

                override fun onAttributionFailure(errorMessage: String) {
                    // No-op: unified deep link API (subscribeForDeepLink) handles this.
                }
            },
            ctx,
        )
        lib.enableTCFDataCollection(config.enableTCFDataCollection)
        config.consentData?.let { consent ->
            lib.setConsentData(
                com.appsflyer.AppsFlyerConsent(
                    consent.isUserSubjectToGDPR,
                    consent.hasConsentForDataUsage,
                    consent.hasConsentForAdsPersonalization,
                    consent.hasConsentForAdStorage,
                ),
            )
        }
        deepLinkCallback = onDeepLink
        config.deepLinkTimeoutMs?.let { deepLinkTimeoutMs = it }
        subscribeForDeepLinks()
        lib.start(
            ctx,
            config.devKey,
            object : AppsFlyerRequestListener {
                override fun onSuccess() = onStart(StartResult.Success)

                override fun onError(code: Int, message: String) = onStart(StartResult.Error(code, message))
            },
        )
    }

    private fun subscribeForDeepLinks() {
        val callback = deepLinkCallback ?: return
        val listener = DeepLinkListener { result -> callback(toDeepLinkResult(result)) }
        val timeout = deepLinkTimeoutMs
        if (timeout != null) {
            lib.subscribeForDeepLink(listener, timeout)
        } else {
            lib.subscribeForDeepLink(listener)
        }
    }

    override fun setCustomerUserId(id: String?) {
        lib.setCustomerUserId(id)
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        lib.logEvent(appContext, name, params)
    }

    override fun logEventForResult(
        name: String,
        params: Map<String, Any?>,
        onResult: (LogEventResult) -> Unit,
    ) {
        lib.logEvent(
            appContext,
            name,
            params,
            object : AppsFlyerRequestListener {
                override fun onSuccess() = onResult(LogEventResult.Success)

                override fun onError(code: Int, message: String) =
                    onResult(LogEventResult.Error(code, message))
            },
        )
    }

    override fun getAppsFlyerUID(): String? =
        lib.getAppsFlyerUID(appContext)

    override fun getSdkVersion(): String =
        lib.sdkVersion

    override fun setCurrencyCode(currency: String) {
        lib.setCurrencyCode(currency)
    }

    override fun logLocation(latitude: Double, longitude: Double) {
        lib.logLocation(appContext, latitude, longitude)
    }

    override fun setAdditionalData(data: Map<String, Any?>) {
        lib.setAdditionalData(data)
    }

    override fun setMinTimeBetweenSessions(seconds: Int) {
        lib.setMinTimeBetweenSessions(seconds)
    }

    override fun setDisableAdvertisingIdentifier(disable: Boolean) {
        lib.setDisableAdvertisingIdentifiers(disable)
    }

    override fun setDisableSKAdNetwork(disable: Boolean) {
        // SKAdNetwork is iOS only — no-op on Android.
    }

    override fun setUserEmails(emails: List<String>, cryptType: AfEmailCryptType) {
        lib.setUserEmails(cryptType.toAndroidCryptType(), *emails.toTypedArray())
    }

    override fun registerUninstall(token: String) {
        lib.updateServerUninstallToken(appContext, token)
    }

    override fun setOneLinkCustomDomain(domains: List<String>) {
        lib.setOneLinkCustomDomain(*domains.toTypedArray())
    }

    override fun appendParametersToDeepLinkingURL(contains: String, parameters: Map<String, String>) {
        lib.appendParametersToDeepLinkingURL(contains, parameters)
    }

    override fun setPartnerData(partnerId: String, data: Map<String, Any?>) {
        lib.setPartnerData(partnerId, data)
    }

    override fun addPushNotificationDeepLinkPath(keys: List<String>) {
        lib.addPushNotificationDeepLinkPath(*keys.toTypedArray())
    }

    override fun setResolveDeepLinkURLs(urls: List<String>) {
        lib.setResolveDeepLinkURLs(*urls.toTypedArray())
    }

    override fun setHost(hostPrefix: String, hostName: String) {
        lib.setHost(hostPrefix, hostName)
    }

    override fun getHostName(): String = lib.hostName

    override fun getHostPrefix(): String = lib.hostPrefix

    override fun setAppInviteOneLink(oneLinkId: String) {
        lib.setAppInviteOneLink(oneLinkId)
    }

    override fun setPhoneNumber(phoneNumber: String?) {
        lib.setPhoneNumber(phoneNumber)
    }

    override fun performOnAppAttribution(url: String) {
        lib.performOnAppAttribution(appContext, java.net.URI(url))
    }

    override fun setIsUpdate(isUpdate: Boolean) {
        lib.setIsUpdate(isUpdate)
    }

    override fun setCollectIMEI(collect: Boolean) {
        lib.setCollectIMEI(collect)
    }

    override fun setCollectOaid(collect: Boolean) {
        lib.setCollectOaid(collect)
    }

    override fun setImeiData(imei: String?) {
        lib.setImeiData(imei)
    }

    override fun setOaidData(oaid: String?) {
        lib.setOaidData(oaid)
    }

    override fun setAndroidIdData(androidId: String?) {
        lib.setAndroidIdData(androidId)
    }

    override fun disableAppSetId() {
        lib.disableAppSetId()
    }

    override fun setDisableNetworkData(disable: Boolean) {
        lib.setDisableNetworkData(disable)
    }

    override fun waitForCustomerUserId(wait: Boolean) {
        lib.waitForCustomerUserId(wait)
    }

    override fun setPreinstallAttribution(mediaSource: String, campaign: String, siteId: String) {
        lib.setPreinstallAttribution(mediaSource, campaign, siteId)
    }

    override fun setOutOfStore(source: String) {
        lib.setOutOfStore(source)
    }

    override fun setDisableIDFVCollection(disable: Boolean) {
        // iOS only — no-op on Android.
    }

    override fun setDisableCollectASA(disable: Boolean) {
        // iOS only — no-op on Android.
    }

    override fun setDisableAppleAdsAttribution(disable: Boolean) {
        // iOS only — no-op on Android.
    }

    override fun setShouldCollectDeviceName(collect: Boolean) {
        // iOS only — no-op on Android.
    }

    override fun setUseReceiptValidationSandbox(enable: Boolean) {
        // iOS only — no-op on Android.
    }

    override fun setUseUninstallSandbox(enable: Boolean) {
        // iOS only — no-op on Android.
    }

    override fun setCurrentDeviceLanguage(language: String?) {
        // iOS only — no-op on Android.
    }

    override fun setDeepLinkTimeout(seconds: Int) {
        deepLinkTimeoutMs = seconds * 1000L
        subscribeForDeepLinks()
    }

    override fun remoteDebuggingCall(data: String) {
        // iOS only — no-op on Android.
    }

    override fun isPreInstalledApp(): Boolean =
        lib.isPreInstalledApp(appContext)

    override fun getAttributionId(): String? =
        lib.getAttributionId(appContext)

    override fun getOutOfStore(): String =
        lib.getOutOfStore(appContext)

    override fun logSession() {
        lib.logSession(appContext)
    }

    override fun onPause() {
        lib.onPause(appContext)
    }

    override fun setCustomerIdAndLogSession(customerUserId: String) {
        lib.setCustomerIdAndLogSession(customerUserId, appContext)
    }

    override fun setLogLevel(level: AfLogLevel) {
        lib.setLogLevel(level.toAndroidLogLevel())
    }

    override fun waitForATTUserAuthorization(timeoutInterval: Double) {
        // iOS only — no-op on Android.
    }

    override fun getAdvertisingIdentifier(): String? = null

    override fun logCrossPromoteImpression(
        appId: String,
        campaign: String,
        parameters: Map<String, String>,
    ) {
        com.appsflyer.share.CrossPromotionHelper.logCrossPromoteImpression(
            appContext,
            appId,
            campaign,
            parameters,
        )
    }

    override fun logAndOpenStore(
        appId: String,
        campaign: String,
        parameters: Map<String, String>,
    ) {
        com.appsflyer.share.CrossPromotionHelper.logAndOpenStore(
            appContext,
            appId,
            campaign,
            parameters,
        )
    }

    override fun logInvite(
        channel: String,
        parameters: Map<String, String>,
    ) {
        com.appsflyer.share.ShareInviteHelper.logInvite(appContext, channel, parameters)
    }

    override fun generateInviteUrl(
        params: InviteLinkParams,
        onResult: (String?) -> Unit,
    ) {
        val generator = com.appsflyer.share.ShareInviteHelper.generateInviteUrl(appContext)
        params.channel?.let { generator.setChannel(it) }
        params.campaign?.let { generator.setCampaign(it) }
        params.referrerCustomerId?.let { generator.setReferrerCustomerId(it) }
        params.referrerUID?.let { generator.setReferrerUID(it) }
        params.referrerName?.let { generator.setReferrerName(it) }
        params.referrerImageURL?.let { generator.setReferrerImageURL(it) }
        params.brandDomain?.let { generator.setBrandDomain(it) }
        params.baseDeeplink?.let { generator.setBaseDeeplink(it) }
        params.deeplinkPath?.let { generator.setDeeplinkPath(it) }
        if (params.customParameters.isNotEmpty()) {
            generator.addParameters(params.customParameters)
        }
        generator.generateLink(
            appContext,
            object : com.appsflyer.CreateOneLinkHttpTask.ResponseListener {
                override fun onResponse(link: String?) = onResult(link)

                override fun onResponseError(errorName: String?) = onResult(null)
            },
        )
    }

    override fun enableFacebookDeferredApplinks(enable: Boolean) {
        lib.enableFacebookDeferredApplinks(enable)
    }

    override fun setPluginInfo(
        plugin: String,
        version: String,
        additionalParameters: Map<String, String>,
    ) {
        val pluginEnum = when (plugin.lowercase()) {
            "unity" -> com.appsflyer.internal.platform_extension.Plugin.UNITY
            "reactnative" -> com.appsflyer.internal.platform_extension.Plugin.REACT_NATIVE
            "flutter" -> com.appsflyer.internal.platform_extension.Plugin.FLUTTER
            "cordova" -> com.appsflyer.internal.platform_extension.Plugin.CORDOVA
            "expo" -> com.appsflyer.internal.platform_extension.Plugin.EXPO
            "unreal" -> com.appsflyer.internal.platform_extension.Plugin.UNREAL
            "xamarin" -> com.appsflyer.internal.platform_extension.Plugin.XAMARIN
            "capacitor" -> com.appsflyer.internal.platform_extension.Plugin.CAPACITOR
            "segment" -> com.appsflyer.internal.platform_extension.Plugin.SEGMENT
            else -> com.appsflyer.internal.platform_extension.Plugin.NATIVE
        }
        val pluginInfo = com.appsflyer.internal.platform_extension.PluginInfo(
            pluginEnum,
            version,
            additionalParameters,
        )
        lib.setPluginInfo(pluginInfo)
    }

    override fun setSharingFilterForAllPartners() {
        lib.setSharingFilterForAllPartners()
    }

    override fun setExtension(extension: String) {
        lib.setExtension(extension)
    }

    override fun setInstallId(installId: String) {
        lib.setInstallId(installId)
    }

    override fun isSessionReady(): Boolean = false

    override fun handlePushNotification(payload: Map<String, Any?>) {
        // iOS only — no-op on Android.
    }

    override fun unregisterSessionReadyListener() {
        // iOS only — no-op on Android.
    }

    override fun setAnonymizeUser(enabled: Boolean) {
        lib.anonymizeUser(enabled)
    }

    override fun setSharingFilterPartners(partners: Set<String>) {
        lib.setSharingFilterForPartners(*partners.toTypedArray())
    }

    override fun logAdRevenue(data: AdRevenueData) {
        val adRevenueData = AFAdRevenueData(
            data.monetizationNetwork,
            data.mediationNetwork.toAndroidMediationNetwork(),
            data.currency,
            data.revenue,
        )
        lib.logAdRevenue(adRevenueData, data.additionalParameters)
    }

    override fun validateAndLogInAppPurchase(
        purchaseDetails: PurchaseDetails,
        additionalParameters: Map<String, Any?>,
        onResult: (PurchaseValidationResult) -> Unit,
    ) {
        val details = AFPurchaseDetails(
            purchaseDetails.purchaseType.toAndroidPurchaseType(),
            purchaseDetails.transactionId,
            purchaseDetails.productId,
        )
        val stringParams = additionalParameters
            .mapValues { it.value.toString() }
        lib.validateAndLogInAppPurchase(
            details,
            stringParams,
            object : AppsFlyerInAppPurchaseValidationCallback {
                override fun onInAppPurchaseValidationFinished(validationResult: Map<String, Any?>) {
                    onResult(PurchaseValidationResult.Success(validationResult))
                }

                override fun onInAppPurchaseValidationError(validationError: Map<String, Any>) {
                    onResult(PurchaseValidationResult.Error(message = validationError.toString()))
                }
            },
        )
    }

    override fun stop(stop: Boolean) =
        lib.stop(stop, appContext)

    override fun isStopped(): Boolean =
        lib.isStopped

    private fun toDeepLinkResult(result: AfDeepLinkResult): DeepLinkResult {
        return when (result.status) {
            Status.FOUND -> {
                val deepLink: DeepLink = result.deepLink
                mapDeepLinkResult(
                    status = DeepLinkStatus.FOUND,
                    deepLinkValue = deepLink.deepLinkValue,
                    isDeferred = deepLink.isDeferred,
                    mediaSource = deepLink.mediaSource,
                    campaign = deepLink.campaign,
                    raw = deepLink.clickEvent.toDeepMap(),
                )
            }
            Status.NOT_FOUND -> mapDeepLinkResult(DeepLinkStatus.NOT_FOUND)
            else -> mapDeepLinkResult(
                status = DeepLinkStatus.ERROR,
                error = result.error?.toString(),
            )
        }
    }
}

internal actual class AppsFlyerClientFactory(
    private val context: Context,
) {
    actual fun create(config: AppsFlyerConfig): AppsFlyerClient {
        return AppsFlyerClientImpl(
            sdk = AndroidAppsFlyerSdk(context),
            config = config,
        )
    }
}

internal fun AfEmailCryptType.toAndroidCryptType(): AppsFlyerProperties.EmailsCryptType = when (this) {
    AfEmailCryptType.NONE -> AppsFlyerProperties.EmailsCryptType.NONE
    AfEmailCryptType.SHA256 -> AppsFlyerProperties.EmailsCryptType.SHA256
}

internal fun AfMediationNetwork.toAndroidMediationNetwork(): MediationNetwork = when (this) {
    AfMediationNetwork.GOOGLE_ADMOB -> MediationNetwork.GOOGLE_ADMOB
    AfMediationNetwork.IRON_SOURCE -> MediationNetwork.IRONSOURCE
    AfMediationNetwork.APP_LOVIN_MAX -> MediationNetwork.APPLOVIN_MAX
    AfMediationNetwork.FYBER -> MediationNetwork.FYBER
    AfMediationNetwork.APPODEAL -> MediationNetwork.APPODEAL
    AfMediationNetwork.ADMOST -> MediationNetwork.ADMOST
    AfMediationNetwork.TOPON -> MediationNetwork.TOPON
    AfMediationNetwork.TRADPLUS -> MediationNetwork.TRADPLUS
    AfMediationNetwork.YANDEX -> MediationNetwork.YANDEX
    AfMediationNetwork.CHARTBOOST -> MediationNetwork.CHARTBOOST
    AfMediationNetwork.UNITY -> MediationNetwork.UNITY
    AfMediationNetwork.TOPON_PTE -> MediationNetwork.TOPON_PTE
    AfMediationNetwork.CUSTOM_MEDIATION -> MediationNetwork.CUSTOM_MEDIATION
    AfMediationNetwork.DIRECT_MONETIZATION -> MediationNetwork.DIRECT_MONETIZATION_NETWORK
}

internal fun AfPurchaseType.toAndroidPurchaseType(): AFPurchaseType = when (this) {
    AfPurchaseType.SUBSCRIPTION -> AFPurchaseType.SUBSCRIPTION
    AfPurchaseType.ONE_TIME_PURCHASE -> AFPurchaseType.ONE_TIME_PURCHASE
}

internal fun AfLogLevel.toAndroidLogLevel(): AFLogger.LogLevel = when (this) {
    AfLogLevel.NONE -> AFLogger.LogLevel.NONE
    AfLogLevel.ERROR -> AFLogger.LogLevel.ERROR
    AfLogLevel.WARN -> AFLogger.LogLevel.WARNING
    AfLogLevel.INFO -> AFLogger.LogLevel.INFO
    AfLogLevel.DEBUG -> AFLogger.LogLevel.DEBUG
    AfLogLevel.VERBOSE -> AFLogger.LogLevel.VERBOSE
}
