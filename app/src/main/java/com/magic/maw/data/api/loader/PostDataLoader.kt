package com.magic.maw.data.api.loader

import co.touchlab.kermit.Logger
import com.magic.maw.data.api.parser.BaseParser
import com.magic.maw.data.local.store.SettingsStore
import com.magic.maw.data.model.PopularOption
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.model.RequestOption
import com.magic.maw.data.model.constant.WebsiteOption
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext

private const val TAG = "PostDataLoader"

typealias PostDataUiState = ListUiState<PostData>

typealias PostDataLoader = DataLoader<PostData>

interface PopularDataLoader : PostDataLoader {
    fun setPopularDate(date: LocalDate): Any = {}
}
