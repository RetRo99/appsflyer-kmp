package com.retro99.appsflyer

interface AppsFlyerClient {
    fun start()
    fun setCustomerUserId(id: String?)
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
}

data class AppsFlyerConfig(
    val devKey: String,
    val isDebug: Boolean = false,
    val iosAppId: String? = null,
)

expect class AppsFlyerClientFactory {
    fun create(config: AppsFlyerConfig): AppsFlyerClient
}
