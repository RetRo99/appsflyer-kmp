package com.retro99.appsflyer

actual class AppsFlyerClientFactory {
    actual fun create(config: AppsFlyerConfig): AppsFlyerClient {
        throw UnsupportedOperationException("JVM target is for testing only.")
    }
}
