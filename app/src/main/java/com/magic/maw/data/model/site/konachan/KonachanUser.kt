package com.magic.maw.data.model.site.konachan

import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.UserInfo
import kotlinx.serialization.Serializable

@Serializable
class KonachanUser {
    var id: Int? = 0
    var name: String? = null
    var blacklisted_tags: String? = null

    fun toUserInfo(): UserInfo? {
        val id = id ?: return null
        val name = name ?: return null
        return UserInfo(website = WebsiteOption.Konachan.name, name = name, userId = id.toString())
    }
}