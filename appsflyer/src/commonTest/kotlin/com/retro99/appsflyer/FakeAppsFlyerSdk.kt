package com.retro99.appsflyer

internal class FakeAppsFlyerSdk : AppsFlyerSdk {
    var configureCount = 0
        private set
    var lastConfig: AppsFlyerConfig? = null
        private set
    var onConversion: ((Map<String, Any?>) -> Unit)? = null
        private set
    var onConversionError: ((String) -> Unit)? = null
        private set
    var onDeepLink: ((DeepLinkResult) -> Unit)? = null
        private set
    var onStart: ((StartResult) -> Unit)? = null
        private set
    var lastCustomerUserId: String? = null
        private set
    var lastEventName: String? = null
        private set
    var lastEventParams: Map<String, Any?>? = null
        private set
    var lastAdRevenueData: AdRevenueData? = null
        private set
    var lastCurrencyCode: String? = null
        private set
    var lastLatitude: Double? = null
        private set
    var lastLongitude: Double? = null
        private set
    var lastAdditionalData: Map<String, Any?>? = null
        private set
    var lastMinTimeBetweenSessions: Int? = null
        private set
    var lastDisableAdvertisingIdentifier: Boolean? = null
        private set
    var lastDisableSKAdNetwork: Boolean? = null
        private set
    var lastUserEmails: List<String>? = null
        private set
    var lastEmailCryptType: AfEmailCryptType? = null
        private set
    var lastUninstallToken: String? = null
        private set
    var lastOneLinkCustomDomains: List<String>? = null
        private set
    var lastDeepLinkContains: String? = null
        private set
    var lastDeepLinkParameters: Map<String, String>? = null
        private set
    var lastPartnerId: String? = null
        private set
    var lastPartnerData: Map<String, Any?>? = null
        private set
    var lastPushNotificationDeepLinkPath: List<String>? = null
        private set
    var lastResolveDeepLinkURLs: List<String>? = null
        private set
    var lastHostPrefix: String? = null
        private set
    var lastHostName: String? = null
        private set
    var lastAppInviteOneLink: String? = null
        private set
    var lastPhoneNumber: String? = null
        private set
    var lastAttributionUrl: String? = null
        private set
    var lastIsUpdate: Boolean? = null
        private set
    var lastCollectIMEI: Boolean? = null
        private set
    var lastCollectOaid: Boolean? = null
        private set
    var lastImeiData: String? = null
        private set
    var lastOaidData: String? = null
        private set
    var lastAndroidIdData: String? = null
        private set
    var disableAppSetIdCalled: Boolean = false
        private set
    var lastDisableNetworkData: Boolean? = null
        private set
    var lastWaitForCustomerUserId: Boolean? = null
        private set
    var lastPreinstallMediaSource: String? = null
        private set
    var lastPreinstallCampaign: String? = null
        private set
    var lastPreinstallSiteId: String? = null
        private set
    var lastOutOfStore: String? = null
        private set
    var lastDisableIDFVCollection: Boolean? = null
        private set
    var lastDisableCollectASA: Boolean? = null
        private set
    var lastDisableAppleAdsAttribution: Boolean? = null
        private set
    var lastShouldCollectDeviceName: Boolean? = null
        private set
    var lastUseReceiptValidationSandbox: Boolean? = null
        private set
    var lastUseUninstallSandbox: Boolean? = null
        private set
    var lastCurrentDeviceLanguage: String? = null
        private set
    var lastDeepLinkTimeout: Int? = null
        private set
    var lastRemoteDebuggingData: String? = null
        private set
    var lastIsPreInstalledApp: Boolean = false
        private set
    var logSessionCalled: Boolean = false
        private set
    var onPauseCalled: Boolean = false
        private set
    var lastCustomerIdForLogSession: String? = null
        private set
    var lastLogLevel: AfLogLevel? = null
        private set
    var lastATTTimeoutInterval: Double? = null
        private set
    var advertisingIdentifierValue: String? = "fake-idfa"
    var lastCrossPromoteAppId: String? = null
        private set
    var lastCrossPromoteCampaign: String? = null
        private set
    var lastCrossPromoteParameters: Map<String, String>? = null
        private set
    var lastOpenStoreAppId: String? = null
        private set
    var lastOpenStoreCampaign: String? = null
        private set
    var lastOpenStoreParameters: Map<String, String>? = null
        private set
    var lastInviteChannel: String? = null
        private set
    var lastInviteParameters: Map<String, String>? = null
        private set
    var lastInviteLinkParams: InviteLinkParams? = null
        private set
    var inviteUrlResult: String? = "https://onelink.app/invite/abc123"
    var lastFacebookDeferredApplinks: Boolean? = null
        private set
    var lastPluginInfoPlugin: String? = null
        private set
    var lastPluginInfoVersion: String? = null
        private set
    var lastPluginInfoParams: Map<String, String>? = null
        private set
    var sharingFilterForAllPartnersCalled: Boolean = false
        private set
    var lastExtension: String? = null
        private set
    var lastInstallId: String? = null
        private set
    var isSessionReadyValue: Boolean = true
        private set
    var lastPushNotificationPayload: Map<String, Any?>? = null
        private set
    var unregisterSessionReadyCalled: Boolean = false
        private set
    var lastAnonymizeUser: Boolean? = null
        private set
    var lastSharingFilterPartners: Set<String>? = null
        private set
    var logEventResult: LogEventResult? = null
    var purchaseValidationResult: PurchaseValidationResult? = null
    var lastPurchaseDetails: PurchaseDetails? = null
        private set
    var lastPurchaseAdditionalParams: Map<String, Any?>? = null
        private set

