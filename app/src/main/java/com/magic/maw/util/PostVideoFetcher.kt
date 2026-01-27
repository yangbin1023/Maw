package com.magic.maw.util

import coil3.ImageLoader
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.Options
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.api.service.BaseApiService
import com.magic.maw.data.model.constant.WebsiteOption
import okhttp3.HttpUrl.Companion.toHttpUrl

data class PostVideoModel(
    val website: WebsiteOption,
    val postId: String,
)

class PostVideoFetcher(
    private val urlCache: MutableMap<PostVideoModel, String>,
    private val apiService: BaseApiService,
    private val model: PostVideoModel,
    private val options: Options,
    private val imageLoader: ImageLoader,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val videoUrl = urlCache[model]
            ?: apiService.getThumbnailUrl(model.postId)
            ?: return null

        urlCache[model] = videoUrl
        return imageLoader.components.newFetcher(videoUrl.toHttpUrl(), options, imageLoader)?.first?.fetch()
    }

    class Factory(private val provider: ApiServiceProvider) : Fetcher.Factory<PostVideoModel> {
        private val urlCache = mutableMapOf<PostVideoModel, String>()

        override fun create(
            data: PostVideoModel,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            val apiService = provider[data.website]
            return PostVideoFetcher(urlCache, apiService, data, options, imageLoader)
        }
    }
}