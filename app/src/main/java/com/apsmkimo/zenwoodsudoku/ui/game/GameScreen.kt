package com.apsmkimo.zenwoodsudoku.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apsmkimo.zenwoodsudoku.R
import com.apsmkimo.zenwoodsudoku.domain.model.Difficulty
import com.apsmkimo.zenwoodsudoku.ui.theme.WoodBackground
import com.apsmkimo.zenwoodsudoku.ui.theme.ZenColors

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
) {
    val missing by viewModel.missing.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onHostResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onHostPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            viewModel.onHostResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onHostPause()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val adMessage = stringResource(R.string.rewarded_ad_unavailable)
    LaunchedEffect(viewModel, adMessage) {
        viewModel.events.collect { event ->
            if (event is GameEvent.RewardedAdUnavailable) {
                snackbarHostState.showSnackbar(adMessage)
            }
        }
    }

    WoodBackground(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
        if (missing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.level_missing),
                    color = ZenColors.OnWood,
                    fontFamily = FontFamily.Serif,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
            ) {
                GameTopBar(viewModel = viewModel, onBack = onBack)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val side = minOf(maxWidth, maxHeight)
                    SudokuBoard(
                        cellFlows = viewModel.cellFlows,
                        selectedIndex = viewModel.selectedIndex,
                        onCellClick = viewModel::onCellTapped,
                        modifier = Modifier.size(side),
                    )
                }
                HintButton(
                    viewModel = viewModel,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 8.dp),
                )
                NumberPad(
                    viewModel = viewModel,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                AdBanner()
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
        )
        CompletionDialog(viewModel = viewModel, onHome = onBack)
    }
}

@Composable
private fun GameTopBar(
    viewModel: GameViewModel,
    onBack: () -> Unit,
) {
    val header by viewModel.header.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val difficulty = stringResource(difficultyName(header.difficultyCode))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = ZenColors.OnWood,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(text = difficulty, color = ZenColors.OnWood, fontFamily = FontFamily.Serif)
            Text(
                text = stringResource(R.string.level_number, header.levelNumber),
                color = ZenColors.PaperDeep,
                fontSize = 12.sp,
            )
        }
        Text(
            text = formatElapsed(header.elapsedMs),
            color = ZenColors.OnWood,
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
        )
        IconButton(onClick = viewModel::undo, enabled = canUndo) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.undo),
                tint = if (canUndo) ZenColors.OnWood else ZenColors.WoodLight,
            )
        }
    }
}

@Composable
private fun HintButton(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    val hints by viewModel.hintsRemaining.collectAsStateWithLifecycle()
    val description = if (hints > 0) {
        stringResource(R.string.hint_with_count, hints)
    } else {
        stringResource(R.string.hint_watch_ad)
    }
    Surface(
        onClick = viewModel::onHintPressed,
        shape = CircleShape,
        color = ZenColors.Paper,
        modifier = modifier.size(52.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = description,
                tint = ZenColors.Ink,
            )
            if (hints > 0) {
                Text(
                    text = hints.toString(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    color = ZenColors.Pen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = ZenColors.Pen,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun AdBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.ad_banner_placeholder),
            color = ZenColors.OnWood.copy(alpha = 0.75f),
            fontFamily = FontFamily.Serif,
        )
    }
}

@Composable
private fun CompletionDialog(
    viewModel: GameViewModel,
    onHome: () -> Unit,
) {
    val completed by viewModel.completed.collectAsStateWithLifecycle()
    if (!completed) return
    val header by viewModel.header.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onHome,
        confirmButton = {
            TextButton(onClick = onHome) {
                Text(stringResource(R.string.home))
            }
        },
        title = { Text(stringResource(R.string.level_complete)) },
        text = { Text(formatElapsed(header.elapsedMs)) },
    )
}

private fun difficultyName(code: Int): Int = when (Difficulty.fromCode(code)) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.MASTER -> R.string.difficulty_master
}
