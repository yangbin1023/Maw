package com.magic.maw.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File
import java.util.Date


enum class DLState(val value: Int) {
    None(0),
    Waiting(1),
    Downloading(2),
    Pause(3),
    Failed(4),
    Finished(5);

    companion object {
        fun Int?.toDLState(): DLState {
            for (item in entries) {
                if (item.value == this)
                    return item
            }
            return None
        }
    }
}

@Entity("dl_info")
data class DLInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val source: String,
    val uid: Int,
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
    val createTime: Date = Date(),
    @ColumnInfo("update_time")
    val updateTime: Date = Date(),
    val state: DLState = DLState.None,
    val md5: String? = null,
) {
    fun getFullPath(): String {
        return filePath + File.separator + fileName
    }
}