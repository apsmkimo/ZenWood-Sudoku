package com.apsmkimo.zenwoodsudoku.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SudokuLevelDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(levels: List<SudokuLevelEntity>)

    @Query("SELECT COUNT(*) FROM sudoku_levels")
    suspend fun count(): Int

    @Query("DELETE FROM sudoku_levels")
    suspend fun deleteAll()

    @Query("SELECT * FROM sudoku_levels WHERE id = :id")
    suspend fun getById(id: Int): SudokuLevelEntity?

    @Query(
        """
        SELECT * FROM sudoku_levels
        WHERE difficulty = :difficulty AND is_completed = 0
        ORDER BY id ASC
        LIMIT 1
        """,
    )
    suspend fun nextIncomplete(difficulty: Int): SudokuLevelEntity?

    @Query(
        """
        SELECT COUNT(*) FROM sudoku_levels
        WHERE difficulty = :difficulty AND is_completed = 1
        """,
    )
    suspend fun completedCount(difficulty: Int): Int

    @Query("UPDATE sudoku_levels SET saved_progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Int, progress: String?)

    @Query(
        """
        UPDATE sudoku_levels
        SET is_completed = 1,
            best_time = CASE
                WHEN best_time = 0 OR :timeMs < best_time THEN :timeMs
                ELSE best_time
            END,
            saved_progress = NULL
        WHERE id = :id
        """,
    )
    suspend fun markCompleted(id: Int, timeMs: Long)
}
