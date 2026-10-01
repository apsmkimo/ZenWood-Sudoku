package com.apsmkimo.zenwoodsudoku.ui.game

import androidx.compose.runtime.Immutable

@Immutable
data class CellUi(
    val value: Int = 0,
    val notesMask: Int = 0,
    val isGiven: Boolean = false,
    val hasError: Boolean = false,
)

data class HeaderUi(
    val difficultyCode: Int = 1,
    val levelNumber: Int = 1,
    val elapsedMs: Long = 0L,
)

sealed interface GameEvent {
    data object RewardedAdUnavailable : GameEvent
}

fun formatElapsed(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
