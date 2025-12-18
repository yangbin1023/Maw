package com.magic.maw.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import com.magic.maw.ui.setting.SettingScreen2

private const val TAG = "MainNavHost"

@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: AppRoute = AppRoute.Post,
    onOpenDrawer: (() -> Unit)? = null
) {
    Logger.d(TAG) { "MainNavHost recompose" }
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        Logger.d(TAG) { "MainNavHost item recompose" }

        postGraph(navController = navController, onOpenDrawer = onOpenDrawer)

        navigation<AppRoute.Pool>(startDestination = AppRoute.PoolList) {
            composable<AppRoute.PoolList> {
                Logger.d(TAG) { "PoolList recompose" }
                TestScaffold(
                    title = "Pool",
                    navigationIconOnClick = onOpenDrawer ?: {},
                    navigationIconImageVector = Icons.Default.Menu,
                    testText = "查看图册",
                    testBtnOnClick = { navController.navigate(route = AppRoute.PoolPost(poolId = 1)) },
                )
            }
            composable<AppRoute.PoolPost> { backStackEntry ->
                Logger.d(TAG) { "PoolPost recompose" }
                val route = backStackEntry.toRoute<AppRoute.PoolPost>()
                TestScaffold(
                    title = "PoolPost",
                    navigationIconOnClick = { navController.navigate(route = AppRoute.PoolList) },
                    testText = "查看大图",
                    testBtnOnClick = {
                        navController.navigate(
                            route = AppRoute.PoolView(poolId = route.poolId, postId = 1)
                        )
                    },
                    test2Text = route.poolId.toString(),
                    test2BtnOnClick = { }
                )
            }
            composable<AppRoute.PoolView> { backStackEntry ->
                Logger.d(TAG) { "PoolView recompose" }
                val route = backStackEntry.toRoute<AppRoute.PoolView>()
                TestScaffold(
                    title = "PoolPostView",
                    navigationIconOnClick = {
                        navController.navigate(
                            route = AppRoute.PoolPost(
                                poolId = route.poolId
                            )
                        )
                    },
                    testText = route.postId.toString(),
                    testBtnOnClick = { }
                )
            }
        }

        composable<AppRoute.Popular> {
            Logger.d(TAG) { "Popular recompose" }
            TestScaffold(
                title = "Popular",
                navigationIconOnClick = onOpenDrawer ?: {},
                navigationIconImageVector = Icons.Default.Menu,
                testText = "Popular",
                testBtnOnClick = { }
            )
        }

        composable<AppRoute.Favorite> {
            Logger.d(TAG) { "Favorite recompose" }
            TestScaffold(
                title = "Favorite",
                navigationIconOnClick = onOpenDrawer ?: {},
                navigationIconImageVector = Icons.Default.Menu,
                testText = "Favorite",
                testBtnOnClick = { }
            )
        }

        composable<AppRoute.Settings> {
            Logger.d(TAG) { "Setting recompose" }
            SettingScreen2(navController = navController)
        }

        composable<AppRoute.Verify> {
            Logger.d(TAG) { "Verify recompose" }
            TestScaffold(
                title = "Verify",
                navigationIconOnClick = onOpenDrawer ?: {},
                testText = "Verify",
                testBtnOnClick = { }
            )
        }
    }
}

fun NavGraphBuilder.postGraph(navController: NavController, onOpenDrawer: (() -> Unit)? = null) {
    navigation<AppRoute.Post>(startDestination = AppRoute.PostList) {
        Logger.d(TAG) { "postGraph content recompose" }
        composable<AppRoute.PostList> {
            Logger.d(TAG) { "PostList recompose" }
            TestScaffold(
                title = "Post",
                navigationIconOnClick = onOpenDrawer ?: {},
                navigationIconImageVector = Icons.Default.Menu,
                testText = "查看大图",
                testBtnOnClick = { navController.navigate(route = AppRoute.PostView(postId = 1)) },
                test2Text = "搜索",
                test2BtnOnClick = { navController.navigate(route = AppRoute.PostSearch()) }
            )
        }
        composable<AppRoute.PostView> { backStackEntry ->
            Logger.d(TAG) { "PostView recompose" }
            val route: AppRoute.PostView = backStackEntry.toRoute()
            TestScaffold(
                title = "PostView",
                navigationIconOnClick = { navController.popBackStack() },
                testText = "搜索",
                testBtnOnClick = { navController.navigate(route = AppRoute.PostSearch(content = "test")) },
                test2Text = route.postId.toString(),
                test2BtnOnClick = { }
            )
        }
        composable<AppRoute.PostSearch> { backStackEntry ->
            Logger.d(TAG) { "PostSearch recompose" }
            val route: AppRoute.PostSearch = backStackEntry.toRoute()
            TestScaffold(
                title = "PostSearch",
                navigationIconOnClick = { navController.popBackStack() },
                testText = "搜索",
                testBtnOnClick = {
                    navController.navigate(route = AppRoute.PostList) { popUpTo(route = AppRoute.PostList) }
                },
                test2Text = route.content ?: "(empty)",
                test2BtnOnClick = { }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScaffold(
    modifier: Modifier = Modifier,
    title: String = "",
    navigationIconOnClick: () -> Unit = {},
    navigationIconImageVector: ImageVector = Icons.AutoMirrored.Default.ArrowBack,
    testText: String = "测试",
    testBtnOnClick: () -> Unit = {},
    test2Text: String = "测试2",
    test2BtnOnClick: (() -> Unit)? = null
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = navigationIconOnClick) {
                        Icon(
                            imageVector = navigationIconImageVector,
                            contentDescription = ""
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = testBtnOnClick) { Text(text = testText) }
                if (test2BtnOnClick != null) {
                    Button(onClick = test2BtnOnClick) { Text(text = test2Text) }
                }
            }
        }
    }
}