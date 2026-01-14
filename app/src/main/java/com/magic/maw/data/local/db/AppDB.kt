package com.magic.maw.data.local.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.magic.maw.MyApp
import com.magic.maw.data.local.db.converters.DBConverters
import com.magic.maw.data.local.db.dao.DLDao
import com.magic.maw.data.local.db.dao.TagDao
import com.magic.maw.data.local.db.dao.UserDao
import com.magic.maw.data.model.entity.DLInfo
import com.magic.maw.data.model.entity.TagHistory
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.entity.UserInfo
import com.magic.maw.data.local.db.migration.Migration3To4
import com.magic.maw.util.dbHandler

@Database(
    version = 4,
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
                .addMigrations(Migration3To4())
                .build()
            return db
        }
    }
}

fun TagDao.updateOrInsert(tagInfo: TagInfo) = dbHandler.post {
    getFromName(tagInfo.website, tagInfo.name)?.let {
        if (tagInfo.count == 0 && it.count > 0 || tagInfo.tagId.isEmpty()) {
            update(it.copy(type = tagInfo.type, readTime = tagInfo.readTime))
        } else {
            update(tagInfo.copy(id = it.id, createTime = it.createTime))
        }
    } ?: insert(tagInfo)
}

fun TagDao.updateOrInsertHistory(tagHistory: TagHistory) = dbHandler.post {
    getHistory(tagHistory.website, tagHistory.name)?.let {
        updateHistory(it.id)
    } ?: insertHistory(tagHistory)
}

fun UserDao.updateOrInsert(userInfo: UserInfo) = dbHandler.post {
    get(userInfo.website, userInfo.userId)?.let {
        update(userInfo.copy(id = it.id))
    } ?: insert(userInfo)
}