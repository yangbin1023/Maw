package com.magic.maw.data.repository

import com.magic.maw.data.api.parser.WebsiteParserProvider
import com.magic.maw.data.local.db.dao.TagInfoDao
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.site.PostData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

class TagRepository(
    private val dao: TagInfoDao,
    private val provider: WebsiteParserProvider
) {
    fun getTagsForPost(data: PostData): Flow<List<TagInfo>> {
        return dao.getAll(data.website, data.tagRefs)
    }

    suspend fun refreshTagsForPost(data: PostData) {
        // 查询数据库
        val tagNames = data.tagRefs.toMutableSet()
        val dbTagList = withContext(Dispatchers.IO) {
            data.tagRefs.map { tagName ->
                async {
                    dao.getByName(data.website, tagName)
                }
            }.awaitAll().filterNotNull()
        }
        // 排除新数据
        val now = Clock.System.now()
        for (tag in dbTagList) {
            if (tag.updateTime.plus(30.minutes) > now) {
                tagNames.remove(tag.name)
            }
        }
        // 请求网络
        val parser = provider[data.website]
        // 请求标签列表
        parser.requestTagsByPostId(data.id)?.let { tagList ->
            data.tags.clear()
            data.tags.addAll(tagList)
            tagList.forEach { saveTagInfo(it) }
            return
        }
        // 单个单个的请求标签
        val tagList2 = withContext(Dispatchers.IO) {
            tagNames.map { tagName ->
                async {
                    parser.requestTagInfo(tagName)
                }
            }.awaitAll().filterNotNull()
        }
        tagList2.forEach { saveTagInfo(it) }
    }

    suspend fun saveTagInfo(tagInfo: TagInfo) {
        dao.upsert(tagInfo)
    }
}