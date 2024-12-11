package com.magic.maw.website

import com.magic.maw.data.UserInfo
import com.magic.maw.db.AppDB
import com.magic.maw.db.updateOrInsert
import com.magic.maw.util.dbHandler
import com.magic.maw.website.parser.BaseParser
import com.magic.maw.website.parser.DanbooruParser
import com.magic.maw.website.parser.YandeParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.SoftReference
import java.util.Calendar
import java.util.Date

class UserManager(val source: String) {
    private val dao by lazy { AppDB.userDao }
    private val userMap = HashMap<Int, UserInfo>()
    private val taskMap = HashMap<Int, UserTask>()
    private val parser: BaseParser get() = BaseParser.get(source)
    private val scope by lazy { CoroutineScope(Dispatchers.IO) }

    init {
        dbHandler.post {
            val readDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time
            val userInfos = dao.getAllByReadDate(source, readDate)
            synchronized(userMap) {
                for (info in userInfos) {
                    userMap[info.userId] = info
                }
            }
        }
    }

    fun get(userId: Int): UserInfo? {
        synchronized(userMap) {
            userMap[userId]?.let {
                it.readTime = Date()
                dbHandler.post { dao.updateReadTime(source, it.userId, it.readTime) }
                return it
            }
        }
        dao.get(source, userId)?.let {
            it.readTime = Date()
            dbHandler.post { add(it) }
            return it
        }
        return null
    }

    fun getStatus(
        userId: Int,
        scope: CoroutineScope = this.scope
    ): MutableStateFlow<LoadStatus<UserInfo>> {
        synchronized(userMap) {
            userMap[userId]?.let {
                it.readTime = Date()
                dbHandler.post { dao.updateReadTime(source, it.userId, it.readTime) }
                return MutableStateFlow(LoadStatus.Success(it))
            }
        }
        synchronized(taskMap) {
            taskMap[userId]?.let {
                return it.status
            } ?: let {
                val task = UserTask(source, userId)
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
    }

    fun add(userInfo: UserInfo) {
        synchronized(userMap) {
            userMap[userInfo.userId] = userInfo
        }
        dbHandler.post {
            dao.updateOrInsert(userInfo)
        }
    }

    private suspend fun UserTask.start() {
        dao.get(source, userId)?.let {
            it.readTime = Date()
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
        private val map = HashMap<String, SoftReference<UserManager>>()

        fun get(source: String): UserManager = synchronized(map) {
            map[source]?.get()?.let { return it }
            val manager = when (source) {
                YandeParser.SOURCE -> UserManager(YandeParser.SOURCE)
                DanbooruParser.SOURCE -> UserManager(DanbooruParser.SOURCE)
                else -> throw RuntimeException("Unknown source: $source")
            }
            map[source] = SoftReference(manager)
            manager
        }
    }
}

data class UserTask(
    val source: String,
    val userId: Int,
    val status: MutableStateFlow<LoadStatus<UserInfo>> = MutableStateFlow(LoadStatus.Waiting)
)