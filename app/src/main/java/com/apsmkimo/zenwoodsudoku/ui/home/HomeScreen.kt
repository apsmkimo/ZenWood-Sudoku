package com.apsmkimo.zenwoodsudoku.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.apsmkimo.zenwoodsudoku.R
import com.apsmkimo.zenwoodsudoku.domain.model.Difficulty
import com.apsmkimo.zenwoodsudoku.domain.model.DifficultyStats
import com.apsmkimo.zenwoodsudoku.domain.model.LevelCatalog
import com.apsmkimo.zenwoodsudoku.ui.theme.WoodBackground
import com.apsmkimo.zenwoodsudoku.ui.theme.ZenColors

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlay: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, viewModel) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }
    WoodBackground(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )
            if (state.loading && state.stats.isEmpty()) {
                CircularProgressIndicator(color = ZenColors.Paper)
            }
            state.stats.forEach { stats ->
                DifficultyCard(stats = stats, onPlay = onPlay)
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    stats: DifficultyStats,
    onPlay: (Int) -> Unit,
) {
    val title = stringResource(difficultyName(stats.difficulty))
    val nextId = stats.nextLevelId
    val nextNumber = stats.nextLevelNumber
    Surface(
        onClick = { if (nextId != null) onPlay(nextId) },
        enabled = nextId != null,
        shape = RoundedCornerShape(16.dp),
        color = ZenColors.Paper,
        contentColor = ZenColors.Ink,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.completed_count,
                        stats.completedCount,
                        LevelCatalog.PER_DIFFICULTY,
                    ),
                    color = ZenColors.WoodMid,
                )
            }
            Text(
                text = when {
                    nextNumber == null -> stringResource(R.string.all_clear)
                    stats.nextHasProgress -> stringResource(R.string.continue_level, nextNumber)
                    else -> stringResource(R.string.play_level, nextNumber)
                },
                style = MaterialTheme.typography.labelLarge,
                color = ZenColors.Pen,
            )
        }
    }
}

private fun difficultyName(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.MASTER -> R.string.difficulty_master
}
