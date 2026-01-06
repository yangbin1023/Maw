package com.magic.maw.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.collection.LruCache
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.viewinterop.AndroidView
import co.touchlab.kermit.Logger
import com.magic.maw.data.UgoiraAnimationInfo
import com.magic.maw.util.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.lang.ref.SoftReference
import java.util.Collections
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.coroutines.coroutineContext

private const val TAG = "UgoiraPlayer"

@Composable
fun UgoiraPlayer(
    modifier: Modifier = Modifier,
    zipFile: File,
    frameRate: Int = 30,
    onTab: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = throttle(func = onTab)
            )
    ) {
        var ugoiraSize by remember { mutableStateOf<Size?>(null) }
        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        private var playbackJob: Job? = null

                        override fun surfaceCreated(holder: SurfaceHolder) {
                            playbackJob = scope.launch(Dispatchers.IO) {
                                val size = getUgoiraSize(zipFile) ?: return@launch
                                ugoiraSize = size
                                try {
                                    renderLoop(holder, zipFile, frameRate, size)
                                } catch (e: ZipException) {
                                    Logger.e(TAG) { "zip file exception: ${e.message}" }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
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
            modifier = Modifier
                .ugoiraPlayerSize(ugoiraSize)
                .align(Alignment.Center)
        )
    }
}

private data class Frame(val entry: ZipEntry, val delay: Long)

@OptIn(ExperimentalSerializationApi::class)
private suspend fun renderLoop(holder: SurfaceHolder, zipFile: File, frameRate: Int, size: Size) {
    val zip = ZipFile(zipFile)

    val frames = mutableListOf<Frame>()

    val ugoiraAnimationInfo: UgoiraAnimationInfo? = zip.entries().asSequence()
        .find { it.name == "animation.json" }?.let { entry ->
            zip.getInputStream(entry).use { stream ->
                try {
                    json.decodeFromStream<UgoiraAnimationInfo>(stream)
                } catch (e: Exception) {
                    Logger.e(TAG) { "decode to ugoira animation info failed: ${e.message}" }
                    null
                }
            }
        }

    if (ugoiraAnimationInfo != null) {
        for (item in ugoiraAnimationInfo.frames) {
            val entry = zip.entries().asSequence().find { it.name == item.file } ?: continue
            frames.add(Frame(entry, item.delay.toLong()))
        }
    } else {
        val frameTime = 1000L / frameRate
        val entries = zip.entries().asSequence()
            .filter { it.name.endsWith(".jpg") || it.name.endsWith(".png") }
            .sortedBy { it.name }
            .toList()
        for (entry in entries) {
            frames.add(Frame(entry, frameTime))
        }
    }
    if (frames.isEmpty()) {
        return
    }

    val drawMatrix = Matrix()
    val reuseManager = BitmapReusabilityManager()
    val options = BitmapFactory.Options().apply {
        inMutable = true
        inPreferredConfig = Bitmap.Config.RGB_565 // 减少内存消耗
    }

    while (coroutineContext.isActive) {
        for (frame in frames) {
            val startTime = System.currentTimeMillis()

            // 1. 获取位图 (根据版本选择解码器)
            var bitmap = reuseManager.getFromCache(frame.entry.name)
            if (bitmap == null) {
                zip.getInputStream(frame.entry).buffered().use { stream ->
                    // 核心：尝试寻找一个软引用池里的旧 Bitmap 进行复用
                    // 注意：这里需要先通过 options.inJustDecodeBounds 获取宽高等信息，或者你有预知宽高
                    options.inBitmap = reuseManager.getReusableBitmap(
                        size.width.toInt(),
                        size.height.toInt(),
                        options.inPreferredConfig
                    )

                    val decoded = BitmapFactory.decodeStream(stream, null, options)
                    if (decoded != null) {
                        reuseManager.putToCache(frame.entry.name, decoded)
                        bitmap = decoded
                    }
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
            delay((frame.delay - cost).coerceAtLeast(0))
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

private fun Modifier.ugoiraPlayerSize(size: Size?): Modifier = this.then(
    if (size == null || size.width <= 0 || size.height <= 0) {
        Modifier.fillMaxSize()
    } else {
        Modifier.aspectRatio(ratio = size.width / size.height, matchHeightConstraintsFirst = true)
    }
)

class BitmapReusabilityManager(maxSize: Int = 20) {
    // 1. 强引用缓存：存储最近使用的 Bitmap，防止被回收
    private val memoryCache = object : LruCache<String, Bitmap>(maxSize) { // 缓存最近20帧
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            // 当图片从 LruCache 移除时，放入软引用池，等待复用
            softReferencePool.add(SoftReference(oldValue))
        }
    }

    // 2. 软引用池：存储不再使用但尚未被 GC 的 Bitmap 内存块
    private val softReferencePool: MutableSet<SoftReference<Bitmap>> =
        Collections.synchronizedSet(HashSet<SoftReference<Bitmap>>())

    /**
     * 获取一个可以复用的位图（用于 BitmapFactory.Options.inBitmap）
     */
    fun getReusableBitmap(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        val iterator = softReferencePool.iterator()
        while (iterator.hasNext()) {
            val bitmap = iterator.next().get()
            if (bitmap != null && bitmap.isMutable) {
                // 检查位图是否可以复用（尺寸需一致，或在 API 19+ 尺寸大于等于所需尺寸）
                if (canUseForInBitmap(bitmap, width, height, config)) {
                    iterator.remove()
                    return bitmap
                }
            } else {
                iterator.remove() // 清理已经失效的软引用
            }
        }
        return null
    }

    private fun canUseForInBitmap(
        candidate: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        config: Bitmap.Config
    ): Boolean {
        // API 19+ 允许复用大于等于目标尺寸的 Bitmap
        val byteCount = targetWidth * targetHeight * getBytesPerPixel(config)
        return candidate.allocationByteCount >= byteCount
    }

    private fun getBytesPerPixel(config: Bitmap.Config): Int {
        return when (config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.RGB_565 -> 2
            else -> 4
        }
    }

    fun getFromCache(key: String) = memoryCache.get(key)
    fun putToCache(key: String, bitmap: Bitmap) = memoryCache.put(key, bitmap)
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
