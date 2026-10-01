package com.apsmkimo.zenwoodsudoku.ui.bootstrap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apsmkimo.zenwoodsudoku.R
import com.apsmkimo.zenwoodsudoku.ui.theme.WoodBackground
import com.apsmkimo.zenwoodsudoku.ui.theme.ZenColors

@Composable
fun ImportScreen(
    state: BootstrapUiState,
    onRetry: () -> Unit,
) {
    WoodBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(
                    if (state.phase == BootstrapUiState.Phase.Error) {
                        R.string.import_failed
                    } else {
                        R.string.loading_levels
                    },
                ),
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                color = ZenColors.OnWood,
            )
            if (state.phase == BootstrapUiState.Phase.Error) {
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.retry))
                }
            } else {
                val fraction = if (state.total == 0) 0f else state.processed.toFloat() / state.total
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = ZenColors.Paper,
                    trackColor = ZenColors.WoodDark,
                )
            }
        }
    }
}
