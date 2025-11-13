package com.magic.maw.website

import android.content.Context
import android.os.Environment
import co.touchlab.kermit.Logger
import com.magic.maw.MyApp
import com.magic.maw.data.BaseData
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import com.magic.maw.util.VerifyRequester
import com.magic.maw.util.client
import com.magic.maw.util.cookie
import com.magic.maw.util.isTextFile
import com.magic.maw.website.DLManager.addTask
import com.magic.maw.website.DLManager.getDLFullPath
import com.magic.maw.website.LoadStatus.Error
import com.magic.maw.website.LoadStatus.Loading
import com.magic.maw.website.LoadStatus.Success
import com.magic.maw.website.LoadStatus.Waiting
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "DLManager"
private val scope by lazy { CoroutineScope(Dispatchers.IO) }

object DLManager {
    private val taskMap = LinkedHashMap<String, DLTask>()
    private val diskPath by lazy { getDiskCachePath(MyApp.app) }

    fun addTask(
        baseData: BaseData,
        url: String,
        path: String = getDLFullPath(baseData)
    ): DLTask {
        return synchronized(taskMap) {
            taskMap[url] ?: let {
                DLTask(baseData, url, path).apply {
                    taskMap[url] = this
                }
            }
        }
    }

    fun addTaskAndStart(
        baseData: BaseData,
        url: String,
        scope: CoroutineScope? = null
    ): StateFlow<LoadStatus<File>> {
        val path = getDLFullPath(baseData)
        val file = File(path)
        if (file.exists()) {
            if (file.isTextFile() && !baseData.fileType.isText) {
                file.delete()
            } else {
                return MutableStateFlow(Success(file))
            }
        }
        val task = addTask(baseData, url, path)
        val stateFlow = scope?.let {
            val initState = task.statusFlow.value
            task.statusFlow.stateIn(it, SharingStarted.Lazily, initState)
        } ?: task.statusFlow
        task.start()
        return stateFlow
    }

    @Suppress("unused")
    fun cancelTask(url: String) {
        synchronized(taskMap) {
            taskMap[url]?.let {
                it.cancel()
                taskMap.remove(url)
            }
        }
    }

    fun getDLFullPath(baseData: BaseData): String {
        return getDLPath(baseData.source, baseData.quality) + File.separator + getDLName(baseData)
    }

    private fun getDLPath(source: String, quality: Quality): String {
        return diskPath + File.separator + quality.name + File.separator + source
    }

    private fun getDiskCachePath(context: Context): String {
        return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
            context.externalCacheDir?.path ?: context.cacheDir.path
        } else {
            context.cacheDir.path
        }
    }

    private fun getDLName(baseData: BaseData): String {
        return baseData.toString()
    }

    internal fun removeTask(task: DLTask) {
        synchronized(taskMap) {
            if (taskMap[task.url] == task) {
                taskMap.remove(task.url)
            } else {
                Logger.w(TAG) { "The task is completed but does not exist in the task map." }
            }
        }
    }
}

data class DLTask(
    val baseData: BaseData,
    val url: String,
    val path: String = "",
    val statusFlow: MutableStateFlow<LoadStatus<File>> = MutableStateFlow(Waiting),
) {
    private var started: Boolean = false
    private var response: HttpResponse? = null

    fun cancel() = synchronized(this) {
        try {
            response?.cancel()
        } catch (_: Throwable) {
        }
        response = null
    }

    override fun toString(): String {
        return "source: ${baseData.source}, id: ${baseData.id}, quality: ${baseData.quality}, url: $url"
    }

    fun start() = scope.launch {
        if (statusFlow.value is Success)
            return@launch
        synchronized(this) {
            if (started)
                return@launch
            started = true
        }
        val file = File(path)
        try {
            file.parentFile?.apply { if (!exists()) mkdirs() }
            // 开始网络请求
            client.prepareGet(url) {
                cookie()
                onDownload { currentLen, contentLen ->
                    val progress = contentLen?.let { currentLen.toFloat() / it.toFloat() } ?: 0f
                    val status = statusFlow.value
                    when (status) {
                        is Error -> cancel()
                        is Loading -> statusFlow.update { Loading(progress) }
                        Waiting -> statusFlow.update { Loading(0f) }
                        is Success<*> -> {}
                    }
                }
            }.execute { response ->
                this@DLTask.response = response
                val channel = response.body<ByteReadChannel>()
                val file = File(path)
                file.parentFile?.apply { if (!exists()) mkdirs() }
                channel.copyAndClose(file.writeChannel())
                delay(100)
                if (baseData.size > 0 && file.length() != baseData.size) {
                    Logger.w(TAG) { "file size error. expected size: ${baseData.size}, actual size: ${file.length()}" }
                }
                if (VerifyRequester.checkDlFile(file, this@DLTask)) {
                    Logger.d(TAG) { "download success, $url" }
                    statusFlow.update { Success(file) }
                } else {
                    Logger.e(TAG) { "file type error. ${file.absolutePath}, $url" }
                    statusFlow.update { Error(RuntimeException("The request result is not the target file")) }
                }
            }
        } catch (e: Exception) {
            if (statusFlow.value !is Success) {
                statusFlow.update { Error(e) }
            }
            try {
                if (file.exists()) file.delete()
            } catch (_: Exception) {
            }
            Logger.e(TAG) { "download failed, $url, ${e.message}" }
        }
        DLManager.removeTask(this@DLTask)
    }
}

fun loadDLFile(
    postData: PostData,
    quality: Quality = postData.quality,
    scope: CoroutineScope? = null
): StateFlow<LoadStatus<File>> {
    val info = postData.getInfo(quality) ?: postData.originalInfo
    val baseData = BaseData(postData, quality, info.size)
    return DLManager.addTaskAndStart(baseData, info.url, scope)
}

@Suppress("unused")
fun loadDLFileWithTask(
    postData: PostData,
    quality: Quality = postData.quality
): DLTask {
    var currentQuality = quality
    val info = postData.getInfo(quality) ?: run {
        currentQuality = Quality.File
        postData.originalInfo
    }
    val baseData = BaseData(postData, currentQuality, info.size)
    val path = getDLFullPath(baseData)
    val file = File(path)
    if (file.exists())
        return DLTask(
            baseData,
            info.url,
            path,
            MutableStateFlow(Success(file))
        )
    return addTask(baseData, info.url, path)
}