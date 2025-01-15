package com.magic.maw.data.konachan

import com.magic.maw.data.TagInfo
import com.magic.maw.data.TagType.Companion.toTagType
import com.magic.maw.website.parser.KonachanParser
import kotlinx.serialization.Serializable

@Serializable
class KonachanTag {
    var id: Int = 0
    var name: String = ""
    var count: Int = 0
    var type: Int = 0
    var ambiguous: Boolean? = null

    fun toTagInfo(): TagInfo? {
        if (id == 0 || name.isEmpty())
            return null
        return TagInfo(
            source = KonachanParser.SOURCE,
            tagId = id,
            name = name,
            type = type.toTagType(),
            count = count
        )
    }
}