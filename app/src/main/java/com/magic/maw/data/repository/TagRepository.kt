package com.magic.maw.data.repository

import com.magic.maw.data.api.parser.WebsiteParserProvider
import com.magic.maw.data.local.db.dao.TagInfoDao
import com.magic.maw.data.model.constant.TagType
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.site.PostData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class TagRepository(
    private val dao: TagInfoDao,
    private val provider: WebsiteParserProvider
) {
    suspend fun getTagsForPost(data: PostData): List<TagInfo> {
        if (data.tags.size == data.tagRefs.size) {
            // 已经获取到数据了，直接返回
            if (data.tags.find { it.type == TagType.None } == null) {
                return data.tags
            }
        }
        // 查询数据库
        val dbTagList = withContext(Dispatchers.IO) {
            data.tagRefs.map { tagName ->
                async {
                    dao.getByName(data.website, tagName)
                }
            }.awaitAll().requireNoNulls()
        }
        if (dbTagList.size == data.tagRefs.size) {
            data.tags.clear()
            data.tags.addAll(dbTagList)
            return dbTagList
        }
        // 请求网络
        val parser = provider[data.website]
        // 请求标签列表
        parser.requestTagsByPostId(data.id)?.let { tagList ->
            data.tags.clear()
            data.tags.addAll(tagList)
            tagList.forEach { saveTagInfo(it) }
            return tagList
        }
        // 单个单个的请求标签
        val tagList2 = withContext(Dispatchers.IO) {
            data.tagRefs.filter { tagName ->
                dbTagList.find { it.name == tagName } == null
            }.map { tagName ->
                async {
                    parser.requestTagInfo(tagName)
                }
            }.awaitAll().requireNoNulls()
        }
        tagList2.forEach { saveTagInfo(it) }

        val finalTagList = mutableListOf<TagInfo>()
        finalTagList.addAll(dbTagList)
        finalTagList.addAll(tagList2)
        finalTagList.sort()

        return finalTagList
    }

    suspend fun saveTagInfo(tagInfo: TagInfo) {
        dao.upsert(tagInfo)
    }
}