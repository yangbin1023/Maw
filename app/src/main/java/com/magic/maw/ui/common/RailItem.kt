package com.magic.maw.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun RailItem(
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    noLabel: Boolean = false,
    onClick: () -> Unit
) {
    val text = stringResource(id = labelRes)
    val label: @Composable (() -> Unit)? = if (noLabel) {
        null
    } else {
        { Text(text = text) }
    }
    NavigationRailItem(
        modifier = Modifier.widthIn(60.dp),
        label = label,
        icon = {
            Icon(painter = painterResource(id = iconRes), contentDescription = text)
        },
        selected = selected,
        onClick = onClick,
    )
}

@Composable
fun RailItem(
    @StringRes labelRes: Int,
    imageVector: ImageVector,
    selected: Boolean,
    noLabel: Boolean = false,
    onClick: () -> Unit
) {
    val text = stringResource(id = labelRes)
    val label: @Composable (() -> Unit)? = if (noLabel) {
        null
    } else {
        { Text(text = text) }
    }
    NavigationRailItem(
        modifier = Modifier.widthIn(60.dp),
        label = label,
        icon = {
            Icon(imageVector = imageVector, contentDescription = text)
        },
        selected = selected,
        onClick = onClick,
    )
}