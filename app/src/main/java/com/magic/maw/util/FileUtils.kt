package com.magic.maw.util

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException


object FileUtils {
    fun isTextFile(path: String, checkLen: Int = 256): Boolean {
        return isTextFile(File(path), checkLen)
    }

    fun isTextFile(file: File, checkLen: Int = 256): Boolean {
        if (!file.exists()) {
            return false
        }
        try {
            BufferedReader(FileReader(file)).use { br ->
                var character: Int
                var count = 0
                while ((br.read()
                        .also { character = it }) != -1 && (checkLen <= 0 || count < checkLen)
                ) {
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

    fun File.isTextFile(): Boolean {
        return isTextFile(this)
    }
}