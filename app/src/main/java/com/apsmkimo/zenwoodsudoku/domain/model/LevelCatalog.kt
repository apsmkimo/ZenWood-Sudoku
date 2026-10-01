package com.apsmkimo.zenwoodsudoku.domain.model

object LevelCatalog {
    const val ASSET_NAME = "sudoku_global_levels.json"
    const val PER_DIFFICULTY = 10_000
    const val TOTAL = 30_000
    const val FREE_HINTS = 10
    const val IMPORT_FLAG = "levels_import_v1"

    fun levelNumberFor(id: Int): Int = ((id - 1) % PER_DIFFICULTY) + 1
}
