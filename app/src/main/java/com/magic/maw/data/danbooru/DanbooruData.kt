package com.magic.maw.data.danbooru

import com.magic.maw.data.FileType
import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.TagInfo
import com.magic.maw.data.TagType
import com.magic.maw.website.parser.DanbooruParser
import kotlinx.serialization.Serializable

@Serializable
class DanbooruData {
    var id: Int = 0
    var created_at: String? = null
    var uploader_id: Int? = null
    var score: Int? = null
    var source: String? = null
    var md5: String? = null
    var last_comment_bumped_at: String? = null
    var rating: String? = null
    var image_width: Int? = null
    var image_height: Int? = null
    var tag_string: String? = null
    var fav_count: Int? = null
    var file_ext: String? = null
    var last_noted_at: String? = null
    var parent_id: Int? = null
    var has_children: Boolean? = null
    var approver_id: Int? = null
    var tag_count_general: Int? = null
    var tag_count_artist: Int? = null
    var tag_count_character: Int? = null
    var tag_count_copyright: Int? = null
    var file_size: Int? = null
    var up_score: Int? = null
    var down_score: Int? = null
    var is_pending: Boolean? = null
    var is_flagged: Boolean? = null
    var is_deleted: Boolean? = null
    var tag_count: Int? = null
    var updated_at: String? = null
    var is_banned: Boolean? = null
    var pixiv_id: Int? = null
    var last_commented_at: String? = null
    var has_active_children: Boolean? = null
    var bit_flags: Int? = null
    var tag_count_meta: Int? = null
    var has_large: Boolean? = null
    var has_visible_children: Boolean? = null
    var media_asset: MediaAsset? = null
    var tag_string_general: String? = null
    var tag_string_artist: String? = null
    var tag_string_copyright: String? = null
    var tag_string_character: String? = null
    var tag_string_meta: String? = null
    var file_url: String? = null
    var large_file_url: String? = null
    var preview_file_url: String? = null

    @Serializable
    class MediaAsset {
        var id: Int = 0
        var created_at: String? = null
        var updated_at: String? = null
        var md5: String? = null
        var file_ext: String? = null
        var file_size: Long? = null
        var image_width: Int? = null
        var image_height: Int? = null
        var duration: Float? = null
        var status: String? = null
        var file_key: String? = null
        var is_public: Boolean? = null
        var pixel_hash: String? = null
        var variants: Array<Variant?> = arrayOf()
    }

    @Serializable
    class Variant {
        var type: String? = null
        var url: String? = null
        var width: Int? = null
        var height: Int? = null
        var file_ext: String? = null

        private fun valid(): Boolean {
            return type != null && url != null && width != null && height != null
        }

        fun toInfo(): PostData.Info? {
            if (valid()) {
                return PostData.Info(
                    width = width!!,
                    height = height!!,
                    url = url!!,
                    type = fileExtToFileType(file_ext) ?: FileType.Jpeg
                )
            }
            return null
        }
    }

    private fun invalid(): Boolean {
        if (id == 0
            || file_size == null
            || md5 == null
            || file_url == null
            || image_width == null
            || image_height == null
            || media_asset?.variants.isNullOrEmpty()
        )
            return true
        var noPreview = true
        var noOriginal = true
        for (variant in media_asset!!.variants) {
            if (variant?.type == "original")
                noOriginal = false
            if (variant?.type == "180x180" || variant?.type == "360x360" || variant?.type == "720x720")
                noPreview = false
        }
        return noPreview || noOriginal
    }

    fun toPostData(): PostData? {
        if (invalid())
            return null
        val data = PostData(DanbooruParser.SOURCE, id)
        data.createId = uploader_id
        data.score = score
        data.srcUrl = source
        // General Sensitive Questionable Explicit  // g s q e
        data.rating = when (rating) {
            "g" -> Rating.General
            "s" -> Rating.Sensitive
            "q" -> Rating.Questionable
            "e" -> Rating.Explicit
            else -> Rating.None
        }
        data.uploadTime = DanbooruParser.getUnixTime(updated_at)
        val tagNames = tag_string?.split(" ")
        if (!tagNames.isNullOrEmpty()) {
            val generalTagList = tag_string_general?.split(" ") ?: emptyList()
            val artistTagList = tag_string_artist?.split(" ") ?: emptyList()
            val copyrightTagList = tag_string_copyright?.split(" ") ?: emptyList()
            val characterTagList = tag_string_character?.split(" ") ?: emptyList()
            val metaTagList = tag_string_meta?.split(" ") ?: emptyList()
            val tagList = ArrayList<TagInfo>()
            for (tagName in tagNames) {
                if (tagName.isEmpty())
                    continue
                val type = if (generalTagList.contains(tagName)) {
                    TagType.General
                } else if (artistTagList.contains(tagName)) {
                    TagType.Artist
                } else if (copyrightTagList.contains(tagName)) {
                    TagType.Copyright
                } else if (characterTagList.contains(tagName)) {
                    TagType.Character
                } else if (metaTagList.contains(tagName)) {
                    TagType.Circle
                } else {
                    TagType.None
                }
                tagList.add(TagInfo(source = DanbooruParser.SOURCE, name = tagName, type = type))
            }
            tagList.sort()
            data.tags = tagList
        }
        data.fileType = fileExtToFileType(file_ext) ?: FileType.Jpeg
        var variant180: Variant? = null
        var variant360: Variant? = null
        var variant720: Variant? = null
        var variantSample: Variant? = null
        var variantOriginal: Variant? = null
        for (variant in media_asset!!.variants) {
            variant ?: continue
            when (variant.type) {
                "180x180" -> variant180 = variant
                "360x360" -> variant360 = variant
                "720x720" -> variant720 = variant
                "sample" -> variantSample = variant
                "original" -> variantOriginal = variant
            }
        }
//        if (!data.fileType.isPicture()) {
//            variantSample = null
//        }
        var previewInfo: PostData.Info? = variant360?.toInfo()
        previewInfo ?: let { previewInfo = variant720?.toInfo() }
        previewInfo ?: let { previewInfo = variant180?.toInfo() }
        previewInfo?.let { data.previewInfo = it } ?: let { return null }
        data.sampleInfo = variantSample?.toInfo()
        data.originalInfo = variantOriginal?.toInfo() ?: return null
        data.originalInfo.size = media_asset?.file_size ?: return null
        data.quality = data.getDefaultQuality()
        return data
    }

    fun get720Preview(): PostData.Info? {
        val variants = media_asset?.variants ?: return null
        for (variant in variants) {
            if (variant?.type == "720x720") {
                return variant.toInfo()
            }
        }
        return null
    }

    companion object {
        fun fileExtToFileType(file_ext: String?): FileType? {
            return when (file_ext) {
                "png" -> FileType.Png
                "gif" -> FileType.Gif
                "mp4" -> FileType.Mp4
                "webm" -> FileType.Webm
                "swf" -> FileType.Swf
                "zip" -> FileType.Ugoira
                else -> null
            }
        }
    }
}
