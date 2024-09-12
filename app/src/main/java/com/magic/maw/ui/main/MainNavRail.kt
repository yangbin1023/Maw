package com.magic.maw.ui.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.magic.maw.R
import com.magic.maw.ui.components.RailItem

@Composable
fun MainNavRail(
    modifier: Modifier = Modifier,
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
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
            selected = currentRoute == MainRoutes.POST,
            onClick = { onNavigate(MainRoutes.POST) }
        )
        RailItem(
            labelRes = R.string.pool,
            iconRes = R.drawable.ic_album,
            selected = currentRoute == MainRoutes.POOL,
            onClick = { onNavigate(MainRoutes.POOL) }
        )
        RailItem(
            labelRes = R.string.popular,
            iconRes = R.drawable.ic_popular,
            selected = currentRoute == MainRoutes.POPULAR,
            onClick = { onNavigate(MainRoutes.POPULAR) }
        )
        RailItem(
            labelRes = R.string.setting,
            iconRes = R.drawable.ic_setting,
            selected = currentRoute == MainRoutes.SETTING,
            onClick = { onNavigate(MainRoutes.SETTING) }
        )
        Spacer(Modifier.weight(1.2f))
    }
}