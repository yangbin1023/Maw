package com.magic.maw.data.api.entity

import kotlinx.serialization.Serializable

@Serializable
data class RequestMeta(
    val prev: String? = null,
    val next: String? = null,
)
