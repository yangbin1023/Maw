package com.magic.maw.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magic.maw.R

@Composable
fun DrawerItem(
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val text = stringResource(id = labelRes)
    NavigationDrawerItem(
        label = { Text(text = text) },
        icon = if (iconRes != 0) ({
            Icon(painter = painterResource(id = iconRes), contentDescription = text)
        }) else null,
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .fillMaxWidth()
            .height(50.dp),
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
    )
}

@Composable
fun DrawerItem(
    @StringRes labelRes: Int,
    imageVector: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val text = stringResource(id = labelRes)
    NavigationDrawerItem(
        label = { Text(text = text) },
        icon = { Icon(imageVector = imageVector, contentDescription = text) },
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .fillMaxWidth()
            .height(50.dp),
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
    )
}

@Preview
@Composable
fun DrawerItemPreview() {
    DrawerItem(labelRes = R.string.setting, iconRes = R.drawable.ic_setting, selected = false) {}
}