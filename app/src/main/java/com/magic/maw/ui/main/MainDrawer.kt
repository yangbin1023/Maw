package com.magic.maw.ui.main

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.magic.maw.R
import com.magic.maw.ui.SettingActivity
import com.magic.maw.ui.components.DrawerItem
import com.magic.maw.util.UiUtils.startActivity

@Composable
fun MainDrawer(
    modifier: Modifier = Modifier,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    closeDrawer: () -> Unit
) {
    BoxWithConstraints {
        val context = LocalContext.current
        val targetWidth = maxWidth * 0.85f
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
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            DrawerItem(
                labelRes = R.string.post,
                iconRes = R.drawable.ic_image,
                selected = currentRoute == MainRoutes.POST,
                onClick = { onNavigate(MainRoutes.POST);closeDrawer() }
            )
            DrawerItem(
                labelRes = R.string.pool,
                iconRes = R.drawable.ic_album,
                selected = currentRoute == MainRoutes.POOL,
                onClick = { onNavigate(MainRoutes.POOL);closeDrawer() }
            )
            DrawerItem(
                labelRes = R.string.popular,
                iconRes = R.drawable.ic_popular,
                selected = currentRoute == MainRoutes.POPULAR,
                onClick = { onNavigate(MainRoutes.POPULAR);closeDrawer() }
            )
            DrawerItem(
                labelRes = R.string.setting,
                iconRes = R.drawable.ic_setting,
                selected = currentRoute == MainRoutes.SETTING,
                onClick = { context.startActivity(SettingActivity::class.java) }
            )
        }
    }
}
