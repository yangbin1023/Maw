package com.magic.maw.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    title: String,
    subTitle: String? = null,
    tips: String? = null,
    contentDescription: String? = null,
    enable: Boolean = true,
    showIcon: Boolean = true,
    imageVector: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    onClick: () -> Unit = {}
) {
    val paddingModifier = if (modifier == Modifier) Modifier.itemPadding() else modifier
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Button
        }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .clickable(enabled = enable, onClick = throttle(func = onClick))
            .fillMaxWidth()
            .defaultMinSize(minHeight = SettingItemDefaults.defaultMinHeight)
            .then(paddingModifier)
            .then(semantics),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = true)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subTitle != null) {
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (tips != null) {
            Text(
                text = tips,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showIcon) {
            Icon(imageVector = imageVector, contentDescription = null)
        }
    }
}

@Composable
fun DialogSettingItem(
    modifier: Modifier = Modifier,
    title: String,
    subTitle: String? = null,
    tips: String? = null,
    contentDescription: String? = null,
    enable: Boolean = true,
    showIcon: Boolean = true,
    imageVector: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    dialogContent: @Composable (onDismiss: () -> Unit) -> Unit = {}
) {
    var showDialog: Boolean by remember { mutableStateOf(false) }
    SettingItem(
        modifier = modifier,
        title = title,
        subTitle = subTitle,
        tips = tips,
        contentDescription = contentDescription,
        enable = enable,
        showIcon = showIcon,
        imageVector = imageVector
    ) {
        showDialog = true
    }
    val onDismiss = {
        showDialog = false
    }
    if (showDialog) {
        dialogContent(onDismiss)
    }
}

@Composable
fun MenuSettingItem(
    modifier: Modifier = Modifier,
    title: String,
    subTitle: String? = null,
    enable: Boolean = true,
    items: Array<String>,
    checkItem: Int = 0,
    onItemChanged: (Int) -> Unit,
) {
    if (checkItem >= items.size) {
        throw ArrayIndexOutOfBoundsException("checkItem cannot be larger than the size of items")
    }
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clickable(enabled = enable) { expanded = true }
            .fillMaxWidth()
            .defaultMinSize(minHeight = SettingItemDefaults.defaultMinHeight)
            .let { if (modifier == Modifier) it.itemPadding() else it.then(modifier) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subTitle != null) {
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = items[checkItem],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(imageVector = Icons.Default.UnfoldMore, contentDescription = null)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                for ((index, item) in items.withIndex()) {
                    DropdownMenuItem(
                        text = { Text(text = item) },
                        onClick = {
                            expanded = false
                            if (index != checkItem) {
                                onItemChanged(index)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SwitchSettingItem(
    modifier: Modifier = Modifier,
    title: String,
    subTitle: String? = null,
    enable: Boolean = true,
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clickable(enabled = enable) {
                onCheckedChange?.invoke(!checked)
            }
            .fillMaxWidth()
            .defaultMinSize(minHeight = SettingItemDefaults.defaultMinHeight)
            .let { if (modifier == Modifier) it.itemPadding() else it.then(modifier) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = true)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subTitle != null) {
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enable,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun Modifier.itemPadding(): Modifier {
    return padding(
        SettingItemDefaults.itemHorizontalPadding,
        SettingItemDefaults.itemVerticalPadding
    )
}

data class ThrottleTimer(
    private val interval: Long = 500L,
    private var time: Long = 0L,
) {
    @Synchronized
    fun expired(): Boolean {
        val now = System.currentTimeMillis()
        if (now - time > interval) {
            time = now
            return true
        }
        return false
    }
}

private val defaultThrottleTimer by lazy { ThrottleTimer() }

fun throttle(
    timer: ThrottleTimer = defaultThrottleTimer,
    func: () -> Unit
): () -> Unit {
    return {
        if (timer.expired()) {
            func.invoke()
        }
    }
}

fun <T> throttle(
    timer: ThrottleTimer = defaultThrottleTimer,
    func: (T) -> Unit
): (T) -> Unit {
    return {
        if (timer.expired()) {
            func.invoke(it)
        }
    }
}

object SettingItemDefaults {
    val itemHorizontalPadding = 16.dp
    val itemVerticalPadding = 12.dp
    val defaultMinHeight = 50.dp
}

@Preview(heightDp = 500)
@Composable
fun SettingItemPreview() {
    Column(modifier = Modifier.background(Color.White)) {
        SettingItem(title = "Test1", subTitle = "desc1", tips = "tips1")
        var selectIndex by remember { mutableIntStateOf(0) }
        MenuSettingItem(
            title = "暗色主题",
            items = arrayOf("跟随系统", "打开", "关闭"),
            checkItem = selectIndex
        ) {
            selectIndex = it
        }


        var localChecked by remember { mutableStateOf(false) }
        SwitchSettingItem(title = "开启启动动画", checked = localChecked) {
            println("on click change: $it")
            localChecked = it
        }
    }
}