package com.magic.maw.data.api.entity

import com.magic.maw.data.model.site.PostData

data class PostResponse(
    val items: List<PostData>,
    val meta: RequestMeta,
)
