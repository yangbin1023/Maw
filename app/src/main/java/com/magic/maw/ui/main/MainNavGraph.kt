package com.magic.maw.ui.main

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.magic.maw.R
import com.magic.maw.ui.post.PostRoute
import com.magic.maw.ui.post.PostViewModel

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
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(
            route = MainRoutes.POST,
            enterTransition = {
                Log.d("MainNavGraph", "enterTransition: $this")
                null
            },
            exitTransition = {
                Log.d("MainNavGraph", "exitTransition: $this")
                null
            }
        ) {
            Log.d("MainNavGraph", "Post vm: $postViewModel, main vm: $mainViewModel")
            PostRoute(postViewModel, isExpandedScreen, openDrawer)
        }
        composable(route = MainRoutes.POOL) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(id = R.string.pool),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        composable(route = MainRoutes.POPULAR) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(id = R.string.popular),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        composable(route = MainRoutes.SETTING) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(id = R.string.setting),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}