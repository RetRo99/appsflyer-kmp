package com.retro99.appsflyer

internal actual class AppsFlyerClientFactory {
    actual fun create(config: AppsFlyerConfig): AppsFlyerClient {
        throw UnsupportedOperationException(
            "AppsFlyer is not supported on JVM. Use the Android or iOS target.",
        )
    }
}
