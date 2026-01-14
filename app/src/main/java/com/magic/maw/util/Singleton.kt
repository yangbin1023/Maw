package com.magic.maw.util

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.webkit.CookieManager
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import com.magic.maw.MyApp.Companion.app
import com.magic.maw.ui.features.verify.VerifyViewDefaults
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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
            "Date",
            kotlinx.serialization.descriptors.PrimitiveKind.STRING
        )

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(TimeUtils.getTimeStr(TimeUtils.FORMAT_1, value))
    }

    override fun deserialize(decoder: Decoder): Date {
        return TimeUtils.getDate(TimeUtils.FORMAT_1, decoder.decodeString())
    }
}

val json by lazy {
    Json {
        ignoreUnknownKeys = true
        explicitNulls = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(Date::class, DateSerializer)
        }
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
val privateStoreFolder: File
    get() = app.filesDir.resolve("store").autoMk()

val imageLoader by lazy {
    ImageLoader.Builder(app)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(AnimatedImageDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
}
