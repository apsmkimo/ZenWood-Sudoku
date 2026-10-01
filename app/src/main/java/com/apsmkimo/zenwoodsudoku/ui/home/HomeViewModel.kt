package com.apsmkimo.zenwoodsudoku.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apsmkimo.zenwoodsudoku.domain.model.DifficultyStats
import com.apsmkimo.zenwoodsudoku.domain.repository.SudokuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val stats: List<DifficultyStats> = emptyList(),
    val loading: Boolean = true,
)

class HomeViewModel(
    private val repository: SudokuRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = HomeUiState(
                stats = repository.loadHome(),
                loading = false,
            )
        }
    }

    companion object {
        fun factory(repository: SudokuRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(repository) as T
            }
    }
}
