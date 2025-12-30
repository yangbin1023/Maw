package com.magic.maw.website

import com.magic.maw.data.TagHistory
import com.magic.maw.data.TagInfo
import com.magic.maw.data.WebsiteOption
import com.magic.maw.db.AppDB
import com.magic.maw.db.updateOrInsert
import com.magic.maw.db.updateOrInsertHistory
import com.magic.maw.util.dbHandler
import com.magic.maw.website.parser.BaseParser
import com.magic.maw.website.parser.DanbooruParser
import com.magic.maw.website.parser.KonachanParser
import com.magic.maw.website.parser.YandeParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
    private val historyList = ArrayList<TagInfo>()

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
        loadTagHistory()
    }

    fun add(tagInfo: TagInfo) {
        synchronized(tagMap) {
            tagMap[tagInfo.name] = tagInfo
        }
        dao.updateOrInsert(tagInfo)
    }

    fun addAll(tagMap: Map<String, TagInfo>) {
        synchronized(tagMap) {
            this.tagMap.putAll(tagMap)
            for (item in tagMap) {
                dao.updateOrInsert(item.value)
            }
        }
    }

    fun get(name: String): TagInfo? {
        synchronized(tagMap) {
            tagMap[name]?.let {
                it.readTime = Date()
                dbHandler.post { dao.updateReadTime(it.id, it.readTime) }
                return it
            }
        }
        dao.get(source, name)?.let {
            it.readTime = Date()
            add(it)
            return it
        }
        return null
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
                if (it.status.value is LoadStatus.Error) {
                    it.status.update { LoadStatus.Waiting }
                    scope.launch(Dispatchers.IO) { it.start() }
                }
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

    fun getHistoryList(): List<TagInfo> = historyList

    fun deleteHistory(name: String) = dbHandler.post {
        synchronized(historyList) {
            val iterator = historyList.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().name == name) {
                    iterator.remove()
                }
            }
        }
        dao.deleteHistory(source, name)
    }

    fun deleteAllHistory() = dbHandler.post {
        synchronized(historyList) {
            historyList.clear()
        }
        dao.deleteAllHistory(source)
    }

    fun dealSearchTags(tagSet: Set<String>) = dbHandler.post {
        for (item in tagSet) {
            dao.updateOrInsertHistory(TagHistory(source = source, name = item))
        }
        loadTagHistory()
    }

    private fun loadTagHistory() = dbHandler.post {
        val historyList = ArrayList<TagInfo>()
        for (item in dao.getAllHistory(source)) {
            historyList.add(getInfo(name = item.name) ?: TagInfo(source = source, name = item.name))
        }
        synchronized(this.historyList) {
            this.historyList.clear()
            this.historyList.addAll(historyList)
        }
    }

    private fun getInfo(name: String): TagInfo? {
        synchronized(tagMap) { tagMap[name] }?.let { return it }
        dao.get(source = source, name = name)?.let { return it }
        return null
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
        delay((0..3000L).random())
        var retryCount = 0
        do {
            parser.requestTagInfo(name)?.let {
                synchronized(tagMap) {
                    tagMap[name] = it
                }
                status.value = LoadStatus.Success(it)
                dbHandler.post { dao.insert(it) }
                return
            }
            retryCount++
            delay(retryCount * (500 + (0L..1000L).random()))
        } while (retryCount < MAX_RETRY_COUNT)
        status.value = LoadStatus.Error(RuntimeException("get tag($name) failed"))
    }

    companion object {
        private val map = HashMap<String, SoftReference<TagManager>>()

        fun get(source: String): TagManager = synchronized(map) {
            map[source]?.get()?.let { return it }
            val manager = when (source) {
                YandeParser.SOURCE -> TagManager(YandeParser.SOURCE)
                KonachanParser.SOURCE -> TagManager(KonachanParser.SOURCE)
                DanbooruParser.SOURCE -> TagManager(DanbooruParser.SOURCE)
                else -> throw RuntimeException("Unknown source: $source")
            }
            map[source] = SoftReference(manager)
            manager
        }

        fun get(website: WebsiteOption): TagManager = synchronized(map) {
            val source = website.name.lowercase()
            map[source]?.get()?.let { return it }
            val manager = when (source) {
                YandeParser.SOURCE -> TagManager(YandeParser.SOURCE)
                KonachanParser.SOURCE -> TagManager(KonachanParser.SOURCE)
                DanbooruParser.SOURCE -> TagManager(DanbooruParser.SOURCE)
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