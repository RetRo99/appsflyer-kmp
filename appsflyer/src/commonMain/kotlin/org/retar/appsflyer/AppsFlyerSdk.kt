package org.retar.appsflyer

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

    fun registerUninstall(token: String)

    fun setOneLinkCustomDomain(domains: List<String>)

    fun appendParametersToDeepLinkingURL(contains: String, parameters: Map<String, String>)

    fun setPartnerData(partnerId: String, data: Map<String, Any?>)

    fun addPushNotificationDeepLinkPath(keys: List<String>)

    fun setResolveDeepLinkURLs(urls: List<String>)

    fun setHost(hostPrefix: String, hostName: String)

    fun getHostName(): String

    fun getHostPrefix(): String

    fun setAppInviteOneLink(oneLinkId: String)

    fun setPhoneNumber(phoneNumber: String?)

    fun performOnAppAttribution(url: String)

    fun setIsUpdate(isUpdate: Boolean)

    fun setCollectIMEI(collect: Boolean)

    fun setCollectOaid(collect: Boolean)

    fun setImeiData(imei: String?)

    fun setOaidData(oaid: String?)

    fun setAndroidIdData(androidId: String?)

    fun disableAppSetId()

    fun setDisableNetworkData(disable: Boolean)

    fun waitForCustomerUserId(wait: Boolean)

    fun setPreinstallAttribution(mediaSource: String, campaign: String, siteId: String)

    fun setOutOfStore(source: String)

    fun setDisableIDFVCollection(disable: Boolean)

    fun setDisableCollectASA(disable: Boolean)

    fun setDisableAppleAdsAttribution(disable: Boolean)

    fun setShouldCollectDeviceName(collect: Boolean)

    fun setUseReceiptValidationSandbox(enable: Boolean)

    fun setUseUninstallSandbox(enable: Boolean)

    fun setCurrentDeviceLanguage(language: String?)

    fun setDeepLinkTimeout(seconds: Int)

    fun remoteDebuggingCall(data: String)

    fun isPreInstalledApp(): Boolean

    fun getAttributionId(): String?

    fun getOutOfStore(): String

    fun logSession()

    fun onPause()

    fun setCustomerIdAndLogSession(customerUserId: String)

    fun setLogLevel(level: AfLogLevel)

    fun waitForATTUserAuthorization(timeoutInterval: Double)

    fun getAdvertisingIdentifier(): String?

    fun logCrossPromoteImpression(
        appId: String,
        campaign: String,
        parameters: Map<String, String>,
    )

    fun logAndOpenStore(
        appId: String,
        campaign: String,
        parameters: Map<String, String>,
    )

    fun logInvite(
        channel: String,
        parameters: Map<String, String>,
    )

    fun generateInviteUrl(
        params: InviteLinkParams,
        onResult: (String?) -> Unit,
    )

    fun enableFacebookDeferredApplinks(enable: Boolean)

    fun setPluginInfo(
        plugin: String,
        version: String,
        additionalParameters: Map<String, String>,
    )

    fun setSharingFilterForAllPartners()

    fun setExtension(extension: String)

    fun setInstallId(installId: String)

    fun isSessionReady(): Boolean

    fun handlePushNotification(payload: Map<String, Any?>)

    fun unregisterSessionReadyListener()

    fun getAppsFlyerUID(): String?

    fun getSdkVersion(): String

    fun logAdRevenue(data: AdRevenueData)

    fun validateAndLogInAppPurchase(
        purchaseDetails: PurchaseDetails,
        additionalParameters: Map<String, Any?>,
        onResult: (PurchaseValidationResult) -> Unit,
    )

    fun stop(stop: Boolean)

    fun isStopped(): Boolean
}
