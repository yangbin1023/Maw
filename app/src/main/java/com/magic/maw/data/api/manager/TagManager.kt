package com.magic.maw.data.api.manager

import co.touchlab.kermit.Logger
import com.magic.maw.data.api.parser.BaseParser
import com.magic.maw.data.local.db.AppDB
import com.magic.maw.data.model.entity.TagHistory
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.local.db.updateOrInsert
import com.magic.maw.data.local.db.updateOrInsertHistory
import com.magic.maw.data.model.LoadStatus
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.util.dbHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.lang.ref.SoftReference
import kotlin.time.Duration.Companion.minutes

private const val TAG = "TagManager"
private const val MAX_RETRY_COUNT = 3

private data class TagTask(
    val website: WebsiteOption,
    val name: String,
    val status: MutableStateFlow<LoadStatus<TagInfo>> = MutableStateFlow(LoadStatus.Waiting),
    var retryCount: Int = 0
)

class TagManager(val website: WebsiteOption) {
    private val dao by lazy { AppDB.tagDao }
    private val tagMap = HashMap<String, TagInfo>()
    private val taskMap = HashMap<String, TagTask>()
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }
    private val parser: BaseParser get() = BaseParser.get(website)
    private val historyList = ArrayList<TagInfo>()

    init {
        dbHandler.post {
            val readDate = Clock.System.now().minus(30.minutes)
            val tags = dao.getAllByReadDate(website, readDate)
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
                it.readTime = Clock.System.now()
                dbHandler.post { dao.updateReadTime(it.id, it.readTime) }
                return it
            }
        }
        dao.getFromName(website, name)?.let {
            it.readTime = Clock.System.now()
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
                it.readTime = Clock.System.now()
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
            }

            val task = TagTask(website, name)
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
        dao.deleteHistory(website, name)
    }

    fun deleteAllHistory() = dbHandler.post {
        synchronized(historyList) {
            historyList.clear()
        }
        dao.deleteAllHistory(website)
    }

    fun dealSearchTags(tagSet: Set<String>) = dbHandler.post {
        for (item in tagSet) {
            dao.updateOrInsertHistory(TagHistory(website = website, name = item))
        }
        loadTagHistory()
    }

    private fun loadTagHistory() = dbHandler.post {
        val historyList = ArrayList<TagInfo>()
        for (item in dao.getAllHistory(website)) {
            historyList.add(getInfo(name = item.name) ?: TagInfo(website = website, name = item.name))
        }
        synchronized(this.historyList) {
            this.historyList.clear()
            this.historyList.addAll(historyList)
        }
    }

    private fun getInfo(name: String): TagInfo? {
        synchronized(tagMap) { tagMap[name] }?.let { return it }
        dao.getFromName(website = website, name = name)?.let { return it }
        return null
    }

    private suspend fun TagTask.start() {
        // 从数据库中获取
        dao.getFromName(website, name)?.let {
            it.readTime = Clock.System.now()
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
        Logger.w(TAG) { "request tag: $name" }
        do {
            parser.requestTagInfo(name)?.let {
                synchronized(tagMap) {
                    tagMap[name] = it
                }
                status.value = LoadStatus.Success(it)
                dao.updateOrInsert(it)
                return
            }
            retryCount++
            delay(retryCount * (500 + (0L..1000L).random()))
        } while (retryCount < MAX_RETRY_COUNT)
        status.value = LoadStatus.Error(RuntimeException("get tag($name) failed"))
    }

    companion object {
        private val map = HashMap<WebsiteOption, SoftReference<TagManager>>()

        fun get(website: WebsiteOption): TagManager = synchronized(map) {
            map[website]?.get()?.let { return it }
            TagManager(website).apply {
                map[website] = SoftReference(this)
            }
        }
    }
}