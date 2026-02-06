package com.magic.maw.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
@Entity("tag_history")
data class TagHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val website: String,
    val name: String,
    @ColumnInfo("create_time")
    val createTime: Instant = Clock.System.now(),
    @ColumnInfo("update_time")
    val updateTime: Instant = Clock.System.now(),
)
