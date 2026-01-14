package com.magic.maw.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.magic.maw.data.model.constant.TagType
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagHistory
import com.magic.maw.data.model.entity.TagInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Dao
interface TagDao {
    @Insert
    fun insert(tagInfo: TagInfo)

    @Query("SELECT * FROM tag_info")
    fun getAll(): List<TagInfo>

    @Query("SELECT * FROM tag_info WHERE website = :website")
    fun getAll(website: WebsiteOption): List<TagInfo>

    @Query("SELECT * FROM tag_info WHERE website = :website AND read_time > :readDate")
    fun getAllByReadDate(website: WebsiteOption, readDate: Instant): List<TagInfo>

    @Query("SELECT * FROM tag_info WHERE website = :website AND name = :name")
    fun getFromName(website: WebsiteOption, name: String): TagInfo?

    @Query("SELECT * FROM tag_info WHERE website = :website AND tag_id = :tagId")
    fun getFromTagId(website: WebsiteOption, tagId: String): TagInfo?

    @Query("SELECT id FROM tag_info WHERE website = :website AND tag_id = :tagId")
    fun getId(website: WebsiteOption, tagId: String): Int?

    @Update
    fun update(info: TagInfo)

    @Query("UPDATE tag_info SET tag_id = :tagId, name = :name, count = :count, type = :type, update_time = :now, read_time = :now WHERE id = :id")
    fun update(id: Int, tagId: String, name: String, count: Int, type: TagType, now: Instant = Clock.System.now())

    @Query("UPDATE tag_info SET read_time = :now WHERE id = :id")
    fun updateReadTime(id: Int, now: Instant = Clock.System.now())

    @Query("UPDATE tag_info SET read_time = :now WHERE website = :website AND tag_id = :tagId")
    fun updateReadTime(website: WebsiteOption, tagId: String, now: Instant = Clock.System.now())

    @Query("DELETE FROM tag_info")
    fun deleteAll(): Int

    @Query("DELETE FROM tag_info WHERE website = :website")
    fun deleteAll(website: WebsiteOption): Int

    @Query("DELETE FROM tag_info WHERE id = :id")
    fun delete(id: Int)

    @Query("DELETE FROM tag_info WHERE website = :website AND tag_id = :tagId")
    fun delete(website: WebsiteOption, tagId: String)

    @Insert
    fun insertHistory(tagHistory: TagHistory)

    @Query("SELECT * FROM tag_history")
    fun getAllHistory(): List<TagHistory>

    @Query("SELECT * FROM tag_history WHERE website = :website ORDER BY update_time DESC")
    fun getAllHistory(website: WebsiteOption): List<TagHistory>

    @Query("SELECT * FROM tag_history WHERE website = :website ORDER BY update_time DESC LIMIT :limit")
    fun getAllHistory(website: WebsiteOption, limit: Int): List<TagHistory>

    @Query("SELECT * FROM tag_history WHERE website = :website AND name = :name")
    fun getHistory(website: WebsiteOption, name: String): TagHistory?

    @Query("UPDATE tag_history SET update_time = :now WHERE id = :id")
    fun updateHistory(id: Int, now: Instant = Clock.System.now())

    @Query("DELETE FROM tag_history")
    fun deleteAllHistory()

    @Query("DELETE FROM tag_history WHERE website = :website")
    fun deleteAllHistory(website: WebsiteOption)

    @Query("DELETE FROM tag_history WHERE website = :website AND name = :name")
    fun deleteHistory(website: WebsiteOption, name: String)
}