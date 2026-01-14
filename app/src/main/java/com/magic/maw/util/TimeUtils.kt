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
        }
        synchronized(dateFormatMap) {
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

    fun getTimeStr(formatStr: String, date: Date): String {
        val simpleDateFormat = getFormat(formatStr)
        return simpleDateFormat.format(date)
    }

    fun getDate(formatStr: String, timeStr: String): Date {
        val simpleDateFormat = getFormat(formatStr)
        return simpleDateFormat.parse(timeStr)!!
    }

    fun getCurrentTimeStr(formatStr: String): String {
        val simpleDateFormat = getFormat(formatStr)
        return simpleDateFormat.format(Date())
    }

    fun Long.toFormatStr(formatStr: String = FORMAT_1): String {
        val simpleDateFormat = getFormat(formatStr)
        return simpleDateFormat.format(Date(this))
    }

    fun Long.formatTimeStr(hasHour: Boolean = false): String {
        val value = this / 1000
        if (hasHour) {
            val second = value % 60
            val minute = value / 60 % 60
            val hour = value / 3600
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hour, minute, second)
        } else {
            val second = value % 60
            val minute = value / 60
            return String.format(Locale.getDefault(), "%02d:%02d", minute, second)
        }
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