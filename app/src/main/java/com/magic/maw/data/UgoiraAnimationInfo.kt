package com.magic.maw.data

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
    val frames: Array<UgoiraFrameInfo> = emptyArray<UgoiraFrameInfo>()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UgoiraAnimationInfo

        if (illustId != other.illustId) return false
        if (userId != other.userId) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (createDate != other.createDate) return false
        if (uploadDate != other.uploadDate) return false
        if (mimeType != other.mimeType) return false
        if (!frames.contentEquals(other.frames)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = illustId ?: 0
        result = 31 * result + (userId ?: 0)
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + (createDate?.hashCode() ?: 0)
        result = 31 * result + (uploadDate?.hashCode() ?: 0)
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + frames.contentHashCode()
        return result
    }
}
