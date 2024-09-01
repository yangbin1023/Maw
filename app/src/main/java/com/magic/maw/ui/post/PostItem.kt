package com.magic.maw.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import com.magic.maw.website.DLManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PostItem(modifier: Modifier = Modifier, postData: PostData, staggered: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .shadow(
                elevation = PostDefaults.ContentPadding,
                shape = RoundedCornerShape(PostDefaults.ContentPadding)
            )
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val info = postData.originalInfo
        val ratio = getPostRatio(staggered, info.width, info.height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
        ) {
            var filePath by rememberSaveable { mutableStateOf("") }
            LaunchedEffect(key1 = coroutineScope) {
                coroutineScope.launch {
                    val tmpFile =
                        File(DLManager.getDLFullPath(postData.source, postData.id, Quality.Preview))
                    if (tmpFile.exists()) {
                        filePath = tmpFile.absolutePath
                    } else {
                        DLManager.addTask(
                            postData.source,
                            postData.id,
                            Quality.Preview,
                            postData.previewInfo.url,
                            success = {
                                filePath = tmpFile.absolutePath
                            }
                        )
                    }
                }
            }
            val model: ImageRequest? = if (filePath.isNotEmpty()) {
                ImageRequest.Builder(LocalContext.current).data(filePath).build()
            } else null
            if (model != null) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = model,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp)

        Text(
            text = "${info.width} x ${info.height}",
            fontSize = 15.sp,
            color = postData.rating.getColor(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(1.dp)
                .wrapContentSize(Alignment.Center),
        )
    }
}

private fun getPostRatio(staggered: Boolean, width: Int, height: Int): Float {
    return if (!staggered || width <= 0 || height <= 0) {
        165 / 130f
    } else if (height.toFloat() / width >= 3) {
        1 / 3f
    } else if (width.toFloat() / height >= 2) {
        2f
    } else {
        width.toFloat() / height
    }
}