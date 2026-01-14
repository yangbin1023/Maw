package com.magic.maw.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.magic.maw.data.model.constant.DLState
import com.magic.maw.data.model.constant.FileType
import com.magic.maw.data.model.constant.Quality
import com.magic.maw.data.model.constant.WebsiteOption
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
@Entity("dl_info")
data class DLInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val website: WebsiteOption,
    val uid: String,
    val quality: Quality,
    val url: String,
    val width: Int = 0,
    val height: Int = 0,
    @ColumnInfo("file_type")
    val fileType: FileType,
    @ColumnInfo("file_name")
    val fileName: String,
    @ColumnInfo("file_path")
    val filePath: String,
    @ColumnInfo("file_size")
    val fileSize: Long = 0,
    @ColumnInfo("current_size")
    var currentSize: Long = 0,
    @ColumnInfo("create_time")
    val createTime: Instant = Clock.System.now(),
    @ColumnInfo("update_time")
    val updateTime: Instant = Clock.System.now(),
    @ColumnInfo("read_time")
    val readTime: Instant = Clock.System.now(),
    val state: DLState = DLState.None,
    val md5: String? = null,
)