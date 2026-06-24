package com.retro99.appsflyer

internal enum class DeepLinkStatus { FOUND, NOT_FOUND, ERROR }

internal fun mapDeepLinkResult(
    status: DeepLinkStatus,
    deepLinkValue: String? = null,
    isDeferred: Boolean? = null,
    mediaSource: String? = null,
    campaign: String? = null,
    raw: Map<String, Any?> = emptyMap(),
    error: String? = null,
): DeepLinkResult = when (status) {
    DeepLinkStatus.FOUND -> DeepLinkResult.Found(
        deepLinkValue = deepLinkValue,
        isDeferred = isDeferred == true,
        mediaSource = mediaSource,
        campaign = campaign,
        raw = raw,
    )
    DeepLinkStatus.NOT_FOUND -> DeepLinkResult.NotFound
    DeepLinkStatus.ERROR -> DeepLinkResult.Error(message = error)
}

internal interface AppsFlyerSdk {
    fun configure(
        config: AppsFlyerConfig,
        onConversion: (Map<String, Any?>) -> Unit,
        onConversionError: (String) -> Unit,
        onDeepLink: (DeepLinkResult) -> Unit,
        onStart: (StartResult) -> Unit,
    )

    fun setCustomerUserId(id: String?)

    fun logEvent(name: String, params: Map<String, Any?>)

    fun logEventForResult(
        name: String,
        params: Map<String, Any?>,
        onResult: (LogEventResult) -> Unit,
    )

    fun setAnonymizeUser(enabled: Boolean)

    fun setSharingFilterPartners(partners: Set<String>)

    fun setCurrencyCode(currency: String)

    fun logLocation(latitude: Double, longitude: Double)

    fun setAdditionalData(data: Map<String, Any?>)

    fun setMinTimeBetweenSessions(seconds: Int)

    fun setDisableAdvertisingIdentifier(disable: Boolean)

    fun setDisableSKAdNetwork(disable: Boolean)

    fun setUserEmails(emails: List<String>, cryptType: AfEmailCryptType)

    fun getAppsFlyerUID(): String?

    fun getSdkVersion(): String

    fun logAdRevenue(data: AdRevenueData)

    fun stop(stop: Boolean)

    fun isStopped(): Boolean
}
