package com.magic.maw.data.api.entity

import com.magic.maw.data.model.site.PostData
import kotlinx.serialization.Serializable

@Serializable
data class PostResponse(
    val items: List<PostData>,
    val meta: RequestMeta,
)
