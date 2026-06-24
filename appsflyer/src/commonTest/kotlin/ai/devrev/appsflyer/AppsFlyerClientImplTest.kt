package com.retro99.appsflyer

import app.cash.turbine.test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
class AppsFlyerClientImplTest {

    private val sdk = FakeAppsFlyerSdk()
    private val config = AppsFlyerConfig(devKey = "dev-key")
    private val client = AppsFlyerClientImpl(sdk, config)

    @Test
    fun startConfiguresBackendOnce() {
        client.start()
        client.start()
        client.start()

        assertEquals(1, sdk.configureCount)
        assertEquals(config, sdk.lastConfig)
    }

    @Test
    fun conversionCallbackEmitsMappedConversionData() = runTest {
        client.start()
        sdk.onConversion?.invoke(
            mapOf(
                "af_status" to "Organic",
                "media_source" to "organic",
                "campaign" to null,
            ),
        )

        val result = client.getConversionData()

        assertIs<CampaignData.Success>(result)
        assertEquals(AfStatus.ORGANIC, result.status)
        assertEquals("organic", result.mediaSource)
        assertNull(result.campaign)
    }

    @Test
    fun conversionCallbackMapsNonOrganic() = runTest {
        client.start()
        sdk.onConversion?.invoke(
            mapOf(
                "af_status" to "Non-organic",
                "media_source" to "facebook",
                "campaign" to "summer",
            ),
        )

        val result = client.getConversionData()

        assertIs<CampaignData.Success>(result)
        assertEquals(AfStatus.NON_ORGANIC, result.status)
        assertEquals("summer", result.campaign)
    }

    @Test
    fun conversionErrorCallbackEmitsError() = runTest {
        client.start()
        sdk.onConversionError?.invoke("Network timeout")

        val result = client.getConversionData()

        assertIs<CampaignData.Error>(result)
        assertEquals("Network timeout", result.message)
    }

    @Test
    fun conversionErrorWithEmptyMessage() = runTest {
        client.start()
        sdk.onConversionError?.invoke("")

        val result = client.getConversionData()

        assertIs<CampaignData.Error>(result)
        assertEquals("", result.message)
    }

    @Test
    fun startResultSuccessEmitted() = runTest {
        client.start()
        sdk.onStart?.invoke(StartResult.Success)

        assertIs<StartResult.Success>(client.getStartResult())
    }

    @Test
    fun startResultErrorEmittedWithCodeAndMessage() = runTest {
        client.start()
        sdk.onStart?.invoke(StartResult.Error(code = 403, message = "Forbidden"))

        val result = client.getStartResult()

        assertIs<StartResult.Error>(result)
        assertEquals(403, result.code)
        assertEquals("Forbidden", result.message)
    }

    @Test
    fun getStartResultIsIdempotent() = runTest {
        client.start()
        sdk.onStart?.invoke(StartResult.Success)

        val first = client.getStartResult()
        val second = client.getStartResult()

        assertEquals(first, second)
    }

    @Test
    fun getConversionDataIsIdempotent() = runTest {
        client.start()
        sdk.onConversion?.invoke(
            mapOf(
                "af_status" to "Non-organic",
                "media_source" to "facebook",
                "campaign" to "summer",
            ),
        )

        val first = client.getConversionData()
        val second = client.getConversionData()

        assertEquals(first, second)
    }

