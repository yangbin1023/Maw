package com.magic.maw.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import co.touchlab.kermit.Logger
import com.magic.maw.data.UgoiraAnimationInfo
import com.magic.maw.util.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.coroutines.coroutineContext
import kotlin.math.min

private const val TAG = "UgoiraPlayer"

@Composable
fun UgoiraPlayer(
    modifier: Modifier = Modifier,
    zipFile: File,
    frameRate: Int = 30
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val boxSize = with(density) {
            Size(maxWidth.toPx(), maxHeight.toPx())
        }
        var itemModifier by remember { mutableStateOf(Modifier.fillMaxSize()) }
        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        private var playbackJob: Job? = null

                        override fun surfaceCreated(holder: SurfaceHolder) {
                            playbackJob = CoroutineScope(Dispatchers.IO).launch {

                                val ugoiraSize = getUgoiraSize(zipFile) ?: return@launch
                                val scale = min(
                                    boxSize.width / ugoiraSize.width,
                                    boxSize.height / ugoiraSize.height
                                )
                                val targetSize =
                                    Size(ugoiraSize.width * scale, ugoiraSize.height * scale)
                                Log.d(
                                    TAG,
                                    "boxSize: $boxSize, ugoiraSize: $ugoiraSize, targetSize: $targetSize, scale: $scale"
                                )

                                val widthDp = with(density) { targetSize.width.toDp() }
                                val heightDp = with(density) { targetSize.height.toDp() }
                                itemModifier = Modifier
                                    .width(widthDp)
                                    .height(heightDp)
                                    .align(Alignment.Center)

                                renderLoop(holder, zipFile, frameRate)
                            }
                        }

                        override fun surfaceDestroyed(h: SurfaceHolder) {
                            playbackJob?.cancel()
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            f: Int,
                            w: Int,
                            h: Int
                        ) {
                        }
                    })
                }
            },
            modifier = itemModifier
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
private suspend fun renderLoop(holder: SurfaceHolder, zipFile: File, frameRate: Int) {
    val zip = ZipFile(zipFile)
    var entries = zip.entries().asSequence()
        .filter { it.name.endsWith(".jpg") || it.name.endsWith(".png") }
        .sortedBy { it.name }
        .toList()

    val delayMap = mutableMapOf<String, Long>()
    zip.entries().asSequence().find { it.name == "animation.json" }?.let { entry ->
        zip.getInputStream(entry).use { stream ->
            try {
                json.decodeFromStream<UgoiraAnimationInfo>(stream).apply {
                    val list = mutableListOf<ZipEntry>()
                    for (item in frames) {
                        val entry = zip.entries().asSequence()
                            .find { it.name == item.file } ?: continue
                        delayMap[item.file] = item.delay.toLong()
                        list.add(entry)
                    }
                    Logger.d(TAG) { "delayMap: $delayMap" }
                    entries = list
                }
            } catch (e: Exception) {
                Logger.e(TAG) { "decode to ugoira animation info failed: ${e.message}" }
            }
        }
    }

    val frameTime = 1000L / frameRate
    val drawMatrix = Matrix()

    // API 21 兼容性 BitmapFactory 选项
    val options = BitmapFactory.Options().apply {
        inMutable = true
        inPreferredConfig = Bitmap.Config.RGB_565 // 减少内存消耗
    }
    var reusableBitmap: Bitmap? = null

    while (coroutineContext.isActive) {
        for (entry in entries) {
            val startTime = System.currentTimeMillis()

            // 1. 获取位图 (根据版本选择解码器)
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API 28+ 使用 ImageDecoder (硬件辅助)
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_HARDWARE
                }
            } else {
                // API 21+ 使用 BitmapFactory (内存复用)
                zip.getInputStream(entry).buffered().use { stream ->
                    options.inBitmap = reusableBitmap
                    val decoded = BitmapFactory.decodeStream(stream, null, options)
                    reusableBitmap = decoded
                    decoded
                }
            }

            // 2. 计算缩放 (适应 SurfaceView 大小)
            bitmap?.let { bmp ->
                val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }

                if (canvas != null) {
                    try {
                        // 清屏 (防止上一帧残留)
                        canvas.drawColor(Color.BLACK)

                        // 计算缩放比例 (Center Inside 效果)
                        drawMatrix.reset()
                        val scale =
                            calculateScale(bmp.width, bmp.height, canvas.width, canvas.height)
                        val dx = (canvas.width - bmp.width * scale) / 2f
                        val dy = (canvas.height - bmp.height * scale) / 2f
                        drawMatrix.postScale(scale, scale)
                        drawMatrix.postTranslate(dx, dy)

                        canvas.drawBitmap(bmp, drawMatrix, null)
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
            }

            val cost = System.currentTimeMillis() - startTime
            val frameTime = delayMap[entry.name] ?: frameTime
            delay((frameTime - cost).coerceAtLeast(0))
        }
    }
    zip.close()
}

// 高效计算缩放比例
private fun calculateScale(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Float {
    val widthScale = dstW.toFloat() / srcW
    val heightScale = dstH.toFloat() / srcH
    return minOf(widthScale, heightScale)
}

/**
 * 高效获取 ZIP 文件内图片序列的分辨率
 * * @param zipFile 目标文件
 * @return 图像的 Size，如果文件非法或不含图片则返回 null
 */
@OptIn(ExperimentalSerializationApi::class)
fun getUgoiraSize(zipFile: File): Size? {
    if (!zipFile.exists() || zipFile.length() == 0L) return null
    try {
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().find { it.name == "animation.json" }?.let { entry ->
                zip.getInputStream(entry).use { stream ->
                    try {
                        json.decodeFromStream<UgoiraAnimationInfo>(stream).apply {
                            if (width > 0 && height > 0) {
                                return Size(width.toFloat(), height.toFloat())
                            }
                        }
                    } catch (e: Exception) {
                        Logger.e(TAG) { "get ugoira animation size info failed: ${e.message}" }
                    }
                }
            }

            val entries = zip.entries().asSequence()
                .filter { it.name.endsWith(".jpg") || it.name.endsWith(".png") }
                .sortedBy { it.name }
                .toList()

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            for (entry in entries) {
                zip.getInputStream(entry).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                    if (options.outWidth != -1 && options.outHeight != -1) {
                        return Size(options.outWidth.toFloat(), options.outHeight.toFloat())
                    }
                }
            }
        }
    } catch (e: ZipException) {
        Logger.e(TAG) { "zip file exception: ${e.message}. delete the file: $zipFile" }
        zipFile.delete()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
