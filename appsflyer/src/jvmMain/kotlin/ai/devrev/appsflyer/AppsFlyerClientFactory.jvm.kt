package com.retro99.appsflyer

internal actual class AppsFlyerClientFactory {
    actual fun create(config: AppsFlyerConfig): AppsFlyerClient {
        throw UnsupportedOperationException("JVM target is for testing only.")
    }
}