    override fun configure(
        config: AppsFlyerConfig,
        onConversion: (Map<String, Any?>) -> Unit,
        onConversionError: (String) -> Unit,
        onDeepLink: (DeepLinkResult) -> Unit,
        onStart: (StartResult) -> Unit,
    ) {
        configureCount++
        lastConfig = config
        this.onConversion = onConversion
        this.onConversionError = onConversionError
        this.onDeepLink = onDeepLink
        this.onStart = onStart
    }

    override fun setCustomerUserId(id: String?) {
        lastCustomerUserId = id
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        lastEventName = name
        lastEventParams = params
    }

    override fun logEventForResult(
        name: String,
        params: Map<String, Any?>,
        onResult: (LogEventResult) -> Unit,
    ) {
        lastEventName = name
        lastEventParams = params
        onResult(logEventResult ?: error("logEventResult not set"))
    }

    override fun getAppsFlyerUID(): String? = "fake-uid"

    override fun getSdkVersion(): String = "6.18.1"

    override fun setCurrencyCode(currency: String) {
        lastCurrencyCode = currency
    }

    override fun logLocation(latitude: Double, longitude: Double) {
        lastLatitude = latitude
        lastLongitude = longitude
    }

    override fun setAdditionalData(data: Map<String, Any?>) {
        lastAdditionalData = data
    }

    override fun setMinTimeBetweenSessions(seconds: Int) {
        lastMinTimeBetweenSessions = seconds
    }

    override fun setDisableAdvertisingIdentifier(disable: Boolean) {
        lastDisableAdvertisingIdentifier = disable
    }

    override fun setDisableSKAdNetwork(disable: Boolean) {
        lastDisableSKAdNetwork = disable
    }

    override fun setUserEmails(emails: List<String>, cryptType: AfEmailCryptType) {
        lastUserEmails = emails
        lastEmailCryptType = cryptType
    }

    override fun registerUninstall(token: String) {
        lastUninstallToken = token
    }

    override fun setOneLinkCustomDomain(domains: List<String>) {
        lastOneLinkCustomDomains = domains
    }

    override fun appendParametersToDeepLinkingURL(contains: String, parameters: Map<String, String>) {
        lastDeepLinkContains = contains
        lastDeepLinkParameters = parameters
    }

    override fun setPartnerData(partnerId: String, data: Map<String, Any?>) {
        lastPartnerId = partnerId
        lastPartnerData = data
    }

    override fun addPushNotificationDeepLinkPath(keys: List<String>) {
        lastPushNotificationDeepLinkPath = keys
    }

    override fun setResolveDeepLinkURLs(urls: List<String>) {
        lastResolveDeepLinkURLs = urls
    }

    override fun setHost(hostPrefix: String, hostName: String) {
        lastHostPrefix = hostPrefix
        lastHostName = hostName
    }

    override fun getHostName(): String = "default-host"

    override fun getHostPrefix(): String = "default-prefix"

    override fun setAppInviteOneLink(oneLinkId: String) {
        lastAppInviteOneLink = oneLinkId
    }

    override fun setPhoneNumber(phoneNumber: String?) {
        lastPhoneNumber = phoneNumber
    }

    override fun performOnAppAttribution(url: String) {
        lastAttributionUrl = url
    }

    override fun setIsUpdate(isUpdate: Boolean) {
        lastIsUpdate = isUpdate
    }

    override fun setCollectIMEI(collect: Boolean) {
        lastCollectIMEI = collect
    }

    override fun setCollectOaid(collect: Boolean) {
        lastCollectOaid = collect
    }

    override fun setImeiData(imei: String?) {
        lastImeiData = imei
    }

    override fun setOaidData(oaid: String?) {
        lastOaidData = oaid
    }

    override fun setAndroidIdData(androidId: String?) {
        lastAndroidIdData = androidId
    }

    override fun disableAppSetId() {
        disableAppSetIdCalled = true
    }

    override fun setDisableNetworkData(disable: Boolean) {
        lastDisableNetworkData = disable
    }

    override fun waitForCustomerUserId(wait: Boolean) {
        lastWaitForCustomerUserId = wait
    }

