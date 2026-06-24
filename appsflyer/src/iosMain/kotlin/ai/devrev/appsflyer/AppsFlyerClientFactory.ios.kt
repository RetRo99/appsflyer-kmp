@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
)

package com.retro99.appsflyer

import AppsFlyerBridge.AppsFlyerBridge
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSNumber
import platform.Foundation.create

internal class IosAppsFlyerSdk(
    private val launchOptions: Map<Any?, *>? = null,
) : AppsFlyerSdk {

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
        val consentDict: Map<Any?, *>? = config.consentData?.let { consent ->
            mapOf(
                "isUserSubjectToGDPR" to consent.isUserSubjectToGDPR?.let { NSNumber(it) },
                "hasConsentForDataUsage" to consent.hasConsentForDataUsage?.let { NSNumber(it) },
                "hasConsentForAdsPersonalization" to consent.hasConsentForAdsPersonalization?.let { NSNumber(it) },
                "hasConsentForAdStorage" to consent.hasConsentForAdStorage?.let { NSNumber(it) },
            )
        }
        bridge.configureWithDevKey(
            devKey = config.devKey,
            appId = config.iosAppId,
            isDebug = config.isDebug,
            anonymizeUser = config.anonymizeUser,
            enableTCFDataCollection = config.enableTCFDataCollection,
            consentData = consentDict,
            sharingFilterPartners = config.sharingFilterPartners.toList(),
            launchOptions = launchOptions,
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

    override fun logEventForResult(
        name: String,
        params: Map<String, Any?>,
        onResult: (LogEventResult) -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        bridge.logEventForResult(name, params as Map<Any?, *>) { success, code, message ->
            onResult(
                if (success) {
                    LogEventResult.Success
                } else {
                    LogEventResult.Error(code.toInt(), message.orEmpty())
                },
            )
        }
    }

    override fun getAppsFlyerUID(): String? =
        bridge.getAppsFlyerUID()

    override fun getSdkVersion(): String =
        bridge.getSdkVersion()

    override fun setCurrencyCode(currency: String) {
        bridge.setCurrencyCode(currency)
    }

    override fun logLocation(latitude: Double, longitude: Double) {
        bridge.logLocation(longitude, latitude)
    }

    override fun setAdditionalData(data: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        bridge.setCustomData(data as Map<Any?, *>?)
    }

    override fun setMinTimeBetweenSessions(seconds: Int) {
        bridge.setMinTimeBetweenSessions(seconds.toLong())
    }

    override fun setDisableAdvertisingIdentifier(disable: Boolean) {
        bridge.setDisableAdvertisingIdentifier(disable)
    }

    override fun setDisableSKAdNetwork(disable: Boolean) {
        bridge.setDisableSKAdNetwork(disable)
    }

    override fun setUserEmails(emails: List<String>, cryptType: AfEmailCryptType) {
        bridge.setUserEmails(emails, cryptType.iosRawValue)
    }

    override fun registerUninstall(token: String) {
        bridge.registerUninstall(token.hexToNSData())
    }

    override fun setOneLinkCustomDomain(domains: List<String>) {
        bridge.setOneLinkCustomDomains(domains)
    }

    override fun setAnonymizeUser(enabled: Boolean) {
        bridge.setAnonymizeUser(enabled)
    }

    override fun setSharingFilterPartners(partners: Set<String>) {
        bridge.setSharingFilterPartners(partners.toList())
    }

    override fun logAdRevenue(data: AdRevenueData) {
        @Suppress("UNCHECKED_CAST")
        bridge.logAdRevenue(
            monetizationNetwork = data.monetizationNetwork,
            mediationNetwork = data.mediationNetwork.iosRawValue,
            currency = data.currency,
            revenue = data.revenue,
            additionalParameters = data.additionalParameters as Map<Any?, *>?,
        )
    }

    override fun validateAndLogInAppPurchase(
        purchaseDetails: PurchaseDetails,
        additionalParameters: Map<String, Any?>,
        onResult: (PurchaseValidationResult) -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        bridge.validateAndLogInAppPurchase(
            productId = purchaseDetails.productId,
            transactionId = purchaseDetails.transactionId,
            purchaseType = purchaseDetails.purchaseType.iosRawValue,
            additionalParameters = additionalParameters as Map<Any?, *>?,
        ) { response, error ->
            when {
                error != null ->
                    onResult(PurchaseValidationResult.Error(message = error))
                response != null -> {
                    @Suppress("UNCHECKED_CAST")
                    onResult(PurchaseValidationResult.Success(result = response as Map<String, Any?>))
                }
                else ->
                    onResult(PurchaseValidationResult.Error(message = null))
            }
        }
    }

    override fun stop(stop: Boolean) {
        bridge.stop(stop)
    }

    override fun isStopped(): Boolean =
        bridge.isStopped()
}

internal actual class AppsFlyerClientFactory(
    private val launchOptions: Map<Any?, *>? = null,
) {
    actual fun create(config: AppsFlyerConfig): AppsFlyerClient {
        return AppsFlyerClientImpl(
            sdk = IosAppsFlyerSdk(launchOptions),
            config = config,
        )
    }
}

internal fun String.hexToNSData(): NSData {
    val bytes = chunked(2).map { byteStr ->
        byteStr.toInt(16).toByte()
    }.toByteArray()
    return bytes.usePinned { pin ->
        NSData.create(bytes = pin.addressOf(0), length = bytes.size.toULong())
    }
}
