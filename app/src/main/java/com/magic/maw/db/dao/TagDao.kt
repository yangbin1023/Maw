package com.magic.maw.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.magic.maw.data.TagInfo
import com.magic.maw.data.TagType
import java.util.Date

@Dao
interface TagDao {
    @Insert
    fun insert(tagInfo: TagInfo)

    @Query("SELECT * FROM tag_info")
    fun getAll(): List<TagInfo>

    @Query("SELECT * FROM tag_info WHERE source = :source")
    fun getAll(source: String): List<TagInfo>

    @Query("SELECT * FROM tag_info WHERE source = :source AND read_time > :readDate")
    fun getAllByReadDate(source: String, readDate: Date): List<TagInfo>

    @Query("SELECT * FROM tag_info WHERE source = :source AND tag_id = :tagId")
    fun get(source: String, tagId: Int): TagInfo?

    @Query("SELECT id FROM tag_info WHERE source = :source AND tag_id = :tagId")
    fun getId(source: String, tagId: Int): Int?

    @Update
    fun update(info: TagInfo)

    @Query("UPDATE tag_info SET tag_id = :tagId, name = :name, count = :count, type = :type, update_time = :now, read_time = :now WHERE id = :id")
    fun update(id: Int, tagId: Int, name: String, count: Int, type: TagType, now: Date = Date())

    @Query("UPDATE tag_info SET read_time = :now WHERE id = :id")
    fun updateReadTime(id: Int, now: Date = Date())

    @Query("UPDATE tag_info SET read_time = :now WHERE source = :source AND tag_id = :tagId")
    fun updateReadTime(source: String, tagId: Int, now: Date = Date())

    @Query("DELETE FROM tag_info")
    fun deleteAll(): Int

    @Query("DELETE FROM tag_info WHERE source = :source")
    fun deleteAll(source: String): Int

    @Query("DELETE FROM tag_info WHERE id = :id")
    fun delete(id: Int)

    @Query("DELETE FROM tag_info WHERE source = :source AND tag_id = :tagId")
    fun delete(source: String, tagId: Int)
}