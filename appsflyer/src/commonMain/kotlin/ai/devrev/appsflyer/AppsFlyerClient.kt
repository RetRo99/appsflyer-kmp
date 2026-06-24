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
 * Ad-network mediation type known to the AppsFlyer SDK. Maps to
 * `MediationNetwork` (Android) and `MediationNetworkType` (iOS).
 */
enum class AfMediationNetwork {
    GOOGLE_ADMOB,
    IRON_SOURCE,
    APP_LOVIN_MAX,
    FYBER,
    APPODEAL,
    ADMOST,
    TOPON,
    TRADPLUS,
    YANDEX,
    CHARTBOOST,
    UNITY,
    TOPON_PTE,
    CUSTOM_MEDIATION,
    DIRECT_MONETIZATION,
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