    @Test
    fun deepLinkCallbackEmitsToFlow() = runTest {
        client.start()
        val expected = DeepLinkResult.Found(
            deepLinkValue = "product_123",
            isDeferred = false,
            mediaSource = "email",
            campaign = "welcome",
            raw = mapOf("deep_link_value" to "product_123"),
        )

        client.deepLink.test {
            sdk.onDeepLink?.invoke(expected)
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun deepLinkFlowEmitsMultipleResults() = runTest {
        client.start()

        client.deepLink.test {
            sdk.onDeepLink?.invoke(DeepLinkResult.NotFound)
            assertIs<DeepLinkResult.NotFound>(awaitItem())

            sdk.onDeepLink?.invoke(
                DeepLinkResult.Found(
                    deepLinkValue = "page_1",
                    isDeferred = true,
                    mediaSource = null,
                    campaign = null,
                    raw = emptyMap(),
                ),
            )
            assertIs<DeepLinkResult.Found>(awaitItem())

            sdk.onDeepLink?.invoke(DeepLinkResult.Error(message = "fail"))
            assertIs<DeepLinkResult.Error>(awaitItem())
        }
    }

    @Test
    fun setCustomerUserIdForwardsToBackend() {
        client.setCustomerUserId("user-42")
        assertEquals("user-42", sdk.lastCustomerUserId)

        client.setCustomerUserId(null)
        assertNull(sdk.lastCustomerUserId)
    }

    @Test
    fun logEventForwardsNameAndParams() {
        client.logEvent("purchase", mapOf("price" to 9.99, "currency" to "USD"))

        assertEquals("purchase", sdk.lastEventName)
        assertEquals(
            mapOf("price" to 9.99, "currency" to "USD"),
            sdk.lastEventParams,
        )
    }

    @Test
    fun logEventStripsNullValues() {
        client.logEvent("event", mapOf("a" to 1, "b" to null, "c" to "x"))

        assertEquals(
            mapOf("a" to 1, "c" to "x"),
            sdk.lastEventParams,
        )
    }

    @Test
    fun logEventEmptyParamsByDefault() {
        client.logEvent("event")

        assertEquals("event", sdk.lastEventName)
        assertEquals(emptyMap(), sdk.lastEventParams)
    }

    @Test
    fun logEventStripsAllNullValuesToEmptyMap() {
        client.logEvent("event", mapOf("a" to null, "b" to null))

        assertEquals("event", sdk.lastEventName)
        assertEquals(emptyMap(), sdk.lastEventParams)
    }

    @Test
    fun logEventEmptyMapForwardsAsIs() {
        client.logEvent("event", emptyMap())

        assertEquals("event", sdk.lastEventName)
        assertEquals(emptyMap(), sdk.lastEventParams)
    }

    @Test
    fun logEventWorksBeforeStart() {
        client.logEvent("event", mapOf("key" to "value"))

        assertEquals("event", sdk.lastEventName)
        assertEquals(mapOf("key" to "value"), sdk.lastEventParams)
    }

    @Test
    fun logEventForResultResumesWithSuccess() = runBlocking {
        sdk.logEventResult = LogEventResult.Success

        val result = client.logEventForResult("event", mapOf("key" to "value"))

        assertIs<LogEventResult.Success>(result)
        assertEquals("event", sdk.lastEventName)
        assertEquals(mapOf("key" to "value"), sdk.lastEventParams)
    }

    @Test
    fun logEventForResultResumesWithError() = runBlocking {
        sdk.logEventResult = LogEventResult.Error(code = 500, message = "fail")

        val result = client.logEventForResult("event")

        assertIs<LogEventResult.Error>(result)
        assertEquals(500, result.code)
        assertEquals("fail", result.message)
    }

    @Test
    fun logEventForResultFiltersNullValues() = runBlocking {
        sdk.logEventResult = LogEventResult.Success

        client.logEventForResult("event", mapOf("a" to "b", "c" to null))

        assertEquals(mapOf("a" to "b"), sdk.lastEventParams)
    }

    @Test
    fun conversionCallbackWithUnexpectedStatusEmitsError() = runTest {
        client.start()
        sdk.onConversion?.invoke(mapOf("af_status" to "Unknown"))

        val result = client.getConversionData()

        assertIs<CampaignData.Error>(result)
        assertEquals("Unexpected af_status: Unknown", result.message)
    }

    @Test
    fun getStartResultSuspendsUntilCallbackFires() = runTest {
        client.start()

        var result: StartResult? = null
        val deferred = async { result = client.getStartResult() }

        assertEquals(null, result)
        sdk.onStart?.invoke(StartResult.Success)
        deferred.join()

        assertIs<StartResult.Success>(result)
    }

    @Test
    fun getConversionDataSuspendsUntilCallbackFires() = runTest {
        client.start()

        var result: CampaignData? = null
        val deferred = async { result = client.getConversionData() }

        assertEquals(null, result)
        sdk.onConversion?.invoke(
            mapOf(
                "af_status" to "Organic",
                "media_source" to "organic",
            ),
        )
        deferred.join()

        assertIs<CampaignData.Success>(result)
        assertEquals(AfStatus.ORGANIC, (result as CampaignData.Success).status)
    }

    @Test
    fun deepLinkFlowDoesNotReplayPastEmissions() = runTest {
        client.start()
        sdk.onDeepLink?.invoke(DeepLinkResult.NotFound)

        client.deepLink.test {
            sdk.onDeepLink?.invoke(DeepLinkResult.Error(message = "new"))
            assertIs<DeepLinkResult.Error>(awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun getSdkVersionForwardsToBackend() {
        assertEquals("6.18.1", client.getSdkVersion())
    }

    @Test
    fun setCurrencyCodeForwardsToBackend() {
        client.setCurrencyCode("EUR")
        assertEquals("EUR", sdk.lastCurrencyCode)
    }

    @Test
    fun logLocationForwardsToBackend() {
        client.logLocation(latitude = 37.7749, longitude = -122.4194)
        assertEquals(37.7749, sdk.lastLatitude)
        assertEquals(-122.4194, sdk.lastLongitude)
    }

    @Test
    fun setAdditionalDataForwardsToBackend() {
        client.setAdditionalData(mapOf("key1" to "value1", "key2" to 42))
        assertEquals(mapOf("key1" to "value1", "key2" to 42), sdk.lastAdditionalData)
    }

    @Test
    fun setAdditionalDataStripsNullValues() {
        client.setAdditionalData(mapOf("a" to "b", "c" to null, "d" to 3))
        assertEquals(mapOf("a" to "b", "d" to 3), sdk.lastAdditionalData)
    }

    @Test
    fun setMinTimeBetweenSessionsForwardsToBackend() {
        client.setMinTimeBetweenSessions(10)
        assertEquals(10, sdk.lastMinTimeBetweenSessions)
    }

    @Test
    fun setDisableAdvertisingIdentifierForwardsToBackend() {
        client.setDisableAdvertisingIdentifier(true)
        assertEquals(true, sdk.lastDisableAdvertisingIdentifier)
    }

    @Test
    fun setDisableSKAdNetworkForwardsToBackend() {
        client.setDisableSKAdNetwork(true)
        assertEquals(true, sdk.lastDisableSKAdNetwork)
    }

    @Test
    fun afEmailCryptTypeIosRawValues() {
        assertEquals(0L, AfEmailCryptType.NONE.iosRawValue)
        assertEquals(3L, AfEmailCryptType.SHA256.iosRawValue)
    }

    @Test
    fun afMediationNetworkIosRawValues() {
        assertEquals(1L, AfMediationNetwork.GOOGLE_ADMOB.iosRawValue)
        assertEquals(2L, AfMediationNetwork.IRON_SOURCE.iosRawValue)
        assertEquals(3L, AfMediationNetwork.APP_LOVIN_MAX.iosRawValue)
        assertEquals(4L, AfMediationNetwork.FYBER.iosRawValue)
        assertEquals(5L, AfMediationNetwork.APPODEAL.iosRawValue)
        assertEquals(6L, AfMediationNetwork.ADMOST.iosRawValue)
        assertEquals(7L, AfMediationNetwork.TOPON.iosRawValue)
        assertEquals(8L, AfMediationNetwork.TRADPLUS.iosRawValue)
        assertEquals(9L, AfMediationNetwork.YANDEX.iosRawValue)
        assertEquals(10L, AfMediationNetwork.CHARTBOOST.iosRawValue)
        assertEquals(11L, AfMediationNetwork.UNITY.iosRawValue)
        assertEquals(12L, AfMediationNetwork.TOPON_PTE.iosRawValue)
        assertEquals(13L, AfMediationNetwork.CUSTOM_MEDIATION.iosRawValue)
        assertEquals(14L, AfMediationNetwork.DIRECT_MONETIZATION.iosRawValue)
    }

    @Test
    fun mapDeepLinkResultFoundMapsAllFields() {
        val result = mapDeepLinkResult(
            status = DeepLinkStatus.FOUND,
            deepLinkValue = "product_123",
            isDeferred = true,
            mediaSource = "email",
            campaign = "welcome",
            raw = mapOf("key" to "value"),
        )

        val found = assertIs<DeepLinkResult.Found>(result)
        assertEquals("product_123", found.deepLinkValue)
        assertEquals(true, found.isDeferred)
        assertEquals("email", found.mediaSource)
        assertEquals("welcome", found.campaign)
        assertEquals(mapOf("key" to "value"), found.raw)
    }

    @Test
    fun mapDeepLinkResultFoundWithNullIsDeferredDefaultsToFalse() {
        val result = mapDeepLinkResult(
            status = DeepLinkStatus.FOUND,
            isDeferred = null,
        )

        val found = assertIs<DeepLinkResult.Found>(result)
        assertEquals(false, found.isDeferred)
    }

    @Test
    fun mapDeepLinkResultNotFound() {
        val result = mapDeepLinkResult(status = DeepLinkStatus.NOT_FOUND)
        assertIs<DeepLinkResult.NotFound>(result)
    }

    @Test
    fun mapDeepLinkResultErrorWithMessage() {
        val result = mapDeepLinkResult(
            status = DeepLinkStatus.ERROR,
            error = "Something went wrong",
        )

        val error = assertIs<DeepLinkResult.Error>(result)
        assertEquals("Something went wrong", error.message)
    }

    @Test
    fun mapDeepLinkResultErrorWithNullMessage() {
        val result = mapDeepLinkResult(status = DeepLinkStatus.ERROR)

        val error = assertIs<DeepLinkResult.Error>(result)
        assertNull(error.message)
    }

    @Test
    fun toCampaignDataOrganicWithAllFields() {
        val result = mapOf(
            "af_status" to "Organic",
            "media_source" to "organic",
            "campaign" to "default",
        ).toCampaignData()

        val success = assertIs<CampaignData.Success>(result)
        assertEquals(AfStatus.ORGANIC, success.status)
        assertEquals("organic", success.mediaSource)
        assertEquals("default", success.campaign)
    }

    @Test
    fun toCampaignDataNonOrganic() {
        val result = mapOf(
            "af_status" to "Non-organic",
            "media_source" to "facebook",
            "campaign" to "summer",
        ).toCampaignData()

        val success = assertIs<CampaignData.Success>(result)
        assertEquals(AfStatus.NON_ORGANIC, success.status)
        assertEquals("facebook", success.mediaSource)
        assertEquals("summer", success.campaign)
    }

    @Test
    fun toCampaignDataMissingMediaSourceReturnsNull() {
        val result = mapOf("af_status" to "Organic").toCampaignData()

        val success = assertIs<CampaignData.Success>(result)
        assertNull(success.mediaSource)
        assertNull(success.campaign)
    }

    @Test
    fun toCampaignDataUnexpectedStatusReturnsError() {
        val result = mapOf("af_status" to "Unknown").toCampaignData()

        val error = assertIs<CampaignData.Error>(result)
        assertEquals("Unexpected af_status: Unknown", error.message)
    }

    @Test
    fun toCampaignDataMissingStatusReturnsError() {
        val result = mapOf("media_source" to "facebook").toCampaignData()

        val error = assertIs<CampaignData.Error>(result)
        assertEquals("Unexpected af_status: null", error.message)
    }

    @Test
    fun toCampaignDataEmptyMapReturnsError() {
        val result = emptyMap<String, Any?>().toCampaignData()

        val error = assertIs<CampaignData.Error>(result)
        assertEquals("Unexpected af_status: null", error.message)
    }

    @Test
    fun toCampaignDataPreservesRawMap() {
        val raw = mapOf(
            "af_status" to "Organic",
            "media_source" to "organic",
            "campaign" to "default",
            "extra_field" to "extra_value",
            "is_lat" to true,
        )

        val result = raw.toCampaignData()

        val success = assertIs<CampaignData.Success>(result)
        assertEquals(raw, success.raw)
    }

    @Test
    fun setUserEmailsForwardsToBackend() {
        client.setUserEmails(listOf("a@b.com", "c@d.com"), cryptType = AfEmailCryptType.SHA256)
        assertEquals(listOf("a@b.com", "c@d.com"), sdk.lastUserEmails)
        assertEquals(AfEmailCryptType.SHA256, sdk.lastEmailCryptType)
    }

    @Test
    fun setUserEmailsWithNoneCryptType() {
        client.setUserEmails(listOf("a@b.com"), cryptType = AfEmailCryptType.NONE)
        assertEquals(AfEmailCryptType.NONE, sdk.lastEmailCryptType)
    }

    @Test
    fun registerUninstallForwardsToBackend() {
        client.registerUninstall("fcm-token-123")
        assertEquals("fcm-token-123", sdk.lastUninstallToken)
    }

    @Test
    fun logAdRevenueForwardsToBackend() {
        val data = AdRevenueData(
            monetizationNetwork = "ironsource",
            mediationNetwork = AfMediationNetwork.GOOGLE_ADMOB,
            currency = "USD",
            revenue = 0.0015,
            additionalParameters = mapOf("country" to "US", "ad_unit" to "89b8c0159a50ebd1"),
        )

        client.logAdRevenue(data)

        assertEquals(data, sdk.lastAdRevenueData)
    }

    @Test
    fun logAdRevenueStripsNullAdditionalParameters() {
        val data = AdRevenueData(
            monetizationNetwork = "ironsource",
            mediationNetwork = AfMediationNetwork.IRON_SOURCE,
            currency = "EUR",
            revenue = 1.2,
            additionalParameters = mapOf("a" to "b", "c" to null, "d" to 3),
        )

        client.logAdRevenue(data)

        val forwarded = sdk.lastAdRevenueData!!
        assertEquals(mapOf("a" to "b", "d" to 3), forwarded.additionalParameters)
    }

    @Test
    fun logAdRevenueWithEmptyAdditionalParameters() {
        val data = AdRevenueData(
            monetizationNetwork = "applovin",
            mediationNetwork = AfMediationNetwork.APP_LOVIN_MAX,
            currency = "USD",
            revenue = 0.0,
        )

        client.logAdRevenue(data)

        assertEquals(emptyMap(), sdk.lastAdRevenueData?.additionalParameters)
    }

    @Test
    fun logAdRevenueAllNullAdditionalParameters() {
        val data = AdRevenueData(
            monetizationNetwork = "fyber",
            mediationNetwork = AfMediationNetwork.FYBER,
            currency = "USD",
            revenue = 0.5,
            additionalParameters = mapOf("x" to null, "y" to null),
        )

        client.logAdRevenue(data)

        assertEquals(emptyMap(), sdk.lastAdRevenueData?.additionalParameters)
    }

    @Test
    fun logAdRevenuePreservesAllCoreFields() {
        val data = AdRevenueData(
            monetizationNetwork = "topon",
            mediationNetwork = AfMediationNetwork.TOPON,
            currency = "JPY",
            revenue = 100.0,
        )

        client.logAdRevenue(data)

        val forwarded = sdk.lastAdRevenueData!!
        assertEquals("topon", forwarded.monetizationNetwork)
        assertEquals(AfMediationNetwork.TOPON, forwarded.mediationNetwork)
        assertEquals("JPY", forwarded.currency)
        assertEquals(100.0, forwarded.revenue)
    }

    @Test
    fun setAnonymizeUserForwardsToBackend() {
        client.setAnonymizeUser(true)
        assertEquals(true, sdk.lastAnonymizeUser)

        client.setAnonymizeUser(false)
        assertEquals(false, sdk.lastAnonymizeUser)
    }

    @Test
    fun setAnonymizeUserWorksBeforeStart() {
        client.setAnonymizeUser(true)

        assertEquals(0, sdk.configureCount)
        assertEquals(true, sdk.lastAnonymizeUser)
    }

    @Test
    fun setSharingFilterPartnersForwardsToBackend() {
        val partners = setOf("facebook", "google")

        client.setSharingFilterPartners(partners)

        assertEquals(partners, sdk.lastSharingFilterPartners)
    }

    @Test
    fun setSharingFilterPartnersReplacesPreviousValue() {
        client.setSharingFilterPartners(setOf("facebook"))
        client.setSharingFilterPartners(setOf("google", "twitter"))

        assertEquals(setOf("google", "twitter"), sdk.lastSharingFilterPartners)
    }

    @Test
    fun setSharingFilterPartnersEmptySetForwardsAsIs() {
        client.setSharingFilterPartners(emptySet())

        assertEquals(emptySet(), sdk.lastSharingFilterPartners)
    }

    @Test
    fun setSharingFilterPartnersWorksBeforeStart() {
        client.setSharingFilterPartners(setOf("facebook"))

        assertEquals(0, sdk.configureCount)
        assertEquals(setOf("facebook"), sdk.lastSharingFilterPartners)
    }

    @Test
    fun afPurchaseTypeIosRawValues() {
        assertEquals(0L, AfPurchaseType.SUBSCRIPTION.iosRawValue)
        assertEquals(1L, AfPurchaseType.ONE_TIME_PURCHASE.iosRawValue)
    }

    @Test
    fun validateAndLogInAppPurchaseResumesWithSuccess() = runBlocking {
        sdk.purchaseValidationResult = PurchaseValidationResult.Success(
            result = mapOf("status" to "ok"),
        )
        val details = PurchaseDetails(
            productId = "com.example.pro",
            transactionId = "txn-123",
            purchaseType = AfPurchaseType.SUBSCRIPTION,
        )

        val result = client.validateAndLogInAppPurchase(details)

        val success = assertIs<PurchaseValidationResult.Success>(result)
        assertEquals(mapOf("status" to "ok"), success.result)
    }

    @Test
    fun validateAndLogInAppPurchaseResumesWithError() = runBlocking {
        sdk.purchaseValidationResult = PurchaseValidationResult.Error(message = "network error")
        val details = PurchaseDetails(
            productId = "com.example.pro",
            transactionId = "txn-123",
            purchaseType = AfPurchaseType.ONE_TIME_PURCHASE,
        )

        val result = client.validateAndLogInAppPurchase(details)

        val error = assertIs<PurchaseValidationResult.Error>(result)
        assertEquals("network error", error.message)
    }

    @Test
    fun validateAndLogInAppPurchaseForwardsPurchaseDetailsAndParams() = runBlocking {
        sdk.purchaseValidationResult = PurchaseValidationResult.Success(emptyMap())
        val details = PurchaseDetails(
            productId = "com.example.pro",
            transactionId = "txn-456",
            purchaseType = AfPurchaseType.SUBSCRIPTION,
        )
        val params = mapOf("key" to "value", "extra" to null)

        client.validateAndLogInAppPurchase(details, params)

        assertEquals(details, sdk.lastPurchaseDetails)
        assertEquals(mapOf("key" to "value"), sdk.lastPurchaseAdditionalParams)
    }

    @Test
    fun validateAndLogInAppPurchaseErrorWithNullMessage() = runBlocking {
        sdk.purchaseValidationResult = PurchaseValidationResult.Error(message = null)
        val details = PurchaseDetails(
            productId = "p",
            transactionId = "t",
            purchaseType = AfPurchaseType.ONE_TIME_PURCHASE,
        )

        val result = client.validateAndLogInAppPurchase(details)

        val error = assertIs<PurchaseValidationResult.Error>(result)
        assertNull(error.message)
    }
}

private class FakeAppsFlyerSdk : AppsFlyerSdk {
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
