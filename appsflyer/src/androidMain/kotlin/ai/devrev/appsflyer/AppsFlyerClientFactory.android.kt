package com.retro99.appsflyer

import android.content.Context
import com.appsflyer.AFAdRevenueData
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
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

private fun AfMediationNetwork.toAndroidMediationNetwork(): MediationNetwork = when (this) {
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

private fun Any?.unwrap(): Any? = when (this) {
    is JSONObject -> toMap()
    is JSONArray -> (0 until length()).map { i -> opt(i).unwrap() }
    JSONObject.NULL -> null
    else -> this
}
