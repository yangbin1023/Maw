package com.magic.maw.website

import android.content.Context
import android.os.Environment
import android.util.Log
import cn.zhxu.okhttps.Download.Ctrl
import cn.zhxu.okhttps.Process
import com.magic.maw.MyApp
import com.magic.maw.data.PostData
import com.magic.maw.data.PostData.*
import com.magic.maw.data.Quality
import com.magic.maw.util.HttpUtils
import com.magic.maw.website.DLManager.addTask
import com.magic.maw.website.DLManager.getDLFullPath
import com.magic.maw.website.DLManager.removeTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

typealias DLSuccess = (File) -> Unit
typealias DLError = (Exception) -> Unit
typealias DLProcess = (Process) -> Unit

private const val TAG = "DLManager"

object DLManager {
    private val taskMap = LinkedHashMap<String, DLTask>()
    private val diskPath by lazy { getDiskCachePath(MyApp.app) }
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    fun addTaskAndStart(
        source: String,
        id: Int,
        quality: Quality,
        url: String,
        success: DLSuccess? = null,
        process: DLProcess? = null,
        error: DLError? = null,
    ) {
        synchronized(taskMap) {
            taskMap[url]?.apply {
                success?.let { addSuccess(it) }
                process?.let { addProcess(it) }
                error?.let { addError(it) }
            } ?: let {
                val task = DLTask(source, id, quality, url).apply {
                    success?.let { addSuccess(it) }
                    process?.let { addProcess(it) }
                    error?.let { addError(it) }
                }
                taskMap[url] = task
                startTask(task)
            }
        }
    }

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

    suspend fun addTaskAndStart2(
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
        task.start()
        return task.statusFlow
    }

    fun cancelTask(url: String) {
        synchronized(taskMap) {
            taskMap[url]?.apply {
                ctrl?.cancel()
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

    private fun dispatchTask() {

    }

    private fun startTask(task: DLTask) {
        Log.d(TAG, "start dl task: $task")
        HttpUtils.getHttp("dl").async(task.url).setOnResponse { response ->
            try {
                val path = getDLFullPath(task.source, task.id, task.quality)
                val tmpPath = "${path}_tmp"
                val ctrl = response.body.stepRate(0.05).setOnProcess {
                    task.onProcess(it)
                }.toFile(tmpPath).setOnFailure {
                    task.onError(it.exception)
                }.setOnSuccess {
                    val newFile = File(path)
                    it.renameTo(newFile)
                    task.onSuccess(newFile)
                }.setOnComplete {
                    removeTask(task)
                    dispatchTask()
                }.start()
                synchronized(taskMap) {
                    if (taskMap[task.url] != task) {
                        Log.e(TAG, "task cancel: $task")
                        ctrl.cancel()
                    } else {
                        task.ctrl = ctrl
                    }
                }
            } catch (e: Exception) {
                task.onError(e)
                removeTask(task)
            }
        }.setOnException {
            task.onError(it)
            removeTask(task)
        }.get()
    }
}

data class DLTask(
    val source: String,
    val id: Int,
    val quality: Quality,
    val url: String,
    val path: String = "",
    val statusFlow: MutableStateFlow<LoadStatus<File>> = MutableStateFlow(LoadStatus.Waiting),
    var ctrl: Ctrl? = null,
    val successList: LinkedHashSet<DLSuccess> = LinkedHashSet(),
    val processList: LinkedHashSet<DLProcess> = LinkedHashSet(),
    val errorList: LinkedHashSet<DLError> = LinkedHashSet(),
) {
    private var lastUpdateProcessTime: Long = 0
    fun onSuccess(file: File) {
        synchronized(successList) {
            for (item in successList) {
                item.invoke(file)
            }
        }
    }

    fun onProcess(process: Process) {
        synchronized(processList) {
            for (item in processList) {
                item.invoke(process)
            }
        }
    }

    fun onError(error: Exception) {
        synchronized(errorList) {
            for (item in errorList) {
                item.invoke(error)
            }
        }
        Log.d(TAG, "download file error: $error, list size: ${errorList.size}")
    }

    fun addSuccess(success: DLSuccess) {
        synchronized(successList) {
            successList.add(success)
        }
    }

    fun addProcess(process: DLProcess) {
        synchronized(processList) {
            processList.add(process)
        }
    }

    fun addError(error: DLError) {
        synchronized(errorList) {
            errorList.add(error)
        }
    }

    override fun toString(): String {
        return "source: $source, id: $id, quality: $quality, url: $url"
    }

    suspend fun start() = suspendCancellableCoroutine { cont ->
        val onComplete: (Exception?) -> Unit = {
            removeTask(this)
            if (cont.isActive) {
                it?.let {
                    statusFlow.value = LoadStatus.Error(it)
                    Log.d(TAG, "start dl task failed: ${it.message}", it)
                }
                cont.resume(Unit)
            }
        }
        if (statusFlow.value is LoadStatus.Success)
            onComplete(null)
        HttpUtils.getHttp("dl").async(url).setOnResponse { response ->
            try {
                val tmpPath = "${path}_tmp"
                val ctrl = response.body.stepRate(0.05).setOnProcess {
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateProcessTime > 100) {
                        statusFlow.value = LoadStatus.Loading(it.rate.toFloat().coerceIn(0f, 1f))
                        lastUpdateProcessTime = now
                    }
                }.toFile(tmpPath).setOnFailure {
                    onComplete.invoke(it.exception)
                }.setOnSuccess {
                    val newFile = File(path)
                    it.renameTo(newFile)
                    statusFlow.value = LoadStatus.Success(newFile)
                }.setOnComplete {
                    onComplete.invoke(null)
                }.start()
                this@DLTask.ctrl = ctrl
            } catch (e: Exception) {
                onComplete.invoke(e)
            }
        }.setOnException {
            onComplete.invoke(it)
        }.get()
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