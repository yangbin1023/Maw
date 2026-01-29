package com.magic.maw.data.repository

import com.magic.maw.data.local.db.dao.TagHistoryDao
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagHistory
import kotlinx.coroutines.flow.Flow

class TagHistoryRepository(
    private val dao: TagHistoryDao,
    private val settingsRepository: SettingsRepository
) {
    fun getAllHistory(limit: Int = 50): Flow<List<TagHistory>> {
        val website = settingsRepository.settings.website
        return dao.getAllFlow(website, limit)
    }

    suspend fun updateTagHistory(website: WebsiteOption, tags: Collection<String>) {
        tags.forEach { tag ->
            if (tag.isNotBlank()) {
                dao.upsert(TagHistory(website = website, name = tag))
            }
        }
    }

    suspend fun deleteHistory(website: WebsiteOption, name: String) {
        dao.delete(website, name)
    }

    suspend fun deleteAllHistory(website: WebsiteOption) {
        dao.deleteAll(website)
    }
}