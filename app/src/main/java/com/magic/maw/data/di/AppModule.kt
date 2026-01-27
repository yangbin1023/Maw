package com.magic.maw.data.di

import com.magic.maw.data.api.parser.BaseParser
import com.magic.maw.data.api.parser.DanbooruParser
import com.magic.maw.data.api.parser.KonachanParser
import com.magic.maw.data.api.parser.WebsiteParserProvider
import com.magic.maw.data.api.parser.YandeParser
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.api.service.BaseApiService
import com.magic.maw.data.api.service.DanbooruApiService
import com.magic.maw.data.api.service.KonachanApiService
import com.magic.maw.data.api.service.YandeApiService
import com.magic.maw.data.local.db.AppDB
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.repository.PostRepository
import com.magic.maw.data.repository.TagHistoryRepository
import com.magic.maw.data.repository.TagRepository
import com.magic.maw.data.repository.UserRepository
import com.magic.maw.ui.features.pool.PoolViewModel
import com.magic.maw.ui.features.popular.PopularViewModel
import com.magic.maw.ui.features.post.PostViewModel
import com.magic.maw.ui.features.search.SearchViewModel
import com.magic.maw.ui.features.setting.SettingsViewModel
import com.magic.maw.util.createAppHttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    // 数据库
    single { AppDB.build(get()) }
    single { get<AppDB>().tagInfoDao() }
    single { get<AppDB>().tagHistoryDao() }
    single { get<AppDB>().userInfoDao() }
    single { get<AppDB>().dlDao() }

    // 网络
    single { createAppHttpClient() }

    // Parser
    single<BaseParser>(named(WebsiteOption.Yande)) { YandeParser(get()) }
    single<BaseParser>(named(WebsiteOption.Konachan)) { KonachanParser(get()) }
    single<BaseParser>(named(WebsiteOption.Danbooru)) { DanbooruParser(get()) }
    single {
        val lists = listOf(
            get<BaseParser>(named(WebsiteOption.Yande)),
            get<BaseParser>(named(WebsiteOption.Konachan)),
            get<BaseParser>(named(WebsiteOption.Danbooru)),
        )
        WebsiteParserProvider(lists)
    }

    // ApiService
    single<BaseApiService>(named(WebsiteOption.Yande)) { YandeApiService(get()) }
    single<BaseApiService>(named(WebsiteOption.Konachan)) { KonachanApiService(get()) }
    single<BaseApiService>(named(WebsiteOption.Danbooru)) { DanbooruApiService(get()) }
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

    // ViewModel
    viewModel { PostViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { PoolViewModel(get(), get()) }
    viewModel { PopularViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}