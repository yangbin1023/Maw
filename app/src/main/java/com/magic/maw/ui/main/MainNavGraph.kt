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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.magic.maw.R
import com.magic.maw.ui.post.PostRoute
import com.magic.maw.ui.post.PostViewModel
import com.magic.maw.ui.setting.SettingScreen

@Composable
fun MainNavGraph(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    isExpandedScreen: Boolean = false,
    navController: NavHostController = rememberNavController(),
    startDestination: String = MainRoutes.POST,
    openDrawer: () -> Unit
) {
    val postViewModel: PostViewModel = viewModel(factory = PostViewModel.providerFactory())
    postViewModel.checkRefresh()
    mainViewModel.gesturesEnabled = !postViewModel.showView.collectAsState().value
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = defaultEnter,
        exitTransition = defaultExit
    ) {
        composable(route = MainRoutes.POST) {
            PostRoute(
                postViewModel = postViewModel,
                isExpandedScreen = isExpandedScreen,
                openDrawer = openDrawer
            )
        }
        composable(route = MainRoutes.POOL) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)) {
                Text(
                    text = stringResource(id = R.string.pool),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        composable(route = MainRoutes.POPULAR) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)) {
                Text(
                    text = stringResource(id = R.string.popular),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        composable(route = MainRoutes.SETTING) {
            Surface(
                modifier = modifier.fillMaxSize(),
                color = Color.Transparent,
                contentColor = contentColorFor(MaterialTheme.colorScheme.surface),
            ) {
                SettingScreen(
                    isExpandedScreen = isExpandedScreen,
                    onFinish = { navController.popBackStack() }
                )
            }
        }
    }
}

private val defaultEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    {
        val base = fadeIn(animationSpec = tween(700))
        if (targetState.destination.route == MainRoutes.SETTING) {
            slideInHorizontally(animationSpec = tween(700), initialOffsetX = { it }) + base
        } else {
            base
        }
    }

private val defaultExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    val base = fadeOut(animationSpec = tween(700))
    if (initialState.destination.route == MainRoutes.SETTING) {
        slideOutHorizontally(animationSpec = tween(700), targetOffsetX = { it }) + base
    } else {
        base
    }
}
