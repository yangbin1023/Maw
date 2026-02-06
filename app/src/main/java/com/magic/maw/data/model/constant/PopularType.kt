package com.magic.maw.data.model.constant

import androidx.annotation.StringRes
import com.magic.maw.R
import kotlinx.serialization.Serializable

@Serializable
enum class PopularType(val value: Int) {
    Unknown(0),
    Day(1 shl 0),
    Week(1 shl 1),
    Month(1 shl 2),
    Year(1 shl 3),
    All(1 shl 4),
    Custom(1 shl 5);

    @StringRes
    fun getResStrId(): Int {
        return when (this) {
            Day -> R.string.every_day
            Week -> R.string.every_week
            Month -> R.string.every_month
            Year -> R.string.every_year
            else -> R.string.all
        }
    }

    companion object {
        val defaultSupportedDateTypes: List<PopularType> = listOf(Day, Week, Month, All)

        fun String.toPopularType(): PopularType? {
            for (item in entries) {
                if (item.name == this)
                    return item
            }
            return null
        }
    }
}