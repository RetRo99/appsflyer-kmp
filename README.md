# appsflyer-kmp

A Kotlin Multiplatform library that wraps the native AppsFlyer SDKs (Android + iOS)
behind a single shared API. There is no official AppsFlyer KMP SDK; this library
provides one by delegating to the native SDKs via `expect`/`actual`.

## Capabilities

- **Install attribution / campaign data** — organic vs non-organic, media source, campaign name.
- **Deep linking** — OneLink resolution via the unified deep link API.
- **Custom event logging** — `logEvent(name, params)` and `logEventForResult(name, params)` with delivery confirmation.
- **Ad revenue** — `logAdRevenue(data)` with typed `AdRevenueData` and `AfMediationNetwork` enum.
- **Customer user ID** — `setCustomerUserId(id)` for attribution linking.
- **Runtime controls** — `stop()`, `setAnonymizeUser()`, `setSharingFilterPartners()`.
- **GDPR/DMA consent** — Pre-start consent via `AppsFlyerConfig.consentData` using `AppsFlyerConsent`.
- **TCF data collection** — Pre-start toggle via `AppsFlyerConfig.enableTCFDataCollection`.

## Shared API

```kotlin
interface AppsFlyerClient {
    fun start()
    suspend fun getStartResult(): StartResult
    suspend fun getConversionData(): CampaignData
    fun setCustomerUserId(id: String?)
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
    suspend fun logEventForResult(name: String, params: Map<String, Any?> = emptyMap()): LogEventResult
    fun logAdRevenue(data: AdRevenueData)
    fun setAnonymizeUser(enabled: Boolean)
    fun setSharingFilterPartners(partners: Set<String>)
    fun getAppsFlyerUID(): String?
    fun stop(stop: Boolean = true)
    val isStopped: Boolean
    val deepLink: Flow<DeepLinkResult>
}
```

### Configuration

```kotlin
data class AppsFlyerConfig(
    val devKey: String,
    val isDebug: Boolean = false,
    val iosAppId: String? = null,
    val collectAndroidId: Boolean = false,
    val anonymizeUser: Boolean = false,
    val enableTCFDataCollection: Boolean = false,
    val consentData: AppsFlyerConsent? = null,
    val sharingFilterPartners: Set<String> = emptySet(),
)
```

`anonymizeUser`, `consentData`, `enableTCFDataCollection`, and `sharingFilterPartners`
are applied during SDK initialization (after `init()`, before `start()`). The
runtime methods `setAnonymizeUser()` and `setSharingFilterPartners()` can be
called at any time after start to update these values dynamically.

### Result types

```kotlin
sealed interface StartResult {
    data object Success : StartResult
    data class Error(val code: Int, val message: String) : StartResult
}

sealed interface LogEventResult {
    data object Success : LogEventResult
    data class Error(val code: Int, val message: String) : LogEventResult
}

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
```

### Ad revenue

```kotlin
data class AdRevenueData(
    val monetizationNetwork: String,
    val mediationNetwork: AfMediationNetwork,
    val currency: String,
    val revenue: Double,
    val additionalParameters: Map<String, Any?> = emptyMap(),
)

enum class AfMediationNetwork {
    GOOGLE_ADMOB,
    IRON_SOURCE,
    APP_LOVIN_MAX,
    FYBER,
    APPODEAL,
    ADMOST,
    TOPON,
    TRADPLUS,
    YANDEX,
    CHARTBOOST,
    UNITY,
    TOPON_PTE,
    CUSTOM_MEDIATION,
    DIRECT_MONETIZATION,
}
```

### Consent

```kotlin
data class AppsFlyerConsent(
    val isUserSubjectToGDPR: Boolean? = null,
    val hasConsentForDataUsage: Boolean? = null,
    val hasConsentForAdsPersonalization: Boolean? = null,
    val hasConsentForAdStorage: Boolean? = null,
) {
    companion object {
        fun forNonGDPRUser() = AppsFlyerConsent(isUserSubjectToGDPR = false)

        fun forGDPRUser(
            hasConsentForDataUsage: Boolean,
            hasConsentForAdsPersonalization: Boolean,
        ) = AppsFlyerConsent(
            isUserSubjectToGDPR = true,
            hasConsentForDataUsage = hasConsentForDataUsage,
            hasConsentForAdsPersonalization = hasConsentForAdsPersonalization,
        )
    }
}
```

## Android Setup

### 1. Add dependency

```kotlin
dependencies {
    implementation("io.github.retro99:appsflyer-kmp:$VERSION")
}
```

### 2. Initialize in your Activity

