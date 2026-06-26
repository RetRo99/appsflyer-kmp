# Changelog

## 0.1.0

Initial release of appsflyer-kmp — a Kotlin Multiplatform wrapper for the AppsFlyer SDK (Android + iOS).

### Core

- Coroutine-first API: `suspend` functions for `getStartResult`, `getConversionData`, `logEventForResult`, `validateAndLogInAppPurchase`, `generateInviteUrl`
- `Flow<DeepLinkResult>` for deep link events instead of listeners
- Idempotent `start()` — safe to call multiple times
- Null values in `logEvent`, `logAdRevenue`, `setAdditionalData`, `setPartnerData`, and `validateAndLogInAppPurchase` params are silently dropped on both platforms

### SDK coverage

- Install attribution / conversion data
- OneLink deep linking (deferred and direct) with configurable timeout
- In-app event logging with delivery confirmation
- Ad revenue measurement (14 mediation networks)
- In-app purchase validation (VAL V2)
- Uninstall measurement
- GDPR/DMA consent (TCF data collection)
- Cross-promotion (impressions, open store)
- User invite links (generate URL, log invite)
- Facebook Deferred AppLinks (Android only; iOS no-op documented)
- Plugin/framework identification (9 plugins)
- Log levels (Android: 6 levels; iOS: debug toggle)
- ATT user authorization wait (iOS only)
- Session readiness listener (iOS only)
- Push notification handling (iOS only)
- Pre-install attribution, out-of-store, IMEI/OAID/Android ID (Android only)
- Sharing filter for partners, host customization, custom domains

### Platform-specific behavior

- 12 iOS-only methods (no-op on Android, documented in KDoc)
- 14 Android-only methods (no-op on iOS, documented in KDoc)
- 5 methods with documented behavioral differences between platforms (`setDeepLinkTimeout`, `setCustomerIdAndLogSession`, `setLogLevel`, `enableFacebookDeferredApplinks`, `InviteLinkParams.deeplinkPath`)
