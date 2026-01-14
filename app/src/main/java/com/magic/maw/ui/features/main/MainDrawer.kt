package com.magic.maw.ui.features.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import co.touchlab.kermit.Logger
import com.magic.maw.R
import com.magic.maw.ui.common.DrawerItem
import com.magic.maw.util.UiUtils.getStatusBarHeight

private const val TAG = "MainDrawer"

@Composable
fun MainModalDrawerSheet(
    modifier: Modifier = Modifier,
    navController: NavController,
    closeDrawer: () -> Unit
) {
    val currentRoute = navController.topRoute
    Logger.d(TAG) { "AppDrawer recompose" }
    ModalDrawerSheet(
        modifier = modifier.resetDrawerWidth(),
        drawerShape = RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp),
        windowInsets = WindowInsets(top = LocalContext.current.getStatusBarHeight()),
    ) {
        Logger.d(TAG) { "AppDrawer item recompose" }
        Spacer(modifier = Modifier.height(16.dp))

        DrawerItem(
            labelRes = R.string.post,
            iconRes = R.drawable.ic_image,
            selected = currentRoute is AppRoute.Post,
            onClick = {
                val route = AppRoute.Post()
                navController.navigateAndCloseDrawer(
                    route = route,
                    closeDrawer = closeDrawer
                ) {
                    popUpTo(route = route)
                }
            }
        )
        DrawerItem(
            labelRes = R.string.pool,
            iconRes = R.drawable.ic_album,
            selected = currentRoute == AppRoute.Pool,
            onClick = {
                navController.navigateAndCloseDrawer(
                    route = AppRoute.Pool,
                    closeDrawer = closeDrawer
                )
            }
        )
        DrawerItem(
            labelRes = R.string.popular,
            iconRes = R.drawable.ic_popular,
            selected = currentRoute == AppRoute.Popular,
            onClick = {
                navController.navigateAndCloseDrawer(
                    route = AppRoute.Popular,
                    closeDrawer = closeDrawer
                )
            }
        )
        DrawerItem(
            labelRes = R.string.favorite,
            imageVector = Icons.Filled.Favorite,
            selected = currentRoute == AppRoute.Favorite,
            onClick = {
                navController.navigateAndCloseDrawer(
                    route = AppRoute.Favorite,
                    closeDrawer = closeDrawer
                )
            }
        )
        DrawerItem(
            labelRes = R.string.setting,
            iconRes = R.drawable.ic_setting,
            selected = currentRoute == AppRoute.Settings,
            onClick = {
                navController.navigateAndCloseDrawer(
                    route = AppRoute.Settings,
                    closeDrawer = closeDrawer
                )
            }
        )
    }
}

@Composable
private fun Modifier.resetDrawerWidth(): Modifier {
    val screenWidth = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    val targetWidth = screenWidth * 0.85f
    return if (targetWidth < 360.dp) {
        sizeIn(
            minWidth = if (targetWidth > 240.dp) 240.dp else targetWidth,
            maxWidth = targetWidth,
        )
    } else {
        this
    }
}

private fun <T : Any> NavController.navigateAndCloseDrawer(
    route: T,
    closeDrawer: () -> Unit,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    if (currentBackStackEntry?.appRoute?.rootRoute != route) {
        navigate(route, builder)
        closeDrawer()
    }
}