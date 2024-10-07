package com.magic.maw.ui.post

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.magic.maw.R
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import com.magic.maw.ui.components.CountSingleton
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.loadDLFile
import java.io.File

@Composable
fun PostItem(modifier: Modifier = Modifier, postData: PostData, staggered: Boolean) {
    var localData by remember { mutableStateOf(postData) }
    if (localData != postData) {
        localData = postData
    }
    Column(
        modifier = modifier
            .shadow(
                elevation = PostDefaults.ContentPadding,
                shape = RoundedCornerShape(PostDefaults.ContentPadding)
            )
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val info = localData.originalInfo
        val ratio = getPostRatio(staggered, info.width, info.height)
        val status by loadDLFile(localData, Quality.Preview).collectAsState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
        ) {
            if (status is LoadStatus.Success<File>) {
                val result = (status as LoadStatus.Success<File>).result
                val model = ImageRequest.Builder(LocalContext.current).data(result).build()
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = model,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
            } else {
                WaitingView()
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

private val waitAnimate by lazy {
    CountSingleton(Color.Transparent) {
        val infiniteTransition = rememberInfiniteTransition(label = "postItem")
        val targetColor = MaterialTheme.colorScheme.onSurface.copy(0.15f)
        infiniteTransition.animateColor(
            initialValue = targetColor,
            targetValue = targetColor.copy(0.05f),
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "postItemColor"
        )
    }
}

@Composable
private fun BoxScope.WaitingView(modifier: Modifier = Modifier) {
    waitAnimate.OnEffect()
    val tintColor by waitAnimate.value
    Icon(
        modifier = modifier
            .align(Alignment.Center)
            .fillMaxSize(0.6f),
        imageVector = ImageVector.vectorResource(R.drawable.ic_image),
        contentDescription = null,
        tint = tintColor
    )
}
