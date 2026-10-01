package com.apsmkimo.zenwoodsudoku

import android.app.Application
import android.content.Context
import com.apsmkimo.zenwoodsudoku.data.local.SudokuDatabase
import com.apsmkimo.zenwoodsudoku.data.repository.SudokuRepositoryImpl
import com.apsmkimo.zenwoodsudoku.domain.repository.SudokuRepository

class ZenWoodApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: SudokuDatabase by lazy { SudokuDatabase.create(appContext) }
    val repository: SudokuRepository by lazy {
        SudokuRepositoryImpl(
            database = database,
            assets = appContext.assets,
            prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
        )
    }

    private companion object {
        const val PREFS = "zenwood_meta"
    }
}
