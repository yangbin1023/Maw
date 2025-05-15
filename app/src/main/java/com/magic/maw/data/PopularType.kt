package com.magic.maw.data

import androidx.annotation.StringRes
import com.magic.maw.R

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
        val defaultSupport: Int = Day.value or Week.value or Month.value or All.value

        fun Int.toPopularType(): PopularType {
            for (item in PopularType.entries) {
                if (item.value == this)
                    return item
            }
            return Unknown
        }

        fun Int.toPopularTypes(): List<PopularType> {
            val list = mutableListOf<PopularType>()
            for (item in PopularType.entries) {
                if (item != Unknown && (this and item.value) == item.value) {
                    list.add(item)
                }
            }
            return list
        }
    }
}