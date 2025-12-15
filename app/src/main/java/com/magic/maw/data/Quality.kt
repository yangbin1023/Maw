package com.magic.maw.data

import android.content.Context
import com.magic.maw.R
import kotlinx.serialization.Serializable

@Serializable
enum class Quality(val value: Int) {
    Preview(1 shl 0),
    Sample(1 shl 1),
    Large(1 shl 2),
    File(1 shl 3);

    fun same(type: Int): Boolean {
        return value == type
    }

    fun toResString(context: Context): String {
        val resources = context.resources
        return when (this) {
            Large -> resources.getString(R.string.quality_large)
            File -> resources.getString(R.string.quality_origin)
            else -> resources.getString(R.string.quality_sample)
        }
    }

    val resId: Int?
        get() = when (this) {
            Large -> R.string.quality_large
            File -> R.string.quality_origin
            Sample -> R.string.quality_sample
            else -> null
        }

    companion object {
        val SaveList: List<Quality> = listOf(Sample, Large, File)

        fun Int.toQuality(): Quality {
            for (item in entries) {
                if (item.value == this)
                    return item
            }
            return Sample
        }

        fun String.toQuality(): Quality {
            for (item in entries) {
                if (item.name == this)
                    return item
            }
            return Sample
        }

        /**
         * 获取支持的质量类型，从高到低
         */
        fun getQualities(): Array<Quality> {
            return arrayOf(File, Large, Sample, Preview)
        }
    }
}