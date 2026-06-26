package org.retar.appsflyer.sample

enum class Platform { ANDROID, IOS }

fun formatTimestamp(millis: Long): String {
    val totalSeconds = millis / 1000
    val ms = millis % 1000
    val h = (totalSeconds / 3600) % 24
    val m = (totalSeconds / 60) % 60
    val s = totalSeconds % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}.${ms.toString().padStart(3, '0')}"
}
