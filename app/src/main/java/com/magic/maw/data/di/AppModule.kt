package com.magic.maw.data.di

import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.api.service.BaseApiService
import com.magic.maw.data.api.service.JsonPathApiService
import com.magic.maw.data.api.service.danbooruJsonPathRule
import com.magic.maw.data.api.service.konachanJsonPathRule
import com.magic.maw.data.api.service.yandeJsonPathRule
import com.magic.maw.data.local.db.AppDB
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.repository.PoolRepository
import com.magic.maw.data.repository.PostRepository
import com.magic.maw.data.repository.TagHistoryRepository
import com.magic.maw.data.repository.TagRepository
import com.magic.maw.data.repository.UserRepository
import com.magic.maw.ui.features.pool.PoolViewModel
import com.magic.maw.ui.features.popular.PopularViewModel
import com.magic.maw.ui.features.post.PostViewModel
import com.magic.maw.ui.features.search.SearchViewModel
import com.magic.maw.ui.features.setting.SettingsViewModel
import com.magic.maw.ui.features.viewer.ViewerViewModel
import com.magic.maw.util.createAppHttpClient
import com.magic.maw.util.createImageLoader
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    // 数据库
    single { AppDB.build(get()) }
    single { get<AppDB>().tagDao() }
    single { get<AppDB>().tagHistoryDao() }
    single { get<AppDB>().userDao() }
    single { get<AppDB>().dlDao() }

    // 网络
    single { createAppHttpClient() }

    // ApiService
    single<BaseApiService>(named(WebsiteOption.Yande)) {
        JsonPathApiService(
            website = WebsiteOption.Yande,
            client = get(),
            jsonRuleSettings = yandeJsonPathRule
        )
//        YandeApiService(get())
    }
    single<BaseApiService>(named(WebsiteOption.Konachan)) {
        JsonPathApiService(
            website = WebsiteOption.Konachan,
            client = get(),
            jsonRuleSettings = konachanJsonPathRule
        )
    }
    single<BaseApiService>(named(WebsiteOption.Danbooru)) {
        JsonPathApiService(
            website = WebsiteOption.Danbooru,
            client = get(),
            jsonRuleSettings = danbooruJsonPathRule
        )
    }
    single {
        val lists = listOf(
            get<BaseApiService>(named(WebsiteOption.Yande)),
            get<BaseApiService>(named(WebsiteOption.Konachan)),
            get<BaseApiService>(named(WebsiteOption.Danbooru)),
        )
        ApiServiceProvider(lists)
    }

    // Repository
    single { SettingsRepository(get()) }
    single { TagRepository(get(), get()) }
    single { TagHistoryRepository(get(), get()) }
    single { UserRepository(get(), get()) }
    single { PostRepository(get(), get(), get()) }
    single { PoolRepository(get(), get()) }

    // 配置 ImageLoader
    single { createImageLoader() }

    // ViewModel
    viewModel { PostViewModel(get(), get(), get(), get(), get()) }
    viewModel { PoolViewModel(get(), get(), get()) }
    viewModel { PopularViewModel(get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get()) }
    viewModel { ViewerViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}