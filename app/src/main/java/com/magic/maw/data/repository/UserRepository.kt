package com.magic.maw.data.repository

import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.db.dao.UserInfoDao
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

class UserRepository(
    private val dao: UserInfoDao,
    private val provider: ApiServiceProvider
) {
    fun getUserInfoFlow(website: WebsiteOption, userId: String): Flow<UserInfo?> {
        return dao.getFlow(website, userId)
    }

    suspend fun refreshUserInfo(website: WebsiteOption, userId: String) {
        val now = Clock.System.now()
        dao.get(website, userId)?.let {
            if (it.updateTime.plus(1.hours) > now) {
                return
            }
        }
        provider[website].getUserInfo(userId)?.let {
            setUserInfo(it)
        }
    }

    suspend fun getUserInfo(website: WebsiteOption, userId: String): UserInfo? {
        dao.get(website, userId)?.let {
            return it
        }
        provider[website].getUserInfo(userId)?.let {
            setUserInfo(it)
            return it
        }
        return null
    }

    suspend fun setUserInfo(userInfo: UserInfo) {
        dao.upsert(userInfo)
    }
}