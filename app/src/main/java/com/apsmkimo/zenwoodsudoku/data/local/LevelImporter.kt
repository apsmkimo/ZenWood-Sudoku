package com.apsmkimo.zenwoodsudoku.data.local

import android.content.SharedPreferences
import android.content.res.AssetManager
import android.util.JsonReader
import android.util.JsonToken
import androidx.room.withTransaction
import com.apsmkimo.zenwoodsudoku.domain.model.LevelCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

/**
 * First-launch import of the 30,000 bundled puzzles.
 *
 * The catalog is already generated and shipped as
 * `assets/sudoku_global_levels.json` (~11 MB). Nothing is generated on device.
 *
 * Memory: [android.util.JsonReader] streams one object at a time. Rows are
 * inserted in batches of [BATCH] inside a single Room transaction, then the
 * batch list is cleared. Peak Java heap is one batch (about 1,000 short
 * strings) plus the SQLite page cache, not the whole file and not 30,000
 * live entities.
 *
 * Time: one transaction avoids 30,000 autocommits. Parsing and writing run
 * on [Dispatchers.IO], so the main thread only paints the loading screen.
 * A failed import rolls back; the next launch tries again.
 *
 * Idempotent: if [LevelCatalog.IMPORT_FLAG] is set and the table already
 * holds [LevelCatalog.TOTAL] rows, this returns without touching the asset.
 * A partial table is cleared and imported again.
 */
internal class LevelImporter(
    private val database: SudokuDatabase,
    private val dao: SudokuLevelDao,
    private val assets: AssetManager,
    private val prefs: SharedPreferences,
) {
    suspend fun ensureImported(onProgress: (processed: Int, total: Int) -> Unit) {
        withContext(Dispatchers.IO) {
            if (prefs.getBoolean(LevelCatalog.IMPORT_FLAG, false)) {
                val count = dao.count()
                if (count >= LevelCatalog.TOTAL) {
                    onProgress(LevelCatalog.TOTAL, LevelCatalog.TOTAL)
                    return@withContext
                }
            }
            val existing = dao.count()
            if (existing >= LevelCatalog.TOTAL) {
                prefs.edit().putBoolean(LevelCatalog.IMPORT_FLAG, true).commit()
                onProgress(LevelCatalog.TOTAL, LevelCatalog.TOTAL)
                return@withContext
            }
            if (existing > 0) dao.deleteAll()
            onProgress(0, LevelCatalog.TOTAL)
            importAsset(onProgress)
            val imported = dao.count()
            check(imported == LevelCatalog.TOTAL) {
                "Imported $imported levels, expected ${LevelCatalog.TOTAL}"
            }
            prefs.edit().putBoolean(LevelCatalog.IMPORT_FLAG, true).commit()
        }
    }

    private suspend fun importAsset(onProgress: (processed: Int, total: Int) -> Unit) {
        assets.open(LevelCatalog.ASSET_NAME).use { input ->
            JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                reader.beginArray()
                database.withTransaction {
                    val batch = ArrayList<SudokuLevelEntity>(BATCH)
                    var processed = 0
                    while (reader.hasNext()) {
                        batch += readLevel(reader)
                        processed++
                        if (batch.size >= BATCH) {
                            dao.insertAll(batch)
                            batch.clear()
                            onProgress(processed, LevelCatalog.TOTAL)
                        }
                    }
                    if (batch.isNotEmpty()) {
                        dao.insertAll(batch)
                        batch.clear()
                    }
                    reader.endArray()
                    onProgress(processed, LevelCatalog.TOTAL)
                }
            }
        }
    }

    private fun readLevel(reader: JsonReader): SudokuLevelEntity {
        var id = 0
        var difficulty = 1
        var puzzle = ""
        var solution = ""
        var completed = false
        var bestTime = 0L
        var saved: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextInt()
                "difficulty" -> difficulty = reader.nextInt()
                "puzzle" -> puzzle = reader.nextString()
                "solution" -> solution = reader.nextString()
                "is_completed" -> completed = reader.nextBoolean()
                "best_time" -> bestTime = reader.nextLong()
                "saved_progress" -> {
                    saved = if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull()
                        null
                    } else {
                        reader.nextString()
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        check(puzzle.length == 81 && solution.length == 81) {
            "Level $id has invalid board strings"
        }
        return SudokuLevelEntity(
            id = id,
            difficulty = difficulty,
            puzzle = puzzle,
            solution = solution,
            isCompleted = completed,
            bestTime = bestTime,
            savedProgress = saved,
        )
    }

    private companion object {
        const val BATCH = 1_000
    }
}
