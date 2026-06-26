package org.retar.appsflyer.sample

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual val currentPlatform: Platform = Platform.IOS

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
