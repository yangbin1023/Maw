package com.magic.maw.data.model.site.konachan

import com.magic.maw.data.model.constant.TagType.Companion.toTagType
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagInfo
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
            website = WebsiteOption.Konachan.name,
            tagId = id.toString(),
            name = name,
            type = type.toTagType(),
            count = count
        )
    }
}