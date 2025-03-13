package com.magic.maw.website

import android.content.Context
import android.os.Environment
import com.magic.maw.MyApp
import com.magic.maw.data.BaseData
import com.magic.maw.data.FileType
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import com.magic.maw.util.Logger
import com.magic.maw.util.client
import com.magic.maw.util.cookie
import com.magic.maw.util.isTextFile
import com.magic.maw.website.DLManager.addTask
import com.magic.maw.website.DLManager.getDLFullPath
import com.magic.maw.website.parser.BaseParser
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
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
private val logger = Logger(TAG)
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
            if (file.isTextFile() && !baseData.fileType.isText()) {
                file.delete()
            } else {
                return MutableStateFlow(LoadStatus.Success(file))
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
                logger.warning("The task is completed but does not exist in the task map.")
            }
        }
    }
}

data class DLTask(
    val baseData: BaseData,
    val url: String,
    val path: String = "",
    val statusFlow: MutableStateFlow<LoadStatus<File>> = MutableStateFlow(LoadStatus.Waiting),
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

    private fun defaultCheckFile(file: File): Boolean {
        return baseData.fileType.isText() || !file.isTextFile()
    }

    fun start() = scope.launch {
        if (statusFlow.value is LoadStatus.Success)
            return@launch
        synchronized(this) {
            if (started)
                return@launch
            started = true
        }
        try {
            val channel = client.get(url) {
                cookie()
                onDownload { currentLen, contentLen ->
                    val progress = contentLen?.let { currentLen.toFloat() / it.toFloat() } ?: 0f
                    val status = statusFlow.value
                    if (status is LoadStatus.Error) {
                        cancel()
                    } else if (status is LoadStatus.Loading) {
                        statusFlow.update { LoadStatus.Loading(progress) }
                    } else if (status !is LoadStatus.Success) {
                        statusFlow.update { LoadStatus.Loading(0f) }
                    }
                }
            }.apply {
                response = this
            }.bodyAsChannel()
            val file = File(path)
            file.parentFile?.apply { if (!exists()) mkdirs() }
            channel.copyAndClose(file.writeChannel())
            delay(100)
            if (baseData.size > 0 && file.length() != baseData.size) {
                logger.warning("file size error. expected size: ${baseData.size}, actual size: ${file.length()}")
            }
            val verifyContainer = BaseParser.get(baseData.source).getVerifyContainer()
            if (verifyContainer?.checkDlFile(file, this@DLTask) ?: defaultCheckFile(file)) {
                logger.info("download success, $url")
                statusFlow.update { LoadStatus.Success(file) }
            } else {
                logger.severe("file type error. ${file.absolutePath}, $url")
                statusFlow.update { LoadStatus.Error(RuntimeException("The request result is not the target file")) }
            }
        } catch (e: Exception) {
            if (statusFlow.value !is LoadStatus.Success) {
                statusFlow.value = LoadStatus.Error(e)
            }
            logger.severe("download failed, $url, ${e.message}")
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
            MutableStateFlow(LoadStatus.Success(file))
        )
    return addTask(baseData, info.url, path)
}