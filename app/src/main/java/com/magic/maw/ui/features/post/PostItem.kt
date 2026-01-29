package com.magic.maw.ui.features.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.magic.maw.R
import com.magic.maw.data.interceptor.PostThumbnailItem
import com.magic.maw.data.model.site.PostData
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

@Composable
fun PostItem(
    modifier: Modifier = Modifier,
    canClick: Boolean = true,
    onClick: () -> Unit,
    postData: PostData,
    staggered: Boolean
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = PostDefaults.ContentPadding,
                shape = RoundedCornerShape(PostDefaults.ContentPadding)
            )
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = canClick, onClick = onClick),
    ) {
        val info = postData.originalInfo
        val ratio = getPostRatio(staggered, info.width, info.height)

        PostItemImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio),
            postData = postData
        )

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

@Composable
fun PostItemImage(
    modifier: Modifier = Modifier,
    postData: PostData,
) {
    val model = postData.previewInfo.url.ifBlank {
        PostThumbnailItem(postData.website, postData.id)
    }
    val imageLoader = koinInject<ImageLoader>()
    Box(modifier = modifier) {
        var state by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
        Logger.d("PostItem") { "model: $model" }
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = model,
            imageLoader = imageLoader,
            alignment = Alignment.Center,
            onState = { state = it },
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )

        if (state is AsyncImagePainter.State.Loading) {
            Icon(
                modifier = modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.6f),
                imageVector = ImageVector.vectorResource(R.drawable.ic_image),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(0.08f)
            )
        } else if (state is AsyncImagePainter.State.Error) {
            Icon(
                modifier = modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.6f),
                imageVector = ImageVector.vectorResource(R.drawable.ic_image_error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(0.08f)
            )
        }

        postData.duration?.toInt()?.seconds?.let { duration ->
            val text = duration.toComponents { hours, minutes, seconds, _ ->
                if (hours > 0) {
                    "%02d:%02d:%02d".format(hours, minutes, seconds)
                } else {
                    "%02d:%02d".format(minutes, seconds)
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .background(
                        MaterialTheme.colorScheme.onPrimary.copy(0.6f),
                        shape = RoundedCornerShape(5.dp)
                    )
                    .padding(vertical = 2.dp, horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = text)
            }
        }
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
