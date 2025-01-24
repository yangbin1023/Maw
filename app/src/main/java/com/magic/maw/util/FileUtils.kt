package com.magic.maw.util

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.IOException

fun File.isTextFile(len: Int = 256): Boolean {
    if (!this.exists()) {
        return false
    }
    try {
        BufferedReader(FileReader(this)).use { br ->
            var character: Int
            var count = 0
            while ((br.read().also { character = it }) != -1 && (len <= 0 || count < len)) {
                if (character > 0x7F) {
                    return false // 非ASCII字符，可能是二进制文件
                }
                count++
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
        return false
    }
    return true
}

fun File.readString(charset: String? = null): String {
    var result: String
    FileInputStream(this).use { fis ->
        val bos = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        var len: Int
        while (fis.read(buf).also { len = it } != -1) {
            bos.write(buf, 0, len)
        }
        result = charset?.let {
            bos.toString(it)
        } ?: let {
            bos.toString()
        }
    }
    return result
}