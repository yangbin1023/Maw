package com.magic.maw.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.magic.maw.MyApp
import com.magic.maw.data.DLInfo
import com.magic.maw.data.TagInfo
import com.magic.maw.db.converters.DBConverters
import com.magic.maw.db.dao.DLDao
import com.magic.maw.db.dao.TagDao

@Database(
    entities = [TagInfo::class, DLInfo::class],
    version = 1
)
@TypeConverters(DBConverters::class)
abstract class AppDB : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun dlDao(): DLDao

    companion object {
        private val db: AppDB by lazy {
            build(MyApp.app)
        }

        val tagDao get() = db.tagDao()
        val dlDao get() = db.dlDao()

        private fun build(context: Context): AppDB {
            val db = Room.databaseBuilder(context, AppDB::class.java, "app.db")
                .allowMainThreadQueries()
                .addMigrations()
                .build()
            return db
        }
    }
}

fun TagDao.updateOrInsert(tagInfo: TagInfo) {
    get(tagInfo.source, tagInfo.name)?.let {
        update(tagInfo.copy(id = it.id))
    } ?: insert(tagInfo)
}