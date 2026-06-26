package org.retar.appsflyer

import android.content.Context

/**
 * Initializes AppsFlyer on Android.
 *
 * Recommended to call from an Activity's `onCreate` — the SDK reads the
 * Activity's incoming intent for deep link resolution. Calling with
 * Application context will work but deep links may not be detected.
 *
 * Safe to call multiple times; only the first call takes effect.
 *
 * @param context the Activity context (recommended for deep link intent reading).
 * @param config SDK configuration including dev key and debug flag.
 */
fun AppsFlyer.initialize(context: Context, config: AppsFlyerConfig) {
    val client = AppsFlyerClientFactory(context).create(config)
    setClient(client)
}
