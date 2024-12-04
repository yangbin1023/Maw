package com.magic.maw.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity("user_info")
data class UserInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val source: String = "",
    @ColumnInfo("user_id")
    val userId: Int = 0,
    val name: String = "",
    @ColumnInfo("update_time")
    val updateTime: Date = Date(),
    @ColumnInfo("read_time")
    var readTime: Date = Date(),
)