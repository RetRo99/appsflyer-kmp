package org.retar.appsflyer

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.toDeepMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    keys().forEach { key -> map[key] = opt(key).unwrap() }
    return map.toMap()
}

private fun Any?.unwrap(): Any? = when (this) {
    is JSONObject -> toDeepMap()
    is JSONArray -> (0 until length()).map { i -> opt(i).unwrap() }
    JSONObject.NULL -> null
    else -> this
}
