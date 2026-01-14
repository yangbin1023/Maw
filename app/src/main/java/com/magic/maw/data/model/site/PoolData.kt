package com.magic.maw.data.model.site

import com.magic.maw.data.model.constant.WebsiteOption
import kotlinx.serialization.Serializable

@Serializable
data class PoolData(
    val website: WebsiteOption,
    val id: String,
    var name: String,
    var description: String? = null,
    var count: Int = 0,
    var createTime: Long? = null,
    var updateTime: Long? = null,
    var createUid: String? = null,
    var uploader: String? = null,
    var category: String? = null,
    var posts: List<PostData> = emptyList(),
    var noMore: Boolean = false,
)