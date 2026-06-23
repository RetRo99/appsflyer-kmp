package com.retro99.appsflyer

import app.cash.turbine.test
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
        assertTrue(sdk.lastConfig === config)
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

        assertFalse(result != null)
        sdk.onStart?.invoke(StartResult.Success)
        deferred.join()

        assertIs<StartResult.Success>(result)
    }

    @Test
    fun getConversionDataSuspendsUntilCallbackFires() = runTest {
        client.start()

        var result: CampaignData? = null
        val deferred = async { result = client.getConversionData() }

        assertFalse(result != null)
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

    override fun getAppsFlyerUID(): String? = "fake-uid"
}
