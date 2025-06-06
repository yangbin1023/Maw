package com.magic.maw.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.magic.maw.MyApp
import com.magic.maw.data.DLInfo
import com.magic.maw.data.TagHistory
import com.magic.maw.data.TagInfo
import com.magic.maw.data.UserInfo
import com.magic.maw.db.converters.DBConverters
import com.magic.maw.db.dao.DLDao
import com.magic.maw.db.dao.TagDao
import com.magic.maw.db.dao.UserDao
import com.magic.maw.util.dbHandler

@Database(
    version = 3,
    entities = [TagInfo::class, DLInfo::class, TagHistory::class, UserInfo::class],
    autoMigrations = [AutoMigration(1, 2), AutoMigration(2, 3)]
)
@TypeConverters(DBConverters::class)
abstract class AppDB : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun dlDao(): DLDao
    abstract fun userDao(): UserDao

    companion object {
        private val db: AppDB by lazy {
            build(MyApp.app)
        }

        val tagDao get() = db.tagDao()
        val dlDao get() = db.dlDao()
        val userDao get() = db.userDao()

        private fun build(context: Context): AppDB {
            val db = Room.databaseBuilder(context, AppDB::class.java, "app.db")
                .allowMainThreadQueries()
                .addMigrations()
                .build()
            return db
        }
    }
}

fun TagDao.updateOrInsert(tagInfo: TagInfo) = dbHandler.post {
    get(tagInfo.source, tagInfo.name)?.let {
        if (tagInfo.count == 0 && it.count > 0 || tagInfo.tagId == 0) {
            update(it.copy(type = tagInfo.type, readTime = tagInfo.readTime))
        } else {
            update(tagInfo.copy(id = it.id, createTime = it.createTime))
        }
    } ?: insert(tagInfo)
}

fun TagDao.updateOrInsertHistory(tagHistory: TagHistory) = dbHandler.post {
    getHistory(tagHistory.source, tagHistory.name)?.let {
        updateHistory(it.id)
    } ?: insertHistory(tagHistory)
}

fun UserDao.updateOrInsert(userInfo: UserInfo) = dbHandler.post {
    get(userInfo.source, userInfo.userId)?.let {
        update(userInfo.copy(id = it.id))
    } ?: insert(userInfo)
}