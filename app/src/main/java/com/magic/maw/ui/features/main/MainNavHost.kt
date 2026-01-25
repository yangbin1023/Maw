package com.magic.maw.ui.features.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import com.magic.maw.data.api.loader.PostDataLoader
import com.magic.maw.ui.features.pool.PoolScreen
import com.magic.maw.ui.features.pool.PoolViewModel
import com.magic.maw.ui.features.popular.PopularScreen
import com.magic.maw.ui.features.popular.PopularViewModel
import com.magic.maw.ui.features.post.PostScreen
import com.magic.maw.ui.features.post.PostViewModel
import com.magic.maw.ui.features.post.getPostIndex
import com.magic.maw.ui.features.search.SearchScreen
import com.magic.maw.ui.features.setting.SettingScreen
import com.magic.maw.ui.features.verify.VerifyScreen
import com.magic.maw.ui.features.viewer.ViewScreen
import com.magic.maw.util.VerifyRequester
import com.magic.maw.util.VerifyResult
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private const val TAG = "MainNavHost"

@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: AppRoute = AppRoute.Post(),
    onOpenDrawer: (() -> Unit)? = null
) {
    Logger.d(TAG) { "MainNavHost recompose" }
    val scope = rememberCoroutineScope()
    VerifyRequester.callback = { url ->
        scope.launch {
            Logger.d(TAG) { "call on verify url: $url" }
            navController.navigate(AppRoute.Verify(url = url))
        }
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
        enterTransition = defaultEnter,
        exitTransition = defaultExit,
        popEnterTransition = defaultPopEnter,
        popExitTransition = defaultPopExit,
    ) {
        Logger.d(TAG) { "MainNavHost item recompose" }

        postGraph(
            navController = navController,
            onOpenDrawer = onOpenDrawer
        )

        poolGraph(
            navController = navController,
            onOpenDrawer = onOpenDrawer
        )

        popularGraph(
            navController = navController,
            onOpenDrawer = onOpenDrawer
        )

        composable<AppRoute.Favorite> {
            Logger.d(TAG) { "Favorite recompose" }
            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavRail()) {
                    MainNavRail(navController = navController, topRoute = AppRoute.Favorite)
                }
                TestScaffold(
                    title = "Favorite",
                    navigationIconOnClick = onOpenDrawer ?: {},
                    navigationIconImageVector = Icons.Default.Menu,
                    testText = "Favorite",
                    testBtnOnClick = { }
                )
            }
        }

        composable<AppRoute.Settings> {
            Logger.d(TAG) { "Setting recompose" }
            SettingScreen(navController = navController)
        }

        composable<AppRoute.Verify> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.Verify>()
            Logger.d(TAG) { "Verify recompose" }

            VerifyScreen(
                url = route.url,
                onSuccess = { text ->
                    VerifyRequester.onVerifyResult(VerifyResult.Success(route.url, text))
                    scope.launch {
                        if (navController.currentBackStackEntry?.appRoute is AppRoute.Verify) {
                            navController.popBackStack()
                        }
                    }
                },
                onCancel = {
                    VerifyRequester.onVerifyResult(VerifyResult.Failure(route.url))
                    scope.launch {
                        if (navController.currentBackStackEntry?.appRoute is AppRoute.Verify) {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }
    }
}

fun NavGraphBuilder.postGraph(
    navController: NavController,
    onOpenDrawer: (() -> Unit)? = null
) {
    navigation<AppRoute.Post>(startDestination = AppRoute.PostList()) {
        composable<AppRoute.PostList> { backStackEntry ->
            PostScreen(
                backStackEntry = backStackEntry,
                navController = navController,
                onOpenDrawer = onOpenDrawer
            )
        }
        composable<AppRoute.PostViewer> { backStackEntry ->
            val viewModel = koinViewModel<PostViewModel, AppRoute.Post>(navController, backStackEntry)
            val route: AppRoute.PostViewer = backStackEntry.toRoute()
            ViewScreen(
                loader = viewModel,
                navController = navController,
                postIndex = route.postIndex,
                route = route
            )
        }
        composable<AppRoute.PostSearch> { backStackEntry ->
            val route: AppRoute.PostSearch = backStackEntry.toRoute()
            SearchScreen(
                initText = route.text,
                onFinish = { navController.popBackStack() },
                onSearch = {
                    navController.navigate(route = AppRoute.Post(searchQuery = it)) {
                        popUpTo(route = route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}

fun NavGraphBuilder.poolGraph(
    navController: NavController,
    onOpenDrawer: (() -> Unit)? = null
) {
    navigation<AppRoute.Pool>(startDestination = AppRoute.PoolList) {
        composable<AppRoute.PoolList> { backStackEntry ->
            val viewModel = koinViewModel<PoolViewModel, AppRoute.Pool>(navController, backStackEntry)
            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavRail()) {
                    MainNavRail(navController = navController, topRoute = AppRoute.Pool)
                }
                PoolScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onNegative = onOpenDrawer
                )
            }
        }
        composable<AppRoute.PoolPost> { backStackEntry ->
            val viewModel = koinViewModel<PoolViewModel, AppRoute.Pool>(navController, backStackEntry)
            val route = backStackEntry.toRoute<AppRoute.PoolPost>()
            val postIndex = backStackEntry.getPostIndex()
            PostScreen(
                loader = viewModel.postLoader,
                navController = navController,
                titleText = "#${route.poolId}",
                postIndex = postIndex,
                searchEnable = false,
                negativeIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNegative = { navController.popBackStack() },
                onItemClick = {
//                    postLoader.setViewIndex(it)
                    navController.navigate(route = AppRoute.PoolViewer(poolId = route.poolId, postIndex = it))
                }
            )
        }
        composable<AppRoute.PoolViewer> { backStackEntry ->
            val viewModel = koinViewModel<PoolViewModel, AppRoute.Pool>(navController, backStackEntry)
            val route = backStackEntry.toRoute<AppRoute.PoolViewer>()
            ViewScreen(
                loader = viewModel.postLoader,
                navController = navController,
                postIndex = route.postIndex,
                route = route
            )
        }
    }
}

fun NavGraphBuilder.popularGraph(
    navController: NavController,
    onOpenDrawer: (() -> Unit)? = null
) {
    navigation<AppRoute.Popular>(startDestination = AppRoute.PopularList) {
        composable<AppRoute.PopularList> { backStackEntry ->
            val postIndex = backStackEntry.getPostIndex()
            val viewModel = koinViewModel<PopularViewModel, AppRoute.Popular>(navController, backStackEntry)
            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavRail()) {
                    MainNavRail(navController = navController, topRoute = AppRoute.Popular)
                }
                PopularScreen(
                    viewModel = viewModel,
                    navController = navController,
                    postIndex = postIndex,
                    onNegative = onOpenDrawer
                )
            }
        }
        composable<AppRoute.PopularViewer> { backStackEntry ->
            val viewModel = koinViewModel<PopularViewModel, AppRoute.Popular>(navController, backStackEntry)
            val route = backStackEntry.toRoute<AppRoute.PopularViewer>()
            val currentData by viewModel.currentData.collectAsStateWithLifecycle()
            ViewScreen(
                loader = currentData.loader,
                navController = navController,
                postIndex = route.postIndex,
                route = route
            )
        }
    }
}

private const val AnimatedDurationMillis = 500

private val AnimatedContentTransitionScope<NavBackStackEntry>.isRootRouteTransition: Boolean
    get() = targetState.appRoute.isRootRoute && initialState.appRoute.isRootRoute

private val defaultEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (isRootRouteTransition) {
        fadeIn(animationSpec = tween(0))
    } else {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(AnimatedDurationMillis)
        )
    }
}

private val defaultExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (isRootRouteTransition) {
        fadeOut(animationSpec = tween(0))
    } else {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(AnimatedDurationMillis)
        )
    }
}

private val defaultPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (isRootRouteTransition) {
        fadeIn(animationSpec = tween(0))
    } else {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(AnimatedDurationMillis)
        )
    }
}

private val defaultPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (isRootRouteTransition) {
        fadeOut(animationSpec = tween(0))
    } else {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(AnimatedDurationMillis)
        )
    }
}

@Composable
inline fun <reified T : ViewModel, reified R : AppRoute> koinViewModel(
    navController: NavController,
    entry: NavBackStackEntry? = navController.currentBackStackEntry
): T {
    val parentEntry = remember(entry) {
        navController.getBackStackEntry<R>()
    }
    return koinViewModel(viewModelStoreOwner = parentEntry)
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