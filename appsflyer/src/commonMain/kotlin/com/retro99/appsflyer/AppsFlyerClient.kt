package com.retro99.appsflyer

import kotlinx.coroutines.flow.Flow

/**
 * Kotlin Multiplatform wrapper for the AppsFlyer SDK.
 *
 * Call [start] to initialize the SDK, then use the suspend accessors and
 * [deepLink] flow to observe results. All suspend functions are idempotent —
 * calling them multiple times returns the same cached result.
 */
interface AppsFlyerClient {
    /**
     * Initializes and starts the AppsFlyer SDK. Must be called before any
     * other method. Safe to call from any thread; does not block.
     */
    fun start()

    /**
     * Suspends until the SDK reports whether it started successfully.
     * Must be called after [start]; suspends indefinitely if [start] was not called.
     * Idempotent — subsequent calls return the cached result immediately.
     */
    suspend fun getStartResult(): StartResult

    /**
     * Suspends until install conversion data is available.
     * Must be called after [start]; suspends indefinitely if [start] was not called.
     * Idempotent — subsequent calls return the cached result immediately.
     */
    suspend fun getConversionData(): CampaignData

    /** Sets the customer user ID for attribution. Pass null to clear. */
    fun setCustomerUserId(id: String?)

    /** Logs an in-app event. Null values in [params] are silently dropped. */
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())

    /**
     * Logs an in-app event and suspends until the SDK confirms delivery.
     * Null values in [params] are silently dropped.
     */
    suspend fun logEventForResult(name: String, params: Map<String, Any?> = emptyMap()): LogEventResult

    /**
     * Logs ad revenue to AppsFlyer. Fire-and-forget; the SDK sends the data
     * asynchronously. Null values in [AdRevenueData.additionalParameters] are
     * silently dropped.
     */
    fun logAdRevenue(data: AdRevenueData)

    /**
     * Enables or disables user anonymization at runtime. When enabled, the SDK
     * zeroes out the device ID before sending it to AppsFlyer. The initial
     * value is set via [AppsFlyerConfig.anonymizeUser].
     */
    fun setAnonymizeUser(enabled: Boolean)

    /**
     * Sets the sharing filter for partners at runtime. The specified
     * partners will be excluded from data sharing; all others continue
     * to receive data as normal. The initial value is set via
     * [AppsFlyerConfig.sharingFilterPartners].
     */
    fun setSharingFilterPartners(partners: Set<String>)

    /** Sets the currency code for revenue events (ISO 4217, e.g. "USD"). */
    fun setCurrencyCode(currency: String)

    /**
     * Logs a location to AppsFlyer for geo-based attribution.
     *
     * @param latitude the latitude in decimal degrees.
     * @param longitude the longitude in decimal degrees.
     */
    fun logLocation(latitude: Double, longitude: Double)

    /** Sets additional custom data to be included in raw data reports. */
    fun setAdditionalData(data: Map<String, Any?>)

    /** Sets the minimum time between sessions in seconds. */
    fun setMinTimeBetweenSessions(seconds: Int)

    /**
     * Disables collection of the advertising identifier (IDFA on iOS,
     * advertising ID on Android).
     */
    fun setDisableAdvertisingIdentifier(disable: Boolean)

    /**
     * Disables SKAdNetwork measurement (iOS only). No-op on Android.
     */
    fun setDisableSKAdNetwork(disable: Boolean)

    /** Sets user emails for attribution with a hashing type. */
    fun setUserEmails(emails: List<String>, cryptType: AfEmailCryptType)

    /**
     * Registers the push notification device token for uninstall measurement.
     * Call this when you receive the token from FCM (Android) or APNs (iOS).
     *
     * On iOS, the [token] should be the hex-encoded string representation of
     * the `Data` received in `didRegisterForRemoteNotificationsWithDeviceToken`.
     * On Android, pass the FCM token string directly.
     *
     * @param token the device token as a string.
     */
    fun registerUninstall(token: String)

    /**
     * Sets custom OneLink domains for deep linking. Call before [start].
     *
     * @param domains the custom domain strings (e.g. `listOf("mydomain.com")`).
     */
    fun setOneLinkCustomDomain(domains: List<String>)

    /**
     * Appends query parameters to deep linking URLs that match the given
     * [contains] substring. Call before [start].
     *
     * @param contains the substring to match in the URL.
     * @param parameters the key-value pairs to append.
     */
    fun appendParametersToDeepLinkingURL(contains: String, parameters: Map<String, String>)

    /**
     * Sets partner-specific data to be sent to AppsFlyer. Null values in
     * [data] are silently dropped.
     *
     * @param partnerId the partner identifier.
     * @param data the key-value pairs to send.
     */
    fun setPartnerData(partnerId: String, data: Map<String, Any?>)

    /**
     * Adds keys to compose the key path used to resolve a deep link from
     * a push notification payload.
     *
     * @param keys the key path components (e.g. `listOf("custom_key", "deep_link")`).
     */
    fun addPushNotificationDeepLinkPath(keys: List<String>)

    /**
     * Sets a list of URLs that the SDK should resolve before redirecting
     * to the final deep link destination. Call before [start].
     *
     * @param urls the URLs to resolve.
     */
    fun setResolveDeepLinkURLs(urls: List<String>)

    /**
     * Sets a custom host for SDK server communication. Call before [start].
     *
     * @param hostPrefix the host prefix.
     * @param hostName the host name.
     */
    fun setHost(hostPrefix: String, hostName: String)

    /** Returns the current host name. */
    fun getHostName(): String

    /** Returns the current host prefix. */
    fun getHostPrefix(): String

    /**
     * Sets the OneLink ID used for cross-promotion invite links.
     * Call before [start].
     *
     * @param oneLinkId the OneLink ID.
     */
    fun setAppInviteOneLink(oneLinkId: String)

    /**
     * Sets the user's phone number. It will be sent as a SHA256 hash.
     * Pass null to clear.
     *
     * @param phoneNumber the phone number, or null.
     */
    fun setPhoneNumber(phoneNumber: String?)

    /**
     * Triggers attribution for the given URL. Use when opening a URL
     * outside the normal deep link flow.
     *
     * @param url the URL to attribute.
     */
    fun performOnAppAttribution(url: String)

    /**
     * Marks this launch as an update (vs. a fresh install).
     * Android only; no-op on iOS.
     */
    fun setIsUpdate(isUpdate: Boolean)

    /**
     * Enables or disables IMEI collection (Android only; no-op on iOS).
     */
    fun setCollectIMEI(collect: Boolean)

    /**
     * Enables or disables OAID collection (Android only; no-op on iOS).
     */
    fun setCollectOaid(collect: Boolean)

    /**
     * Sets IMEI data manually (Android only; no-op on iOS).
     */
    fun setImeiData(imei: String?)

    /**
     * Sets OAID data manually (Android only; no-op on iOS).
     */
    fun setOaidData(oaid: String?)

    /**
     * Sets Android ID data manually (Android only; no-op on iOS).
     */
    fun setAndroidIdData(androidId: String?)

    /**
     * Disables app set ID collection (Android only; no-op on iOS).
     */
    fun disableAppSetId()

    /**
     * Disables network data collection (Android only; no-op on iOS).
     */
    fun setDisableNetworkData(disable: Boolean)

    /**
     * Whether to wait for customer user ID before starting. When enabled,
     * the SDK holds off on sending events until [setCustomerUserId] is called.
     * Android only; no-op on iOS.
     */
    fun waitForCustomerUserId(wait: Boolean)

    /**
     * Sets preinstall attribution data (Android only; no-op on iOS).
     *
     * @param mediaSource the media source.
     * @param campaign the campaign.
     * @param siteId the site ID.
     */
    fun setPreinstallAttribution(mediaSource: String, campaign: String, siteId: String)

    /**
     * Sets the out-of-store source (Android only; no-op on iOS).
     */
    fun setOutOfStore(source: String)

    /**
     * Disables IDFV collection (iOS only; no-op on Android).
     */
    fun setDisableIDFVCollection(disable: Boolean)

    /**
     * Disables Apple Search Ads collection (iOS only; no-op on Android).
     */
    fun setDisableCollectASA(disable: Boolean)

    /**
     * Disables Apple Ads attribution (iOS only; no-op on Android).
     */
    fun setDisableAppleAdsAttribution(disable: Boolean)

    /**
     * Sets whether to collect the device name (iOS only; no-op on Android).
     */
    fun setShouldCollectDeviceName(collect: Boolean)

    /**
     * Enables receipt validation sandbox (iOS only; no-op on Android).
     */
    fun setUseReceiptValidationSandbox(enable: Boolean)

    /**
     * Enables uninstall sandbox (iOS only; no-op on Android).
     */
    fun setUseUninstallSandbox(enable: Boolean)

    /**
     * Sets the current device language (iOS only; no-op on Android).
     */
    fun setCurrentDeviceLanguage(language: String?)

    /**
     * Sets the deep link timeout in seconds. When the timeout elapses
     * without a resolved deep link, the SDK reports a not-found result.
     * Takes effect immediately on both platforms.
     */
    fun setDeepLinkTimeout(seconds: Int)

    /**
     * Sends remote debugging data (iOS only; no-op on Android).
     */
    fun remoteDebuggingCall(data: String)

    /**
     * Whether the app was pre-installed on the device (Android only;
     * returns `false` on iOS).
     */
    fun isPreInstalledApp(): Boolean

    /**
     * Returns the Facebook attribution ID, or null if unavailable
     * (Android only; returns `null` on iOS).
     */
    fun getAttributionId(): String?

    /**
     * Returns the out-of-store source identifier (Android only;
     * returns an empty string on iOS).
     */
    fun getOutOfStore(): String

    /**
     * Logs a session manually. The SDK auto-tracks sessions by default;
     * call this only if you've disabled auto-tracking (Android only; no-op on iOS).
     */
    fun logSession()

    /**
     * Notifies the SDK that the app was paused. The SDK handles this
     * automatically via lifecycle callbacks; call this only for manual
     * integration (Android only; no-op on iOS).
     */
    fun onPause()

    /**
     * Sets the customer user ID and immediately logs a new session.
     * On iOS, this also calls `start()` to re-initialize the SDK session.
     */
    fun setCustomerIdAndLogSession(customerUserId: String)

    /**
     * Sets the SDK log level. On Android this maps to [AFLogger.LogLevel]
     * for fine-grained control. On iOS, the SDK only exposes a boolean
     * `isDebug` toggle, so any level other than [AfLogLevel.NONE] enables
     * full debug logging. Takes precedence over [AppsFlyerConfig.isDebug].
     */
    fun setLogLevel(level: AfLogLevel)

    /**
     * Delays SDK start until the ATT (App Tracking Transparency) prompt
     * resolves or the given timeout elapses (iOS only; no-op on Android).
     *
     * @param timeoutInterval the maximum wait time in seconds.
     */
    fun waitForATTUserAuthorization(timeoutInterval: Double)

    /**
     * Returns the advertising identifier (IDFA on iOS, advertising ID on
     * Android). Returns `null` if unavailable or not authorized.
     * On Android, always returns `null`; use [getAttributionId] for
     * Facebook attribution instead.
     */
    fun getAdvertisingIdentifier(): String?

    /**
     * Logs a cross-promotion impression for an app you own.
     *
     * @param appId the AppsFlyer app ID of the promoted app.
     * @param campaign the campaign name.
     * @param parameters optional key-value parameters.
     */
    fun logCrossPromoteImpression(
        appId: String,
        campaign: String,
        parameters: Map<String, String> = emptyMap(),
    )

    /**
     * Logs a cross-promotion impression and opens the store listing for
     * the promoted app. The SDK handles the store navigation natively.
     *
     * @param appId the AppsFlyer app ID of the promoted app.
     * @param campaign the campaign name.
     * @param parameters optional key-value parameters.
     */
    fun logAndOpenStore(
        appId: String,
        campaign: String,
        parameters: Map<String, String> = emptyMap(),
    )

    /**
     * Logs a "user invited a friend" in-app event.
     *
     * @param channel the invite channel (e.g. "gmail", "facebook").
     * @param parameters optional key-value parameters.
     */
    fun logInvite(
        channel: String,
        parameters: Map<String, String> = emptyMap(),
    )

    /**
     * Generates a OneLink user-invite URL from the given [params].
     * Suspends until the SDK returns the URL or fails.
     *
     * @return the generated URL, or `null` if generation failed.
     */
    suspend fun generateInviteUrl(params: InviteLinkParams = InviteLinkParams()): String?

    /**
     * Enables or disables collection of Facebook Deferred AppLinks.
     * Requires the Facebook SDK to be integrated.
     */
    fun enableFacebookDeferredApplinks(enable: Boolean)

    /**
     * Identifies the consuming wrapper/plugin to the AppsFlyer backend.
     * Useful when this SDK is consumed inside another framework (e.g.
     * React Native, Flutter, Capacitor).
     *
     * @param plugin the plugin/framework name (e.g. "reactnative").
     * @param version the plugin/framework version.
     * @param additionalParameters optional extra metadata.
     */
    fun setPluginInfo(
        plugin: String,
        version: String,
        additionalParameters: Map<String, String> = emptyMap(),
    )

    /**
     * Excludes all partners from data sharing. Use
     * [setSharingFilterPartners] to exclude specific partners only.
     */
    fun setSharingFilterForAllPartners()

    /**
     * Sets the extension name for server-side reporting
     * (Android only; no-op on iOS).
     */
    fun setExtension(extension: String)

    /**
     * Sets a custom install ID for server-side matching.
     */
    fun setInstallId(installId: String)

    /**
     * Whether the SDK session is ready (iOS only; returns `false` on Android).
     */
    fun isSessionReady(): Boolean

    /**
     * Handles push notification payload for attribution. Call with the
     * `userInfo` dictionary from `didReceiveRemoteNotification`
     * (iOS only; no-op on Android).
     *
     * @param payload the push notification payload.
     */
    fun handlePushNotification(payload: Map<String, Any?>)

    /**
     * Unregisters the session-ready listener (iOS only; no-op on Android).
     */
    fun unregisterSessionReadyListener()

    /**
     * Validates and logs an in-app purchase using the AppsFlyer VAL V2 flow.
     * Suspends until the SDK receives a response from the server.
     *
     * @param purchaseDetails the purchase details (product ID, transaction ID, type).
     * @param additionalParameters optional metadata associated with the purchase.
     * Null values are silently dropped.
     */
    suspend fun validateAndLogInAppPurchase(
        purchaseDetails: PurchaseDetails,
        additionalParameters: Map<String, Any?> = emptyMap(),
    ): PurchaseValidationResult

    /** Returns the AppsFlyer device ID, or null if the SDK hasn't started yet. */
    fun getAppsFlyerUID(): String?

    /** Returns the AppsFlyer SDK version string. */
    fun getSdkVersion(): String

    /**
     * Stops (or re-enables) the SDK. When stopped, the SDK will not collect
     * data or communicate with AppsFlyer servers. Pass `false` to re-enable.
     */
    fun stop(stop: Boolean = true)

    /** Whether the SDK is currently stopped. */
    val isStopped: Boolean

    /**
     * Emits deep link results as they arrive (including re-engagement links).
     * Does not replay past emissions — collect before calling [start] to
     * avoid missing the initial deep link.
     */
    val deepLink: Flow<DeepLinkResult>
}

