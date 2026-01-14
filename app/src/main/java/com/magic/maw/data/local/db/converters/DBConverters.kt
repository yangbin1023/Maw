package com.magic.maw.data.local.db.converters

import androidx.room.TypeConverter
import com.magic.maw.data.model.constant.DLState
import com.magic.maw.data.model.constant.DLState.Companion.toDLState
import com.magic.maw.data.model.constant.TagType
import com.magic.maw.data.model.constant.TagType.Companion.toTagType
import kotlinx.datetime.Instant

class DBConverters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun fromDLState(value: Int?): DLState = value.toDLState()

    @TypeConverter
    fun dlStateToValue(state: DLState?): Int? = state?.value

    @TypeConverter
    fun fromTagType(value: Int?): TagType = value.toTagType()

    @TypeConverter
    fun tagTypeToValue(type: TagType?): Int? = type?.value
}