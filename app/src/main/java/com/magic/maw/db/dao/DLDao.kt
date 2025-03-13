package com.magic.maw.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.magic.maw.data.DLInfo
import com.magic.maw.data.DLState
import com.magic.maw.data.Quality

@Dao
interface DLDao {
    @Insert
    fun insert(info: DLInfo)

    @Query("SELECT * FROM dl_info")
    fun getAll(): List<DLInfo>

    @Query("SELECT * FROM dl_info WHERE source = :source")
    fun getAll(source: String): List<DLInfo>

    @Query("SELECT * FROM dl_info WHERE source = :source AND quality = :quality")
    fun getAll(source: String, quality: Quality): List<DLInfo>

    @Query("SELECT * FROM dl_info WHERE source = :source AND uid = :uid")
    fun getAll(source: String, uid: Int): List<DLInfo>

    @Query("SELECT * FROM dl_info WHERE source = :source AND uid = :uid AND quality = :quality")
    fun get(source: String, uid: Int, quality: Quality): DLInfo?

    @Update
    fun update(info: DLInfo)

    @Query("UPDATE dl_info SET state = :dlState WHERE id = :id")
    fun updateDLState(id: Int, dlState: DLState)

    @Query("UPDATE dl_info SET state = :dlState WHERE source = :source AND uid = :uid AND quality = :quality")
    fun updateDLState(source: String, uid: Int, quality: Quality, dlState: DLState)

    @Query("DELETE FROM dl_info")
    fun deleteAll(): Int

    @Query("DELETE FROM dl_info WHERE source = :source")
    fun deleteAll(source: String): Int

    @Query("DELETE FROM dl_info WHERE source = :source AND uid = :uid")
    fun deleteAll(source: String, uid: Int): Int

    @Query("DELETE FROM dl_info WHERE id = :id")
    fun delete(id: Int)

    @Query("DELETE FROM dl_info WHERE source = :source AND uid = :uid AND quality = :quality")
    fun delete(source: String, uid: Int, quality: Quality)
}