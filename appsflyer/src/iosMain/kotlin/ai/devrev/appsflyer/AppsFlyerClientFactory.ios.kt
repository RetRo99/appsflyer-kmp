@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.retro99.appsflyer

import AppsFlyerBridge.AppsFlyerBridge

internal class IosAppsFlyerSdk : AppsFlyerSdk {

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
        bridge.configureWithDevKey(
            devKey = config.devKey,
            appId = config.iosAppId,
            isDebug = config.isDebug,
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

    override fun getAppsFlyerUID(): String? =
        bridge.getAppsFlyerUID()

    override fun stop(stop: Boolean) {
        bridge.stop(stop)
    }

    override fun isStopped(): Boolean =
        bridge.isStopped()
}

internal actual class AppsFlyerClientFactory {
    actual fun create(config: AppsFlyerConfig): AppsFlyerClient {
        return AppsFlyerClientImpl(
            sdk = IosAppsFlyerSdk(),
            config = config,
        )
    }
}
