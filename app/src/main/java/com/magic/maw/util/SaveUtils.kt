package com.magic.maw.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import java.io.File
import java.io.FileInputStream

fun saveToPicture(context: Context, postData: PostData, quality: Quality, file: File): Uri {
    val source = postData.source
    val name = "${source}_${postData.id}_${quality.name.lowercase()}"
    val extension = postData.fileType.getPrefixName()
    val fileName = "$name.$extension"
    val contentResolver = context.contentResolver
    val mimeType = postData.fileType.getMediaType()
    val uri = contentResolver.insertMediaImage(fileName, mimeType, "Maw")
    uri ?: throw RuntimeException("insert media image failed")
    val fos = contentResolver.openOutputStream(uri)
    fos ?: throw RuntimeException("open output stream failed")
    val fis = FileInputStream(file)
    val buf = ByteArray(1024)
    var len: Int
    var totalSize: Long = 0
    while (true) {
        len = fis.read(buf)
        if (len == -1) break
        totalSize += len
        fos.write(buf, 0, len)
    }
    fis.close()
    fos.close()
    val imageValues = ContentValues()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        imageValues.put(MediaStore.Images.Media.SIZE, file.length())
        contentResolver.update(uri, imageValues, null, null)
//        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
    } else {
        imageValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        contentResolver.update(uri, imageValues, null, null)
    }
    return uri
}

private fun ContentResolver.insertMediaImage(
    fileName: String,
    mimeType: String?,
    relativePath: String? = null
): Uri? {
    val imageValues = ContentValues()
    mimeType?.let { imageValues.put(MediaStore.Images.Media.MIME_TYPE, it) }
    // 插入时间
    val date = System.currentTimeMillis() / 1000
    imageValues.put(MediaStore.Images.Media.DATE_ADDED, date)
    imageValues.put(MediaStore.Images.Media.DATE_MODIFIED, date)
    // 保存的位置
    val albumDir = Environment.DIRECTORY_PICTURES
    val collection: Uri
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val path = if (relativePath != null) "${albumDir}/${relativePath}" else albumDir
        imageValues.apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.RELATIVE_PATH, path)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        // 高版本不用查重直接插入，会自动重命名
    } else {
        // 老版本
        val pictures = Environment.getExternalStoragePublicDirectory(albumDir)
        val saveDir = if (relativePath != null) File(pictures, relativePath) else pictures

        if (!saveDir.exists() && !saveDir.mkdirs()) {
            throw RuntimeException("can't create Pictures directory")
        }

        // 文件路径查重，重复的话在文件名后拼接数字
        var imageFile = File(saveDir, fileName)
        val fileNameWithoutExtension = imageFile.nameWithoutExtension
        val fileExtension = imageFile.extension

        // 查询文件是否已经存在
        var queryUri = this.queryMediaImage(imageFile.absolutePath)
        var suffix = 1
        while (queryUri != null) {
            // 存在的话重命名，路径后面拼接 fileNameWithoutExtension(数字).png
            val newName = fileNameWithoutExtension + "(${suffix++})." + fileExtension
            imageFile = File(saveDir, newName)
            queryUri = this.queryMediaImage(imageFile.absolutePath)
        }

        imageValues.apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
            // 保存路径
            val imagePath = imageFile.absolutePath
            put(MediaStore.Images.Media.DATA, imagePath)
        }
        collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    // 插入图片信息
    return this.insert(collection, imageValues)
}

private fun ContentResolver.queryMediaImage(path: String): Uri? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null
    val imageFile = File(path)
    if (imageFile.canRead() && imageFile.exists()) {
        return Uri.fromFile(imageFile)
    }
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val query = this.query(
        collection,
        arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA),
        "${MediaStore.Images.Media.DATA} == ?",
        arrayOf(path), null
    )
    query?.use {
        while (it.moveToNext()) {
            val idColum = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val id = it.getLong(idColum)
            val existUri = ContentUris.withAppendedId(collection, id)
            return existUri
        }
    }
    return null
}