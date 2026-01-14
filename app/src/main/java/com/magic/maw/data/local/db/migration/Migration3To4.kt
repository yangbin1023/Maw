package com.magic.maw.data.local.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration3To4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateDLInfo(db)
        migrateTagInfo(db)
        migrateTagHistory(db)
        migrateUserInfo(db)
    }

    private fun migrateDLInfo(db: SupportSQLiteDatabase) {
        // 1. 创建新结构的临时表
        // 注意：uid 变为 TEXT，新增了 read_time 字段，source 变为 website
        //language=RoomSql
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `dl_info_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `website` TEXT NOT NULL, 
                `uid` TEXT NOT NULL, 
                `quality` TEXT NOT NULL, 
                `url` TEXT NOT NULL, 
                `width` INTEGER NOT NULL, 
                `height` INTEGER NOT NULL, 
                `file_type` TEXT NOT NULL, 
                `file_name` TEXT NOT NULL, 
                `file_path` TEXT NOT NULL, 
                `file_size` INTEGER NOT NULL, 
                `current_size` INTEGER NOT NULL, 
                `create_time` INTEGER NOT NULL, 
                `update_time` INTEGER NOT NULL, 
                `read_time` INTEGER NOT NULL, 
                `state` INTEGER NOT NULL, 
                `md5` TEXT
            )
        """.trimIndent()
        )

        // 2. 迁移数据
        // - (UPPER...) 处理 website 首字母大写
        // - CAST(uid AS TEXT) 处理类型转换
        // - 将 update_time 的值同时赋给 update_time 和 read_time 字段
        //language=RoomSql
        db.execSQL(
            """
            INSERT INTO dl_info_new (
                website, uid, quality, url, width, height, file_type, 
                file_name, file_path, file_size, current_size, 
                create_time, update_time, read_time, state, md5
            )
            SELECT 
                (UPPER(SUBSTR(source, 1, 1)) || SUBSTR(source, 2)), 
                CAST(uid AS TEXT), 
                quality, url, width, height, file_type, 
                file_name, file_path, file_size, current_size, 
                create_time, update_time, update_time,
                state, md5
            FROM dl_info
        """.trimIndent()
        )

        // 3. 删除旧表
        //language=RoomSql
        db.execSQL("DROP TABLE dl_info")

        // 4. 重命名新表
        //language=RoomSql
        db.execSQL("ALTER TABLE dl_info_new RENAME TO dl_info")
    }

    private fun migrateTagInfo(db: SupportSQLiteDatabase) {
        // 1. 创建新表
        // website 对应旧 source, tag_id 变为 TEXT
        //language=RoomSql
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `tag_info_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `website` TEXT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `tag_id` TEXT NOT NULL, 
                    `count` INTEGER NOT NULL, 
                    `type` INTEGER NOT NULL, 
                    `create_time` INTEGER NOT NULL, 
                    `update_time` INTEGER NOT NULL, 
                    `read_time` INTEGER NOT NULL
                )
        """.trimIndent()
        )

        // 2. 迁移数据并处理逻辑：
        // - 首字母大写：UPPER(SUBSTR(source, 1, 1)) || SUBSTR(source, 2)
        // - 类型转换：CAST(tag_id AS TEXT)
        // - 去重：使用 GROUP BY source, name (会自动选择每组的第一条)
        //language=RoomSql
        db.execSQL(
            """
            INSERT INTO tag_info_new (website, name, tag_id, count, type, create_time, update_time, read_time)
            SELECT 
                (UPPER(SUBSTR(source, 1, 1)) || SUBSTR(source, 2)) AS website, -- 首字母大写
                name, 
                CAST(tag_id AS TEXT), 
                count, 
                type, 
                create_time, 
                update_time, 
                read_time
            FROM tag_info
            GROUP BY source, name
            ORDER BY id ASC
        """.trimIndent()
        )

        // 3. 删除旧表
        //language=RoomSql
        db.execSQL("DROP TABLE tag_info")

        // 4. 将新表重命名为原表名
        //language=RoomSql
        db.execSQL("ALTER TABLE tag_info_new RENAME TO tag_info")
    }

    private fun migrateTagHistory(db: SupportSQLiteDatabase) {
        //language=RoomSql
        db.execSQL(
            """
            CREATE TABLE `tag_history_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `website` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                `create_time` INTEGER NOT NULL, 
                `update_time` INTEGER NOT NULL
            )
        """.trimIndent()
        )
        //language=RoomSql
        db.execSQL(
            """
            INSERT INTO tag_history_new (website, name, create_time, update_time)
            SELECT 
                (UPPER(SUBSTR(source, 1, 1)) || SUBSTR(source, 2)), 
                name, create_time, update_time
            FROM tag_history
        """.trimIndent()
        )
        //language=RoomSql
        db.execSQL("DROP TABLE tag_history")
        //language=RoomSql
        db.execSQL("ALTER TABLE tag_history_new RENAME TO tag_history")
    }

    private fun migrateUserInfo(db: SupportSQLiteDatabase) {
        //language=RoomSql
        db.execSQL("CREATE TABLE `user_info_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `website` TEXT NOT NULL, `user_id` TEXT NOT NULL, `name` TEXT NOT NULL, `create_time` INTEGER NOT NULL, `update_time` INTEGER NOT NULL, `read_time` INTEGER NOT NULL)")
        //language=RoomSql
        db.execSQL(
            """
            INSERT INTO user_info_new (website, user_id, name, create_time, update_time, read_time)
            SELECT 
                (UPPER(SUBSTR(source, 1, 1)) || SUBSTR(source, 2)), 
                CAST(user_id AS TEXT), 
                name, 
                update_time, -- 将旧表的 update_time 赋给新表的 create_time
                update_time, 
                read_time
            FROM user_info
        """
        )
        //language=RoomSql
        db.execSQL("DROP TABLE user_info")
        //language=RoomSql
        db.execSQL("ALTER TABLE user_info_new RENAME TO user_info")
    }
}