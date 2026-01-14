package com.magic.maw.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.magic.maw.data.model.constant.WebsiteOption
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Entity("user_info")
data class UserInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val website: WebsiteOption,
    @ColumnInfo("user_id")
    val userId: String,
    val name: String = "",
    @ColumnInfo("create_time")
    val createTime: Instant = Clock.System.now(),
    @ColumnInfo("update_time")
    val updateTime: Instant = Clock.System.now(),
    @ColumnInfo("read_time")
    var readTime: Instant = Clock.System.now(),
)