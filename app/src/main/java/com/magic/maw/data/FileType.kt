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

    fun isPicture(): Boolean {
        return this == Jpeg || this == Png || this == Webp || this == Gif
    }

    fun isVideo(): Boolean {
        return this == Mp4 || this == Webm || this == Swf
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
