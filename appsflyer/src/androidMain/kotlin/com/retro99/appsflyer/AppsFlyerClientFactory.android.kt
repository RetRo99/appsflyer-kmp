package com.retro99.appsflyer

import android.content.Context
import com.appsflyer.AFAdRevenueData
import com.appsflyer.AFPurchaseDetails
import com.appsflyer.AFPurchaseType
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerInAppPurchaseValidationCallback
import com.appsflyer.AppsFlyerLib
import com.appsflyer.AppsFlyerProperties
import com.appsflyer.MediationNetwork
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.appsflyer.deeplink.DeepLink
import com.appsflyer.deeplink.DeepLinkResult as AfDeepLinkResult
import com.appsflyer.deeplink.DeepLinkResult.Status
import java.lang.ref.WeakReference
import org.json.JSONArray
import org.json.JSONObject

internal class AndroidAppsFlyerSdk(
    context: Context,
) : AppsFlyerSdk {

    private val appContext: Context = context.applicationContext
    private var startContextRef: WeakReference<Context>? = WeakReference(context)

    private val lib: AppsFlyerLib = AppsFlyerLib.getInstance()

    override fun configure(
        config: AppsFlyerConfig,
        onConversion: (Map<String, Any?>) -> Unit,
        onConversionError: (String) -> Unit,
        onDeepLink: (DeepLinkResult) -> Unit,
        onStart: (StartResult) -> Unit,
    ) {
        val ctx = startContextRef?.get() ?: appContext
        startContextRef = null
        lib.setDebugLog(config.isDebug)
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
        lib.subscribeForDeepLink { result -> onDeepLink(toDeepLinkResult(result)) }
        lib.start(
            ctx,
            config.devKey,
            object : AppsFlyerRequestListener {
                override fun onSuccess() = onStart(StartResult.Success)

                override fun onError(code: Int, message: String) = onStart(StartResult.Error(code, message))
            },
        )
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
        // iOS only — no-op on Android.
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
                    raw = deepLink.clickEvent.toMap(),
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

internal fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    keys().forEach { key -> map[key] = opt(key).unwrap() }
    return map
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

private fun Any?.unwrap(): Any? = when (this) {
    is JSONObject -> toMap()
    is JSONArray -> (0 until length()).map { i -> opt(i).unwrap() }
    JSONObject.NULL -> null
    else -> this
}
