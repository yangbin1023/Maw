package com.magic.maw.data

data class PostData(
    val source: String,
    val id: Int,
    var createId: Int? = null,
    var uploader: String? = null,
    var score: Int? = null,
    var srcUrl: String? = null,
    var uploadTime: Long? = null,
    var rating: Rating = Rating.None,
    var fileType: FileType = FileType.Jpeg,
    var tags: List<TagInfo> = emptyList(),
    var quality: Quality = Quality.Sample,
    var previewInfo: Info = Info(),
    var sampleInfo: Info? = null,
    var largeInfo: Info? = null,
    var originalInfo: Info = Info()
) : IKey<Int> {

    override val key: Int get() = id

    override fun toString(): String {
        return "source: $source, id: $id"
    }

    data class Info(
        var width: Int = 0,
        var height: Int = 0,
        var size: Long = 0,
        var url: String = "",
        var md5: String? = null
    ) {
        fun isInvalid(): Boolean {
            return width == 0 || height == 0 || size == 0L || url.isEmpty()
        }
    }
}

typealias PostList = DataList<Int, PostData>
