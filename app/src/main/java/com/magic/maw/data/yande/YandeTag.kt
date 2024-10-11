package com.magic.maw.data.yande

import com.magic.maw.data.IData
import com.magic.maw.data.TagInfo
import com.magic.maw.data.TagType.Companion.toTagType
import com.magic.maw.website.parser.YandeParser
import kotlinx.serialization.Serializable

@Serializable
class YandeTag : IData<TagInfo> {
    var id: Int = 0
    var name: String = ""
    var count: Int = 0
    var type: Int = 0
    var ambiguous: Boolean? = null

    override fun toData(): TagInfo? {
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