package com.magic.maw.website

import android.util.Log
import com.magic.maw.data.TagInfo
import com.magic.maw.db.AppDB
import com.magic.maw.db.updateOrInsert
import com.magic.maw.util.dbHandler
import com.magic.maw.website.parser.BaseParser
import com.magic.maw.website.parser.YandeParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.SoftReference
import java.util.Date

private const val TAG = "TagManager"
private const val MAX_RETRY_COUNT = 3

class TagManager(val source: String) {
    private val dao by lazy { AppDB.tagDao }
    private val tagMap = HashMap<String, TagInfo>()
    private val taskMap = HashMap<String, TagTask>()
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }
    private val parser: BaseParser get() = BaseParser.get(source)

    init {
        dbHandler.post {
            val readDate = Date(System.currentTimeMillis() - 30 * 60 * 1000L)
            val tags = dao.getAllByReadDate(source, readDate)
            synchronized(tagMap) {
                for (tag in tags) {
                    tagMap[tag.name] = tag
                }
            }
        }
    }

    fun addAll(tagList: List<TagInfo>) {
        synchronized(tagMap) {
            for (item in tagList) {
                tagMap[item.name] = item
            }
        }
        dbHandler.post {
            for (item in tagList) {
                dao.updateOrInsert(item)
            }
        }
    }

    fun addAll(tagMap: Map<String, TagInfo>) {
        synchronized(tagMap) {
            this.tagMap.putAll(tagMap)
        }
        dbHandler.post {
            for (item in tagMap) {
                dao.updateOrInsert(item.value)
            }
        }
    }

    fun getInfoStatus(
        name: String,
        scope: CoroutineScope = coroutineScope
    ): MutableStateFlow<LoadStatus<TagInfo>> {
        synchronized(tagMap) {
            tagMap[name]?.let {
                it.readTime = Date()
                dbHandler.post { dao.updateReadTime(it.id, it.readTime) }
                return MutableStateFlow(LoadStatus.Success(it))
            }
        }
        synchronized(taskMap) {
            taskMap[name]?.let {
                return it.status
            } ?: let {
                val task = TagTask(source, name)
                taskMap[name] = task
                scope.launch(Dispatchers.IO) {
                    task.start()
                    synchronized(taskMap) {
                        taskMap.remove(task.name)
                    }
                }
                return task.status
            }
        }
    }

    private suspend fun TagTask.start() {
        // 从数据库中获取
        dao.get(source, name)?.let {
            it.readTime = Date()
            synchronized(tagMap) {
                tagMap[name] = it
            }
            status.value = LoadStatus.Success(it)
            dbHandler.post { dao.updateReadTime(it.id, it.readTime) }
            return
        }
        // 网络请求
        parser.requestTagInfo(name)?.let {
            synchronized(tagMap) {
                tagMap[name] = it
            }
            status.value = LoadStatus.Success(it)
            Log.d(TAG, "get tag from web: $name")
            dbHandler.post { dao.insert(it) }
            return
        }

        retryCount++
        if (retryCount < MAX_RETRY_COUNT) {
            delay(1000)
            start()
        } else {
            status.value = LoadStatus.Error(RuntimeException("get tag($name) failed"))
        }
    }

    companion object {
        private val map = HashMap<String, SoftReference<TagManager>>()

        fun get(source: String): TagManager = synchronized(map) {
            map[source]?.get()?.let { return it }
            val manager = when (source) {
                YandeParser.SOURCE -> TagManager(YandeParser.SOURCE)
                else -> throw RuntimeException("Unknown source: $source")
            }
            map[source] = SoftReference(manager)
            manager
        }
    }
}

data class TagTask(
    val source: String,
    val name: String,
    val status: MutableStateFlow<LoadStatus<TagInfo>> = MutableStateFlow(LoadStatus.Waiting),
    var retryCount: Int = 0
)