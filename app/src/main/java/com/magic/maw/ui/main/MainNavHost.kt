package com.magic.maw.ui.main

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import com.magic.maw.data.loader.PostDataLoader
import com.magic.maw.ui.pool.PoolScreen
import com.magic.maw.ui.pool.PoolViewModel2
import com.magic.maw.ui.popular.PopularScreen
import com.magic.maw.ui.popular.PopularViewModel2
import com.magic.maw.ui.post.PostScreen
import com.magic.maw.ui.post.PostViewModel
import com.magic.maw.ui.search.SearchScreen
import com.magic.maw.ui.setting.SettingScreen
import com.magic.maw.ui.verify.VerifyScreen
import com.magic.maw.ui.view.ViewScreen
import com.magic.maw.util.VerifyRequester
import com.magic.maw.util.VerifyResult
import kotlinx.coroutines.launch

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
        startDestination = startDestination
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
                        if (navController.currentBackStackEntry?.currentRoute is AppRoute.Verify) {
                            navController.popBackStack()
                        }
                    }
                },
                onCancel = {
                    VerifyRequester.onVerifyResult(VerifyResult.Failure(route.url))
                    scope.launch {
                        if (navController.currentBackStackEntry?.currentRoute is AppRoute.Verify) {
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
        Logger.d(TAG) { "postGraph content recompose" }
        composable<AppRoute.PostList> { backStackEntry ->
            Logger.d(TAG) { "PostList recompose." }
            PostScreen(
                backStackEntry = backStackEntry,
                navController = navController,
                onOpenDrawer = onOpenDrawer
            )
        }
        composable<AppRoute.PostView> { backStackEntry ->
            Logger.d(TAG) { "PostView recompose" }
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<AppRoute.Post>()
            }
            val viewModel: PostViewModel = viewModel(parentEntry)
            val route: AppRoute.PostView = backStackEntry.toRoute()
            ViewScreen(
                loader = viewModel.loader,
                navController = navController,
                postIndex = route.postId,
                route = route
            )
        }
        composable<AppRoute.PostSearch> { backStackEntry ->
            Logger.d(TAG) { "PostSearch recompose" }
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
            Logger.d(TAG) { "PoolList recompose" }
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<AppRoute.Pool>()
            }
            val viewModel: PoolViewModel2 = viewModel(parentEntry)
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
            Logger.d(TAG) { "PoolPost recompose" }
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<AppRoute.Pool>()
            }
            val viewModel: PoolViewModel2 = viewModel(parentEntry)
            val route = backStackEntry.toRoute<AppRoute.PoolPost>()
            val postIndex = backStackEntry.savedStateHandle.getLiveData<Int>(POST_INDEX).value
            if (postIndex != null) {
                backStackEntry.savedStateHandle.remove<String>(POST_INDEX)
            }
            val loader by viewModel.postLoader.collectAsStateWithLifecycle()
            val postLoader = loader ?: PostDataLoader(
                scope = viewModel.viewModelScope,
                poolId = route.poolId
            )
            PostScreen(
                loader = postLoader,
                navController = navController,
                titleText = "#${route.poolId}",
                postIndex = postIndex,
                searchEnable = false,
                negativeIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNegative = { navController.popBackStack() },
            )
        }
        composable<AppRoute.PoolView> { backStackEntry ->
            Logger.d(TAG) { "PoolView recompose" }
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<AppRoute.Pool>()
            }
            val viewModel: PoolViewModel2 = viewModel(parentEntry)
            val route = backStackEntry.toRoute<AppRoute.PoolView>()
            val loader by viewModel.postLoader.collectAsStateWithLifecycle()
            val postLoader = loader ?: PostDataLoader(
                scope = viewModel.viewModelScope,
                poolId = route.poolId,
            )
            ViewScreen(
                loader = postLoader,
                navController = navController,
                postIndex = route.postId,
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
            Logger.d(TAG) { "PopularList recompose" }
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<AppRoute.Popular>()
            }
            val viewModel: PopularViewModel2 = viewModel(parentEntry)
            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavRail()) {
                    MainNavRail(navController = navController, topRoute = AppRoute.Popular)
                }
                PopularScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onNegative = onOpenDrawer
                )
            }
        }
        composable<AppRoute.PopularView> { backStackEntry ->
            Logger.d(TAG) { "PopularView recompose" }
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<AppRoute.Popular>()
            }
            val viewModel: PopularViewModel2 = viewModel(parentEntry)
            val route = backStackEntry.toRoute<AppRoute.PopularView>()
            val loader by viewModel.currentLoader.collectAsStateWithLifecycle()
            ViewScreen(
                loader = loader,
                navController = navController,
                postIndex = route.postId,
                route = route
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