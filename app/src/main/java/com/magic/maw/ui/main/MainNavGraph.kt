package com.magic.maw.ui.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.magic.maw.R
import com.magic.maw.ui.components.SourceChangeChecker
import com.magic.maw.ui.pool.PoolRoute
import com.magic.maw.ui.pool.PoolViewModel
import com.magic.maw.ui.post.PostRoute
import com.magic.maw.ui.post.PostViewModel
import com.magic.maw.ui.search.SearchScreen
import com.magic.maw.ui.setting.SettingScreen

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

    SourceChangeChecker {
        postViewModel.clearData()
        poolViewModel.clearData()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = defaultEnter,
        exitTransition = defaultExit
    ) {
        composable(
            route = MainRoutes.POST,
            arguments = listOf(navArgument("tagText") { type = NavType.StringType })
        ) {
            PostRoute(
                postViewModel = postViewModel,
                onNegative = openDrawer,
                openSearch = { navController.navigate(MainRoutes.search(it)) },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = stringResource(id = R.string.popular),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
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
    }
}

typealias AnimatedScope = AnimatedContentTransitionScope<NavBackStackEntry>

private val defaultEnter: AnimatedScope.() -> EnterTransition = {
    val base = fadeIn(animationSpec = tween(700))
    val slideIn = slideInHorizontally(animationSpec = tween(700), initialOffsetX = { it })
    when (targetState.destination.route) {
        MainRoutes.SETTING -> slideIn + base
        MainRoutes.SEARCH -> slideIn + base
        else -> base
    }
}

private val defaultExit: AnimatedScope.() -> ExitTransition = {
    val base = fadeOut(animationSpec = tween(700))
    val slideOut = slideOutHorizontally(animationSpec = tween(700), targetOffsetX = { it })
    when (initialState.destination.route) {
        MainRoutes.SETTING -> slideOut + base
        MainRoutes.SEARCH -> slideOut + base
        else -> base
    }
}