data class AppsFlyerConfig(
    val devKey: String,
    val isDebug: Boolean = false,
    val iosAppId: String? = null,
    val collectAndroidId: Boolean = false,
    val anonymizeUser: Boolean = false,
    val enableTCFDataCollection: Boolean = false,
    val consentData: AppsFlyerConsent? = null,
    val sharingFilterPartners: Set<String> = emptySet(),
    val logLevel: AfLogLevel? = null,
    val deepLinkTimeoutMs: Long? = null,
) {
    init {
        require(devKey.isNotBlank()) { "AppsFlyerConfig.devKey must not be blank." }
    }
}

sealed interface CampaignData {
    data class Success(
        val status: AfStatus,
        val mediaSource: String?,
        val campaign: String?,
        val raw: Map<String, Any?>,
    ) : CampaignData

    data class Error(val message: String?) : CampaignData
}

enum class AfStatus { ORGANIC, NON_ORGANIC }

/**
 * Email hashing type for [AppsFlyerClient.setUserEmails]. Maps to
 * `EmailsCryptType` (Android) and `EmailCryptType` (iOS).
 */
enum class AfEmailCryptType(val iosRawValue: Long) {
    NONE(iosRawValue = 0L),
    SHA256(iosRawValue = 3L),
}

/**
 * Ad-network mediation type known to the AppsFlyer SDK. Maps to
 * `MediationNetwork` (Android) and `MediationNetworkType` (iOS).
 */
