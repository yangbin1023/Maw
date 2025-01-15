package com.magic.maw.data.konachan

import com.magic.maw.data.UserInfo
import com.magic.maw.website.parser.KonachanParser
import kotlinx.serialization.Serializable

@Serializable
class KonachanUser {
    var id: Int? = 0
    var name: String? = null
    var blacklisted_tags: String? = null

    fun toUserInfo(): UserInfo? {
        val id = id ?: return null
        val name = name ?: return null
        return UserInfo(source = KonachanParser.SOURCE, name = name, userId = id)
    }
}