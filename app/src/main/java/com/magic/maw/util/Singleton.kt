package com.magic.maw.util

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread
import android.webkit.CookieManager
import com.magic.maw.MyApp.Companion.app
import com.magic.maw.ui.verify.VerifyViewDefaults
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.MainScope
import kotlinx.serialization.json.Json
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

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
        install(UserAgent) {
            agent = VerifyViewDefaults.UserAgent
        }
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
                proxySelector(SwitchProxySelector)
            }
        }
    }
}

fun HttpRequestBuilder.cookie() = apply {
    CookieManager.getInstance().getCookie(url.toString())?.let {
        header("Cookie", it)
    }
}

private val dbHandlerThread: HandlerThread by lazy {
    HandlerThread("DBHandler")
}

val dbHandler by lazy {
    dbHandlerThread.start()
    Handler(dbHandlerThread.looper)
}

private fun File.autoMk(): File {
    if (!exists()) {
        mkdirs()
    }
    return this
}

val appScope by lazy { MainScope() }

private val filesDir: File by lazy {
    app.getExternalFilesDir(null) ?: error("failed getExternalFilesDir")
}

val dbFolder: File
    get() = filesDir.resolve("db").autoMk()
val storeFolder: File
    get() = filesDir.resolve("store").autoMk()
val subsFolder: File
    get() = filesDir.resolve("subscription").autoMk()
val snapshotFolder: File
    get() = filesDir.resolve("snapshot").autoMk()

val privateStoreFolder: File
    get() = app.filesDir.resolve("store").autoMk()