enum class AfMediationNetwork(val iosRawValue: Long) {
    GOOGLE_ADMOB(iosRawValue = 1L),
    IRON_SOURCE(iosRawValue = 2L),
    APP_LOVIN_MAX(iosRawValue = 3L),
    FYBER(iosRawValue = 4L),
    APPODEAL(iosRawValue = 5L),
    ADMOST(iosRawValue = 6L),
    TOPON(iosRawValue = 7L),
    TRADPLUS(iosRawValue = 8L),
    YANDEX(iosRawValue = 9L),
    CHARTBOOST(iosRawValue = 10L),
    UNITY(iosRawValue = 11L),
    TOPON_PTE(iosRawValue = 12L),
    CUSTOM_MEDIATION(iosRawValue = 13L),
    DIRECT_MONETIZATION(iosRawValue = 14L),
}

/**
 * Ad revenue data for [AppsFlyerClient.logAdRevenue]. Matches the AppsFlyer
 * SDK's `AFAdRevenueData` constructor on both platforms.
 */
data class AdRevenueData(
    val monetizationNetwork: String,
    val mediationNetwork: AfMediationNetwork,
    val currency: String,
    val revenue: Double,
    val additionalParameters: Map<String, Any?> = emptyMap(),
)

sealed interface DeepLinkResult {
    data class Found(
        val deepLinkValue: String?,
        val isDeferred: Boolean,
        val mediaSource: String?,
        val campaign: String?,
        val raw: Map<String, Any?>,
    ) : DeepLinkResult

