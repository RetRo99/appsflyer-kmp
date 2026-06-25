@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
)

package com.retro99.appsflyer

import AppsFlyerBridge.AppsFlyerBridge
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSNumber
import platform.Foundation.create

internal class IosAppsFlyerSdk(
    private val launchOptions: Map<Any?, *>? = null,
) : AppsFlyerSdk {

    internal val bridge = AppsFlyerBridge()

    override fun configure(
        config: AppsFlyerConfig,
        onConversion: (Map<String, Any?>) -> Unit,
        onConversionError: (String) -> Unit,
        onDeepLink: (DeepLinkResult) -> Unit,
        onStart: (StartResult) -> Unit,
    ) {
        requireNotNull(config.iosAppId) {
            "AppsFlyerConfig.iosAppId must be set on iOS."
        }
        val consentDict: Map<Any?, *>? = config.consentData?.let { consent ->
            mapOf(
                "isUserSubjectToGDPR" to consent.isUserSubjectToGDPR?.let { NSNumber(it) },
                "hasConsentForDataUsage" to consent.hasConsentForDataUsage?.let { NSNumber(it) },
                "hasConsentForAdsPersonalization" to consent.hasConsentForAdsPersonalization?.let { NSNumber(it) },
                "hasConsentForAdStorage" to consent.hasConsentForAdStorage?.let { NSNumber(it) },
            )
        }
        bridge.configureWithDevKey(
            devKey = config.devKey,
            appId = config.iosAppId,
            isDebug = config.isDebug,
            anonymizeUser = config.anonymizeUser,
            enableTCFDataCollection = config.enableTCFDataCollection,
            consentData = consentDict,
            sharingFilterPartners = config.sharingFilterPartners.toList(),
            launchOptions = launchOptions,
            logLevelOrdinal = config.logLevel?.let { NSNumber(it.ordinal) },
            deepLinkTimeoutSeconds = config.deepLinkTimeoutMs?.let { ms ->
                NSNumber(((ms + 999L) / 1000L).toInt())
            },
            onConversion = { data ->
                if (data != null) {
                    @Suppress("UNCHECKED_CAST")
                    onConversion(data as Map<String, Any?>)
                } else {
                    onConversionError("Conversion data was null")
                }
            },
            onConversionError = { message -> onConversionError(message ?: "Unknown error") },
            onDeepLinkFound = { deepLinkValue, isDeferred, mediaSource, campaign, raw ->
                @Suppress("UNCHECKED_CAST")
                val rawMap = raw as? Map<String, Any?> ?: emptyMap()
                onDeepLink(
                    mapDeepLinkResult(
                        status = DeepLinkStatus.FOUND,
                        deepLinkValue = deepLinkValue,
                        isDeferred = isDeferred,
                        mediaSource = mediaSource,
                        campaign = campaign,
                        raw = rawMap,
                    ),
                )
            },
            onDeepLinkNotFound = {
                onDeepLink(mapDeepLinkResult(DeepLinkStatus.NOT_FOUND))
            },
            onDeepLinkError = { message ->
                onDeepLink(mapDeepLinkResult(DeepLinkStatus.ERROR, error = message))
            },
            onStart = { success, code, message ->
                onStart(
                    if (success) {
                        StartResult.Success
                    } else {
                        StartResult.Error(code.toInt(), message.orEmpty())
                    },
                )
            },
        )
    }

    override fun setCustomerUserId(id: String?) {
        bridge.setCustomerUserId(id)
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        bridge.logEvent(name, params as Map<Any?, *>)
    }

    override fun logEventForResult(
        name: String,
        params: Map<String, Any?>,
        onResult: (LogEventResult) -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        bridge.logEventForResult(name, params as Map<Any?, *>) { success, code, message ->
            onResult(
                if (success) {
                    LogEventResult.Success
                } else {
                    LogEventResult.Error(code.toInt(), message.orEmpty())
                },
            )
        }
    }

    override fun getAppsFlyerUID(): String? =
        bridge.getAppsFlyerUID()

    override fun getSdkVersion(): String =
        bridge.getSdkVersion()

    override fun setCurrencyCode(currency: String) {
        bridge.setCurrencyCode(currency)
    }

    override fun logLocation(latitude: Double, longitude: Double) {
        bridge.logLocation(longitude, latitude)
    }

    override fun setAdditionalData(data: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        bridge.setCustomData(data as Map<Any?, *>?)
    }

    override fun setMinTimeBetweenSessions(seconds: Int) {
        bridge.setMinTimeBetweenSessions(seconds.toLong())
    }

    override fun setDisableAdvertisingIdentifier(disable: Boolean) {
        bridge.setDisableAdvertisingIdentifier(disable)
    }

    override fun setDisableSKAdNetwork(disable: Boolean) {
        bridge.setDisableSKAdNetwork(disable)
    }

    override fun setUserEmails(emails: List<String>, cryptType: AfEmailCryptType) {
        bridge.setUserEmails(emails, cryptType.iosRawValue)
    }

    override fun registerUninstall(token: String) {
        bridge.registerUninstall(token.hexToNSData())
    }

    override fun setOneLinkCustomDomain(domains: List<String>) {
        bridge.setOneLinkCustomDomains(domains)
    }

    override fun appendParametersToDeepLinkingURL(contains: String, parameters: Map<String, String>) {
        @Suppress("UNCHECKED_CAST")
        bridge.appendParametersToDeepLinkingURL(contains, parameters as Map<Any?, *>)
    }

    override fun setPartnerData(partnerId: String, data: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        bridge.setPartnerData(partnerId, data as Map<Any?, *>)
    }

    override fun addPushNotificationDeepLinkPath(keys: List<String>) {
        bridge.addPushNotificationDeepLinkPath(keys)
    }

    override fun setResolveDeepLinkURLs(urls: List<String>) {
        bridge.setResolveDeepLinkURLs(urls)
    }

    override fun setHost(hostPrefix: String, hostName: String) {
        bridge.setHost(hostPrefix, hostName)
    }

    override fun getHostName(): String = bridge.getHostName()

    override fun getHostPrefix(): String = bridge.getHostPrefix()

    override fun setAppInviteOneLink(oneLinkId: String) {
        bridge.setAppInviteOneLink(oneLinkId)
    }

    override fun setPhoneNumber(phoneNumber: String?) {
        bridge.setPhoneNumber(phoneNumber)
    }

    override fun performOnAppAttribution(url: String) {
        bridge.performOnAppAttribution(url)
    }

    override fun setIsUpdate(isUpdate: Boolean) {
        // Android only — no-op on iOS.
    }

    override fun setCollectIMEI(collect: Boolean) {
        // Android only — no-op on iOS.
    }

    override fun setCollectOaid(collect: Boolean) {
        // Android only — no-op on iOS.
    }

    override fun setImeiData(imei: String?) {
        // Android only — no-op on iOS.
    }

    override fun setOaidData(oaid: String?) {
        // Android only — no-op on iOS.
    }

    override fun setAndroidIdData(androidId: String?) {
        // Android only — no-op on iOS.
    }

    override fun disableAppSetId() {
        // Android only — no-op on iOS.
    }

    override fun setDisableNetworkData(disable: Boolean) {
        // Android only — no-op on iOS.
    }

    override fun waitForCustomerUserId(wait: Boolean) {
        // Android only — no-op on iOS.
    }

    override fun setPreinstallAttribution(mediaSource: String, campaign: String, siteId: String) {
        // Android only — no-op on iOS.
    }

    override fun setOutOfStore(source: String) {
        // Android only — no-op on iOS.
    }

    override fun setDisableIDFVCollection(disable: Boolean) {
        bridge.setDisableIDFVCollection(disable)
    }

    override fun setDisableCollectASA(disable: Boolean) {
        bridge.setDisableCollectASA(disable)
    }

    override fun setDisableAppleAdsAttribution(disable: Boolean) {
        bridge.setDisableAppleAdsAttribution(disable)
    }

    override fun setShouldCollectDeviceName(collect: Boolean) {
        bridge.setShouldCollectDeviceName(collect)
    }

    override fun setUseReceiptValidationSandbox(enable: Boolean) {
        bridge.setUseReceiptValidationSandbox(enable)
    }

    override fun setUseUninstallSandbox(enable: Boolean) {
        bridge.setUseUninstallSandbox(enable)
    }

    override fun setCurrentDeviceLanguage(language: String?) {
        bridge.setCurrentDeviceLanguage(language)
    }

    override fun setDeepLinkTimeout(seconds: Int) {
        bridge.setDeepLinkTimeout(seconds.toLong())
    }

    override fun remoteDebuggingCall(data: String) {
        bridge.remoteDebuggingCall(data)
    }

    override fun isPreInstalledApp(): Boolean = false

    override fun getAttributionId(): String? = null

    override fun getOutOfStore(): String = ""

    override fun logSession() {
        // Android only — no-op on iOS.
    }

    override fun onPause() {
        // Android only — no-op on iOS.
    }

    override fun setCustomerIdAndLogSession(customerUserId: String) {
        bridge.setCustomerIdAndLogSession(customerUserId)
    }

    override fun setLogLevel(level: AfLogLevel) {
        bridge.setLogLevel(level.ordinal.toLong())
    }

    override fun waitForATTUserAuthorization(timeoutInterval: Double) {
        bridge.waitForATTUserAuthorization(timeoutInterval)
    }

    override fun getAdvertisingIdentifier(): String? =
        bridge.getAdvertisingIdentifier()

    override fun logCrossPromoteImpression(
        appId: String,
        campaign: String,
        parameters: Map<String, String>,
    ) {
        @Suppress("UNCHECKED_CAST")
        bridge.logCrossPromoteImpression(appId, campaign, parameters as Map<Any?, *>?)
    }

    override fun logAndOpenStore(
        appId: String,
        campaign: String,
        parameters: Map<String, String>,
    ) {
        @Suppress("UNCHECKED_CAST")
        bridge.logAndOpenStore(appId, campaign, parameters as Map<Any?, *>?)
    }

    override fun logInvite(
        channel: String,
        parameters: Map<String, String>,
    ) {
        @Suppress("UNCHECKED_CAST")
        bridge.logInvite(channel, parameters as Map<Any?, *>?)
    }

    override fun generateInviteUrl(
        params: InviteLinkParams,
        onResult: (String?) -> Unit,
    ) {
        bridge.generateInviteUrl(
            channel = params.channel,
            campaign = params.campaign,
            referrerCustomerId = params.referrerCustomerId,
            referrerUID = params.referrerUID,
            referrerName = params.referrerName,
            referrerImageURL = params.referrerImageURL,
            brandDomain = params.brandDomain,
            baseDeeplink = params.baseDeeplink,
            deeplinkPath = params.deeplinkPath,
            customParameters = params.customParameters as Map<Any?, *>?,
        ) { url: String? -> onResult(url) }
    }

    override fun enableFacebookDeferredApplinks(enable: Boolean) {
        // iOS requires FBSDKAppLinkUtility class; not available through KMP.
    }

    override fun setPluginInfo(
        plugin: String,
        version: String,
        additionalParameters: Map<String, String>,
    ) {
        @Suppress("UNCHECKED_CAST")
        bridge.setPluginInfo(plugin, version, additionalParameters as Map<Any?, *>?)
    }

    override fun setSharingFilterForAllPartners() {
        bridge.setSharingFilterForAllPartners()
    }

    override fun setExtension(extension: String) {
        // Android only — no-op on iOS.
    }

    override fun setInstallId(installId: String) {
        bridge.setInstallId(installId)
    }

    override fun isSessionReady(): Boolean =
        bridge.isSessionReady()

    override fun handlePushNotification(payload: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        bridge.handlePushNotification(payload as Map<Any?, *>?)
    }

    override fun unregisterSessionReadyListener() {
        bridge.unregisterSessionReadyListener()
    }

    override fun setAnonymizeUser(enabled: Boolean) {
        bridge.setAnonymizeUser(enabled)
    }

    override fun setSharingFilterPartners(partners: Set<String>) {
        bridge.setSharingFilterPartners(partners.toList())
    }

    override fun logAdRevenue(data: AdRevenueData) {
        @Suppress("UNCHECKED_CAST")
        bridge.logAdRevenue(
            monetizationNetwork = data.monetizationNetwork,
            mediationNetwork = data.mediationNetwork.iosRawValue,
            currency = data.currency,
            revenue = data.revenue,
            additionalParameters = data.additionalParameters as Map<Any?, *>?,
        )
    }

    override fun validateAndLogInAppPurchase(
        purchaseDetails: PurchaseDetails,
        additionalParameters: Map<String, Any?>,
        onResult: (PurchaseValidationResult) -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        bridge.validateAndLogInAppPurchase(
            productId = purchaseDetails.productId,
            transactionId = purchaseDetails.transactionId,
            purchaseType = purchaseDetails.purchaseType.iosRawValue,
            additionalParameters = additionalParameters as Map<Any?, *>?,
        ) { response, error ->
            when {
                error != null ->
                    onResult(PurchaseValidationResult.Error(message = error))
                response != null -> {
                    @Suppress("UNCHECKED_CAST")
                    onResult(PurchaseValidationResult.Success(result = response as Map<String, Any?>))
                }
                else ->
                    onResult(PurchaseValidationResult.Error(message = null))
            }
        }
    }

    override fun stop(stop: Boolean) {
        bridge.stop(stop)
    }

    override fun isStopped(): Boolean =
        bridge.isStopped()
}

internal actual class AppsFlyerClientFactory(
    private val launchOptions: Map<Any?, *>? = null,
) {
    actual fun create(config: AppsFlyerConfig): AppsFlyerClient {
        return AppsFlyerClientImpl(
            sdk = IosAppsFlyerSdk(launchOptions),
            config = config,
        )
    }
}

internal fun String.hexToNSData(): NSData {
    val bytes = chunked(2).map { byteStr ->
        byteStr.toInt(16).toByte()
    }.toByteArray()
    return bytes.usePinned { pin ->
        NSData.create(bytes = pin.addressOf(0), length = bytes.size.toULong())
    }
}
