package com.magic.maw.data.yande

import com.magic.maw.data.TagInfo
import com.magic.maw.data.TagType.Companion.toTagType
import com.magic.maw.website.parser.YandeParser
import kotlinx.serialization.Serializable

@Serializable
class YandeTag {
    var id: Int = 0
    var name: String = ""
    var count: Int = 0
    var type: Int = 0
    var ambiguous: Boolean? = null

    fun toTagInfo(): TagInfo? {
        if (id == 0 || name.isEmpty())
            return null
        return TagInfo(
            source = YandeParser.SOURCE,
            tagId = id,
            name = name,
            type = type.toTagType(),
            count = count
        )
    }
}