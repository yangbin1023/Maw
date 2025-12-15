package com.magic.maw.ui.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.magic.maw.R
import com.magic.maw.ui.components.RailItem
import com.magic.maw.util.UiUtils.currentRoute

@Composable
fun MainNavRail(
    modifier: Modifier = Modifier,
    navController: NavController,
) {
    val currentRoute by remember { derivedStateOf { navController.currentAppRoute.rootRoute } }
    NavigationRail(
        modifier = modifier,
        header = {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "",
            )
        }
    ) {
        Spacer(Modifier.weight(1f))

        RailItem(
            labelRes = R.string.post,
            iconRes = R.drawable.ic_image,
            selected = currentRoute == AppRoute.Post,
            onClick = { navController.navigate(route = AppRoute.Post) }
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
            selected = currentRoute == AppRoute.Setting,
            onClick = { navController.navigate(route = AppRoute.Setting) }
        )
        Spacer(Modifier.weight(1.2f))
    }
}