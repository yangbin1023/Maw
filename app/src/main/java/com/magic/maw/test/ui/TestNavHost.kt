package com.magic.maw.test.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import com.magic.maw.ui.main.slideEnd
import com.magic.maw.ui.main.slideStart
import com.magic.maw.ui.setting.SettingScreen2

private const val TRANSITION_TIME = 500

@Composable
fun TestNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = "main",
        enterTransition = {
            val fadeIn = fadeIn(animationSpec = tween(TRANSITION_TIME))
            val slideIn = slideIntoContainer(slideStart, tween(TRANSITION_TIME))
            fadeIn + slideIn
        },
        exitTransition = {
            val fadeOut = fadeOut(animationSpec = tween(TRANSITION_TIME))
            val slideOut = slideOutOfContainer(slideStart, tween(TRANSITION_TIME))
            fadeOut + slideOut
        },
        popEnterTransition = {
            val fadeIn = fadeIn(animationSpec = tween(TRANSITION_TIME))
            val slideIn = slideIntoContainer(slideEnd, tween(TRANSITION_TIME))
            fadeIn + slideIn
        },
        popExitTransition = {
            val fadeOut = fadeOut(animationSpec = tween(TRANSITION_TIME))
            val slideOut = slideOutOfContainer(slideEnd, tween(TRANSITION_TIME))
            fadeOut + slideOut
        }
    ) {
        composable("main") {
            TestScreen(navController = navController)
        }
        composable("settings") {
            TestSettings(navController = navController)
        }
        composable("settings2") {
            Logger.d("TestNavHost") { "settings2 compose" }
            SettingScreen2(navController = navController)
        }
        composable<TestSearchRoute> { backStackEntry ->
            val testSearchRoute: TestSearchRoute = backStackEntry.toRoute()
            TestSearchScreen(
                navController = navController,
                context = testSearchRoute.context,
            )
        }
    }
}