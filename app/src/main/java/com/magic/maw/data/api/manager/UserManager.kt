package com.magic.maw.data.api.manager

import com.magic.maw.data.api.parser.BaseParser
import com.magic.maw.data.local.db.AppDB
import com.magic.maw.data.model.entity.UserInfo
import com.magic.maw.data.local.db.updateOrInsert
import com.magic.maw.data.model.LoadStatus
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.util.dbHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.lang.ref.SoftReference
import kotlin.time.Duration.Companion.days

private data class UserTask(
    val website: WebsiteOption,
    val userId: String,
    val status: MutableStateFlow<LoadStatus<UserInfo>> = MutableStateFlow(LoadStatus.Waiting)
)

class UserManager(val website: WebsiteOption) {
    private val dao by lazy { AppDB.userDao }
    private val userMap = HashMap<String, UserInfo>()
    private val taskMap = HashMap<String, UserTask>()
    private val parser: BaseParser get() = BaseParser.get(website)
    private val scope by lazy { CoroutineScope(Dispatchers.IO) }

    init {
        dbHandler.post {
            val readDate = Clock.System.now().minus(1.days)
            val userInfos = dao.getAllByReadDate(website, readDate)
            synchronized(userMap) {
                for (info in userInfos) {
                    userMap[info.userId] = info
                }
            }
        }
    }

    fun get(userId: String): UserInfo? {
        synchronized(userMap) {
            userMap[userId]?.let {
                it.readTime = Clock.System.now()
                dbHandler.post { dao.updateReadTime(website, it.userId, it.readTime) }
                return it
            }
        }
        dao.get(website, userId)?.let {
            it.readTime = Clock.System.now()
            add(it)
            return it
        }
        return null
    }

    fun getStatus(
        userId: String,
        scope: CoroutineScope = this.scope
    ): MutableStateFlow<LoadStatus<UserInfo>> {
        synchronized(userMap) {
            userMap[userId]?.let {
                it.readTime = Clock.System.now()
                dbHandler.post { dao.updateReadTime(website, it.userId, it.readTime) }
                return MutableStateFlow(LoadStatus.Success(it))
            }
        }
        synchronized(taskMap) {
            taskMap[userId]?.let {
                return it.status
            }
            val task = UserTask(website, userId)
            taskMap[userId] = task
            scope.launch(Dispatchers.IO) {
                task.start()
                synchronized(taskMap) {
                    taskMap.remove(task.userId)
                }
            }
            return task.status
        }
    }

    fun add(userInfo: UserInfo) {
        synchronized(userMap) {
            userMap[userInfo.userId] = userInfo
        }
        dao.updateOrInsert(userInfo)
    }

    private suspend fun UserTask.start() {
        dao.get(website, userId)?.let {
            it.readTime = Clock.System.now()
            status.value = LoadStatus.Success(it)
            add(it)
            return
        }
        delay((0..1000L).random())
        var retryCount = 0
        do {
            try {
                parser.requestUserInfo(userId)?.let {
                    status.value = LoadStatus.Success(it)
                    add(it)
                    return
                }
            } catch (_: Throwable) {
            }
            retryCount++
            delay(retryCount * (500 + (0L..1000L).random()))
        } while (retryCount < 3)
        status.value = LoadStatus.Error(RuntimeException("get user info failed. id: $userId"))
    }

    companion object {
        private val map = HashMap<WebsiteOption, SoftReference<UserManager>>()

        fun get(website: WebsiteOption): UserManager = synchronized(map) {
            map[website]?.get()?.let { return it }
            UserManager(website).apply {
                map[website] = SoftReference(this)
            }
        }
    }
}