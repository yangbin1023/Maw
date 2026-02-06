package com.magic.maw.data.model.site.yande

import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.UserInfo
import kotlinx.serialization.Serializable

@Serializable
class YandeUser {
    var id: Int? = 0
    var name: String? = null

    fun toUserInfo(): UserInfo? {
        val id = id ?: return null
        val name = name ?: return null
        return UserInfo(website = WebsiteOption.Yande.name, name = name, userId = id.toString())
    }
}