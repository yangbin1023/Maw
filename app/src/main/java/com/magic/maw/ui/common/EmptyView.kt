package com.magic.maw.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.magic.maw.R
import com.magic.maw.data.api.loader.LoadState

@Composable
fun EmptyView(
    modifier: Modifier = Modifier,
    loadState: LoadState,
    onRefresh: () -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val text = if (loadState.isLoading) {
            stringResource(R.string.loading)
        } else if (loadState is LoadState.Error) {
            stringResource(R.string.loading_failed)
        } else {
            stringResource(R.string.no_data)
        }
        Text(
            text = text,
            modifier = Modifier
                .padding(15.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRefresh
                )
        )
    }
}