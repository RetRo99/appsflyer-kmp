@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.retro99.appsflyer

import AppsFlyerBridge.AppsFlyerBridge
import kotlin.concurrent.Volatile
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first

actual class AppsFlyerClientFactory {
    actual fun create(config: AppsFlyerConfig): AppsFlyerClient {
        return IosAppsFlyerClient(config)
    }
}

internal class IosAppsFlyerClient(
    private val config: AppsFlyerConfig,
) : AppsFlyerClient {

    internal val bridge = AppsFlyerBridge()

    @Volatile
    private var started = false

    private val conversionRelay = MutableSharedFlow<CampaignData>(
        replay = 1,
    )

    private val deepLinkRelay = MutableSharedFlow<DeepLinkResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val startResultRelay = MutableSharedFlow<StartResult>(
        replay = 1,
    )

    override val deepLink: Flow<DeepLinkResult> = deepLinkRelay.asSharedFlow()

    override suspend fun getStartResult(): StartResult = startResultRelay.first()

    override suspend fun getConversionData(): CampaignData = conversionRelay.first()

    override fun start() {
        if (started) return
        started = true
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
                    val map = data as Map<String, Any?>
                    conversionRelay.tryEmit(map.toCampaignData())
                } else {
                    conversionRelay.tryEmit(
                        CampaignData.Error(message = "Conversion data was null"),
                    )
                }
            },
            onConversionError = { message ->
                conversionRelay.tryEmit(CampaignData.Error(message = message))
            },
            onDeepLinkFound = { deepLinkValue, isDeferred, mediaSource, campaign, raw ->
                @Suppress("UNCHECKED_CAST")
                val rawMap = raw as? Map<String, Any?> ?: emptyMap()
                deepLinkRelay.tryEmit(
                    DeepLinkResult.Found(
                        deepLinkValue = deepLinkValue,
                        isDeferred = isDeferred,
                        mediaSource = mediaSource,
                        campaign = campaign,
                        raw = rawMap,
                    ),
                )
            },
            onDeepLinkNotFound = {
                deepLinkRelay.tryEmit(DeepLinkResult.NotFound)
            },
            onDeepLinkError = { message ->
                deepLinkRelay.tryEmit(DeepLinkResult.Error(message = message))
            },
            onStart = { success, code, message ->
                startResultRelay.tryEmit(
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
        val filtered = params.filterValues { value -> value != null }
        @Suppress("UNCHECKED_CAST")
        bridge.logEvent(name, filtered as Map<Any?, *>)
    }
}
