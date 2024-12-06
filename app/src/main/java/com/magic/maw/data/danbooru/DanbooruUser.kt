package com.magic.maw.data.danbooru

import com.magic.maw.data.UserInfo
import com.magic.maw.website.parser.DanbooruParser
import kotlinx.serialization.Serializable

@Serializable
class DanbooruUser {
    var id: Int? = null
    var name: String? = null
    var level: Int? = null
    var inviter_id: Int? = null
    var created_at: String? = null
    var post_update_count: Int? = null
    var note_update_count: Int? = null
    var post_upload_count: Int? = null
    var is_deleted: Boolean? = null
    var level_string: String? = null
    var is_banned: Boolean? = null

    fun toUserInfo(): UserInfo? {
        val id = id ?: return null
        val name = name ?: return null
        return UserInfo(source = DanbooruParser.SOURCE, userId = id, name = name)
    }
}