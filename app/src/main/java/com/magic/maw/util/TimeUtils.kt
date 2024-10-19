package com.magic.maw.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    private const val TAG = "TimeUtils"
    private val dateFormatMap = HashMap<String, SimpleDateFormat>()
    const val FORMAT_1 = "yyyy-MM-dd HH:mm:ss"
    const val FORMAT_2 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    const val FORMAT_3 = "yyyyMMdd_HHmmss"

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
}