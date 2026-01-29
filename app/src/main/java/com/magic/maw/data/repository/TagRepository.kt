package com.magic.maw.data.repository

import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.db.dao.TagDao
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.site.PostData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

class TagRepository(
    private val dao: TagDao,
    private val apiServiceProvider: ApiServiceProvider
) {
    fun getTagsFlowForPost(data: PostData): Flow<List<TagInfo>> {
        return dao.getAllFlow(data.website, data.tagRefs)
    }

    fun getTagsByNamesFlow(website: WebsiteOption, names: List<String>): Flow<List<TagInfo>> {
        return dao.getAllFlow(website, names)
    }

    suspend fun refreshTagsForPost(data: PostData) {
        // 查询数据库
        val tagNames = data.tagRefs.toMutableSet()
        val dbTagList = withContext(Dispatchers.IO) {
            dao.getAll(data.website, tagNames)
        }
        // 排除新数据
        val now = Clock.System.now()
        for (tag in dbTagList) {
            if (tag.updateTime.plus(30.minutes) > now && tag.count > 0) {
                tagNames.remove(tag.name)
            }
        }
        if (tagNames.isEmpty()) {
            return
        }
        // 请求网络
        val apiService = apiServiceProvider[data.website]
        // 请求标签列表
        try {
            apiService.getTagsByPostId(data.id)?.let { tagList ->
                data.tags.clear()
                data.tags.addAll(tagList)
                tagList.forEach { saveTagInfo(it) }
                return
            }
        } catch (_: Exception) {
        }
        // 单个单个的请求标签
        withContext(Dispatchers.IO) {
            tagNames.forEach { tagName ->
                val tagInfo = dao.getByName(data.website, tagName)
                if (tagInfo == null || tagInfo.count <= 0) {
                    try {
                        apiService.getTagByName(tagName)?.forEach {
                            saveTagInfo(it)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    suspend fun saveTagInfo(tagInfo: TagInfo) {
        dao.upsert(tagInfo)
    }
}