package com.magic.maw.ui.main

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.os.BundleCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavController.Companion.KEY_DEEP_LINK_INTENT
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import kotlinx.serialization.Serializable

const val POST_INDEX = "postIndex"
const val POOL_INDEX = "poolIndex"

@Serializable
sealed class AppRoute {
    @Serializable
    data class Post(val searchQuery: String? = null) : AppRoute()

    @Serializable
    data class PostList(val searchQuery: String? = null) : AppRoute()

    @Serializable
    data class PostView(val postId: Int) : AppRoute()

    @Serializable
    data class PostSearch(val text: String = "") : AppRoute()

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
    object PopularList : AppRoute()

    @Serializable
    data class PopularView(val postId: Int) : AppRoute()

    @Serializable
    object Favorite : AppRoute()

    @Serializable
    object Settings : AppRoute()

    @Serializable
    data class Verify(val url: String) : AppRoute()
}

val AppRoute.rootRoute: AppRoute
    get() = when (this) {
        is AppRoute.Post, is AppRoute.PostList, is AppRoute.PostView, is AppRoute.PostSearch -> AppRoute.Post()
        AppRoute.Pool, AppRoute.PoolList, is AppRoute.PoolPost, is AppRoute.PoolView -> AppRoute.Pool
        AppRoute.Popular, AppRoute.PopularList, is AppRoute.PopularView -> AppRoute.Popular
        AppRoute.Favorite -> AppRoute.Favorite
        AppRoute.Settings -> AppRoute.Settings
        is AppRoute.Verify -> this
    }

val AppRoute.isRootRoute: Boolean
    get() = when (this) {
        is AppRoute.Post, is AppRoute.PostList,
        AppRoute.Pool, AppRoute.PoolList,
        AppRoute.Popular, AppRoute.PopularList,
        AppRoute.Favorite -> true

        else -> false
    }

val NavController.topRoute: AppRoute
    @Composable
    get() {
        val backStackEntry by this.currentBackStackEntryAsState()
        val route by remember {
            derivedStateOf {
                backStackEntry?.currentRoute?.rootRoute ?: AppRoute.Post()
            }
        }
        return route
    }

val NavBackStackEntry.currentRoute: AppRoute
    get() {
        arguments?.let { arguments ->
            val intent: Intent? =
                BundleCompat.getParcelable(arguments, KEY_DEEP_LINK_INTENT, Intent::class.java)
            intent?.data?.pathSegments?.let {
                val appRouteClassName = AppRoute::class.java.name
                if (it.isNotEmpty() && it[0].startsWith(appRouteClassName)) {
                    try {
                        val className =
                            appRouteClassName + it[0].substring(appRouteClassName.length)
                                .replace(".", "$")
                        val javaClass: Class<*> = Class.forName(className)
                        val kotlinClass = javaClass.kotlin
                        val route: AppRoute = toRoute(kotlinClass)
                        return route
                    } catch (e: Exception) {
                        Logger.e("MainRoute", e)
                    }
                }
            }
        }

        return AppRoute.Post()
    }