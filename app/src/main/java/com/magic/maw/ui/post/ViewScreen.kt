package com.magic.maw.ui.post

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import com.magic.maw.website.loadDLFile
import java.io.File

@Composable
fun ViewScreen(postViewModel: PostViewModel, isExpandedScreen: Boolean) {
    val pagerState = rememberPagerState { postViewModel.dataList.size }
    val onExit: () -> Unit = {
        postViewModel.viewIndex = -1
    }
    LaunchedEffect(Unit) {
        val index = postViewModel.viewIndex
        pagerState.scrollToPage(index)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        ViewContent(
            postViewModel = postViewModel,
            pagerState = pagerState,
            onExit = onExit
        )
        ViewTopBar(
            postViewModel = postViewModel,
            pagerState = pagerState,
            onExit = onExit
        )

        BackHandler(enabled = postViewModel.viewIndex >= 0) { onExit.invoke() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.ViewTopBar(
    postViewModel: PostViewModel,
    pagerState: PagerState,
    onExit: () -> Unit
) {
    CenterAlignedTopAppBar(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .shadow(5.dp),
        title = {
            val index = pagerState.currentPage
            if (index >= 0 && index < postViewModel.dataList.size) {
                val data = postViewModel.dataList[index]
                Text(text = data.id.toString())
            }
        },
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "",
                )
            }
        }
    )
}

@Composable
private fun BoxScope.ViewContent(
    postViewModel: PostViewModel,
    pagerState: PagerState,
    onExit: () -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center),
    ) { index ->
        if (index >= postViewModel.dataList.size || index < 0) {
            onExit.invoke()
            return@HorizontalPager
        }
        val data = postViewModel.dataList[index]
        val info = data.sampleInfo ?: data.largeInfo ?: data.originalInfo
        val quality = when (info) {
            data.sampleInfo -> Quality.Sample
            data.largeInfo -> Quality.Large
            else -> Quality.File
        }
        ViewScreenItem(data, info, quality)
    }
}

@Composable
private fun ViewScreenItem(data: PostData, info: PostData.Info, quality: Quality) {
    val file = remember { mutableStateOf<File?>(null) }
    LaunchedEffect(Unit) {
        file.value = loadDLFile(data, info, quality)
        println("ViewScreen load file: $file")
    }
    file.value?.let {
        println("ViewScreen show AsyncImage, id: ${data.id}")
        val model = ImageRequest.Builder(LocalContext.current).data(it).build()
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = model,
            alignment = Alignment.Center,
            contentScale = ContentScale.Inside,
            contentDescription = null,
        )
    }
}
