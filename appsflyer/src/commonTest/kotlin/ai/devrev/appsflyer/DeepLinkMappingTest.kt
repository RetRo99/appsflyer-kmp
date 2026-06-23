package com.retro99.appsflyer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepLinkMappingTest {

    @Test
    fun foundMapsAllFields() {
        val raw = mapOf("deep_link_value" to "product_123")

        val result = mapDeepLinkResult(
            status = DeepLinkStatus.FOUND,
            deepLinkValue = "product_123",
            isDeferred = false,
            mediaSource = "email",
            campaign = "welcome",
            raw = raw,
        )

        assertIs<DeepLinkResult.Found>(result)
        assertEquals("product_123", result.deepLinkValue)
        assertFalse(result.isDeferred)
        assertEquals("email", result.mediaSource)
        assertEquals("welcome", result.campaign)
        assertEquals(raw, result.raw)
    }

    @Test
    fun foundCoercesNullIsDeferredToFalse() {
        val result = mapDeepLinkResult(
            status = DeepLinkStatus.FOUND,
            isDeferred = null,
        )

        assertIs<DeepLinkResult.Found>(result)
        assertFalse(result.isDeferred)
    }

    @Test
    fun foundKeepsTrueIsDeferred() {
        val result = mapDeepLinkResult(
            status = DeepLinkStatus.FOUND,
            isDeferred = true,
        )

        assertIs<DeepLinkResult.Found>(result)
        assertTrue(result.isDeferred)
    }

    @Test
    fun foundDefaultsOptionalFieldsToNullAndEmptyRaw() {
        val result = mapDeepLinkResult(DeepLinkStatus.FOUND)

        assertIs<DeepLinkResult.Found>(result)
        assertNull(result.deepLinkValue)
        assertFalse(result.isDeferred)
        assertNull(result.mediaSource)
        assertNull(result.campaign)
        assertEquals(emptyMap(), result.raw)
    }

    @Test
    fun notFoundIgnoresOtherFields() {
        val result = mapDeepLinkResult(
            status = DeepLinkStatus.NOT_FOUND,
            deepLinkValue = "ignored",
            isDeferred = true,
            mediaSource = "ignored",
        )

        assertIs<DeepLinkResult.NotFound>(result)
    }

    @Test
    fun errorIncludesMessage() {
        val result = mapDeepLinkResult(
            status = DeepLinkStatus.ERROR,
            error = "Timeout",
        )

        assertIs<DeepLinkResult.Error>(result)
        assertEquals("Timeout", result.message)
    }

    @Test
    fun errorWithNullMessage() {
        val result = mapDeepLinkResult(DeepLinkStatus.ERROR)

        assertIs<DeepLinkResult.Error>(result)
        assertNull(result.message)
    }
}
