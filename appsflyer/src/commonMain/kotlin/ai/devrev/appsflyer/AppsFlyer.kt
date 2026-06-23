package com.retro99.appsflyer

/**
 * Singleton entry point for the AppsFlyer KMP SDK.
 *
 * Call the platform-specific [initialize] extension before accessing [client].
 * Initialization is one-shot — subsequent calls are silently ignored.
 */
object AppsFlyer {

    private val lock = Any()

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
     * Atomically sets the client. Returns `true` if this call performed the
     * initialization, `false` if a client was already set (no-op).
     */
    internal fun setClient(client: AppsFlyerClient): Boolean {
        synchronized(lock) {
            if (_client != null) return false
            _client = client
            return true
        }
    }
}