    data object NotFound : DeepLinkResult

    data class Error(val message: String?) : DeepLinkResult
}

/**
 * AppsFlyer conversion-data keys and values. These are AppsFlyer's documented
 * `onConversionDataSuccess` payload contract (the SDK exposes no typed status),
 * stated once here so the Android and iOS actuals map identically.
 */
private object AfConversionKeys {
    const val STATUS = "af_status"
    const val MEDIA_SOURCE = "media_source"
    const val CAMPAIGN = "campaign"

    const val STATUS_ORGANIC = "Organic"
    const val STATUS_NON_ORGANIC = "Non-organic"
}

/**
 * Maps a raw AppsFlyer conversion-data map into [CampaignData]. Shared by both
 * platform actuals so Android and iOS produce identical results.
 */
internal fun Map<String, Any?>.toCampaignData(): CampaignData {
    val status = when (this[AfConversionKeys.STATUS]?.toString()) {
        AfConversionKeys.STATUS_ORGANIC -> AfStatus.ORGANIC
        AfConversionKeys.STATUS_NON_ORGANIC -> AfStatus.NON_ORGANIC
        else -> return CampaignData.Error(
            message = "Unexpected af_status: ${this[AfConversionKeys.STATUS]}",
        )
    }
    return CampaignData.Success(
        status = status,
        mediaSource = this[AfConversionKeys.MEDIA_SOURCE]?.toString(),
        campaign = this[AfConversionKeys.CAMPAIGN]?.toString(),
        raw = this,
    )
}

