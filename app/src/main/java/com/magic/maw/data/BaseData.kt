package com.magic.maw.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.magic.maw.data.Quality.Companion.toQuality
import com.magic.maw.util.configFlow
import com.magic.maw.website.DLManager
import java.io.File
import java.util.Locale

data class BaseData(
    val source: String,
    val id: Int,
    val quality: Quality
) {
    override fun toString(): String {
        return "${source}_${id}_${quality.name.lowercase()}"
    }
}

data class PostData(
    val source: String,
    val id: Int,
    var createId: Int? = null,
    var uploader: String? = null,
    var score: Int? = null,
    var srcUrl: String? = null,
    var uploadTime: Long? = null,
    var rating: Rating = Rating.None,
    var fileType: FileType = FileType.Jpeg,
    var tags: MutableList<TagInfo> = ArrayList(),
    var previewInfo: Info = Info(),
    var sampleInfo: Info? = null,
    var largeInfo: Info? = null,
    var originalInfo: Info = Info()
) {
    var quality: Quality by mutableStateOf(Quality.Sample)

    fun getDefaultQuality(): Quality {
        val targetQuality = configFlow.value.websiteConfig.quality.toQuality()
        val qualities = Quality.getQualities()
        val baseData = BaseData(source, id, Quality.File)
        for (quality in qualities) {
            val path = DLManager.getDLFullPath(baseData.copy(quality = quality))
            if (File(path).exists()) {
                return quality
            } else if (targetQuality == quality) {
                if (getInfo(targetQuality) != null) {
                    return targetQuality
                } else {
                    break
                }
            }
        }
        return Quality.File
    }

    override fun toString(): String {
        return "source: $source, id: $id"
    }

    fun getInfo(quality: Quality): Info? {
        return when (quality) {
            Quality.Preview -> previewInfo
            Quality.Sample -> sampleInfo
            Quality.Large -> largeInfo
            Quality.File -> originalInfo
            else -> null
        }
    }

    fun updateTag(tagInfo: TagInfo) {
        for ((index, tag) in tags.withIndex()) {
            if (tag.name == tagInfo.name) {
                tags[index] = tagInfo
                return
            }
        }
    }

    fun updateFrom(data: PostData) {
        this.createId = data.createId
        this.uploader = data.uploader
        this.score = data.score
        this.srcUrl = data.srcUrl
        this.uploadTime = data.uploadTime
        this.rating = data.rating
        this.fileType = data.fileType
        this.tags = data.tags
        this.previewInfo = data.previewInfo
        this.sampleInfo = data.sampleInfo
        this.largeInfo = data.largeInfo
        this.originalInfo = data.originalInfo
        if (getInfo(quality) == null) {
            quality = getDefaultQuality()
        }
    }

    data class Info(
        var width: Int = 0,
        var height: Int = 0,
        var size: Long = 0,
        var url: String = "",
        var type: FileType = FileType.Jpeg,
        var md5: String? = null
    ) {
        fun isInvalid(): Boolean {
            return width == 0 || height == 0 || size == 0L || url.isEmpty()
        }

        override fun toString(): String {
            return "${width}x${height} ${size.toSizeString()} ${type.name} url: $url"
        }
    }
}

fun Long.toSizeString(): String {
    val unitList = listOf("B", "KB", "MB", "GB", "TB")
    var count = 0
    var value = this.toDouble()
    while (value / 1024 > 0.97) {
        value /= 1024
        count++
        if (count >= unitList.size - 1) {
            break
        }
    }
    return String.format(Locale.getDefault(), "%.2f%s", value, unitList[count])
}