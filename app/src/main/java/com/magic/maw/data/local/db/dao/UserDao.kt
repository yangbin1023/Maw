package com.magic.maw.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.magic.maw.data.model.entity.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Dao
interface UserDao {
    @Query("SELECT * FROM user_info WHERE website = :website AND user_id = :userId")
    fun getFlow(website: String, userId: String): Flow<UserInfo?>

    @Insert
    suspend fun insert(info: UserInfo)

    @Query("SELECT * FROM user_info")
    suspend fun getAll(): List<UserInfo>

    @Query("SELECT * FROM user_info WHERE website = :website")
    suspend fun getAll(website: String): List<UserInfo>

    @Query("SELECT * FROM user_info WHERE website = :website AND read_time > :readDate")
    suspend fun getAllByReadDate(website: String, readDate: Instant): List<UserInfo>

    @Query("SELECT * FROM user_info WHERE website = :website AND user_id = :userId")
    suspend fun get(website: String, userId: String): UserInfo?

    @Transaction
    suspend fun upsert(info: UserInfo) {
        get(info.website, info.userId)?.let {
            update(info.copy(id = it.id))
        } ?: insert(info)
    }

    @Update
    suspend fun update(info: UserInfo)

    @Query("UPDATE user_info SET read_time = :now WHERE id = :id")
    suspend fun updateReadTime(id: Int, now: Instant = Clock.System.now())

    @Query("UPDATE user_info SET read_time = :now WHERE website = :website AND user_id = :userId")
    suspend fun updateReadTime(
        website: String,
        userId: String,
        now: Instant = Clock.System.now()
    )

    @Query("DELETE FROM user_info")
    suspend fun deleteAll()

    @Query("DELETE FROM user_info WHERE website = :website")
    suspend fun deleteAll(website: String)

    @Query("DELETE FROM user_info WHERE website = :website AND user_id = :userId")
    suspend fun delete(website: String, userId: String)
}