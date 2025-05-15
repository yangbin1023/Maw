package com.magic.maw.ui.main

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.touchlab.kermit.Logger
import com.magic.maw.ui.components.SourceChangeChecker
import com.magic.maw.ui.pool.PoolRoute
import com.magic.maw.ui.pool.PoolViewModel
import com.magic.maw.ui.popular.PopularRoute
import com.magic.maw.ui.popular.PopularViewModel
import com.magic.maw.ui.post.PostRoute
import com.magic.maw.ui.post.PostViewModel
import com.magic.maw.ui.search.SearchScreen
import com.magic.maw.ui.setting.SettingScreen
import com.magic.maw.ui.verify.VerifyScreen
import com.magic.maw.util.UiUtils.checkTopRoute
import com.magic.maw.util.configFlow
import com.magic.maw.website.parser.BaseParser
import com.magic.maw.website.parser.OnVerifyCallback
import com.magic.maw.website.parser.VerifyContainer
import kotlinx.coroutines.launch

private const val TAG = "MainNavGraph"

@Composable
fun MainNavGraph(
    modifier: Modifier = Modifier,
    inSubView: MutableState<Boolean>,
    isExpandedScreen: Boolean = false,
    navController: NavHostController = rememberNavController(),
    startDestination: String = MainRoutes.POST,
    openDrawer: () -> Unit
) {
    val postViewModel: PostViewModel = viewModel(factory = PostViewModel.providerFactory())
    val poolViewModel: PoolViewModel = viewModel()
    val popularViewModel: PopularViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val verifyContainer = registerVerifyCallback { url ->
        Logger.d(TAG) { "call on verify url: $url" }
        navController.navigate(MainRoutes.verify(url))
    }

    SourceChangeChecker {
        postViewModel.clearData()
        poolViewModel.clearData()
        popularViewModel.clearData()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = defaultEnter,
        exitTransition = defaultExit,
        popEnterTransition = defaultPopEnter,
        popExitTransition = defaultPopExit
    ) {
        composable(route = MainRoutes.POST) {
            PostRoute(
                postViewModel = postViewModel,
                onNegative = openDrawer,
                onSearch = { text, _ -> navController.navigate(MainRoutes.search(text)) },
                onOpenSubView = { inSubView.value = it }
            )
        }
        composable(route = MainRoutes.POOL) {
            PoolRoute(
                poolViewModel = poolViewModel,
                openDrawer = openDrawer,
                onOpenSubView = { inSubView.value = it }
            )
        }
        composable(route = MainRoutes.POPULAR) {
            PopularRoute(
                popularViewModel = popularViewModel,
                openDrawer = openDrawer
            )
        }
        composable(route = MainRoutes.SETTING) {
            SettingScreen(
                isExpandedScreen = isExpandedScreen,
                onFinish = { navController.popBackStack() }
            )
        }
        composable(
            route = MainRoutes.SEARCH,
            arguments = listOf(navArgument("text") { type = NavType.StringType })
        ) { navBackStackEntry ->
            val initText = navBackStackEntry.arguments?.getString("text") ?: ""
            SearchScreen(
                initText = initText,
                onFinish = { navController.popBackStack() },
                onSearch = {
                    postViewModel.search(it)
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = MainRoutes.VERIFY,
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { navBackStackEntry ->
            val url = navBackStackEntry.arguments?.getString("url") ?: ""
            printNavBackStack(navController)
            VerifyScreen(
                url = url,
                onSuccess = { text ->
                    verifyContainer?.verifySuccess(url, text)
                    scope.launch {
                        if (navController.checkTopRoute(MainRoutes.VERIFY)) {
                            navController.popBackStack()
                        }
                    }
                },
                onCancel = {
                    verifyContainer?.cancelVerify()
                    scope.launch {
                        if (navController.checkTopRoute(MainRoutes.VERIFY)) {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun registerVerifyCallback(verifyCallback: OnVerifyCallback?): VerifyContainer? {
    val scope = rememberCoroutineScope()
    val parser = BaseParser.get(configFlow.collectAsStateWithLifecycle().value.source)
    parser.setOnVerifyCallback { url ->
        scope.launch {
            verifyCallback?.invoke(url)
        }
    }
    return parser.getVerifyContainer()
}

typealias AnimatedScope = AnimatedContentTransitionScope<NavBackStackEntry>

val slideStart = AnimatedContentTransitionScope.SlideDirection.Start
val slideEnd = AnimatedContentTransitionScope.SlideDirection.End

private val defaultEnter: AnimatedScope.() -> EnterTransition = {
    val base = fadeIn(animationSpec = tween(700))
    val slideIn = slideIntoContainer(slideStart, tween(700))
    val targetRoute = targetState.destination.route
    val initialRoute = initialState.destination.route
    Logger.d(TAG) { "enter from: $initialRoute, to: $targetRoute" }
    if (!MainRoutes.isMainView(initialRoute) || !MainRoutes.isMainView(targetRoute)) {
        slideIn
    } else {
        base
    }
}

private val defaultExit: AnimatedScope.() -> ExitTransition = {
    val base = fadeOut(animationSpec = tween(700))
    val slideOut = slideOutOfContainer(slideStart, tween(700))
    val targetRoute = targetState.destination.route
    val initialRoute = initialState.destination.route
    if (!MainRoutes.isMainView(initialRoute) || !MainRoutes.isMainView(targetRoute)) {
        slideOut
    } else {
        base
    }
}

private val defaultPopEnter: AnimatedScope.() -> EnterTransition = {
    val base = fadeIn(animationSpec = tween(700))
    val slideIn = slideIntoContainer(slideEnd, tween(700))
    val targetRoute = targetState.destination.route
    val initialRoute = initialState.destination.route
    if (!MainRoutes.isMainView(initialRoute) || !MainRoutes.isMainView(targetRoute)) {
        slideIn
    } else {
        base
    }
}

private val defaultPopExit: AnimatedScope.() -> ExitTransition = {
    val base = fadeOut(animationSpec = tween(700))
    val slideOut = slideOutOfContainer(slideEnd, tween(700))
    val targetRoute = targetState.destination.route
    val initialRoute = initialState.destination.route
    if (!MainRoutes.isMainView(initialRoute) || !MainRoutes.isMainView(targetRoute)) {
        slideOut
    } else {
        base
    }
}

@SuppressLint("RestrictedApi")
private fun printNavBackStack(navController: NavHostController) {
    try {
        val list = navController.currentBackStack.value
        val routeList = ArrayList<String>()
        for (item in list) {
            routeList.add(item.destination.route ?: "unknown")
        }
        Logger.d(TAG)  { "current back stack list: $routeList" }
    } catch (e: Exception) {
        Logger.e(e, TAG) { "printNavBackStack failed. ${e.message}" }
    }
}