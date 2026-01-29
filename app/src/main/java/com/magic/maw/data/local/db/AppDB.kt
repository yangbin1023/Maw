package com.magic.maw.data.local.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.magic.maw.data.local.db.converters.DBConverters
import com.magic.maw.data.local.db.dao.DLDao
import com.magic.maw.data.local.db.dao.TagHistoryDao
import com.magic.maw.data.local.db.dao.TagDao
import com.magic.maw.data.local.db.dao.UserDao
import com.magic.maw.data.model.entity.DLInfo
import com.magic.maw.data.model.entity.TagHistory
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.entity.UserInfo
import com.magic.maw.data.local.db.migration.Migration3To4

@Database(
    version = 4,
    entities = [TagInfo::class, DLInfo::class, TagHistory::class, UserInfo::class],
    autoMigrations = [AutoMigration(1, 2), AutoMigration(2, 3)]
)
@TypeConverters(DBConverters::class)
abstract class AppDB : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun tagHistoryDao(): TagHistoryDao
    abstract fun dlDao(): DLDao
    abstract fun userDao(): UserDao

    companion object {
        fun build(context: Context): AppDB {
            val db = Room.databaseBuilder(context, AppDB::class.java, "app.db")
                .allowMainThreadQueries()
                .addMigrations(Migration3To4())
                .build()
            return db
        }
    }
}