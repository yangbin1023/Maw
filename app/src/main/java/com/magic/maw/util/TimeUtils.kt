package com.magic.maw.util

import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object TimeUtils {
    private const val TAG = "TimeUtils"
    private val dateFormatMap = HashMap<String, SimpleDateFormat>()
    const val FORMAT_1 = "yyyy-MM-dd HH:mm:ss"
    const val FORMAT_2 = "yyyy/MM/dd HH:mm:ss"
    const val FORMAT_3 = "yyyyMMdd_HHmmss"
    const val FORMAT_4 = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    const val FORMAT_5 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    private fun getFormat(formatStr: String): SimpleDateFormat {
        dateFormatMap[formatStr]?.let {
            return it
        } ?: synchronized(dateFormatMap) {
            dateFormatMap[formatStr]?.let { return it }
            val format = SimpleDateFormat(formatStr, Locale.getDefault())
            dateFormatMap[formatStr] = format
            return format
        }
    }

    fun getUnixTime(formatStr: String, timeStr: String?): Long? {
        timeStr ?: return null
        val simpleDateFormat = getFormat(formatStr)
        try {
            return simpleDateFormat.parse(timeStr)?.time
        } catch (_: Throwable) {
        }
        return null
    }

    fun getTimeStr(formatStr: String, time: Long): String {
        val simpleDateFormat = getFormat(formatStr)
        return simpleDateFormat.format(Date(time))
    }

    fun getCurrentTimeStr(formatStr: String): String {
        val simpleDateFormat = getFormat(formatStr)
        return simpleDateFormat.format(Date())
    }

    fun Long.toFormatStr(formatStr: String = FORMAT_1): String {
        val simpleDateFormat = getFormat(formatStr)
        return simpleDateFormat.format(Date(this))
    }
}

fun LocalDate.toMonday(): LocalDate {
    return minusDays((dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
}

fun LocalDate.toSunday(): LocalDate {
    return plusDays((DayOfWeek.SUNDAY.value - dayOfWeek.value).toLong())
}

fun LocalDate.format(): String {
    return format(DateTimeFormatter.ofPattern("yyyy/M/d"))
}