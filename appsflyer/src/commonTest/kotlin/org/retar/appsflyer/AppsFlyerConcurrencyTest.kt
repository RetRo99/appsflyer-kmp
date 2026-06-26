package org.retar.appsflyer

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppsFlyerConcurrencyTest {

    @AfterTest
    fun tearDown() {
        AppsFlyer.resetForTesting()
    }

    @Test
    fun setClientOnlyOneWinnerUnderConcurrentAccess() = runTest {
        val results = coroutineScope {
            (1..100).map {
                async {
                    delay(1)
                    val client = AppsFlyerClientImpl(
                        FakeAppsFlyerSdk(),
                        AppsFlyerConfig(devKey = "key-$it"),
                    )
                    AppsFlyer.setClient(client)
                }
            }.awaitAll()
        }

        val winners = results.count { it }
        assertEquals(1, winners, "Exactly one setClient call should win")
        assertTrue(AppsFlyer.isInitialized)
    }

    @Test
    fun setClientReturnsFalseWhenAlreadyInitialized() = runTest {
        val first = AppsFlyerClientImpl(
            FakeAppsFlyerSdk(),
            AppsFlyerConfig(devKey = "key-1"),
        )
        val second = AppsFlyerClientImpl(
            FakeAppsFlyerSdk(),
            AppsFlyerConfig(devKey = "key-2"),
        )

        assertTrue(AppsFlyer.setClient(first))
        assertFalse(AppsFlyer.setClient(second))
    }
}
