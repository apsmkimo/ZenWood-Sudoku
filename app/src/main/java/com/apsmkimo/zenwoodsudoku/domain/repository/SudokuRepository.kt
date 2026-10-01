package com.apsmkimo.zenwoodsudoku.domain.repository

import com.apsmkimo.zenwoodsudoku.domain.model.DifficultyStats
import com.apsmkimo.zenwoodsudoku.domain.model.SavedProgress
import com.apsmkimo.zenwoodsudoku.domain.model.SudokuLevel

interface SudokuRepository {
    /**
     * Imports the bundled catalog the first time the database is empty.
     * Safe to call again: a finished import returns immediately.
     * [onProgress] may be invoked from a background thread.
     */
    suspend fun ensureLevelsImported(onProgress: (processed: Int, total: Int) -> Unit)

    suspend fun loadHome(): List<DifficultyStats>

    suspend fun getLevel(id: Int): SudokuLevel?

    suspend fun saveProgress(id: Int, progress: SavedProgress)

    suspend fun markCompleted(id: Int, timeMs: Long)
}
