package com.magic.maw.website

import android.content.Context
import android.os.Environment
import android.util.Log
import cn.zhxu.okhttps.Download.Ctrl
import com.magic.maw.MyApp
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import com.magic.maw.util.client
import com.magic.maw.website.DLManager.addTask
import com.magic.maw.website.DLManager.getDLFullPath
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "DLManager"

object DLManager {
    private val taskMap = LinkedHashMap<String, DLTask>()
    private val diskPath by lazy { getDiskCachePath(MyApp.app) }
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    fun addTask(
        source: String,
        id: Int,
        quality: Quality,
        url: String,
        path: String = getDLFullPath(source, id, quality)
    ): DLTask {
        return synchronized(taskMap) {
            taskMap[url] ?: let {
                DLTask(source, id, quality, url, path).apply {
                    taskMap[url] = this
                }
            }
        }
    }

    fun addTaskAndStart(
        source: String,
        id: Int,
        quality: Quality,
        url: String
    ): MutableStateFlow<LoadStatus<File>> {
        val path = getDLFullPath(source, id, quality)
        val file = File(path)
        if (file.exists())
            return MutableStateFlow(LoadStatus.Success(file))
        val task = addTask(source, id, quality, url, path)
        coroutineScope.launch { task.start() }
        return task.statusFlow
    }

    fun cancelTask(url: String) {
        synchronized(taskMap) {
            taskMap[url]?.let {
                it.cancel()
                taskMap.remove(url)
            }
        }
    }

    fun getDLFullPath(source: String, id: Int, quality: Quality): String {
        return getDLPath(source, quality) + File.separator + getDLName(
            source,
            id,
            quality
        )
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

    private fun getDLName(source: String, id: Int, quality: Quality): String {
        return "${source}_${id}_${quality.name}"
    }

    internal fun removeTask(task: DLTask) {
        synchronized(taskMap) {
            if (taskMap[task.url] == task) {
                taskMap.remove(task.url)
            } else {
                Log.w(TAG, "The task is completed but does not exist in the task map.")
            }
        }
    }

}

data class DLTask(
    val source: String,
    val id: Int,
    val quality: Quality,
    val url: String,
    val path: String = "",
    val statusFlow: MutableStateFlow<LoadStatus<File>> = MutableStateFlow(LoadStatus.Waiting),
) {
    private var response: HttpResponse? = null
    private var ctrl: Ctrl? = null
    private var lastUpdateProcessTime: Long = 0

    fun cancel() = synchronized(this) {
        try {
            ctrl?.cancel()
        } catch (_: Throwable) {
        }
        ctrl = null
        try {
            response?.cancel()
        } catch (_: Throwable) {
        }
        response = null
    }

    override fun toString(): String {
        return "source: $source, id: $id, quality: $quality, url: $url"
    }

    suspend fun start() {
        if (statusFlow.value is LoadStatus.Success)
            return
        try {
            val channel = client.get(url) {
                onDownload { currentLen, contentLen ->
                    val progress = contentLen?.let { currentLen.toFloat() / it.toFloat() } ?: 0f
                    val status = statusFlow.value
                    if (status is LoadStatus.Error) {
                        cancel()
                    } else if (status !is LoadStatus.Success) {
                        statusFlow.value = LoadStatus.Loading(progress)
                    }
                }
            }.apply {
                response = this
            }.bodyAsChannel()
            val file = File(path)
            file.parentFile?.apply { if (!exists()) mkdirs() }
            channel.copyAndClose(file.writeChannel())
            statusFlow.value = LoadStatus.Success(file)
            Log.d(TAG, "download success, $url")
        } catch (e: Exception) {
            if (statusFlow.value !is LoadStatus.Success) {
                statusFlow.value = LoadStatus.Error(e)
            }
            Log.d(TAG, "download failed, $url")
        }
    }
}

fun loadDLFile(
    postData: PostData,
    quality: Quality = postData.quality
): MutableStateFlow<LoadStatus<File>> {
    val info = postData.getInfo(quality) ?: postData.originalInfo
    return DLManager.addTaskAndStart(postData.source, postData.id, quality, info.url)
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

    val path = getDLFullPath(postData.source, postData.id, currentQuality)
    val file = File(path)
    if (file.exists())
        return DLTask(
            postData.source,
            postData.id,
            quality,
            info.url,
            path,
            MutableStateFlow(LoadStatus.Success(file))
        )
    return addTask(postData.source, postData.id, quality, info.url, path)
}