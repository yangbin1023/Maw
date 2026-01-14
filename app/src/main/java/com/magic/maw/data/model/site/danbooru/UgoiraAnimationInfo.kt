package com.magic.maw.data.model.site.danbooru

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class UgoiraFrameInfo(
    val file: String = "",
    val delay: Int = 40,
    val md5: String = ""
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UgoiraAnimationInfo(
    val illustId: Int? = null,
    val userId: Int? = null,
    val createDate: String? = null,
    val uploadDate: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    @JsonNames("mime_type")
    val mimeType: String = "",
    val frames: List<UgoiraFrameInfo> = emptyList()
)
