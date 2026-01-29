package com.magic.maw.data.interceptor

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.entity.RequestMeta
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.site.PoolData
import com.magic.maw.data.model.site.PostData

data class PostThumbnailItem(val website: WebsiteOption, val postId: String)

class DataThumbnailInterceptor(
    private val apiServiceProvider: ApiServiceProvider,
    private val settingsRepository: SettingsRepository,
) : Interceptor {
    private val urlCache = mutableMapOf<PostThumbnailItem, String>()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data
        val url = when (data) {
            is PostThumbnailItem -> getThumbnailUrl(data)
            is PostData -> getPostThumbnailUrl(data)
            is PoolData -> {
                var postData = data.posts.firstOrNull()
                if (postData == null) {
                    val ratings = settingsRepository.settings.websiteSettings.ratings
                    val filter = RequestFilter(poolId = data.id, ratings = ratings)
                    val apiService = apiServiceProvider[data.website]
                    val response = apiService.getPostData(filter = filter, meta = RequestMeta())
                    response.items.firstOrNull()?.let {
                        data.posts = response.items
                        postData = it
                    }
                }
                getPostThumbnailUrl(postData)
            }

            else -> null
        }

        url?.let { url ->
            val request = chain.request.newBuilder().data(url).build()
            return chain.withRequest(request).proceed()
        }

        return chain.proceed()
    }

    private suspend fun getThumbnailUrl(data: PostThumbnailItem): String? {
        val apiService = apiServiceProvider[data.website]
        val url = urlCache[data]
            ?: apiService.getThumbnailUrl(data.postId)
            ?: return null
        urlCache[data] = url
        return url
    }

    private suspend fun getPostThumbnailUrl(postData: PostData?): String? {
        val postData = postData ?: return null
        postData.sampleInfo?.url?.let {
            if (it.isNotBlank()) return it
        }
        postData.previewInfo.url.let {
            if (it.isNotBlank()) return it
        }
        postData.largeInfo?.url?.let {
            if (it.isNotBlank()) return it
        }
        postData.originalInfo.url.let {
            if (it.isNotBlank()) return it
        }
        val model = PostThumbnailItem(website = postData.website, postId = postData.id)
        return getThumbnailUrl(model)
    }
}