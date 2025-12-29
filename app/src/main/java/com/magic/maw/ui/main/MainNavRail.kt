package com.magic.maw.ui.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import co.touchlab.kermit.Logger
import com.magic.maw.R
import com.magic.maw.ui.components.RailItem

private const val TAG = "MainNavRail"

@Composable
fun MainNavRail(
    modifier: Modifier = Modifier,
    navController: NavController,
    topRoute: AppRoute = navController.topRoute,
) {
    Logger.d(TAG) { "MainNavRail recompose" }
    NavigationRail(
        modifier = modifier,
        header = {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "",
            )
        }
    ) {
        Logger.d(TAG) { "MainNavRail item recompose" }
        RailItem(
            labelRes = R.string.post,
            iconRes = R.drawable.ic_image,
            selected = topRoute is AppRoute.Post,
            onClick = {
                val route = AppRoute.Post()
                navController.navigateTo(route = route) {
                    popUpTo(route = route)
                }
            }
        )
        RailItem(
            labelRes = R.string.pool,
            iconRes = R.drawable.ic_album,
            selected = topRoute == AppRoute.Pool,
            onClick = { navController.navigateTo(route = AppRoute.Pool) }
        )
        RailItem(
            labelRes = R.string.popular,
            iconRes = R.drawable.ic_popular,
            selected = topRoute == AppRoute.Popular,
            onClick = { navController.navigateTo(route = AppRoute.Popular) }
        )
        RailItem(
            labelRes = R.string.favorite,
            imageVector = Icons.Default.Favorite,
            selected = topRoute == AppRoute.Favorite,
            onClick = { navController.navigateTo(route = AppRoute.Favorite) }
        )
        RailItem(
            labelRes = R.string.setting,
            iconRes = R.drawable.ic_setting,
            selected = topRoute == AppRoute.Settings,
            onClick = { navController.navigateTo(route = AppRoute.Settings) }
        )
        Spacer(Modifier.weight(1.2f))
    }
}

private fun <T : Any> NavController.navigateTo(
    route: T,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    if (currentBackStackEntry?.currentRoute?.rootRoute != route) {
        navigate(route) {
            builder()
            launchSingleTop = true
        }
    }
}
