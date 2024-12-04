package com.magic.maw.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.magic.maw.data.UserInfo
import java.util.Date

@Dao
interface UserDao {
    @Insert
    fun insert(info: UserInfo)

    @Query("SELECT * FROM user_info")
    fun getAll(): List<UserInfo>

    @Query("SELECT * FROM user_info WHERE source = :source")
    fun getAll(source: String): List<UserInfo>

    @Query("SELECT * FROM user_info WHERE source = :source AND read_time > :readDate")
    fun getAllByReadDate(source: String, readDate: Date): List<UserInfo>

    @Query("SELECT * FROM user_info WHERE source = :source AND user_id = :userId")
    fun get(source: String, userId: Int): UserInfo?

    @Update
    fun update(info: UserInfo)

    @Query("UPDATE user_info SET read_time = :now WHERE id = :id")
    fun updateReadTime(id: Int, now: Date = Date())

    @Query("UPDATE user_info SET read_time = :now WHERE source = :source AND user_id = :userId")
    fun updateReadTime(source: String, userId: Int, now: Date = Date())

    @Query("DELETE FROM user_info")
    fun deleteAll()

    @Query("DELETE FROM user_info WHERE source = :source")
    fun deleteAll(source: String)

    @Query("DELETE FROM user_info WHERE source = :source AND user_id = :userId")
    fun delete(source: String, userId: Int)
}