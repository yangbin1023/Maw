package com.magic.maw.data.model.site.danbooru

import com.magic.maw.data.model.site.PoolData
import com.magic.maw.data.model.constant.WebsiteOption
import kotlinx.serialization.Serializable

@Serializable
class DanbooruPool {
    var id: Int? = null
    var name: String? = null
    var created_at: String? = null
    var updated_at: String? = null
    var description: String? = null
    var is_active: Boolean? = null
    var is_deleted: Boolean? = null
    var post_ids: ArrayList<Int>? = null
    var category: String? = null //series collection
    var post_count: Int? = null

    private fun invalid(): Boolean {
        return id == null || name == null
    }

    fun toPoolData(): PoolData? {
        if (invalid())
            return null
        val data = PoolData(website = WebsiteOption.Danbooru, id = id!!.toString(), name = name!!)
        data.description = description
        data.count = post_count ?: 0
        data.createTime = getDanbooruUnixTime(created_at)
        data.updateTime = getDanbooruUnixTime(updated_at)
        data.category = category
        return data
    }
}