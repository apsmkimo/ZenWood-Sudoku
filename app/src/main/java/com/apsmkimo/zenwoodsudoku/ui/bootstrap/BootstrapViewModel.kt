package com.apsmkimo.zenwoodsudoku.ui.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apsmkimo.zenwoodsudoku.domain.model.LevelCatalog
import com.apsmkimo.zenwoodsudoku.domain.repository.SudokuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BootstrapUiState(
    val phase: Phase = Phase.Loading,
    val processed: Int = 0,
    val total: Int = LevelCatalog.TOTAL,
) {
    enum class Phase { Loading, Ready, Error }
}

class BootstrapViewModel(
    private val repository: SudokuRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BootstrapUiState())
    val state: StateFlow<BootstrapUiState> = _state.asStateFlow()

    init {
        start()
    }

    fun start() {
        viewModelScope.launch {
            _state.value = BootstrapUiState(phase = BootstrapUiState.Phase.Loading)
            try {
                repository.ensureLevelsImported { processed, total ->
                    _state.value = BootstrapUiState(
                        phase = BootstrapUiState.Phase.Loading,
                        processed = processed,
                        total = total,
                    )
                }
                _state.value = BootstrapUiState(
                    phase = BootstrapUiState.Phase.Ready,
                    processed = LevelCatalog.TOTAL,
                    total = LevelCatalog.TOTAL,
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(phase = BootstrapUiState.Phase.Error)
            }
        }
    }
}
