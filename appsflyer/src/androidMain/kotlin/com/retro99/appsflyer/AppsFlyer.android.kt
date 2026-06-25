package com.retro99.appsflyer

import android.content.Context

/**
 * Initializes AppsFlyer on Android.
 *
 * Must be called from an Activity's `onCreate` — the SDK reads the Activity's
 * incoming intent for deep link resolution. Calling with Application context
 * will prevent deep links from being detected.
 *
 * Safe to call multiple times; only the first call takes effect.
 *
 * @param context the Activity context (required for deep link intent reading).
 * @param config SDK configuration including dev key and debug flag.
 */
fun AppsFlyer.initialize(context: Context, config: AppsFlyerConfig) {
    val client = AppsFlyerClientFactory(context).create(config)
    setClient(client)
}
