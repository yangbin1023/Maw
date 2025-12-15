package com.magic.maw.test.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Test") },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(5.dp),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .scrollable(state = rememberLazyListState(), orientation = Orientation.Vertical),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                navController.navigate(route = TestSearchRoute())
            }) {
                Text(text = "测试搜索")
            }
            Button(onClick = {
                navController.navigate(route = TestSearchRoute(context = "123"))
            }) {
                Text(text = "测试搜索1234")
            }
            Button(onClick = {
                navController.navigate(route = "settings")
            }) {
                Text(text = "测试设置")
            }
            Button(onClick = {
                navController.navigate(route = "settings2")
            }) {
                Text(text = "测试设置2")
            }
        }
    }
}
