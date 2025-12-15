package com.magic.maw.util

import co.touchlab.kermit.Logger
import com.hjq.toast.Toaster
import com.magic.maw.MyApp
import com.magic.maw.util.VerifyRequester.callback
import com.magic.maw.website.DLTask
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max


sealed class VerifyResult(val url: String) {
    class Success(url: String, val text: String) : VerifyResult(url)
    class Failure(url: String) : VerifyResult(url)
}

data class VerifyStatus(
    val mutex: Mutex = Mutex(),
    var deferred: Deferred<VerifyResult>? = null,
    var lastVerifyTime: Long = 0
)

class VerificationCancelledException(message: String = "The user canceled or gave up human-machine verification") :
    CancellationException(message)

typealias OnVerifyCallback = (String) -> Unit

object VerifyRequester {
    private val verifyStatusMap by lazy { mutableMapOf<String, VerifyStatus>() }
    var callback: OnVerifyCallback? = null

    @Synchronized
    fun getVerifyStatus(host: String, create: Boolean = true): VerifyStatus? {
        var status = verifyStatusMap[host]
        if (create && status == null) {
            status = VerifyStatus()
            verifyStatusMap[host] = status
        }
        return status
    }

    fun onVerifyResult(result: VerifyResult) {
        val host = URLBuilder(result.url).host
        val status = getVerifyStatus(host, false) ?: let {
            Logger.e(TAG) { "verify status can't find, url: ${result.url}" }
            return
        }

        val deferred = status.deferred as? CompletableDeferred<VerifyResult> ?: let {
            Logger.e(TAG) { "verify deferred can't find, url: ${result.url}" }
            return
        }
        if (!deferred.isCompleted) {
            status.lastVerifyTime = System.currentTimeMillis()
            deferred.complete(result)
        } else {
            Logger.w(TAG) { "verify deferred is completed. url: ${result.url}" }
        }
    }

    suspend fun checkDlFile(file: File, task: DLTask): Boolean {
        if (!file.isTextFile()) {
            // 文件非文本文件
            return true
        }
        if (file.length() < FILE_SIZE_1MiB) {
            // 文件大小大于1MiB，如果是认证页面不会这么大
            return true
        }
        if (file.readString().isVerifyHtml()) {
            val host = URLBuilder(task.url).host
            val status = getVerifyStatus(host, false) ?: return false

            status.mutex.withLock {
                status.deferred?.let {
                    return@withLock it.await()
                }

                val callback = callback ?: return@withLock null
                val deferred = CompletableDeferred<VerifyResult>()
                status.deferred = deferred

                // 延迟1秒，防止连续验证
                delay(max(0, 1000 - (System.currentTimeMillis() - status.lastVerifyTime)))

                Logger.d(TAG) { "invoke onVerifyCallback, url: ${task.url}" }
                callback.invoke(task.url)

                val result = deferred.await()
                status.deferred = null
                result
            }
            return false
        }
        val fileType = task.baseData.fileType
        if (!fileType.isText) {
            // 如果原本要下载的不是文本文件，但当前文件是文本文件，则说明下载异常
            Logger.e(TAG) { "Download file type error. target type: ${fileType.getPrefixName()}" }
            return false
        } else {
            return true
        }
    }

    private const val TAG = "VerifyRequester"
}

suspend inline fun <reified T> HttpClient.get(urlString: String): T {
    val url = URLBuilder().takeFrom(urlString)
    val status = VerifyRequester.getVerifyStatus(url.host)!!

    do {
        val response = get(urlString) {
            cookie()
            header(HttpHeaders.Referrer, url.host)
        }
        val msg: String = response.body()
        if (!msg.isVerifyHtml()) {
            if (msg.isHtml()) {
                try {
                    val logDir = MyApp.app.getExternalFilesDir("log")
                    val fileName = TimeUtils.getCurrentTimeStr(TimeUtils.FORMAT_3) + ".log"
                    withContext(Dispatchers.IO) {
                        val fWriter = FileWriter(File(logDir, fileName))
                        fWriter.write(msg)
                        fWriter.close()
                    }
                    Toaster.show("写入日志：$fileName")
                } catch (e: Exception) {
                    Toaster.show("""write log failed: ${e.message}""")
                }
            }
            return response.body()
        }
        val callback = callback ?: let { return response.body() }
        val result = status.mutex.withLock {
            val response = get(urlString) {
                cookie()
                header(HttpHeaders.Referrer, url.host)
            }
            val msg: String = response.body()
            if (!msg.isVerifyHtml()) {
                return response.body()
            }
            status.deferred?.let {
                return@withLock it.await()
            }

            val deferred = CompletableDeferred<VerifyResult>()
            status.deferred = deferred

            // 延迟1秒，防止连续验证
            delay(max(0, 1000 - (System.currentTimeMillis() - status.lastVerifyTime)))

            Logger.d(VerifyRequester.javaClass.simpleName) { "invoke onVerifyCallback, url: $url. msg: $msg" }
            callback.invoke(urlString)

            val result = deferred.await()
            status.deferred = null
            result
        }
        when (result) {
            is VerifyResult.Failure -> throw VerificationCancelledException()
            is VerifyResult.Success -> {
                if (result.url == urlString) {
                    return if (T::class == String::class) {
                        result.text as T
                    } else {
                        json.decodeFromString(result.text)
                    }
                }
            }
        }
    } while (true)
}
