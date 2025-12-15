package com.magic.maw.ui.main

import androidx.navigation.NavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

sealed class AppRoute {
    @Serializable
    object Post : AppRoute()

    @Serializable
    object PostList : AppRoute()

    @Serializable
    data class PostView(val postId: Int) : AppRoute()

    @Serializable
    data class PostSearch(val content: String? = null) : AppRoute()

    @Serializable
    object Pool : AppRoute()

    @Serializable
    object PoolList : AppRoute()

    @Serializable
    data class PoolPost(val poolId: Int) : AppRoute()

    @Serializable
    data class PoolView(val postId: Int) : AppRoute()

    @Serializable
    object Popular : AppRoute()

    @Serializable
    object Favorite : AppRoute()

    @Serializable
    object Setting : AppRoute()
}

val AppRoute.rootRoute: AppRoute
    get() = when (this) {
        AppRoute.Post, AppRoute.PostList, is AppRoute.PostView, is AppRoute.PostSearch -> AppRoute.Post
        AppRoute.Pool, AppRoute.PoolList, is AppRoute.PoolPost, is AppRoute.PoolView -> AppRoute.Pool
        AppRoute.Popular -> AppRoute.Popular
        AppRoute.Favorite -> AppRoute.Favorite
        AppRoute.Setting -> AppRoute.Setting
    }

val AppRoute.isRootRoute: Boolean
    get() = rootRoute == this

val NavController.currentAppRoute: AppRoute
    get() = currentBackStackEntry?.toRoute<AppRoute>() ?: AppRoute.Post