> **Note:** Pass the Activity context so the SDK can read the incoming intent for
> deep link resolution. The library uses `applicationContext` internally for
> long-lived operations to avoid memory leaks.

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppsFlyer.initialize(this, AppsFlyerConfig(
            devKey = "YOUR_AF_DEV_KEY",
            isDebug = BuildConfig.DEBUG,
            collectAndroidId = true,
        ))
    }
}
```

### 3. Add intent filter for deep links

In your `AndroidManifest.xml`, add an intent filter to the Activity that handles
deep links:

```xml
<activity android:name=".MainActivity">
    <!-- App Links (Universal Links equivalent) -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="https"
            android:host="yoursubdomain.onelink.me" />
    </intent-filter>

    <!-- URI scheme fallback -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="yourscheme" />
    </intent-filter>
</activity>
```

### 4. Observe results and start

> **Important:** Set up deep link collectors **before** calling `start()` to
> avoid missing the initial deep link (the flow does not replay past emissions).

```kotlin
// 1. Subscribe to deep links first
scope.launch {
    AppsFlyer.client.deepLink.collect { result ->
        when (result) {
            is DeepLinkResult.Found -> navigate(result.deepLinkValue)
            is DeepLinkResult.NotFound -> { /* no link */ }
            is DeepLinkResult.Error -> log("Deep link error: ${result.message}")
        }
    }
}

// 2. Then start the SDK
AppsFlyer.client.start()

// 3. Observe one-shot results (these use replay, so order doesn't matter)
scope.launch {
    when (val result = AppsFlyer.client.getStartResult()) {
        is StartResult.Success -> log("SDK started")
        is StartResult.Error -> log("SDK failed: ${result.message}")
    }
}

scope.launch {
    when (val data = AppsFlyer.client.getConversionData()) {
        is CampaignData.Success -> log("Attribution: ${data.status}")
        is CampaignData.Error -> log("Conversion error: ${data.message}")
    }
}
```

**That's it for Android.** Deep links are handled automatically via the intent filter — no extra wiring needed.

---

## iOS Setup

### 1. Add dependency

Same Gradle dependency as Android (shared KMP module).

### 2. Add AppsFlyer SPM package to your Xcode project

- URL: `https://github.com/AppsFlyerSDK/AppsFlyerFramework`
- Version: `7.0.0`
- Product: `AppsFlyerLib`

### 3. Initialize and forward deep links

```swift
import SwiftUI
import AppsFlyerKmp // or your shared framework name if you re-export the module

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        AppsFlyer.shared.initialize(config: AppsFlyerConfig(
            devKey: "YOUR_AF_DEV_KEY",
            isDebug: true,
            iosAppId: "YOUR_APPLE_APP_ID"
        ))
        return true
    }
}

@main
struct MyApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    AppsFlyer.shared.linkHandler.handleOpenUrl(url: url)
                }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    AppsFlyer.shared.linkHandler.handleUserActivity(userActivity: activity)
                }
        }
    }
}
```

- **`.onOpenURL`** handles URI scheme deep links (e.g. `yourscheme://...`)
- **`.onContinueUserActivity`** handles Universal Links (e.g. `https://yoursubdomain.onelink.me/...`)

For UIKit apps, use the equivalent AppDelegate methods instead:

```swift
func application(
    _ app: UIApplication,
    open url: URL,
    options: [UIApplication.OpenURLOptionsKey: Any] = [:]
) -> Bool {
    AppsFlyer.shared.linkHandler.handleOpenUrl(url: url)
    return true
}

func application(
    _ application: UIApplication,
    continue userActivity: NSUserActivity,
    restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void
) -> Bool {
    AppsFlyer.shared.linkHandler.handleUserActivity(userActivity: userActivity)
    return true
}
```

### 4. Configure URL scheme and Associated Domains

**Info.plist** — add your URI scheme:
```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>yourscheme</string>
        </array>
    </dict>
</array>
```

**Entitlements** — add Associated Domains for Universal Links:
```xml
<key>com.apple.developer.associated-domains</key>
<array>
    <string>applinks:yoursubdomain.onelink.me</string>
</array>
```

### 5. Observe results and start

Same as Android — subscribe to deep links first, then call `start()` from shared code:

```kotlin
scope.launch {
    AppsFlyer.client.deepLink.collect { result ->
        when (result) {
            is DeepLinkResult.Found -> navigate(result.deepLinkValue)
            is DeepLinkResult.NotFound -> { /* no link */ }
            is DeepLinkResult.Error -> log("Deep link error: ${result.message}")
        }
    }
}

AppsFlyer.client.start()
```

---

## Usage Examples

### Logging events

```kotlin
// Fire-and-forget
AppsFlyer.client.logEvent("purchase", mapOf(
    "price" to 9.99,
    "currency" to "USD",
))

// With delivery confirmation
scope.launch {
    when (val result = AppsFlyer.client.logEventForResult("purchase", mapOf(
        "price" to 9.99,
        "currency" to "USD",
    ))) {
        is LogEventResult.Success -> log("Event sent")
        is LogEventResult.Error -> log("Event failed: ${result.message}")
    }
}
```

Null values in params are silently dropped on both methods.

### Logging ad revenue

