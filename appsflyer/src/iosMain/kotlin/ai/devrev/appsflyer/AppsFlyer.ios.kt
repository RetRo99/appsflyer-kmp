@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.retro99.appsflyer

import AppsFlyerBridge.AppsFlyerBridge
import kotlin.concurrent.Volatile
import platform.Foundation.NSURL
import platform.Foundation.NSUserActivity

/**
 * Forwards deep link URLs to the AppsFlyer SDK on iOS.
 *
 * Wire this into your SwiftUI `.onOpenURL` / `.onContinueUserActivity` or
 * the equivalent UIKit AppDelegate methods.
 */
class AppsFlyerLinkHandler internal constructor(
    private val bridge: AppsFlyerBridge,
) {
    /**
     * Forwards a URI-scheme deep link to the SDK.
     *
     * @param url the URL received in `application(_:open:options:)` or `.onOpenURL`.
     * @param options the UIApplication open-URL options dictionary (pass from
     *   AppDelegate for best attribution accuracy; omit in SwiftUI where
     *   options are unavailable).
     */
    fun handleOpenUrl(url: NSURL, options: Map<Any?, *>? = null) {
        bridge.handleOpenUrl(url, options = options)
    }

    /**
     * Forwards a Universal Link (NSUserActivity) to the SDK.
     *
     * @param userActivity the activity from `application(_:continue:restorationHandler:)`
     *   or `.onContinueUserActivity`.
     */
    fun handleUserActivity(userActivity: NSUserActivity) {
        bridge.handleUniversalLink(userActivity)
    }
}

/**
 * The link handler for forwarding deep link URLs to the AppsFlyer SDK.
 *
 * @throws IllegalStateException if [initialize] has not been called.
 */
val AppsFlyer.linkHandler: AppsFlyerLinkHandler
    get() = _linkHandler ?: error(
        "AppsFlyer not initialized. Call AppsFlyer.initialize() first.",
    )

@Volatile
private var _linkHandler: AppsFlyerLinkHandler? = null

/**
 * Initializes AppsFlyer on iOS.
 *
 * Call from `AppDelegate.didFinishLaunchingWithOptions` or the SwiftUI app's
 * init. After this, use [AppsFlyer.linkHandler] to forward deep link URLs.
 *
 * Safe to call multiple times; only the first call takes effect.
 *
 * @param config SDK configuration including dev key, app ID, and debug flag.
 */
fun AppsFlyer.initialize(config: AppsFlyerConfig) {
    if (isInitialized) return
    val sdk = IosAppsFlyerSdk()
    val client = AppsFlyerClientImpl(sdk, config)
    _linkHandler = AppsFlyerLinkHandler(sdk.bridge)
    if (!setClient(client)) {
        _linkHandler = null
    }
}
