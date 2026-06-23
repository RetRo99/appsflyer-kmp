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

## Android Setup

### 1. Add dependency

```kotlin
dependencies {
    implementation("io.github.retro99:appsflyer-kmp:$VERSION")
}
```

### 2. Initialize in your deep-link Activity

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
            iosAppId: "YOUR_APPLE_APP_ID",
            collectAndroidId: false
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

## Platform Comparison

| | Android | iOS |
|---|---|---|
| Initialize | 2 lines (Activity context) | 2 lines |
| Deep link wiring | None (automatic via intent) | 1 line (`.onOpenURL` or AppDelegate method) |
| Config files | Intent filter in manifest | URL scheme in Info.plist + entitlements |
| SPM/SDK dependency | Transitive (automatic) | Must add `AppsFlyerLib` via SPM manually |

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
- **`setCustomerUserId(null)`** — passes null to the native SDK on both platforms.

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
