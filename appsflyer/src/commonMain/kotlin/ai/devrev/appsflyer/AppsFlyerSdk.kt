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

    fun getAppsFlyerUID(): String?
}
