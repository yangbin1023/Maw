package com.magic.maw.ui.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.magic.maw.R
import com.magic.maw.ui.components.DrawerItem
import com.magic.maw.util.UiUtils.getStatusBarHeight

@Composable
fun MainDrawer(
    modifier: Modifier = Modifier,
    currentRoute: String,
    navController: NavController,
    closeDrawer: () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val targetWidth = screenWidth * 0.85f
    val sheetModifier = if (targetWidth < 360.dp) {
        val minWidth = if (targetWidth > 240.dp) 240.dp else targetWidth
        modifier.sizeIn(
            minWidth = minWidth,
            maxWidth = targetWidth,
        )
    } else {
        modifier
    }
    ModalDrawerSheet(
        modifier = sheetModifier,
        drawerShape = RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp),
        windowInsets = WindowInsets(top = LocalContext.current.getStatusBarHeight()),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        DrawerItem(
            labelRes = R.string.post,
            iconRes = R.drawable.ic_image,
            selected = currentRoute == MainRoutes.POST,
            onClick = { navController.navigate(MainRoutes.POST);closeDrawer() }
        )
        DrawerItem(
            labelRes = R.string.pool,
            iconRes = R.drawable.ic_album,
            selected = currentRoute == MainRoutes.POOL,
            onClick = { navController.navigate(MainRoutes.POOL);closeDrawer() }
        )
        DrawerItem(
            labelRes = R.string.popular,
            iconRes = R.drawable.ic_popular,
            selected = currentRoute == MainRoutes.POPULAR,
            onClick = { navController.navigate(MainRoutes.POPULAR);closeDrawer() }
        )
        DrawerItem(
            labelRes = R.string.setting,
            iconRes = R.drawable.ic_setting,
            selected = currentRoute == MainRoutes.SETTING,
            onClick = { navController.navigate(MainRoutes.SETTING);closeDrawer() }
        )
    }
}
