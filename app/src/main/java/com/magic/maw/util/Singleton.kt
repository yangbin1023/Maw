package com.magic.maw.util

import android.os.Handler
import android.os.HandlerThread
import com.tencent.mmkv.MMKV
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val kv by lazy { MMKV.mmkvWithID("config") }

val json by lazy {
    Json {
        ignoreUnknownKeys = true
        explicitNulls = true
        encodeDefaults = true
    }
}

val client by lazy {
    HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json, ContentType.Any)
        }
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
        engine {
            config {
//                proxy()
            }
        }
    }
}

private val dbHandlerThread: HandlerThread by lazy {
    HandlerThread("DBHandler")
}

val dbHandler by lazy {
    dbHandlerThread.start()
    Handler(dbHandlerThread.looper)
}