```kotlin
AppsFlyer.client.logAdRevenue(AdRevenueData(
    monetizationNetwork = "ironsource",
    mediationNetwork = AfMediationNetwork.GOOGLE_ADMOB,
    currency = "USD",
    revenue = 0.0015,
    additionalParameters = mapOf(
        "country" to "US",
        "ad_unit" to "89b8c0159a50ebd1",
        "ad_type" to "Banner",
        "placement" to "place",
    ),
))
```

### GDPR/DMA consent

Set consent data before starting the SDK:

```kotlin
AppsFlyer.initialize(context, AppsFlyerConfig(
    devKey = "YOUR_AF_DEV_KEY",
    iosAppId = "YOUR_APPLE_APP_ID",
    consentData = AppsFlyerConsent.forGDPRUser(
        hasConsentForDataUsage = true,
        hasConsentForAdsPersonalization = false,
    ),
    enableTCFDataCollection = true,
))
```

For users not subject to GDPR:

```kotlin
AppsFlyer.initialize(context, AppsFlyerConfig(
    devKey = "YOUR_AF_DEV_KEY",
    iosAppId = "YOUR_APPLE_APP_ID",
    consentData = AppsFlyerConsent.forNonGDPRUser(),
))
```

### Runtime controls

```kotlin
// Toggle anonymization
AppsFlyer.client.setAnonymizeUser(true)

// Exclude specific partners from data sharing
AppsFlyer.client.setSharingFilterPartners(setOf("partner1_int", "partner2_int"))

// Stop SDK data collection
AppsFlyer.client.stop()
// Re-enable
AppsFlyer.client.stop(false)

// Check if SDK is stopped
if (AppsFlyer.client.isStopped) { /* ... */ }

// Get AppsFlyer device ID (null before start)
val uid = AppsFlyer.client.getAppsFlyerUID()
```

## API Design

- **`start()`** — fire-and-forget, callable from `Application.onCreate()` or
  `AppDelegate` without a coroutine scope.
- **`getStartResult()` / `getConversionData()`** — suspend functions for one-shot
  results. Idempotent: subsequent calls return the cached result immediately.
  Must be called after `start()`; suspends indefinitely otherwise.
- **`deepLink`** — a `Flow` for ongoing deep link events (including re-engagement).
  Does not replay past emissions; collect before calling `start()` to avoid
  missing the initial deep link.
- **`logEvent()`** — null values in params are silently dropped.
- **`logEventForResult()`** — suspends until the SDK confirms delivery; null
  values in params are silently dropped.
- **`logAdRevenue()`** — fire-and-forget; null values in `additionalParameters`
  are silently dropped.
- **`setCustomerUserId(null)`** — passes null to the native SDK on both platforms.
- **`setAnonymizeUser()` / `setSharingFilterPartners()`** — runtime updates for
  values initially set in `AppsFlyerConfig`. Can be called any time after start.
- **`stop()`** — stops all SDK data collection and server communication.
  Pass `false` to re-enable.
- **`getAppsFlyerUID()`** — returns null before the SDK has started.

## Platform Comparison

| | Android | iOS |
|---|---|---|
| Initialize | 2 lines (Activity context) | 2 lines |
| Deep link wiring | None (automatic via intent) | 1 line (`.onOpenURL` or AppDelegate method) |
| Config files | Intent filter in manifest | URL scheme in Info.plist + entitlements |
| SPM/SDK dependency | Transitive (automatic) | Must add `AppsFlyerLib` via SPM manually |

## Project Structure

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
    demo-app/                      # Compose Multiplatform demo (Android + iOS)
```

## Running the Demo App Locally

The demo app reads AppsFlyer credentials from files that are **gitignored** so
secrets are never committed.

### 1. Add credentials to `local.properties` (Android)

In the project root `local.properties` (already in `.gitignore`), add:

```properties
appsflyer.devKey=YOUR_AF_DEV_KEY
appsflyer.iosAppId=YOUR_APPLE_APP_ID
```

The Android `build.gradle.kts` reads these and injects them as `BuildConfig.AF_DEV_KEY`.

### 2. Add credentials to `Secrets.xcconfig` (iOS)

Create `samples/demo-app/iosApp/Secrets.xcconfig` (also gitignored):

```
AF_DEV_KEY = YOUR_AF_DEV_KEY
AF_IOS_APP_ID = YOUR_APPLE_APP_ID
```

The values are exposed via `Info.plist` and read at runtime with
`Bundle.main.object(forInfoDictionaryKey:)`.

### 3. Run

```bash
# Android
./gradlew :samples:demo-app:androidApp:assembleDebug

# iOS (via Xcode)
cd samples/demo-app/iosApp && xcodegen && open iosApp.xcodeproj
```

---

## Publishing

- **Android:** Maven publication (group `com.retro99.appsflyer`).
- **iOS:** KMP klib + framework. Consumers must add AppsFlyer via SPM separately
  (KMP does not propagate iOS frameworks transitively).

The library version is controlled via `LIBRARY_VERSION` in `gradle.properties`.
