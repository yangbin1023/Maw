package com.magic.maw.data

import androidx.compose.ui.graphics.Color
import com.magic.maw.ui.theme.rating_explicit
import com.magic.maw.ui.theme.rating_general
import com.magic.maw.ui.theme.rating_none
import com.magic.maw.ui.theme.rating_questionable
import com.magic.maw.ui.theme.rating_safe
import com.magic.maw.ui.theme.rating_sensitive
import kotlinx.serialization.Serializable

@Serializable
enum class Rating(val value: Int) {
    None(0),
    Safe(1 shl 0),
    General(1 shl 1),
    Sensitive(1 shl 2),
    Questionable(1 shl 3),
    Explicit(1 shl 4);

    fun getColor(): Color {
        return when (this) {
            None -> rating_none
            Safe -> rating_safe
            General -> rating_general
            Sensitive -> rating_sensitive
            Questionable -> rating_questionable
            Explicit -> rating_explicit
        }
    }

    override fun toString(): String {
        return name
    }

    companion object {
        fun Int.toRating(): Rating {
            for (item in entries) {
                if (item.value == this)
                    return item
            }
            return None
        }

        fun Int.getRatings(): Array<Rating> {
            val ratings = mutableListOf<Rating>()
            for (item in entries) {
                if (item != None && hasRating(item))
                    ratings.add(item)
            }
            return ratings.toTypedArray()
        }

        fun Int.addRating(rating: Rating): Int {
            return this or rating.value
        }

        fun Int.hasRating(rating: Rating): Boolean {
            return (this and rating.value) == rating.value
        }
    }
}