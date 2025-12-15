package com.magic.maw.test.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data class TestSearchRoute(
    val context: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSearchScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    context: String? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth().shadow(5.dp),
                title = { Text(text = "测试搜索") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            Text("搜索内容：$context")
        }
    }
}