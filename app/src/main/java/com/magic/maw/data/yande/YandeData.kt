package com.magic.maw.data.yande

import com.magic.maw.data.FileType
import com.magic.maw.data.IData
import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.TagInfo
import com.magic.maw.website.parser.YandeParser

class YandeData : IData<PostData> {
    var id: Int = 0
    var tags: String? = null
    var created_at: Long? = null
    var updated_at: Long? = null //单位秒
    var creator_id: Int? = null
    var approver_id: String? = null //基本为null
    var author: String? = null
    var change: Int? = null
    var source: String? = null
    var score: Int? = null //分级
    var md5: String? = null
    var file_size: Long? = null
    var file_ext: String? = null //文件拓展名
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
    var is_rating_locked: Boolean? = null
    var has_children: Boolean? = null
    var parent_id: String? = null
    var status: String? = null
    var is_pending: Boolean? = null
    var width: Int? = null
    var height: Int? = null
    var is_held: Boolean? = null
    var frames_pending_string: String? = null
    var frames_pending //类型未知，数组
            : Array<Any> = arrayOf()
    var frames_string: String? = null
    var frames //类型未知，数组
            : Array<Any> = arrayOf()
    var is_note_locked: Boolean? = null
    var last_noted_at: Int? = null
    var last_commented_at: Int? = null
    var flag_detail: FlagDetail? = null

    class FlagDetail {
        var post_id: Int? = null
        var reason: String? = null
        var created_at: String? = null // 格式： yyyy-MM-DDTHH:mm:ss.SSSZ
        var user_id: Int? = null //具体类型不清楚，可为null
        var flagged_by: String? = null
    }

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

    override fun toData(): PostData? {
        if (invalid() || preview_url == null) {
            return null
        }
        val data = PostData(YandeParser.SOURCE, id)
        data.createId = creator_id
        data.uploader = author
        data.score = score
        data.srcUrl = source
        data.rating = getRating()
        data.uploadTime = updated_at
        val tagNames = tags?.split(" ")
        if (!tagNames.isNullOrEmpty()) {
            val tagList = ArrayList<TagInfo>()
            for (tagName in tagNames) {
                tagList.add(TagInfo(source = YandeParser.SOURCE, name = tagName))
            }
            data.tags = tagList
        }
        file_ext?.let {
            if (it == "png")
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
        data.originalInfo = fileInfo

        return data
    }
}
