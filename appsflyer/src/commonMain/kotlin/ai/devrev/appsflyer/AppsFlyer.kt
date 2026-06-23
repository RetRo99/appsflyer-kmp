package com.retro99.appsflyer

import kotlin.concurrent.Volatile

/**
 * Singleton entry point for the AppsFlyer KMP SDK.
 *
 * Call the platform-specific [initialize] extension before accessing [client].
 * Initialization is one-shot — subsequent calls are silently ignored.
 */
object AppsFlyer {

    @Volatile
    private var _client: AppsFlyerClient? = null

    /**
     * The initialized [AppsFlyerClient] instance.
     *
     * @throws IllegalStateException if [initialize] has not been called.
     */
    val client: AppsFlyerClient
        get() = _client ?: error(
            "AppsFlyer not initialized. Call AppsFlyer.initialize() first.",
        )

    /** Returns `true` once [initialize] has completed successfully. */
    val isInitialized: Boolean
        get() = _client != null

    /**
     * Sets the client if not already set. Returns `true` if this call performed
     * the initialization, `false` if a client was already set (no-op).
     *
     * Thread-safe: only one caller wins; subsequent calls return false.
     */
    internal fun setClient(client: AppsFlyerClient): Boolean {
        if (_client != null) return false
        _client = client
        return true
    }
}
