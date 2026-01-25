package com.magic.maw.data.repository

import com.magic.maw.data.api.parser.WebsiteParserProvider
import com.magic.maw.data.local.db.dao.UserInfoDao
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.UserInfo

class UserRepository(
    private val dao: UserInfoDao,
    private val provider: WebsiteParserProvider
) {
    suspend fun getUserInfo(website: WebsiteOption, userId: String): UserInfo? {
        dao.get(website, userId)?.let {
            return it
        }
        val parser = provider[website]
        parser.requestUserInfo(userId)?.let {
            setUserInfo(it)
            return it
        }
        return null
    }

    suspend fun setUserInfo(userInfo: UserInfo) {
        dao.upsert(userInfo)
    }
}