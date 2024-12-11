package com.magic.maw.data

data class PoolData(
    val source: String,
    val id: Int,
    var name: String,
    var description: String? = null,
    var count: Int = 0,
    var createTime: Long? = null,
    var updateTime: Long? = null,
    var createUid: Int? = null,
    var uploader: String? = null,
    var category: String? = null,
    var posts: List<PostData> = emptyList(),
    var noMore: Boolean = false,
) {
    fun updateFrom(data: PoolData) {
        this.name = data.name
        this.description = data.description
        this.count = data.count
        this.createTime = data.createTime
        this.updateTime = data.updateTime
        this.createUid = data.createUid
        this.uploader = data.uploader
        this.category = data.category
        this.posts = data.posts
    }
}