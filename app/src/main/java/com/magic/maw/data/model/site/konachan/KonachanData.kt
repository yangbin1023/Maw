package com.magic.maw.data.model.site.konachan

import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.model.constant.FileType
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption
import kotlinx.serialization.Serializable

@Serializable
class KonachanData {
    var id: Int = 0
    var tags: String? = null
    var created_at: Long? = null //单位秒
    var creator_id: Int? = null
    var author: String? = null
    var change: Int? = null
    var source: String? = null
    var score: Int? = null //分级
    var md5: String? = null
    var file_size: Long? = null
    var file_url: String? = null //
    var is_shown_in_index: Boolean? = null
    var preview_url: String? = null
    var preview_width: Int? = null
    var preview_height: Int? = null
    var actual_preview_width: Int? = null
    var actual_preview_height: Int? = null
    var sample_url: String? = null
    var sample_width: Int? = null
    var sample_height: Int? = null
    var sample_file_size: Long? = null
    var jpeg_url: String? = null
    var jpeg_width: Int? = null
    var jpeg_height: Int? = null
    var jpeg_file_size: Long? = null
    var rating: String? = null //分级 "s" 安全
    var has_children: Boolean? = null
    var parent_id: Int? = null
    var status: String? = null
    var width: Int? = null
    var height: Int? = null
    var is_held: Boolean? = null
//    var frames_pending_string: String? = null
//    var frames_pending: Array<Any> = arrayOf() //类型未知，数组
//    var frames_string: String? = null
//    var frames: Array<Any> = arrayOf() //类型未知，数组

    private fun getRating(): Rating {
        return when (rating) {
            "s" -> Rating.Safe
            "q" -> Rating.Questionable
            "e" -> Rating.Explicit
            else -> Rating.None
        }
    }

    private fun invalid(): Boolean {
        return id == 0 || file_size == null || width == null || height == null || md5 == null || file_url == null
    }

    fun toPostData(): PostData? {
        if (invalid() || preview_url == null) {
            return null
        }
        val data = PostData(WebsiteOption.Konachan, id.toString())
        data.createId = creator_id?.toString()
        data.uploader = author
        data.score = score
        data.srcUrl = source
        data.rating = getRating()
        data.uploadTime = created_at?.let { it * 1000 }
        tags?.split(" ")?.toSet()?.let { tagNames ->
            for (tagName in tagNames) {
                data.tags.add(TagInfo(website = WebsiteOption.Konachan, name = tagName))
            }
            data.tagRefs = tagNames.mapNotNull { it.ifBlank { null } }
        }
        file_url?.let {
            if (it.endsWith("png"))
                data.fileType = FileType.Png
        }

        val previewInfo = PostData.Info()
        actual_preview_width?.let { previewInfo.width = it }
        actual_preview_height?.let { previewInfo.height = it }
        previewInfo.url = preview_url!!
        data.previewInfo = previewInfo

        if (sample_url != null && sample_width != null && sample_height != null) {
            val info = PostData.Info()
            info.width = sample_width!!
            info.height = sample_height!!
            info.url = sample_url!!
            sample_file_size?.let { info.size = it }
            data.sampleInfo = info
        }

        if (jpeg_url != null && jpeg_url != file_url && jpeg_width != null && jpeg_height != null) {
            val info = PostData.Info()
            info.width = jpeg_width!!
            info.height = jpeg_height!!
            info.url = jpeg_url!!
            jpeg_file_size?.let { info.size = it }
            data.largeInfo = info
        }

        val fileInfo = PostData.Info()
        fileInfo.size = file_size!!
        fileInfo.width = width!!
        fileInfo.height = height!!
        fileInfo.url = file_url!!
        fileInfo.md5 = md5!!
        if (fileInfo.url.endsWith(".png", true)) {
            fileInfo.type = FileType.Png
        }
        data.originalInfo = fileInfo

        data.quality = data.getDefaultQuality()
        return data
    }
}