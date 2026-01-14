package com.magic.maw.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.UserInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Dao
interface UserDao {
    @Insert
    fun insert(info: UserInfo)

    @Query("SELECT * FROM user_info")
    fun getAll(): List<UserInfo>

    @Query("SELECT * FROM user_info WHERE website = :website")
    fun getAll(website: WebsiteOption): List<UserInfo>

    @Query("SELECT * FROM user_info WHERE website = :website AND read_time > :readDate")
    fun getAllByReadDate(website: WebsiteOption, readDate: Instant): List<UserInfo>

    @Query("SELECT * FROM user_info WHERE website = :website AND user_id = :userId")
    fun get(website: WebsiteOption, userId: String): UserInfo?

    @Update
    fun update(info: UserInfo)

    @Query("UPDATE user_info SET read_time = :now WHERE id = :id")
    fun updateReadTime(id: Int, now: Instant = Clock.System.now())

    @Query("UPDATE user_info SET read_time = :now WHERE website = :website AND user_id = :userId")
    fun updateReadTime(website: WebsiteOption, userId: String, now: Instant = Clock.System.now())

    @Query("DELETE FROM user_info")
    fun deleteAll()

    @Query("DELETE FROM user_info WHERE website = :website")
    fun deleteAll(website: WebsiteOption)

    @Query("DELETE FROM user_info WHERE website = :website AND user_id = :userId")
    fun delete(website: WebsiteOption, userId: String)
}