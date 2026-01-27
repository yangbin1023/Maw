package com.magic.maw.data.api.entity

import com.magic.maw.data.model.site.PoolData

data class PoolResponse(
    val items: List<PoolData>,
    val meta: RequestMeta,
)