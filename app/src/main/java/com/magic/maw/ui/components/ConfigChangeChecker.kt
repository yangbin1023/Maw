package com.magic.maw.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.magic.maw.util.configFlow

@Composable
fun ConfigChangeChecker(onForceRefresh: () -> Unit) {
    val config by configFlow.collectAsStateWithLifecycle()
    var source by rememberSaveable { mutableStateOf(config.source) }
    var rating by rememberSaveable { mutableIntStateOf(config.websiteConfig.rating) }
    if (source != config.source) {
        source = config.source
        onForceRefresh.invoke()
    } else if (rating != config.websiteConfig.rating) {
        rating = config.websiteConfig.rating
        onForceRefresh.invoke()
    }
}
