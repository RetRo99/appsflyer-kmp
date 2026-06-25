package com.retro99.appsflyer.sample

actual val currentPlatform: Platform = Platform.ANDROID

actual fun nowMillis(): Long = System.currentTimeMillis()
