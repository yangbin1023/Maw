package com.magic.maw.util

import android.annotation.SuppressLint
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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

val kv by lazy { MMKV.mmkvWithID("config") }

val json by lazy {
    Json {
        ignoreUnknownKeys = true
        explicitNulls = true
        encodeDefaults = true
    }
}

@SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager")
private val myTrustManager = object : X509TrustManager {
    //checkServerTrusted和checkClientTrusted 这两个方法好像是用于，server和client双向验证
    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
    override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
    override fun getAcceptedIssuers(): Array<X509Certificate?> {
        return arrayOfNulls(0)
    }
}

private val mySSLSocketFactory by lazy {
    val sslCtx = SSLContext.getInstance("TLS")
    sslCtx.init(null, arrayOf(myTrustManager), SecureRandom())
    sslCtx.socketFactory
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
                hostnameVerifier { _, _ -> true }
                sslSocketFactory(mySSLSocketFactory, myTrustManager)
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

private val loggerMap by lazy { HashMap<String, Logger>() }

val logger by lazy {
    logger("MawTAG").apply { level = Level.ALL }
}

fun logger(tag: String): Logger {
    if (tag.isEmpty())
        return logger
    val logger: Logger
    synchronized(loggerMap) {
        logger = loggerMap[tag] ?: Logger.getLogger(tag)
        loggerMap[tag] = logger
    }
    return logger
}
