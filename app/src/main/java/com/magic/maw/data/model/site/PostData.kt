package com.magic.maw.data.model.site

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.magic.maw.data.api.manager.BaseData
import com.magic.maw.data.api.manager.DLManager
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.local.store.SettingsStore
import com.magic.maw.data.model.constant.FileType
import com.magic.maw.data.model.constant.Quality
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.util.toSizeString
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class PostData(
    val website: WebsiteOption,
    val id: String,
    var createId: String? = null,
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
        val targetQuality = SettingsStore.settings.websiteSettings.showQuality
        val qualities = Quality.getQualities()
        val baseData = BaseData(this, Quality.File)
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
        return "website: $website, id: $id, quality: ${quality.name}"
    }

    fun getInfo(quality: Quality): Info? {
        return when (quality) {
            Quality.Preview -> previewInfo
            Quality.Sample -> sampleInfo
            Quality.Large -> largeInfo
            Quality.File -> originalInfo
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

    @Serializable
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