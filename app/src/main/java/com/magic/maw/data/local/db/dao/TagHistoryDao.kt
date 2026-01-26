package com.magic.maw.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagHistory
import com.magic.maw.data.model.entity.TagInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Dao
interface TagHistoryDao {
    @Query("SELECT * FROM tag_history WHERE website = :website ORDER BY update_time DESC")
    fun getAllFlow(website: WebsiteOption): Flow<List<TagHistory>>

    @Query("SELECT * FROM tag_history WHERE website = :website ORDER BY update_time DESC LIMIT :limit")
    fun getAllFlow(website: WebsiteOption, limit: Int): Flow<List<TagHistory>>

    @Query("SELECT * FROM tag_info WHERE name IN (SELECT name FROM tag_history WHERE website = :website ORDER BY update_time DESC LIMIT :limit)")
    fun getAllTagFlow(website: WebsiteOption, limit: Int): Flow<List<TagInfo>>

    @Insert
    suspend fun insert(tagHistory: TagHistory)

    @Query("SELECT * FROM tag_history")
    suspend fun getAll(): List<TagHistory>

    @Query("SELECT * FROM tag_history WHERE website = :website ORDER BY update_time DESC")
    suspend fun getAll(website: WebsiteOption): List<TagHistory>

    @Query("SELECT * FROM tag_history WHERE website = :website ORDER BY update_time DESC LIMIT :limit")
    suspend fun getAll(website: WebsiteOption, limit: Int): List<TagHistory>

    @Query("SELECT * FROM tag_history WHERE website = :website AND name = :name")
    suspend fun get(website: WebsiteOption, name: String): TagHistory?

    @Transaction
    suspend fun upsert(tagHistory: TagHistory) {
        get(tagHistory.website, tagHistory.name)?.let {
            update(it.id)
        } ?: insert(tagHistory)
    }

    @Query("UPDATE tag_history SET update_time = :now WHERE id = :id")
    suspend fun update(id: Int, now: Instant = Clock.System.now())

    @Query("DELETE FROM tag_history")
    suspend fun deleteAll()

    @Query("DELETE FROM tag_history WHERE website = :website")
    suspend fun deleteAll(website: WebsiteOption)

    @Query("DELETE FROM tag_history WHERE website = :website AND name = :name")
    suspend fun delete(website: WebsiteOption, name: String)
}