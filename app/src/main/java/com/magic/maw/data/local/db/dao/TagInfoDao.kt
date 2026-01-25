package com.magic.maw.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.magic.maw.data.model.constant.TagType
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Dao
interface TagInfoDao {
    @Insert
    suspend fun insert(tagInfo: TagInfo)

    @Query("SELECT * FROM tag_info")
    suspend fun getAll(): List<TagInfo>

    @Query("SELECT * FROM tag_info WHERE website = :website")
    suspend fun getAll(website: WebsiteOption): List<TagInfo>

    @Query("SELECT * FROM tag_info WHERE website = :website AND read_time > :readDate")
    suspend fun getAllByReadDate(website: WebsiteOption, readDate: Instant): List<TagInfo>

    @Query("SELECT * FROM tag_info WHERE website = :website AND name = :name")
    suspend fun getByName(website: WebsiteOption, name: String): TagInfo?

    @Query("SELECT * FROM tag_info WHERE website = :website AND tag_id = :tagId")
    suspend fun getTagId(website: WebsiteOption, tagId: String): TagInfo?

    @Update
    suspend fun update(info: TagInfo)

    @Transaction
    suspend fun upsert(info: TagInfo) {
        getByName(website = info.website, name = info.name)?.let {
            if (info.count == 0 && it.count > 0 || info.tagId.isEmpty()) {
                update(it.copy(type = info.type, readTime = info.readTime))
            } else {
                update(info.copy(id = it.id, createTime = it.createTime))
            }
        } ?: insert(info)
    }

    @Query("UPDATE tag_info SET tag_id = :tagId, name = :name, count = :count, type = :type, update_time = :now, read_time = :now WHERE id = :id")
    suspend fun update(
        id: Int,
        tagId: String,
        name: String,
        count: Int,
        type: TagType,
        now: Instant = Clock.System.now()
    )

    @Query("UPDATE tag_info SET read_time = :now WHERE id = :id")
    suspend fun updateReadTime(id: Int, now: Instant = Clock.System.now())

    @Query("UPDATE tag_info SET read_time = :now WHERE website = :website AND tag_id = :tagId")
    suspend fun updateReadTime(
        website: WebsiteOption,
        tagId: String,
        now: Instant = Clock.System.now()
    )

    @Query("DELETE FROM tag_info")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM tag_info WHERE website = :website")
    suspend fun deleteAll(website: WebsiteOption): Int

    @Query("DELETE FROM tag_info WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM tag_info WHERE website = :website AND tag_id = :tagId")
    suspend fun delete(website: WebsiteOption, tagId: String)
}