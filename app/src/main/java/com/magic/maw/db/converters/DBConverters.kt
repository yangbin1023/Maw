package com.magic.maw.db.converters

import androidx.room.TypeConverter
import com.magic.maw.data.DLState
import com.magic.maw.data.DLState.Companion.toDLState
import com.magic.maw.data.FileType
import com.magic.maw.data.FileType.Companion.toFileType
import com.magic.maw.data.Quality
import com.magic.maw.data.Quality.Companion.toQuality
import com.magic.maw.data.TagType
import com.magic.maw.data.TagType.Companion.toTagType
import java.util.Date

class DBConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromFileType(string: String?): FileType? {
        return string.toFileType()
    }

    @TypeConverter
    fun fileTypeToName(type: FileType?): String? {
        return type?.name
    }

    @TypeConverter
    fun fromQuality(name: String?): Quality? {
        return name?.toQuality()
    }

    @TypeConverter
    fun qualityToName(quality: Quality?): String? {
        return quality?.name
    }

    @TypeConverter
    fun fromDLState(value: Int?): DLState {
        return value.toDLState()
    }

    @TypeConverter
    fun dlStateToValue(state: DLState?): Int? {
        return state?.value
    }

    @TypeConverter
    fun fromTagType(value: Int?): TagType {
        return value.toTagType()
    }

    @TypeConverter
    fun tagTypeToValue(type: TagType?): Int? {
        return type?.value
    }
}