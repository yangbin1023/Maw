package com.magic.maw.data.yande

import com.magic.maw.data.UserInfo
import com.magic.maw.website.parser.YandeParser
import kotlinx.serialization.Serializable

@Serializable
class YandeUser {
    var id: Int? = 0
    var name: String? = null

    fun toUserInfo(): UserInfo? {
        val id = id ?: return null
        val name = name ?: return null
        return UserInfo(source = YandeParser.SOURCE, name = name, userId = id)
    }
}