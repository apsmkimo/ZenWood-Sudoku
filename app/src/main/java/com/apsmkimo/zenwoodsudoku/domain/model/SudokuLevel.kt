package com.apsmkimo.zenwoodsudoku.domain.model

data class SudokuLevel(
    val id: Int,
    val difficulty: Difficulty,
    val levelNumber: Int,
    val puzzle: String,
    val solution: String,
    val isCompleted: Boolean,
    val bestTimeMs: Long,
    val savedProgress: SavedProgress?,
)

data class DifficultyStats(
    val difficulty: Difficulty,
    val completedCount: Int,
    val nextLevelId: Int?,
    val nextLevelNumber: Int?,
    val nextHasProgress: Boolean,
)