sealed interface StartResult {
    data object Success : StartResult
    data class Error(val code: Int, val message: String) : StartResult
}

sealed interface LogEventResult {
    data object Success : LogEventResult
    data class Error(val code: Int, val message: String) : LogEventResult
}

/**
 * Purchase type for [PurchaseDetails]. Maps to `AFPurchaseType` (Android)
 * and `AFSDKPurchaseType` (iOS).
 */
enum class AfPurchaseType(val iosRawValue: Long) {
    SUBSCRIPTION(iosRawValue = 0L),
    ONE_TIME_PURCHASE(iosRawValue = 1L),
}

/**
 * Log level for [AppsFlyerClient.setLogLevel] and [AppsFlyerConfig.logLevel].
 * Maps to `AFLogger.LogLevel` (Android) and the `isDebug` boolean toggle (iOS).
 * On iOS only [NONE] and non-[NONE] are distinguishable; all other levels
 * enable full debug logging.
 */
enum class AfLogLevel {
    NONE,
    ERROR,
    WARN,
    INFO,
    DEBUG,
    VERBOSE,
}

/**
 * Parameters for [AppsFlyerClient.generateInviteUrl]. Mirrors the official
 * `LinkGenerator` builder API. All fields are optional; only set ones are
 * forwarded to the SDK.
 */
