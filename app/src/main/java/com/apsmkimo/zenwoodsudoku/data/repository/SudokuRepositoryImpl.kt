package com.apsmkimo.zenwoodsudoku.data.repository

import android.content.SharedPreferences
import android.content.res.AssetManager
import com.apsmkimo.zenwoodsudoku.data.local.LevelImporter
import com.apsmkimo.zenwoodsudoku.data.local.SudokuDatabase
import com.apsmkimo.zenwoodsudoku.data.local.SudokuLevelEntity
import com.apsmkimo.zenwoodsudoku.domain.model.Difficulty
import com.apsmkimo.zenwoodsudoku.domain.model.DifficultyStats
import com.apsmkimo.zenwoodsudoku.domain.model.LevelCatalog
import com.apsmkimo.zenwoodsudoku.domain.model.SavedProgress
import com.apsmkimo.zenwoodsudoku.domain.model.SavedProgressCodec
import com.apsmkimo.zenwoodsudoku.domain.model.SudokuLevel
import com.apsmkimo.zenwoodsudoku.domain.repository.SudokuRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SudokuRepositoryImpl(
    database: SudokuDatabase,
    assets: AssetManager,
    prefs: SharedPreferences,
) : SudokuRepository {
    private val dao = database.sudokuLevelDao()
    private val importer = LevelImporter(database, dao, assets, prefs)

    override suspend fun ensureLevelsImported(onProgress: (processed: Int, total: Int) -> Unit) {
        importer.ensureImported(onProgress)
    }

    override suspend fun loadHome(): List<DifficultyStats> = coroutineScope {
        Difficulty.entries.map { difficulty ->
            async {
                val completed = dao.completedCount(difficulty.code)
                val next = dao.nextIncomplete(difficulty.code)
                DifficultyStats(
                    difficulty = difficulty,
                    completedCount = completed,
                    nextLevelId = next?.id,
                    nextLevelNumber = next?.id?.let(LevelCatalog::levelNumberFor),
                    nextHasProgress = next?.savedProgress != null,
                )
            }
        }.awaitAll()
    }

    override suspend fun getLevel(id: Int): SudokuLevel? = dao.getById(id)?.toDomain()

    override suspend fun saveProgress(id: Int, progress: SavedProgress) {
        dao.updateProgress(id, SavedProgressCodec.encode(progress))
    }

    override suspend fun markCompleted(id: Int, timeMs: Long) {
        dao.markCompleted(id, timeMs.coerceAtLeast(0L))
    }
}

private fun SudokuLevelEntity.toDomain(): SudokuLevel = SudokuLevel(
    id = id,
    difficulty = Difficulty.fromCode(difficulty),
    levelNumber = LevelCatalog.levelNumberFor(id),
    puzzle = puzzle,
    solution = solution,
    isCompleted = isCompleted,
    bestTimeMs = bestTime,
    savedProgress = SavedProgressCodec.decode(savedProgress),
)
