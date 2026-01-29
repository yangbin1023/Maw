package com.magic.maw.util

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.webkit.CookieManager
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.magic.maw.data.interceptor.DataThumbnailInterceptor
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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import org.koin.core.scope.Scope
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit
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

fun createAppHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
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
                retryOnConnectionFailure(true)

                // 配置最大请求数
                val dispatcher = Dispatcher()
                dispatcher.maxRequests = 100
                dispatcher.maxRequestsPerHost = 50
                dispatcher(dispatcher)

                // 配置连接池
                connectionPool(ConnectionPool(50, 5, TimeUnit.MINUTES))
            }
        }
    }
}

val client by lazy { createAppHttpClient() }

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

fun Scope.createImageLoader(): ImageLoader {
    return ImageLoader.Builder(get())
        .components {
            // 添加ktor支持
            add(KtorNetworkFetcherFactory(get<HttpClient>()))
            // 添加获取略缩图Interceptor
            add(DataThumbnailInterceptor(get(), get()))
            // 添加视频抽帧插件
            add(VideoFrameDecoder.Factory())
            // 添加Gif支持
            if (Build.VERSION.SDK_INT >= 28) {
                add(AnimatedImageDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .crossfade(true)
        .build()
}
