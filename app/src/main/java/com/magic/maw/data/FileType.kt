package com.magic.maw.data

enum class FileType(val value: Int) {
    Jpeg(1 shl 0),
    Png(1 shl 1),
    Webp(1 shl 2),
    Gif(1 shl 3),
    Mp4(1 shl 4),
    Webm(1 shl 5),
    Swf(1 shl 6),
    Ugoira(1 shl 7);

    val isPicture: Boolean
        get() = this == Jpeg || this == Png || this == Webp || this == Gif

    val isVideo: Boolean
        get() = this == Mp4 || this == Webm || this == Swf

    val isText: Boolean
        get() = false

    val isUgoira: Boolean
        get() = this == Ugoira

    fun getPrefixName(): String {
        return when (this) {
            Jpeg -> "jpg"
            Png -> "png"
            Webp -> "webp"
            Gif -> "gif"
            Mp4 -> "mp4"
            Webm -> "webm"
            Swf -> "swf"
            Ugoira -> "zip"
        }
    }

    fun getMediaType(): String {
        return when (this) {
            Jpeg -> "image/jpeg"
            Png -> "image/png"
            Webp -> "image/webp"
            Gif -> "image/gif"
            Mp4 -> "video/mp4"
            Webm -> "video/webm"
            Swf -> "*/*"
            Ugoira -> "*/*"
        }
    }

    companion object {
        fun getType(name: String): FileType? {
            for (item in entries) {
                if (item.name == name)
                    return item
            }
            return null
        }

        fun String?.toFileType(): FileType? {
            for (item in entries) {
                if (item.name == this)
                    return item
            }
            return null
        }
    }
}
