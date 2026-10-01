package com.apsmkimo.zenwoodsudoku.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sudoku_levels",
    indices = [
        Index(value = ["difficulty", "is_completed", "id"]),
    ],
)
data class SudokuLevelEntity(
    @PrimaryKey val id: Int,
    val difficulty: Int,
    val puzzle: String,
    val solution: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "best_time") val bestTime: Long,
    @ColumnInfo(name = "saved_progress") val savedProgress: String?,
)
