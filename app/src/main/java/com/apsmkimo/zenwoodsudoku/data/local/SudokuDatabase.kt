package com.apsmkimo.zenwoodsudoku.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SudokuLevelEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SudokuDatabase : RoomDatabase() {
    abstract fun sudokuLevelDao(): SudokuLevelDao

    companion object {
        fun create(context: Context): SudokuDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                SudokuDatabase::class.java,
                "zenwood_sudoku.db",
            )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
