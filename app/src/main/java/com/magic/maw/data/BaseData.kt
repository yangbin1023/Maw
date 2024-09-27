package com.magic.maw.data

import com.magic.maw.data.Quality.Companion.toQuality
import com.magic.maw.util.configFlow

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
    var tags: List<TagInfo> = emptyList(),
    var previewInfo: Info = Info(),
    var sampleInfo: Info? = null,
    var largeInfo: Info? = null,
    var originalInfo: Info = Info()
) : IKey<Int> {
    private var currentQuality: Quality = Quality.Preview

    var quality: Quality
        set(value) {
            currentQuality = value
        }
        get() {
            if (currentQuality != Quality.Preview)
                return currentQuality
            val targetQuality = configFlow.value.websiteConfig.quality.toQuality()
            getInfo(targetQuality)?.let {
                return targetQuality
            } ?: let {
                val qualities = Quality.getQualities()
                for (quality in qualities) {
                    if (targetQuality.value <= quality.value && getInfo(quality) != null) {
                        return quality
                    }
                }
            }
            return Quality.File
        }

    override val key: Int get() = id

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

    data class Info(
        var width: Int = 0,
        var height: Int = 0,
        var size: Long = 0,
        var url: String = "",
        var md5: String? = null
    ) {
        fun isInvalid(): Boolean {
            return width == 0 || height == 0 || size == 0L || url.isEmpty()
        }
    }
}

typealias PostList = DataList<Int, PostData>