    override fun setPreinstallAttribution(mediaSource: String, campaign: String, siteId: String) {
        lastPreinstallMediaSource = mediaSource
        lastPreinstallCampaign = campaign
        lastPreinstallSiteId = siteId
    }

    override fun setOutOfStore(source: String) {
        lastOutOfStore = source
    }

    override fun setDisableIDFVCollection(disable: Boolean) {
        lastDisableIDFVCollection = disable
    }

    override fun setDisableCollectASA(disable: Boolean) {
        lastDisableCollectASA = disable
    }

    override fun setDisableAppleAdsAttribution(disable: Boolean) {
        lastDisableAppleAdsAttribution = disable
    }

    override fun setShouldCollectDeviceName(collect: Boolean) {
        lastShouldCollectDeviceName = collect
    }

    override fun setUseReceiptValidationSandbox(enable: Boolean) {
        lastUseReceiptValidationSandbox = enable
    }

    override fun setUseUninstallSandbox(enable: Boolean) {
        lastUseUninstallSandbox = enable
    }

    override fun setCurrentDeviceLanguage(language: String?) {
        lastCurrentDeviceLanguage = language
    }

    override fun setDeepLinkTimeout(seconds: Int) {
        lastDeepLinkTimeout = seconds
    }

    override fun remoteDebuggingCall(data: String) {
        lastRemoteDebuggingData = data
    }

    var isPreInstalledAppValue: Boolean = true

    override fun isPreInstalledApp(): Boolean {
        lastIsPreInstalledApp = isPreInstalledAppValue
        return isPreInstalledAppValue
    }

    override fun getAttributionId(): String? = "fb-attribution-123"

    override fun getOutOfStore(): String = "amazon"

    override fun logSession() {
        logSessionCalled = true
    }

    override fun onPause() {
        onPauseCalled = true
    }

    override fun setCustomerIdAndLogSession(customerUserId: String) {
        lastCustomerIdForLogSession = customerUserId
    }

    override fun setLogLevel(level: AfLogLevel) {
        lastLogLevel = level
    }

    override fun waitForATTUserAuthorization(timeoutInterval: Double) {
        lastATTTimeoutInterval = timeoutInterval
    }

    override fun getAdvertisingIdentifier(): String? = advertisingIdentifierValue

    override fun logCrossPromoteImpression(
        appId: String,
        campaign: String,
        parameters: Map<String, String>,
    ) {
        lastCrossPromoteAppId = appId
        lastCrossPromoteCampaign = campaign
        lastCrossPromoteParameters = parameters
    }

    override fun logAndOpenStore(
        appId: String,
        campaign: String,
        parameters: Map<String, String>,
    ) {
        lastOpenStoreAppId = appId
        lastOpenStoreCampaign = campaign
        lastOpenStoreParameters = parameters
    }

    override fun logInvite(
        channel: String,
        parameters: Map<String, String>,
    ) {
        lastInviteChannel = channel
        lastInviteParameters = parameters
    }

    override fun generateInviteUrl(
        params: InviteLinkParams,
        onResult: (String?) -> Unit,
    ) {
        lastInviteLinkParams = params
        onResult(inviteUrlResult)
    }

    override fun enableFacebookDeferredApplinks(enable: Boolean) {
        lastFacebookDeferredApplinks = enable
    }

    override fun setPluginInfo(
        plugin: String,
        version: String,
        additionalParameters: Map<String, String>,
    ) {
        lastPluginInfoPlugin = plugin
        lastPluginInfoVersion = version
        lastPluginInfoParams = additionalParameters
    }

    override fun setSharingFilterForAllPartners() {
        sharingFilterForAllPartnersCalled = true
    }

    override fun setExtension(extension: String) {
        lastExtension = extension
    }

    override fun setInstallId(installId: String) {
        lastInstallId = installId
    }

    override fun isSessionReady(): Boolean = isSessionReadyValue

    override fun handlePushNotification(payload: Map<String, Any?>) {
        lastPushNotificationPayload = payload
    }

    override fun unregisterSessionReadyListener() {
        unregisterSessionReadyCalled = true
    }

    override fun setAnonymizeUser(enabled: Boolean) {
        lastAnonymizeUser = enabled
    }

    override fun setSharingFilterPartners(partners: Set<String>) {
        lastSharingFilterPartners = partners
    }

    override fun logAdRevenue(data: AdRevenueData) {
        lastAdRevenueData = data
    }

    override fun validateAndLogInAppPurchase(
        purchaseDetails: PurchaseDetails,
        additionalParameters: Map<String, Any?>,
        onResult: (PurchaseValidationResult) -> Unit,
    ) {
        lastPurchaseDetails = purchaseDetails
        lastPurchaseAdditionalParams = additionalParameters
        onResult(purchaseValidationResult ?: error("purchaseValidationResult not set"))
    }

    override fun stop(stop: Boolean) {
        isStoppedValue = stop
    }

    private var isStoppedValue: Boolean = false

    override fun isStopped(): Boolean = isStoppedValue
}
