package com.magic.maw.data

import android.content.Context
import com.magic.maw.R

enum class Quality(val value: Int) {
    Preview(1),
    Sample(2),
    Large(3),
    File(4);

    fun same(type: Int): Boolean {
        return value == type
    }

    fun toResString(context: Context): String {
        val resources = context.resources
        return when (this) {
            Large -> resources.getString(R.string.quality_origin)
            File -> resources.getString(R.string.quality_large)
            else -> resources.getString(R.string.quality_sample)
        }
    }

    companion object {
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
    }
}