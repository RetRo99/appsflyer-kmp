# appsflyer-kmp

A Kotlin Multiplatform library that wraps the native AppsFlyer SDKs (Android + iOS)
behind a single shared API. There is no official AppsFlyer KMP SDK; this library
provides one by delegating to the native SDKs via `expect`/`actual`.

## Capabilities

- **Install attribution / campaign data** — organic vs non-organic, media source, campaign name.
- **Deep linking** — OneLink resolution via the unified deep link API.
- **Custom event logging** — `logEvent(name, params)` passthrough.
- **Customer user ID** — `setCustomerUserId(id)` for attribution linking.

## Shared API

```kotlin
interface AppsFlyerClient {
    fun start()
    suspend fun getStartResult(): StartResult
    suspend fun getConversionData(): CampaignData
    fun setCustomerUserId(id: String?)
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
    val deepLink: Flow<DeepLinkResult>
}

data class AppsFlyerConfig(
    val devKey: String,
    val isDebug: Boolean = false,
    val iosAppId: String? = null,
    val collectAndroidId: Boolean = false,
)

sealed interface CampaignData {
    data class Success(
        val status: AfStatus,
        val mediaSource: String?,
        val campaign: String?,
        val raw: Map<String, Any?>,
    ) : CampaignData

    data class Error(val message: String?) : CampaignData
}

enum class AfStatus { ORGANIC, NON_ORGANIC }

sealed interface DeepLinkResult {
    data class Found(
        val deepLinkValue: String?,
        val isDeferred: Boolean,
        val mediaSource: String?,
        val campaign: String?,
        val raw: Map<String, Any?>,
    ) : DeepLinkResult

    data object NotFound : DeepLinkResult

    data class Error(val message: String?) : DeepLinkResult
}

sealed interface StartResult {
    data object Success : StartResult
    data class Error(val code: Int, val message: String) : StartResult
}
```

## Android setup

```kotlin
dependencies {
    implementation("io.github.retro99:appsflyer-kmp:$VERSION")
}
```

```kotlin
// In Application.onCreate()
val client = AppsFlyerClientFactory(context).create(
    AppsFlyerConfig(devKey = "YOUR_AF_DEV_KEY", isDebug = BuildConfig.DEBUG),
)
client.setCustomerUserId("user-123")
client.start()

// In a coroutine scope
scope.launch {
    when (val result = client.getStartResult()) {
        is StartResult.Success -> log("SDK started")
        is StartResult.Error -> log("SDK failed: ${result.message}")
    }
}

scope.launch {
    when (val data = client.getConversionData()) {
        is CampaignData.Success -> log("Attribution: ${data.status}")
        is CampaignData.Error -> log("Conversion error: ${data.message}")
    }
}

scope.launch {
    client.deepLink.collect { result ->
        when (result) {
            is DeepLinkResult.Found -> navigate(result.deepLinkValue)
            is DeepLinkResult.NotFound -> { /* no link */ }
            is DeepLinkResult.Error -> log("Deep link error: ${result.message}")
        }
    }
}
```

## iOS setup

1. Add the Gradle dependency (same as Android).
2. In your Xcode project, add the AppsFlyer SPM package:
   - URL: `https://github.com/AppsFlyerSDK/AppsFlyerFramework`
   - Product: `AppsFlyerLib`
3. Initialize in shared Kotlin code:

```kotlin
val client = AppsFlyerClientFactory().create(
    AppsFlyerConfig(
        devKey = "YOUR_AF_DEV_KEY",
        iosAppId = "YOUR_APPLE_APP_ID", // required on iOS
        isDebug = true,
    ),
)
client.start()
```

Note: `iosAppId` is required on iOS and validated at `start()` time. The iOS
factory takes no constructor arguments (unlike Android which requires `Context`).

## API design

- **`start()`** — fire-and-forget, callable from `Application.onCreate()` or
  `AppDelegate` without a coroutine scope.
- **`getStartResult()` / `getConversionData()`** — suspend functions for one-shot
  results. Idempotent: subsequent calls return the cached result immediately.
  Must be called after `start()`; suspends indefinitely otherwise.
- **`deepLink`** — a `Flow` for ongoing deep link events (including re-engagement).
  Does not replay past emissions; collect before calling `start()` to avoid
  missing the initial deep link.
- **`logEvent()`** — null values in params are silently dropped.
- **`setCustomerUserId(null)`** — passes null to the native SDK on both platforms.

## Project structure

```
appsflyer-kmp/
  settings.gradle.kts
  gradle/libs.versions.toml
  appsflyer/                       # the library module
    build.gradle.kts
    src/
      commonMain/                  # shared API + expect factory
      androidMain/                 # actual -> af-android-sdk
      iosMain/                     # actual -> AppsFlyerLib via spm4Kmp bridge
      swift/AppsFlyerBridge/       # Swift bridge wrapping the iOS delegates
  samples/
    android-sample/                # Android sample app
```

## Publishing

- **Android:** Maven publication (group `com.retro99.appsflyer`).
- **iOS:** KMP klib + framework. Consumers must add AppsFlyer via SPM separately
  (KMP does not propagate iOS frameworks transitively).

The library version is controlled via `LIBRARY_VERSION` in `gradle.properties`.
