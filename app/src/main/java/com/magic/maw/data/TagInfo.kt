package com.magic.maw.data

import androidx.compose.ui.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.magic.maw.ui.theme.tag_artist
import com.magic.maw.ui.theme.tag_character
import com.magic.maw.ui.theme.tag_circle
import com.magic.maw.ui.theme.tag_copyright
import com.magic.maw.ui.theme.tag_faults
import com.magic.maw.ui.theme.tag_general
import com.magic.maw.ui.theme.tag_none
import java.util.Date

enum class TagType(val value: Int) {
    None(-1),
    General(0),
    Artist(1),
    Copyright(3),
    Character(4),
    Circle(5),
    Faults(6);

    fun getColor(): Color {
        return when (this) {
            General -> tag_general
            Artist -> tag_artist
            Copyright -> tag_copyright
            Character -> tag_character
            Circle -> tag_circle
            Faults -> tag_faults
            else -> tag_none
        }
    }

    /**
     * 用于标签排序时的优先级
     */
    fun getPriority(): Int {
        return when (this) {
            Artist -> 1
            Copyright -> 2
            Character -> 3
            Circle -> 4
            Faults -> 5
            General -> 6
            else -> 7
        }
    }

    companion object {
        fun Int?.toTagType(): TagType {
            for (item in entries) {
                if (item.value == this)
                    return item
            }
            return None
        }
    }
}

@Entity("tag_info")
data class TagInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val source: String,
    val name: String,
    @ColumnInfo("tag_id")
    val tagId: Int = 0,
    val count: Int = 0,
    val type: TagType = TagType.None,
    @ColumnInfo("create_time")
    val createTime: Date = Date(),
    @ColumnInfo("update_time")
    val updateTime: Date = Date(),
    @ColumnInfo("read_time")
    val readTime: Date = Date(),
)