data class InviteLinkParams(
    val channel: String? = null,
    val campaign: String? = null,
    val referrerCustomerId: String? = null,
    val referrerUID: String? = null,
    val referrerName: String? = null,
    val referrerImageURL: String? = null,
    val brandDomain: String? = null,
    val baseDeeplink: String? = null,
    /**
     * Deep link path appended to the base link.
     *
     * Supported on Android only — the iOS `AppsFlyerLinkGenerator` does not
     * expose a `setDeeplinkPath` method. The value is silently ignored on iOS.
     */
    val deeplinkPath: String? = null,
    val customParameters: Map<String, String> = emptyMap(),
)

/**
 * Purchase details for [AppsFlyerClient.validateAndLogInAppPurchase].
 *
 * @param productId the product identifier from the store.
 * @param transactionId the transaction/purchase token from the store.
 * @param purchaseType whether this is a subscription or one-time purchase.
 */
data class PurchaseDetails(
    val productId: String,
    val transactionId: String,
    val purchaseType: AfPurchaseType,
)

/**
 * Result of [AppsFlyerClient.validateAndLogInAppPurchase].
 */
sealed interface PurchaseValidationResult {
    data class Success(val result: Map<String, Any?>) : PurchaseValidationResult
    data class Error(val message: String?) : PurchaseValidationResult
}

/**
 * GDPR/DMA consent data for AppsFlyer. All fields are nullable to represent
 * tri-state consent (granted / denied / unknown). Set via
 * [AppsFlyerConfig.consentData] before [AppsFlyerClient.start].
 */
data class AppsFlyerConsent(
    val isUserSubjectToGDPR: Boolean? = null,
    val hasConsentForDataUsage: Boolean? = null,
    val hasConsentForAdsPersonalization: Boolean? = null,
    val hasConsentForAdStorage: Boolean? = null,
) {
    companion object {
        fun forNonGDPRUser() = AppsFlyerConsent(isUserSubjectToGDPR = false)

        fun forGDPRUser(
            hasConsentForDataUsage: Boolean,
            hasConsentForAdsPersonalization: Boolean,
        ) = AppsFlyerConsent(
            isUserSubjectToGDPR = true,
            hasConsentForDataUsage = hasConsentForDataUsage,
            hasConsentForAdsPersonalization = hasConsentForAdsPersonalization,
        )
    }
}

internal expect class AppsFlyerClientFactory {
    fun create(config: AppsFlyerConfig): AppsFlyerClient
}
