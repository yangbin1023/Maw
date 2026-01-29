package com.magic.maw.ui.features.pool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.magic.maw.R
import com.magic.maw.data.model.site.PoolData
import com.magic.maw.util.TimeUtils
import org.koin.compose.koinInject

@Composable
fun PoolItem(
    modifier: Modifier = Modifier,
    poolData: PoolData,
    canClick: Boolean = true,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        enabled = canClick,
        onClick = onClick,
        shape = RoundedCornerShape(5.dp)
    ) {
        Box(modifier = Modifier.aspectRatio(1.78f)) {
            ItemImageView(modifier = Modifier.fillMaxSize(), poolData)
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
                Icon(
                    modifier = Modifier
                        .scale(0.85f)
                        .padding(end = 3.dp),
                    painter = painterResource(R.drawable.ic_album),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Text(text = poolData.count.toString())
            }
        }
        HorizontalDivider()
        Text(
            text = poolData.name.replace("_", " "),
            modifier = Modifier.padding(start = 10.dp, top = 3.dp, end = 10.dp)
        )
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(R.drawable.ic_artist), contentDescription = null)
            Text(
                text = poolData.uploader ?: stringResource(R.string.unknown),
                modifier = Modifier.padding(3.dp),
                style = MaterialTheme.typography.bodySmall
            )
            val timeStr = poolData.updateTime?.let { TimeUtils.getTimeStr(TimeUtils.FORMAT_1, it) }
            Icon(painter = painterResource(R.drawable.ic_time), contentDescription = null)
            Text(
                text = timeStr ?: stringResource(R.string.unknown),
                modifier = Modifier.padding(3.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ItemImageView(modifier: Modifier = Modifier, poolData: PoolData) {
    val imageLoader = koinInject<ImageLoader>()
    Box(modifier = modifier) {
        var state by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = poolData,
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
    }
}