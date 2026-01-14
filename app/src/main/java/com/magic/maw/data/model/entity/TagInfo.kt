package com.magic.maw.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.magic.maw.data.model.constant.TagType
import com.magic.maw.data.model.constant.WebsiteOption
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
@Entity("tag_info")
data class TagInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val website: WebsiteOption,
    val name: String,
    @ColumnInfo("tag_id")
    val tagId: String = "",
    val count: Int = 0,
    val type: TagType = TagType.None,
    @ColumnInfo("create_time")
    val createTime: Instant = Clock.System.now(),
    @ColumnInfo("update_time")
    val updateTime: Instant = Clock.System.now(),
    @ColumnInfo("read_time")
    var readTime: Instant = Clock.System.now(),
) : Comparable<TagInfo> {
    override fun compareTo(other: TagInfo): Int {
        return if (type != other.type)
            type.priority compareTo other.type.priority
        else
            name compareTo other.name
    }
}
