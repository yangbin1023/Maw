package com.magic.maw.ui.main

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavController.Companion.KEY_DEEP_LINK_INTENT
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import kotlinx.serialization.Serializable

@Serializable
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
    data class PoolView(val poolId: Int, val postId: Int) : AppRoute()

    @Serializable
    object Popular : AppRoute()

    @Serializable
    object Favorite : AppRoute()

    @Serializable
    object Settings : AppRoute()

    @Serializable
    object Verify : AppRoute()
}

val AppRoute.rootRoute: AppRoute
    get() = when (this) {
        AppRoute.Post, AppRoute.PostList, is AppRoute.PostView, is AppRoute.PostSearch -> AppRoute.Post
        AppRoute.Pool, AppRoute.PoolList, is AppRoute.PoolPost, is AppRoute.PoolView -> AppRoute.Pool
        AppRoute.Popular -> AppRoute.Popular
        AppRoute.Favorite -> AppRoute.Favorite
        AppRoute.Settings -> AppRoute.Settings
        AppRoute.Verify -> AppRoute.Verify
    }

val AppRoute.isRootRoute: Boolean
    get() = when (this) {
        AppRoute.Post, AppRoute.PostList, AppRoute.Pool, AppRoute.PoolList,
        AppRoute.Popular, AppRoute.Favorite -> true

        else -> false
    }

val NavController.topRoute: AppRoute
    @Composable
    get() {
        val backStackEntry by this.currentBackStackEntryAsState()
        val route by remember {
            derivedStateOf {
                backStackEntry?.currentRoute?.rootRoute ?: AppRoute.Post
            }
        }
        return route
    }

val NavBackStackEntry.currentRoute: AppRoute
    get() {
        arguments?.getParcelable<Intent>(KEY_DEEP_LINK_INTENT)?.data?.pathSegments?.let {
            val appRouteClassName = AppRoute::class.java.name
            if (it.isNotEmpty() && it[0].startsWith(appRouteClassName)) {
                try {
                    val className = appRouteClassName + it[0].substring(appRouteClassName.length)
                        .replace(".", "$")
                    Logger.d("MainRoute") { "className: $className" }
                    val javaClass: Class<*> = Class.forName(className)
                    val kotlinClass = javaClass.kotlin
                    val route: AppRoute = toRoute(kotlinClass)
                    Logger.d("MainRoute") { "find route: $route" }
                    return route
                } catch (e: Exception) {
                    Logger.e("MainRoute", e)
                }
            }
        }

        return AppRoute.Post
    }