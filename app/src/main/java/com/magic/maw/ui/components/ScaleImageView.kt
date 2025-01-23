package com.magic.maw.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.magic.maw.ui.components.scale.ScaleCanvas
import com.magic.maw.ui.components.scale.ScaleDecoder
import com.magic.maw.ui.components.scale.ScaleState
import com.magic.maw.ui.components.scale.ScaleView
import com.magic.maw.ui.components.scale.createScaleDecoder
import com.magic.maw.ui.components.scale.getViewPort
import com.magic.maw.ui.components.scale.rememberScaleState
import com.magic.maw.website.LoadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File

@Composable
fun ScaleImageView(
    model: Any? = null,
    boundClip: Boolean = true,
    scaleState: ScaleState = rememberScaleState(),
    onTap: (Offset) -> Unit = {},
    onDoubleTap: (Offset) -> Unit = {}
) {
    ScaleView(
        boundClip = boundClip,
        scaleState = scaleState,
        onTap = onTap,
        onDoubleTap = onDoubleTap
    ) {
        when (model) {
            is ScaleDecoder -> {
                ScaleCanvas(
                    scaleDecoder = model,
                    viewPort = scaleState.getViewPort(),
                )
                DisposableEffect(model) {
                    onDispose { model.release() }
                }
            }

            is Painter -> Image(
                painter = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )

            is ImageBitmap -> Image(
                bitmap = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )

            is ImageVector -> Image(
                imageVector = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )

            else -> AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

fun loadModel(
    context: Context,
    file: File,
    defaultSize: Size
): StateFlow<LoadStatus<Pair<Any, Size>>> {
    val defaultStatus = LoadStatus.Loading(1f)
    val flow = MutableStateFlow<LoadStatus<Pair<Any, Size>>>(defaultStatus)
    try {
        createScaleDecoder(file)?.let { decoder ->
            flow.update {
                LoadStatus.Success(Pair(decoder, decoder.intrinsicSize))
            }
            return flow
        }
    } catch (e: Throwable) {
        println(e)
    }
    val imageRequest = ImageRequest.Builder(context)
        .data(file)
        .size(coil.size.Size.ORIGINAL)
        .target(
            onSuccess = {
                val painter = BitmapPainter(it.toBitmap().asImageBitmap())
                flow.update {
                    LoadStatus.Success(Pair(painter, painter.intrinsicSize))
                }
            },
            onError = {
                flow.update {
                    LoadStatus.Success(Pair(file, defaultSize))
                }
            }
        )
        .build()
    context.imageLoader.enqueue(imageRequest)
    return flow
}
