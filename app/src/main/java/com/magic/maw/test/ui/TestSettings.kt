package com.magic.maw.test.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.magic.maw.ui.components.SettingItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSettings(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isAtTop by remember {
        // 使用derivedStateOf可防止滚动每一个像素都会重组一次的问题
        derivedStateOf {
            scrollState.value == 0
        }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth().run {
                    if (isAtTop) {
                        this
                    } else {
                        this.shadow(elevation = 3.dp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings") }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .align(alignment = Alignment.TopCenter)
                    .width(600.dp)
                    .verticalScroll(scrollState)
                    .background(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
            ) {
                SettingGroupTitle(title = "Settings group 1")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                SettingGroupTitle(title = "Settings group 2")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                SettingGroupTitle(title = "Settings group 3")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                SettingItem(title = "测试")
                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = { scope.launch { snackbarHostState.showSnackbar("保存成功！") } }
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun SettingGroupTitle(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        text = title,
        modifier = modifier.padding(horizontal = 15.dp, vertical = 3.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}