package com.retro99.appsflyer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.resume

internal class AppsFlyerClientImpl(
    private val sdk: AppsFlyerSdk,
    private val config: AppsFlyerConfig,
) : AppsFlyerClient {

    @OptIn(ExperimentalAtomicApi::class)
    private val started = AtomicInt(0)

    private val conversionRelay = MutableSharedFlow<CampaignData>(
        replay = 1,
    )

    private val deepLinkRelay = MutableSharedFlow<DeepLinkResult>(
        extraBufferCapacity = 1,
    )

    private val startResultRelay = MutableSharedFlow<StartResult>(
        replay = 1,
    )

    override val deepLink: Flow<DeepLinkResult> = deepLinkRelay.asSharedFlow()

    override suspend fun getStartResult(): StartResult = startResultRelay.first()

    override suspend fun getConversionData(): CampaignData = conversionRelay.first()

    @OptIn(ExperimentalAtomicApi::class)
    override fun start() {
        if (started.compareAndExchange(0, 1) != 0) return
        sdk.configure(
            config = config,
            onConversion = { map -> conversionRelay.tryEmit(map.toCampaignData()) },
            onConversionError = { message -> conversionRelay.tryEmit(CampaignData.Error(message = message)) },
            onDeepLink = { result -> deepLinkRelay.tryEmit(result) },
            onStart = { result -> startResultRelay.tryEmit(result) },
        )
    }

    override fun setCustomerUserId(id: String?) {
        sdk.setCustomerUserId(id)
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        sdk.logEvent(name, params.filterValues { value -> value != null })
    }

    override suspend fun logEventForResult(
        name: String,
        params: Map<String, Any?>,
    ): LogEventResult = suspendCancellableCoroutine { continuation ->
        sdk.logEventForResult(
            name = name,
            params = params.filterValues { value -> value != null },
        ) { result ->
            continuation.resume(result)
        }
    }

    override fun getAppsFlyerUID(): String? = sdk.getAppsFlyerUID()

    override fun setCurrencyCode(currency: String) = sdk.setCurrencyCode(currency)

    override fun logLocation(latitude: Double, longitude: Double) =
        sdk.logLocation(latitude, longitude)

    override fun setAdditionalData(data: Map<String, Any?>) =
        sdk.setAdditionalData(data.filterValues { it != null })

    override fun setMinTimeBetweenSessions(seconds: Int) =
        sdk.setMinTimeBetweenSessions(seconds)

    override fun setDisableAdvertisingIdentifier(disable: Boolean) =
        sdk.setDisableAdvertisingIdentifier(disable)

    override fun setDisableSKAdNetwork(disable: Boolean) =
        sdk.setDisableSKAdNetwork(disable)

    override fun setUserEmails(emails: List<String>, cryptType: AfEmailCryptType) =
        sdk.setUserEmails(emails, cryptType)

    override fun registerUninstall(token: String) =
        sdk.registerUninstall(token)

    override fun setOneLinkCustomDomain(domains: List<String>) =
        sdk.setOneLinkCustomDomain(domains)

    override fun appendParametersToDeepLinkingURL(contains: String, parameters: Map<String, String>) =
        sdk.appendParametersToDeepLinkingURL(contains, parameters)

    override fun setPartnerData(partnerId: String, data: Map<String, Any?>) =
        sdk.setPartnerData(partnerId, data.filterValues { it != null })

    override fun addPushNotificationDeepLinkPath(keys: List<String>) =
        sdk.addPushNotificationDeepLinkPath(keys)

    override fun setResolveDeepLinkURLs(urls: List<String>) =
        sdk.setResolveDeepLinkURLs(urls)

    override fun getSdkVersion(): String = sdk.getSdkVersion()

    override fun setAnonymizeUser(enabled: Boolean) {
        sdk.setAnonymizeUser(enabled)
    }

    override fun setSharingFilterPartners(partners: Set<String>) {
        sdk.setSharingFilterPartners(partners)
    }

    override fun logAdRevenue(data: AdRevenueData) {
        sdk.logAdRevenue(
            data.copy(additionalParameters = data.additionalParameters.filterValues { it != null }),
        )
    }

    override suspend fun validateAndLogInAppPurchase(
        purchaseDetails: PurchaseDetails,
        additionalParameters: Map<String, Any?>,
    ): PurchaseValidationResult = suspendCancellableCoroutine { continuation ->
        sdk.validateAndLogInAppPurchase(
            purchaseDetails = purchaseDetails,
            additionalParameters = additionalParameters.filterValues { it != null },
        ) { result ->
            continuation.resume(result)
        }
    }

    override fun stop(stop: Boolean) = sdk.stop(stop)

    override val isStopped: Boolean
        get() = sdk.isStopped()
}
