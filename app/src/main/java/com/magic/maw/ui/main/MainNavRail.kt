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
import co.touchlab.kermit.Logger
import com.magic.maw.R
import com.magic.maw.ui.components.RailItem

private const val TAG = "MainNavRail"

@Composable
fun MainNavRail(
    modifier: Modifier = Modifier,
    navController: NavController,
) {
    val currentRoute = navController.topRoute

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
            selected = currentRoute == AppRoute.Post,
            onClick = { navController.navigate(route = AppRoute.Post) { popUpTo(route = AppRoute.Post) } }
        )
        RailItem(
            labelRes = R.string.pool,
            iconRes = R.drawable.ic_album,
            selected = currentRoute == AppRoute.Pool,
            onClick = { navController.navigate(route = AppRoute.Pool) }
        )
        RailItem(
            labelRes = R.string.popular,
            iconRes = R.drawable.ic_popular,
            selected = currentRoute == AppRoute.Popular,
            onClick = { navController.navigate(route = AppRoute.Popular) }
        )
        RailItem(
            labelRes = R.string.favorite,
            imageVector = Icons.Default.Favorite,
            selected = currentRoute == AppRoute.Favorite,
            onClick = { navController.navigate(route = AppRoute.Favorite) }
        )
        RailItem(
            labelRes = R.string.setting,
            iconRes = R.drawable.ic_setting,
            selected = currentRoute == AppRoute.Settings,
            onClick = { navController.navigate(route = AppRoute.Settings) }
        )
        Spacer(Modifier.weight(1.2f))
    }
}