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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.magic.maw.ui.components.ConfigChangeChecker
import com.magic.maw.util.TimeUtils
import com.magic.maw.util.configFlow
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.LoadType
import com.magic.maw.website.RequestOption
import com.magic.maw.website.loadDLFile
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PoolItem(
    modifier: Modifier = Modifier,
    poolData: PoolData,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
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
    val scope = rememberCoroutineScope()
    val type = rememberSaveable { mutableStateOf(LoadType.Waiting) }
    val noMore = rememberSaveable { mutableStateOf(false) }
    val model = remember { mutableStateOf<Any?>(null) }
    val context = LocalContext.current
    if (noMore.value) {
        ConfigChangeChecker {
            noMore.value = false
        }
    }
    LaunchedEffect(poolData, noMore.value) {
        if (poolData.posts.isEmpty() && !noMore.value) {
            val list = withContext(Dispatchers.IO) {
                try {
                    val parser = BaseParser.get(poolData.source)
                    val option = RequestOption(
                        page = parser.firstPageIndex,
                        poolId = poolData.id,
                        ratings = configFlow.value.getWebsiteConfig(poolData.source).rating
                    )
                    parser.requestPostData(option)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            poolData.posts = list
            noMore.value = list.isEmpty()
        }
        if (noMore.value && poolData.posts.isEmpty()) {
            type.value = LoadType.Error
            return@LaunchedEffect
        }
        val postData = try {
            poolData.posts.first()
        } catch (_:Throwable) {
            type.value = LoadType.Error
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            loadDLFile(postData, Quality.Sample, scope)
        }.collect {
            if (it is LoadStatus.Success) {
                model.value = ImageRequest.Builder(context).data(it.result).build()
                type.value = LoadType.Success
            } else if (it is LoadStatus.Error) {
                type.value = LoadType.Error
            } else {
                type.value = LoadType.Loading
            }
        }
    }

    if (type.value == LoadType.Success) {
        AsyncImage(
            modifier = modifier,
            model = model.value,
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
    }
}