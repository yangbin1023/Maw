package com.magic.maw.util

import android.annotation.SuppressLint
import cn.zhxu.okhttps.HTTP
import okhttp3.ConnectionPool
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.net.Proxy
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import kotlin.math.max

object HttpUtils {
    private val cookies = HashMap<String, List<Cookie>>()
    private val sslCtx = SSLContext.getInstance("TLS")
    private val mySSLSocketFactory: SSLSocketFactory
    private val cpuCount = Runtime.getRuntime().availableProcessors()
    private val supportMaxCount = cpuCount * 2
    private val httpMap = HashMap<String, HTTP>()
    private val typeMap by lazy {
        val map = HashMap<String, Int>()
        map["def"] = supportMaxCount
        map["tag"] = max(supportMaxCount, 16)
        map["dl"] = max(supportMaxCount, 20)
        map
    }

    private fun HTTP.Builder.setConfig(maxConnectCount: Int = 16): HTTP.Builder {
        this.config { builder ->
            // 连接池，20个，保活5分钟
            builder.connectionPool(ConnectionPool(maxConnectCount, 5, TimeUnit.MINUTES))
            // 连接超时时间（默认10秒）
            builder.connectTimeout(10, TimeUnit.SECONDS);
            // 写入超时时间（默认10秒）
            builder.writeTimeout(10, TimeUnit.SECONDS);
            // 读取超时时间（默认10秒）
            builder.readTimeout(10, TimeUnit.SECONDS);
            // 设置cookies
            builder.cookieJar(myCookieJar)
            // 设置缓存
//            builder.cache(Cache(File(PathManager.httpCachePath), 10 * 1024 * 1024L))
            // 支持https
            builder.sslSocketFactory(mySSLSocketFactory, myTrustManager)
            // 关闭https认证
            builder.hostnameVerifier(myHostnameVerifier)
            // 设置代理
            getProxy()?.let { builder.proxy(it) }
        }
        return this
    }

    fun getHttp(type: String, maxCount: Int = supportMaxCount): HTTP {
        synchronized(httpMap) {
            httpMap[type]?.let { return it }
            val count = if (maxCount != supportMaxCount) maxCount else (typeMap[type] ?: maxCount)
            val http = newHttp(count)
            httpMap[type] = http
            return http
        }
    }

    fun getHttp(): HTTP {
        return getHttp("def")
    }

    fun update() {
        synchronized(httpMap) {
            httpMap.clear()
        }
    }

    private fun newHttp(maxConnectCount: Int): HTTP {
        return HTTP.builder()
            .exceptionListener { task, e ->
                println("task: ${task.url} exception: " + e)
                true
            }
            .setConfig(maxConnectCount)
            .build()
    }

    private fun getProxy(): Proxy? {
//        val sharedParam = MyApp.instance.sharedParam
//        if (sharedParam.proxyType == 1) {
//            val address = InetSocketAddress(sharedParam.proxyHostname, sharedParam.proxyPort)
//            println("proxy: $address")
//            return Proxy(Proxy.Type.HTTP, address)
//        }
//        val address = InetSocketAddress("192.168.6.27", 1080)
//        println("proxy: $address")
//        return Proxy(Proxy.Type.HTTP, address)
        return null
    }

    private val myCookieJar = object : CookieJar {
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookies[url.host] ?: ArrayList<Cookie>()
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            HttpUtils.cookies[url.host] = cookies
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

    private val myHostnameVerifier = HostnameVerifier { _, _ -> true }

    private class MyInterceptor(val retryCount: Int) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var retryTimes = 0
            while (true) {
                try {
                    retryTimes++
                    val response = chain.proceed(chain.request())
                    return response
                } catch (e: SocketTimeoutException) {
                    if (retryTimes > retryCount) {
                        throw e
                    }
                }
            }
        }
    }

    init {
        sslCtx.init(null, arrayOf(myTrustManager), SecureRandom())
        mySSLSocketFactory = sslCtx.socketFactory
    }
}
