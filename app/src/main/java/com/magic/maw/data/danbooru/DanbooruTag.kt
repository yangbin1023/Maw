package com.magic.maw.data.danbooru

import com.magic.maw.data.TagInfo
import com.magic.maw.data.TagType.Companion.toTagType
import com.magic.maw.website.parser.DanbooruParser
import kotlinx.serialization.Serializable

@Serializable
class DanbooruTag {
    var id: Int = 0
    var name: String = ""
    var post_count: Int = 0
    var category: Int = 0
    var created_at: String? = null
    var updated_at: String? = null
    var is_deprecated: Boolean? = null
    var words: List<String?>? = null

    fun toTagInfo(): TagInfo? {
        if (id == 0 || name.isEmpty())
            return null
        return TagInfo(
            source = DanbooruParser.SOURCE,
            tagId = id,
            name = name,
            type = category.toTagType(),
            count = post_count
        )
    }
}