package com.magic.maw.website

import android.content.Context
import android.util.Log
import cn.zhxu.okhttps.Download.Ctrl
import cn.zhxu.okhttps.Process
import com.magic.maw.MyApp
import com.magic.maw.data.PostData
import com.magic.maw.data.PostData.*
import com.magic.maw.data.Quality
import com.magic.maw.util.HttpUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

typealias DLSuccess = (File) -> Unit
typealias DLError = (Exception) -> Unit
typealias DLProcess = (Process) -> Unit

private const val TAG = "DLManager"

object DLManager {
    private val taskMap = LinkedHashMap<String, DLTask>()

    fun addTask(
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

    fun cancelTask(url: String) {
        synchronized(taskMap) {
            taskMap[url]?.apply {
                ctrl?.cancel()
                taskMap.remove(url)
            }
        }
    }

    fun getDLFullPath(source: String, id: Int, quality: Quality): String {
        return getDLPath(MyApp.app, source, quality) + File.separator + getDLName(
            source,
            id,
            quality
        )
    }

    fun getDLPath(context: Context, source: String, quality: Quality): String {
        return context.externalCacheDir!!.absolutePath + File.separator + quality.name + File.separator + source
    }

    fun getDLName(source: String, id: Int, quality: Quality): String {
        return "${source}_${id}_${quality.name}"
    }

    private fun removeTask(task: DLTask) {
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
                    Log.e(TAG, "task complete: $task")
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

internal data class DLTask(
    val source: String,
    val id: Int,
    val quality: Quality,
    val url: String,
    var ctrl: Ctrl? = null,
    val successList: LinkedHashSet<DLSuccess> = LinkedHashSet(),
    val processList: LinkedHashSet<DLProcess> = LinkedHashSet(),
    val errorList: LinkedHashSet<DLError> = LinkedHashSet(),
) {
    fun onSuccess(file: File) {
        synchronized(successList) {
            for (item in successList) {
                item.invoke(file)
            }
        }
        Log.d(TAG, "download file success: $file, list size: ${successList.size}")
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
}

suspend fun loadDLFile(
    postData: PostData,
    info: Info,
    quality: Quality
): File? = suspendCancellableCoroutine func@{ cont ->
    val file = File(DLManager.getDLFullPath(postData.source, postData.id, quality))
    val resume: (File?) -> Unit = {
        if (cont.isActive) cont.resume(it) else println("cancel load dl file: $postData")
    }
    if (file.exists()) {
        resume.invoke(file)
        return@func
    }
    DLManager.addTask(
        source = postData.source,
        id = postData.id,
        quality = quality,
        url = info.url,
        success = { resume.invoke(it) },
        error = { resume.invoke(null) }
    )
}
