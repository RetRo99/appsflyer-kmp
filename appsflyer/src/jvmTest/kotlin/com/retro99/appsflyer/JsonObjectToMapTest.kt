package com.retro99.appsflyer

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the production [toMap] extension that converts JSONObject to a
 * deeply-unwrapped Kotlin Map. This exercises the actual code used by the
 * Android SDK at runtime (shared via the jvmCommon source set).
 */
class JsonObjectToMapTest {

    @Test
    fun flatObjectConvertsToMap() {
        val json = JSONObject().apply {
            put("name", "test")
            put("count", 42)
            put("active", true)
        }

        val result = json.toDeepMap()

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

        val result = json.toDeepMap()

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

        val result = json.toDeepMap()

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

        val result = json.toDeepMap()

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

        val result = json.toDeepMap()

        assertEquals("value", result["present"])
        assertNull(result["absent"])
    }

    @Test
    fun emptyObjectConvertsToEmptyMap() {
        val json = JSONObject()

        val result = json.toDeepMap()

        assertEquals(emptyMap(), result)
    }

    @Test
    fun emptyArrayConvertsToEmptyList() {
        val json = JSONObject().apply {
            put("empty", JSONArray())
        }

        val result = json.toDeepMap()

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

        val result = json.toDeepMap()

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

        val result = json.toDeepMap()

        val mixed = result["mixed"] as List<*>
        assertEquals("string", mixed[0])
        assertEquals(42, mixed[1])
        assertEquals(mapOf("nested" to true), mixed[2])
        assertNull(mixed[3])
    }
}
