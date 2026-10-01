package com.apsmkimo.zenwoodsudoku.ui.game

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apsmkimo.zenwoodsudoku.domain.game.HintOutcome
import com.apsmkimo.zenwoodsudoku.domain.game.SudokuGame
import com.apsmkimo.zenwoodsudoku.domain.repository.SudokuRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(
    private val repository: SudokuRepository,
    private val levelId: Int,
) : ViewModel() {
    private val game = SudokuGame()
    private val cellStates = Array(81) { MutableStateFlow(CellUi()) }

    /**
     * One flow per cell. Composables collect a single index, so editing one
     * cell does not publish a new board snapshot to the other eighty.
     */
    val cellFlows: List<StateFlow<CellUi>> = cellStates.asList()

    private val _selectedIndex = MutableStateFlow(-1)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    private val _header = MutableStateFlow(HeaderUi())
    val header: StateFlow<HeaderUi> = _header.asStateFlow()

    private val _pencilMode = MutableStateFlow(false)
    val pencilMode: StateFlow<Boolean> = _pencilMode.asStateFlow()

    private val _hintsRemaining = MutableStateFlow(0)
    val hintsRemaining: StateFlow<Int> = _hintsRemaining.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    private val _missing = MutableStateFlow(false)
    val missing: StateFlow<Boolean> = _missing.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private var loaded = false
    private var saveJob: Job? = null
    private var ticker: Job? = null
    private var lastMark = 0L

    init {
        viewModelScope.launch {
            val level = repository.getLevel(levelId)
            if (level == null) {
                _missing.value = true
                return@launch
            }
            game.load(level.puzzle, level.solution, level.savedProgress)
            for (index in 0 until 81) pushCell(index)
            _selectedIndex.value = game.selectedIndex
            _pencilMode.value = game.pencilMode
            _hintsRemaining.value = game.hintsRemaining
            _canUndo.value = game.canUndo()
            _header.value = HeaderUi(
                difficultyCode = level.difficulty.code,
                levelNumber = level.levelNumber,
                elapsedMs = game.elapsedMs,
            )
            val solved = game.isCompleted || level.isCompleted
            _completed.value = solved
            loaded = true
            if (game.isCompleted && !level.isCompleted) {
                repository.markCompleted(levelId, game.elapsedMs)
            }
            if (!solved) onHostResume()
        }
    }

    fun onCellTapped(index: Int) {
        if (!loaded || game.isCompleted) return
        game.select(index)
        _selectedIndex.value = game.selectedIndex
        scheduleSave()
    }

    fun onDigit(digit: Int) {
        if (!loaded) return
        publish(game.inputDigit(digit))
    }

    fun onClear() {
        if (!loaded) return
        publish(game.clear())
    }

    fun undo() {
        if (!loaded || game.isCompleted) return
        publish(game.undo())
    }

    fun togglePencil() {
        if (!loaded || game.isCompleted) return
        game.togglePencil()
        _pencilMode.value = game.pencilMode
        scheduleSave()
    }

    fun onHintPressed() {
        if (!loaded) return
        when (val outcome = game.requestHint()) {
            is HintOutcome.Applied -> publish(outcome.changed)
            HintOutcome.NeedsAd -> onWatchAdForHint()
            HintOutcome.None -> Unit
        }
    }

    /**
     * Stub for a future AdMob rewarded video. Calling this does not reveal a
     * digit; wire the reward callback to a real hint grant later.
     */
    fun onWatchAdForHint() {
        _events.tryEmit(GameEvent.RewardedAdUnavailable)
    }

    fun onHostResume() {
        if (!loaded || game.isCompleted) return
        lastMark = SystemClock.elapsedRealtime()
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                if (!loaded || game.isCompleted) continue
                val now = SystemClock.elapsedRealtime()
                game.elapsedMs += (now - lastMark).coerceAtLeast(0L)
                lastMark = now
                _header.value = _header.value.copy(elapsedMs = game.elapsedMs)
            }
        }
    }

    fun onHostPause() {
        ticker?.cancel()
        ticker = null
        if (!loaded || game.isCompleted) return
        if (lastMark != 0L) {
            val now = SystemClock.elapsedRealtime()
            game.elapsedMs += (now - lastMark).coerceAtLeast(0L)
            lastMark = now
            _header.value = _header.value.copy(elapsedMs = game.elapsedMs)
        }
        persistNow()
    }

    private fun publish(changed: IntArray) {
        if (changed.isEmpty() && !game.isCompleted) return
        for (index in changed) pushCell(index)
        _canUndo.value = game.canUndo()
        _hintsRemaining.value = game.hintsRemaining
        if (game.isCompleted) {
            saveJob?.cancel()
            ticker?.cancel()
            if (!_completed.value) {
                _completed.value = true
                val time = game.elapsedMs
                viewModelScope.launch(Dispatchers.IO) {
                    repository.markCompleted(levelId, time)
                }
            }
        } else {
            scheduleSave()
        }
    }

    private fun pushCell(index: Int) {
        val next = CellUi(
            value = game.values[index],
            notesMask = game.notes[index],
            isGiven = game.given[index],
            hasError = game.errors[index],
        )
        if (cellStates[index].value != next) {
            cellStates[index].value = next
        }
    }

    private fun scheduleSave() {
        if (!loaded || game.isCompleted) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(800)
            val progress = game.toSavedProgress()
            withContext(Dispatchers.IO) {
                repository.saveProgress(levelId, progress)
            }
        }
    }

    private fun persistNow() {
        saveJob?.cancel()
        val progress = game.toSavedProgress()
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProgress(levelId, progress)
        }
    }

    companion object {
        fun factory(repository: SudokuRepository, levelId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(repository, levelId) as T
            }
    }
}
