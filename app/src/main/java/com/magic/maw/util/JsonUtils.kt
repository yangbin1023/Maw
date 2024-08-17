package com.magic.maw.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonUtils {
    val gson = Gson()

    /**
     * fromJson2List
     */
    inline fun <reified T> fromJson2List(json: String) = fromJson<List<T>>(json)

    /**
     * fromJson
     */
    inline fun <reified T> fromJson(json: String): T? {
        return try {
            val type = object : TypeToken<T>() {}.type
            return gson.fromJson(json, type)
        } catch (e: Exception) {
            println("try exception,${e.message}")
            null
        }
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T? {
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            println("try exception,${e.message}")
            null
        }
    }

    fun toJson(src: Any): String {
        return try {
            return gson.toJson(src)
        } catch (e: Exception) {
            println("try exception,${e.message}")
            ""
        }
    }
}