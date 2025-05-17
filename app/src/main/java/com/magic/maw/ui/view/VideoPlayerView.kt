package com.magic.maw.ui.view

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.magic.maw.ui.components.ThumbSizeSlider
import com.magic.maw.ui.components.throttle
import com.magic.maw.ui.theme.PreviewTheme
import com.magic.maw.util.TimeUtils.formatTimeStr
import kotlinx.coroutines.delay

private const val TAG = "VideoPlayer"

@Composable
fun VideoPlayerView(
    modifier: Modifier = Modifier,
    state: VideoPlayerState,
    videoUri: Uri,
    onTab: () -> Unit = {},
) {
    LaunchedEffect(videoUri) {
        state.changeVideoSource(videoUri)
        while (true) {
            delay(40)
            state.currentPosition.longValue = state.exoPlayer.currentPosition
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = throttle(func = onTab)
            )
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = state.exoPlayer
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .clickable(onClick = throttle(func = state::togglePlayPause))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerControllerBar(
    modifier: Modifier = Modifier,
    state: VideoPlayerState
) {
    if (!state.isEnable.value) {
        return
    }
    var tempPosition by remember { mutableLongStateOf(-1) }
    val currentPosition = if (tempPosition >= 0) tempPosition else state.currentPosition.longValue
    val duration = state.duration.longValue
    val hasHour = duration > 3600_000
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Color.Red.copy(0.6f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = throttle(func = state::togglePlayPause)) {
            Icon(
                imageVector = if (state.isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null
            )
        }

        Text(text = currentPosition.formatTimeStr(hasHour))

        ThumbSizeSlider(
            value = currentPosition.toFloat(),
            valueRange = 0f..duration.toFloat(),
            onValueChange = { tempPosition = it.toLong().coerceIn(0, duration) },
            onValueChangeFinished = {
                state.seekTo(tempPosition)
                tempPosition = -1
            },
            thumbSize = DpSize(4.dp, 22.dp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        )

        Text(text = duration.formatTimeStr(hasHour))

        IconButton(onClick = throttle(func = state::toggleMute)) {
            Icon(
                imageVector = if (state.isMuted.value) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null
            )
        }
    }
}

@Composable
@Preview(heightDp = 360)
fun VideoPlayerControllerBarPreview() {
    val context = LocalContext.current
    val state = remember { VideoPlayerState(context = context) }

    state.isEnable.value = true

    PreviewTheme {
        Scaffold {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                VideoPlayerControllerBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    state = state
                )
            }
        }
    }
}

class VideoPlayerState(
    isPlaying: Boolean = true,
    isMuted: Boolean = true,
    context: Context
) : Player.Listener {
    val exoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ALL
        playWhenReady = true
        volume = if (isMuted) 0f else 1f
    }

    val isEnable = mutableStateOf(false)
    val isPlaying = mutableStateOf(isPlaying)
    val isMuted = mutableStateOf(isMuted)
    val currentPosition = mutableLongStateOf(0)
    val duration = mutableLongStateOf(0)

    init {
        exoPlayer.addListener(this)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun toggleMute() {
        isMuted.value = !isMuted.value
        exoPlayer.volume = if (isMuted.value) 0f else 1f
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position.coerceIn(0, duration.longValue))
        currentPosition.longValue = position
    }

    fun changeVideoSource(uri: Uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        currentPosition.longValue = 0
    }

    fun release() {
        exoPlayer.release()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) duration.longValue = exoPlayer.duration
        else if (playbackState == Player.STATE_ENDED) isPlaying.value = false
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        this.isPlaying.value = isPlaying
    }
}
