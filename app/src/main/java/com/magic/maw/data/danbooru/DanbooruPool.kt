package com.magic.maw.data.danbooru

import com.magic.maw.data.PoolData
import com.magic.maw.website.parser.DanbooruParser
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
        val data = PoolData(source = DanbooruParser.SOURCE, id = id!!, name = name!!)
        data.description = description
        data.count = post_count ?: 0
        data.createTime = DanbooruParser.getUnixTime(created_at)
        data.updateTime = DanbooruParser.getUnixTime(updated_at)
        data.category = category
        return data
    }
}