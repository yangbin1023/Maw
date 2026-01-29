package com.magic.maw.data.api.entity

import com.magic.maw.data.model.site.PoolData
import kotlinx.serialization.Serializable

@Serializable
data class PoolResponse(
    val items: List<PoolData>,
    val meta: RequestMeta,
)