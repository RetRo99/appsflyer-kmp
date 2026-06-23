package com.retro99.appsflyer

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the recursive JSONObject → Map conversion that mirrors the production
 * code in androidMain. Uses the same org.json library (pure-Java artifact) to
 * verify nested structures are fully unwrapped to Kotlin collections.
 */
class JsonObjectToMapTest {

    @Test
    fun flatObjectConvertsToMap() {
        val json = JSONObject().apply {
            put("name", "test")
            put("count", 42)
            put("active", true)
        }

        val result = json.recursiveToMap()

        assertEquals("test", result["name"])
        assertEquals(42, result["count"])
        assertEquals(true, result["active"])
    }

    @Test
    fun nestedObjectConvertsRecursively() {
        val inner = JSONObject().apply {
            put("city", "Ljubljana")
            put("zip", 1000)
        }
        val json = JSONObject().apply {
            put("user", "Rok")
            put("address", inner)
        }

        val result = json.recursiveToMap()

        assertEquals("Rok", result["user"])
        assertEquals(mapOf("city" to "Ljubljana", "zip" to 1000), result["address"])
    }

    @Test
    fun nestedArrayConvertsRecursively() {
        val array = JSONArray().apply {
            put("one")
            put("two")
            put("three")
        }
        val json = JSONObject().apply {
            put("tags", array)
        }

        val result = json.recursiveToMap()

        assertEquals(listOf("one", "two", "three"), result["tags"])
    }

    @Test
    fun deeplyNestedStructureConverts() {
        val deepObject = JSONObject().apply {
            put("key", "deep_value")
        }
        val innerArray = JSONArray().apply {
            put(deepObject)
            put(99)
        }
        val json = JSONObject().apply {
            put("items", innerArray)
        }

        val result = json.recursiveToMap()

        val items = result["items"] as List<*>
        assertEquals(mapOf("key" to "deep_value"), items[0])
        assertEquals(99, items[1])
    }

    @Test
    fun jsonNullConvertsToKotlinNull() {
        val json = JSONObject().apply {
            put("present", "value")
            put("absent", JSONObject.NULL)
        }

        val result = json.recursiveToMap()

        assertEquals("value", result["present"])
        assertNull(result["absent"])
    }

    @Test
    fun emptyObjectConvertsToEmptyMap() {
        val json = JSONObject()

        val result = json.recursiveToMap()

        assertEquals(emptyMap(), result)
    }

    @Test
    fun emptyArrayConvertsToEmptyList() {
        val json = JSONObject().apply {
            put("empty", JSONArray())
        }

        val result = json.recursiveToMap()

        assertEquals(emptyList<Any>(), result["empty"])
    }

    @Test
    fun arrayOfObjectsConverts() {
        val obj1 = JSONObject().apply { put("id", 1) }
        val obj2 = JSONObject().apply { put("id", 2) }
        val array = JSONArray().apply {
            put(obj1)
            put(obj2)
        }
        val json = JSONObject().apply {
            put("results", array)
        }

        val result = json.recursiveToMap()

        val results = result["results"] as List<*>
        assertEquals(mapOf("id" to 1), results[0])
        assertEquals(mapOf("id" to 2), results[1])
    }

    @Test
    fun mixedTypesInArrayConvert() {
        val nested = JSONObject().apply { put("nested", true) }
        val array = JSONArray().apply {
            put("string")
            put(42)
            put(nested)
            put(JSONObject.NULL)
        }
        val json = JSONObject().apply {
            put("mixed", array)
        }

        val result = json.recursiveToMap()

        val mixed = result["mixed"] as List<*>
        assertEquals("string", mixed[0])
        assertEquals(42, mixed[1])
        assertEquals(mapOf("nested" to true), mixed[2])
        assertNull(mixed[3])
    }
}

private fun JSONObject.recursiveToMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    keys().forEach { key -> map[key] = opt(key).unwrap() }
    return map
}

private fun Any?.unwrap(): Any? = when (this) {
    is JSONObject -> recursiveToMap()
    is JSONArray -> (0 until length()).map { i -> opt(i).unwrap() }
    JSONObject.NULL -> null
    else -> this
}
