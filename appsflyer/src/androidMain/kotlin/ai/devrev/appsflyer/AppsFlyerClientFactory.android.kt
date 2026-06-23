package com.retro99.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
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

private fun Any?.unwrap(): Any? = when (this) {
    is JSONObject -> toMap()
    is JSONArray -> (0 until length()).map { i -> opt(i).unwrap() }
    JSONObject.NULL -> null
    else -> this
}
