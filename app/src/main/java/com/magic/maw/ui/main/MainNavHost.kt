package com.magic.maw.ui.main

import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.magic.maw.ui.setting.SettingScreen2

@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: AppRoute = AppRoute.Post,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        navigation<AppRoute.Post>(startDestination = AppRoute.PostList) {
            composable<AppRoute.PostList> {

            }
            composable<AppRoute.PostView> {

            }
            composable<AppRoute.PostSearch> {
                Button(onClick = {
                    navController.navigate(route = AppRoute.PostList) {
                        popUpTo(route = AppRoute.PostList) {
                            inclusive = true
                        }
                    }
                }) { }
            }
        }

        navigation<AppRoute.Pool>(startDestination = AppRoute.PoolList) {
            composable<AppRoute.PoolList> {

            }
            composable<AppRoute.PoolPost> { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.PoolPost>()
            }
            composable<AppRoute.PoolView> {

            }
        }

        composable<AppRoute.Popular> {

        }

        composable<AppRoute.Favorite> {

        }

        composable<AppRoute.Setting> {
            SettingScreen2(navController = navController)
        }
    }
}