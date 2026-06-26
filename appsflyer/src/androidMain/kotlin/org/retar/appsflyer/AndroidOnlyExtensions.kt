package org.retar.appsflyer

import android.app.Activity
import android.content.Intent
import com.appsflyer.AppsFlyerLib

/**
 * Performs deep link resolution on the given intent. Use this for manual
 * deep link handling. The SDK handles this automatically in most cases.
 *
 * Android only.
 */
fun AppsFlyerClient.performOnDeepLinking(intent: Intent, context: android.content.Context) {
    AppsFlyerLib.getInstance().performOnDeepLinking(intent, context)
}

/**
 * Sends push notification data from the given activity for deep link resolution.
 *
 * Android only.
 */
fun AppsFlyerClient.sendPushNotificationData(activity: Activity) {
    AppsFlyerLib.getInstance().sendPushNotificationData(activity)
}
