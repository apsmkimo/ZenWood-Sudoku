package com.apsmkimo.zenwoodsudoku.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apsmkimo.zenwoodsudoku.ui.theme.drawSelection
import com.apsmkimo.zenwoodsudoku.ui.theme.paperBoard
import kotlinx.coroutines.flow.StateFlow

/**
 * The board composable does not collect cell or selection state.
 * [CellSlot] collects one [StateFlow] and receives primitives, so a tap or
 * edit restarts only that cell. Selection is a separate underlay.
 */
@Composable
fun SudokuBoard(
    cellFlows: List<StateFlow<CellUi>>,
    selectedIndex: StateFlow<Int>,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val click = remember { onCellClick }
    Box(modifier.paperBoard()) {
        SelectionUnderlay(
            selectedIndex = selectedIndex,
            modifier = Modifier.matchParentSize(),
        )
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until 9) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    for (column in 0 until 9) {
                        val index = row * 9 + column
                        key(index) {
                            CellSlot(
                                cellFlow = cellFlows[index],
                                index = index,
                                onCellClick = click,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionUnderlay(
    selectedIndex: StateFlow<Int>,
    modifier: Modifier = Modifier,
) {
    val index by selectedIndex.collectAsStateWithLifecycle()
    Canvas(modifier) {
        drawSelection(index)
    }
}

@Composable
private fun CellSlot(
    cellFlow: StateFlow<CellUi>,
    index: Int,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cell by cellFlow.collectAsStateWithLifecycle()
    SudokuCell(
        index = index,
        value = cell.value,
        notesMask = cell.notesMask,
        isGiven = cell.isGiven,
        hasError = cell.hasError,
        onClick = { onCellClick(index) },
        modifier = modifier,
    )
}
