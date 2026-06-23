package com.retro99.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.appsflyer.deeplink.DeepLink
import com.appsflyer.deeplink.DeepLinkResult as AfDeepLinkResult
import com.appsflyer.deeplink.DeepLinkResult.Status
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import org.json.JSONObject

actual class AppsFlyerClientFactory(
    private val context: Context,
) {
    actual fun create(config: AppsFlyerConfig): AppsFlyerClient {
        return AndroidAppsFlyerClient(context, config)
    }
}

private class AndroidAppsFlyerClient(
    private val context: Context,
    private val config: AppsFlyerConfig,
) : AppsFlyerClient {

    private val lib: AppsFlyerLib = AppsFlyerLib.getInstance()

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
        lib.setDebugLog(config.isDebug)
        lib.setCollectAndroidID(config.collectAndroidId)
        lib.init(
            config.devKey,
            object : AppsFlyerConversionListener {
                override fun onConversionDataSuccess(data: Map<String, Any>) {
                    conversionRelay.tryEmit(data.toCampaignData())
                }

                override fun onConversionDataFail(errorMessage: String) {
                    conversionRelay.tryEmit(
                        CampaignData.Error(message = errorMessage),
                    )
                }

                override fun onAppOpenAttribution(attributionData: Map<String, String>) {
                    // No-op: unified deep link API (subscribeForDeepLink) handles this.
                }

                override fun onAttributionFailure(errorMessage: String) {
                    // No-op: unified deep link API (subscribeForDeepLink) handles this.
                }
            },
            context,
        )
        lib.subscribeForDeepLink { result ->
            deepLinkRelay.tryEmit(toDeepLinkResult(result))
        }
        lib.start(
            context,
            config.devKey,
            object : AppsFlyerRequestListener {
                override fun onSuccess() {
                    startResultRelay.tryEmit(StartResult.Success)
                }

                override fun onError(code: Int, message: String) {
                    startResultRelay.tryEmit(StartResult.Error(code, message))
                }
            },
        )
    }

    override fun setCustomerUserId(id: String?) {
        lib.setCustomerUserId(id)
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        lib.logEvent(context, name, params.filterValues { value -> value != null })
    }

    private fun toDeepLinkResult(result: AfDeepLinkResult): DeepLinkResult {
        return when (result.status) {
            Status.FOUND -> {
                val deepLink: DeepLink = result.deepLink
                DeepLinkResult.Found(
                    deepLinkValue = deepLink.deepLinkValue,
                    isDeferred = deepLink.isDeferred == true,
                    mediaSource = deepLink.mediaSource,
                    campaign = deepLink.campaign,
                    raw = deepLink.clickEvent.toMap(),
                )
            }
            Status.NOT_FOUND -> DeepLinkResult.NotFound
            else -> DeepLinkResult.Error(message = result.error?.toString())
        }
    }
}

private fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    keys().forEach { key -> map[key] = opt(key) }
    return map
}
