package com.magic.maw.data.api.loader

import com.magic.maw.data.model.site.PostData
import java.time.LocalDate

private const val TAG = "PostDataLoader"

typealias PostDataUiState = ListUiState<PostData>

typealias PostDataLoader = DataLoader<PostData>

interface PopularDataLoader : PostDataLoader {
    fun setPopularDate(date: LocalDate): Any = {}
}
