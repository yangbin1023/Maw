package com.magic.maw.ui.pool

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.magic.maw.R
import com.magic.maw.data.PoolData
import com.magic.maw.data.Quality
import com.magic.maw.util.TimeUtils
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.loadDLFile
import java.io.File

@Composable
fun PoolItem(
    modifier: Modifier = Modifier,
    poolData: PoolData,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(5.dp)
    ) {
        Box(modifier = Modifier.aspectRatio(1.78f)) {
            if (poolData.posts.isNotEmpty()) {
                val postData = poolData.posts[0]
                val status by loadDLFile(postData, Quality.Sample, scope).collectAsState()
                if (status is LoadStatus.Success<File>) {
                    val file = (status as? LoadStatus.Success<File>)?.result
                    val model = ImageRequest.Builder(LocalContext.current).data(file).build()
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = model,
                        alignment = Alignment.Center,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                    )
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
