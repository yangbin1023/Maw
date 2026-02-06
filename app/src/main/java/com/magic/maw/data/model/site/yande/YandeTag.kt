package com.magic.maw.data.model.site.yande

import com.magic.maw.data.model.constant.TagType.Companion.toTagType
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagInfo
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
            website = WebsiteOption.Yande.name,
            tagId = id.toString(),
            name = name,
            type = type.toTagType(),
            count = count
        )
    }
}