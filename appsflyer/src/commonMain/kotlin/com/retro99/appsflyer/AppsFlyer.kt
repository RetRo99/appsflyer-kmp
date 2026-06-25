package com.retro99.appsflyer

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Singleton entry point for the AppsFlyer KMP SDK.
 *
 * Call the platform-specific [initialize] extension before accessing [client].
 * Initialization is one-shot — subsequent calls are silently ignored.
 */
@OptIn(ExperimentalAtomicApi::class)
object AppsFlyer {

    private val _client = AtomicReference<AppsFlyerClient?>(null)

    /**
     * The initialized [AppsFlyerClient] instance.
     *
     * @throws IllegalStateException if [initialize] has not been called.
     */
    val client: AppsFlyerClient
        get() = _client.load() ?: error(
            "AppsFlyer not initialized. Call AppsFlyer.initialize() first.",
        )

    /** Returns `true` once [initialize] has completed successfully. */
    val isInitialized: Boolean
        get() = _client.load() != null

    /**
     * Sets the client if not already set. Returns `true` if this call performed
     * the initialization, `false` if a client was already set (no-op).
     *
     * Thread-safe: atomic compare-and-set guarantees only one caller wins.
     */
    internal fun setClient(client: AppsFlyerClient): Boolean =
        _client.compareAndSet(null, client)
}
