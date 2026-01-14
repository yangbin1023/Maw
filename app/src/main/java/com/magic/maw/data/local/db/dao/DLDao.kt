package com.magic.maw.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.magic.maw.data.model.constant.DLState
import com.magic.maw.data.model.constant.Quality
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.DLInfo

@Dao
interface DLDao {
    @Insert
    fun insert(info: DLInfo)

    @Query("SELECT * FROM dl_info")
    fun getAll(): List<DLInfo>

    @Query("SELECT * FROM dl_info WHERE website = :website")
    fun getAll(website: WebsiteOption): List<DLInfo>

    @Query("SELECT * FROM dl_info WHERE website = :website AND quality = :quality")
    fun getAll(website: WebsiteOption, quality: Quality): List<DLInfo>

    @Query("SELECT * FROM dl_info WHERE website = :website AND uid = :uid")
    fun getAll(website: WebsiteOption, uid: String): List<DLInfo>

    @Query("SELECT * FROM dl_info WHERE website = :website AND uid = :uid AND quality = :quality")
    fun get(website: WebsiteOption, uid: String, quality: Quality): DLInfo?

    @Update
    fun update(info: DLInfo)

    @Query("UPDATE dl_info SET state = :dlState WHERE id = :id")
    fun updateDLState(id: Int, dlState: DLState)

    @Query("UPDATE dl_info SET state = :dlState WHERE website = :website AND uid = :uid AND quality = :quality")
    fun updateDLState(website: WebsiteOption, uid: String, quality: Quality, dlState: DLState)

    @Query("DELETE FROM dl_info")
    fun deleteAll(): Int

    @Query("DELETE FROM dl_info WHERE website = :website")
    fun deleteAll(website: WebsiteOption): Int

    @Query("DELETE FROM dl_info WHERE website = :website AND uid = :uid")
    fun deleteAll(website: WebsiteOption, uid: String): Int

    @Query("DELETE FROM dl_info WHERE id = :id")
    fun delete(id: Int)

    @Query("DELETE FROM dl_info WHERE website = :website AND uid = :uid AND quality = :quality")
    fun delete(website: WebsiteOption, uid: String, quality: Quality)
}