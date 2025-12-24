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
import androidx.compose.runtime.getValue
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
    startDestination: AppRoute = AppRoute.Post,
    onOpenDrawer: (() -> Unit)? = null
) {
    Logger.d(TAG) { "MainNavHost recompose" }
    val scope = rememberCoroutineScope()
    val postViewModel: PostViewModel = viewModel()
    val poolViewModel: PoolViewModel2 = viewModel()
    val popularViewModel: PopularViewModel2 = viewModel()

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
            postViewModel = postViewModel,
            navController = navController,
            onOpenDrawer = onOpenDrawer
        )

        poolGraph(
            poolViewModel = poolViewModel,
            navController = navController,
            onOpenDrawer = onOpenDrawer
        )

        popularGraph(
            popularViewModel = popularViewModel,
            navController = navController,
            onOpenDrawer = onOpenDrawer
        )

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
    postViewModel: PostViewModel,
    navController: NavController,
    onOpenDrawer: (() -> Unit)? = null
) {
    navigation<AppRoute.Post>(startDestination = AppRoute.PostList) {
        Logger.d(TAG) { "postGraph content recompose" }
        composable<AppRoute.PostList> { backStackEntry ->
            val postIndex = backStackEntry.savedStateHandle.getLiveData<Int>(POST_INDEX).value
            if (postIndex != null) {
                backStackEntry.savedStateHandle.remove<String>(POST_INDEX)
            }
            Logger.d(TAG) { "PostList recompose $postIndex" }
            PostScreen(
                loader = postViewModel.loader,
                navController = navController,
                onNegative = onOpenDrawer,
                postIndex = postIndex
            )
        }
        composable<AppRoute.PostView> { backStackEntry ->
            Logger.d(TAG) { "PostView recompose" }
            val route: AppRoute.PostView = backStackEntry.toRoute()
            ViewScreen(
                loader = postViewModel.loader,
                navController = navController,
                postIndex = route.postId
            )
        }
        composable<AppRoute.PostSearch> { backStackEntry ->
            Logger.d(TAG) { "PostSearch recompose" }
            val route: AppRoute.PostSearch = backStackEntry.toRoute()
            SearchScreen(
                initText = route.text,
                onFinish = { navController.popBackStack() },
                onSearch = {
                    postViewModel.loader.search(text = it)
                    navController.navigate(route = AppRoute.Post) { popUpTo(route = AppRoute.Post) }
                }
            )
        }
    }
}

fun NavGraphBuilder.poolGraph(
    poolViewModel: PoolViewModel2,
    navController: NavController,
    onOpenDrawer: (() -> Unit)? = null
) {
    navigation<AppRoute.Pool>(startDestination = AppRoute.PoolList) {
        composable<AppRoute.PoolList> {
            Logger.d(TAG) { "PoolList recompose" }
            PoolScreen(
                viewModel = poolViewModel,
                navController = navController,
                onNegative = onOpenDrawer
            )
        }
        composable<AppRoute.PoolPost> { backStackEntry ->
            Logger.d(TAG) { "PoolPost recompose" }
            val route = backStackEntry.toRoute<AppRoute.PoolPost>()
            val postIndex = backStackEntry.savedStateHandle.getLiveData<Int>(POST_INDEX).value
            if (postIndex != null) {
                backStackEntry.savedStateHandle.remove<String>(POST_INDEX)
            }
            val loader by poolViewModel.postLoader.collectAsStateWithLifecycle()
            val postLoader = loader ?: PostDataLoader(
                scope = poolViewModel.viewModelScope,
                poolId = route.poolId
            )
            PostScreen(
                loader = postLoader,
                navController = navController,
                titleText = "#${route.poolId}",
                postIndex = postIndex,
                negativeIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNegative = { navController.popBackStack() },
            )
        }
        composable<AppRoute.PoolView> { backStackEntry ->
            Logger.d(TAG) { "PoolView recompose" }
            val route = backStackEntry.toRoute<AppRoute.PoolView>()
            val loader by poolViewModel.postLoader.collectAsStateWithLifecycle()
            val postLoader = loader ?: PostDataLoader(
                scope = poolViewModel.viewModelScope,
                poolId = route.poolId
            )
            ViewScreen(
                loader = postLoader,
                navController = navController,
                postIndex = route.postId
            )
        }
    }
}

fun NavGraphBuilder.popularGraph(
    popularViewModel: PopularViewModel2,
    navController: NavController,
    onOpenDrawer: (() -> Unit)? = null
) {
    navigation<AppRoute.Popular>(startDestination = AppRoute.PopularList) {
        composable<AppRoute.PopularList> {
            Logger.d(TAG) { "PopularList recompose" }
            PopularScreen(
                viewModel = popularViewModel,
                navController = navController,
                onNegative = onOpenDrawer
            )
        }
        composable<AppRoute.PopularView> { backStackEntry ->
            Logger.d(TAG) { "PopularView recompose" }
            val route = backStackEntry.toRoute<AppRoute.PopularView>()
            val loader by popularViewModel.currentLoader.collectAsStateWithLifecycle()
            ViewScreen(
                loader = loader,
                navController = navController,
                postIndex = route.postId
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