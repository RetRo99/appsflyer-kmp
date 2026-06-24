# appsflyer-kmp

A Kotlin Multiplatform wrapper for the AppsFlyer SDK (Android + iOS). There is no
official AppsFlyer KMP SDK — this library provides one by delegating to the
native SDKs via `expect`/`actual`, with a coroutine-first API.

## Capabilities

- Install attribution / conversion data
- OneLink deep linking (deferred and direct)
- In-app event logging with delivery confirmation
- Ad revenue measurement
- In-app purchase validation (VAL V2)
- Uninstall measurement
- GDPR/DMA consent
- 1:1 API parity with the native SDKs

## Kotlin-first design

Asynchronous operations use suspending functions instead of callbacks:

```kotlin
suspend fun getStartResult(): StartResult
suspend fun getConversionData(): CampaignData
suspend fun logEventForResult(name: String, params: Map<String, Any?>): LogEventResult
suspend fun validateAndLogInAppPurchase(...): PurchaseValidationResult
```

Deep link events use a `Flow` instead of listeners:

```kotlin
val deepLink: Flow<DeepLinkResult>
```

Null values in `logEvent`, `logAdRevenue`, `setAdditionalData`, `setPartnerData`,
and `validateAndLogInAppPurchase` params are silently dropped on both platforms.

## Setup

### Android

```kotlin
dependencies {
    implementation("io.github.retro99:appsflyer-kmp:$VERSION")
}
```

```kotlin
AppsFlyer.initialize(this, AppsFlyerConfig(
    devKey = "YOUR_AF_DEV_KEY",
    isDebug = BuildConfig.DEBUG,
))
```

Add an intent filter for deep links in your `AndroidManifest.xml` (standard
AppsFlyer OneLink configuration). That's it — the SDK handles the rest.

### iOS

Add the AppsFlyer SPM package to your Xcode project:
- URL: `https://github.com/AppsFlyerSDK/AppsFlyerFramework`
- Version: `7.0.0`
- Product: `AppsFlyerLib`

```swift
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        AppsFlyer.shared.initialize(
            config: AppsFlyerConfig(
                devKey: "YOUR_AF_DEV_KEY",
                isDebug: true,
                iosAppId: "YOUR_APPLE_APP_ID"
            ),
            launchOptions: launchOptions
        )
        return true
    }
}
```

Forward deep links from your SwiftUI views or AppDelegate:

```swift
.onOpenURL { url in AppsFlyer.shared.linkHandler.handleOpenUrl(url: url) }
.onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
    AppsFlyer.shared.linkHandler.handleUserActivity(userActivity: activity)
}
```

## Usage

```kotlin
// Subscribe to deep links before starting
scope.launch {
    AppsFlyer.client.deepLink.collect { result ->
        when (result) {
            is DeepLinkResult.Found -> navigate(result.deepLinkValue)
            is DeepLinkResult.NotFound -> { }
            is DeepLinkResult.Error -> log(result.message)
        }
    }
}

// Start the SDK
AppsFlyer.client.start()

// Observe attribution
scope.launch {
    when (val data = AppsFlyer.client.getConversionData()) {
        is CampaignData.Success -> log("${data.status} — ${data.mediaSource}")
        is CampaignData.Error -> log(data.message)
    }
}

// Log events
AppsFlyer.client.logEvent("purchase", mapOf("price" to 9.99))

// Ad revenue
AppsFlyer.client.logAdRevenue(AdRevenueData(
    monetizationNetwork = "ironsource",
    mediationNetwork = AfMediationNetwork.GOOGLE_ADMOB,
    currency = "USD",
    revenue = 0.0015,
))

// In-app purchase validation
scope.launch {
    val result = AppsFlyer.client.validateAndLogInAppPurchase(
        PurchaseDetails(
            productId = "com.example.pro",
            transactionId = "txn-123",
            purchaseType = AfPurchaseType.SUBSCRIPTION,
        ),
    )
}

// GDPR consent
AppsFlyer.initialize(context, AppsFlyerConfig(
    devKey = "YOUR_AF_DEV_KEY",
    consentData = AppsFlyerConsent.forGDPRUser(
        hasConsentForDataUsage = true,
        hasConsentForAdsPersonalization = false,
    ),
))

// Uninstall measurement
AppsFlyer.client.registerUninstall(fcmToken)
```

## Platform-specific behavior

APIs that exist on only one platform are no-ops on the other (e.g.
`setDisableSKAdNetwork` is iOS-only, `setCollectIMEI` is Android-only).
Android-only extensions that require `Context`/`Activity`/`Intent`:

```kotlin
fun AppsFlyerClient.performOnDeepLinking(intent: Intent, context: Context)
fun AppsFlyerClient.sendPushNotificationData(activity: Activity)
```

## Project structure

```
appsflyer/
  src/
    commonMain/    # shared API + implementation
    androidMain/   # delegates to af-android-sdk
    iosMain/       # delegates to AppsFlyerLib via Swift bridge
    swift/         # Swift bridge wrapping iOS delegates
samples/
  demo-app/        # Compose Multiplatform demo
```

## Demo app

Credentials are gitignored. Add them to:

- `local.properties`: `appsflyer.devKey=...` / `appsflyer.iosAppId=...`
- `samples/demo-app/iosApp/Secrets.xcconfig`: `AF_DEV_KEY = ...` / `AF_IOS_APP_ID = ...`

```bash
# Android
./gradlew :samples:demo-app:androidApp:assembleDebug

# iOS
cd samples/demo-app/iosApp && xcodegen && open iosApp.xcodeproj
```

## Publishing

- **Android:** Maven publication (group `com.retro99.appsflyer`).
- **iOS:** KMP klib + framework. Consumers must add `AppsFlyerLib` via SPM separately.

Library version is controlled via `LIBRARY_VERSION` in `gradle.properties`.
