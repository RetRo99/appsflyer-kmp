package org.retar.appsflyer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CampaignDataMappingTest {

    @Test
    fun organicStatusMapsToSuccess() {
        val raw = mapOf(
            "af_status" to "Organic",
            "media_source" to "organic",
            "campaign" to null,
        )

        val result = raw.toCampaignData()

        assertIs<CampaignData.Success>(result)
        assertEquals(AfStatus.ORGANIC, result.status)
        assertEquals("organic", result.mediaSource)
        assertEquals(null, result.campaign)
        assertEquals(raw, result.raw)
    }

    @Test
    fun nonOrganicStatusMapsToSuccess() {
        val raw = mapOf(
            "af_status" to "Non-organic",
            "media_source" to "facebook",
            "campaign" to "summer_sale",
        )

        val result = raw.toCampaignData()

        assertIs<CampaignData.Success>(result)
        assertEquals(AfStatus.NON_ORGANIC, result.status)
        assertEquals("facebook", result.mediaSource)
        assertEquals("summer_sale", result.campaign)
    }

    @Test
    fun unknownStatusMapsToError() {
        val raw = mapOf(
            "af_status" to "Unknown",
        )

        val result = raw.toCampaignData()

        assertIs<CampaignData.Error>(result)
        assertEquals("Unexpected af_status: Unknown", result.message)
    }

    @Test
    fun missingStatusMapsToError() {
        val raw = mapOf(
            "media_source" to "facebook",
        )

        val result = raw.toCampaignData()

        assertIs<CampaignData.Error>(result)
        assertEquals("Unexpected af_status: null", result.message)
    }

    @Test
    fun mediaSourceAndCampaignExtractedAsStrings() {
        val raw = mapOf(
            "af_status" to "Organic",
            "media_source" to 12345,
            "campaign" to true,
        )

        val result = raw.toCampaignData()

        assertIs<CampaignData.Success>(result)
        assertEquals("12345", result.mediaSource)
        assertEquals("true", result.campaign)
    }

    @Test
    fun emptyMapMapsToError() {
        val raw = emptyMap<String, Any?>()

        val result = raw.toCampaignData()

        assertIs<CampaignData.Error>(result)
    }
}
