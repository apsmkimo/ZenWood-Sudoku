package com.apsmkimo.zenwoodsudoku.domain.game

import com.apsmkimo.zenwoodsudoku.domain.model.LevelCatalog
import com.apsmkimo.zenwoodsudoku.domain.model.SavedProgress

internal fun noteBit(digit: Int): Int = 1 shl (digit - 1)

class CellSnap(
    val index: Int,
    val value: Int,
    val notesMask: Int,
)

class UndoRecord(
    val cells: List<CellSnap>,
    val hintsRemaining: Int,
)

sealed interface HintOutcome {
    data class Applied(val changed: IntArray) : HintOutcome
    data object NeedsAd : HintOutcome
    data object None : HintOutcome
}

/**
 * Pure board session. Mutations stay on the caller thread (the main thread).
 * Each edit returns the indexes whose value, notes, or conflict flag changed
 * so the UI can push only those cells.
 */
class SudokuGame {
    val values = IntArray(81)
    val notes = IntArray(81)
    val given = BooleanArray(81)
    val errors = BooleanArray(81)
    private val solutionDigits = IntArray(81)
    private val undoStack = ArrayDeque<UndoRecord>()
    private val dirty = BooleanArray(81)

    var hintsRemaining: Int = LevelCatalog.FREE_HINTS
        private set
    var elapsedMs: Long = 0L
    var selectedIndex: Int = -1
        private set
    var pencilMode: Boolean = false
        private set
    var isCompleted: Boolean = false
        private set

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun load(puzzle: String, solution: String, saved: SavedProgress?) {
        require(puzzle.length == 81 && solution.length == 81) {
            "Boards must be 81 characters"
        }
        undoStack.clear()
        isCompleted = false
        hintsRemaining = LevelCatalog.FREE_HINTS
        elapsedMs = 0L
        selectedIndex = -1
        pencilMode = false
        for (i in 0 until 81) {
            val clue = puzzle[i].digitToInt()
            solutionDigits[i] = solution[i].digitToInt()
            given[i] = clue != 0
            values[i] = clue
            notes[i] = 0
            errors[i] = false
        }
        if (saved != null && saved.board.length == 81 && saved.notes.size == 81) {
            for (i in 0 until 81) {
                if (!given[i]) {
                    val entered = saved.board[i].digitToIntOrNull() ?: 0
                    values[i] = entered.coerceIn(0, 9)
                    notes[i] = saved.notes[i]
                }
            }
            hintsRemaining = saved.hintsRemaining.coerceIn(0, LevelCatalog.FREE_HINTS)
            elapsedMs = saved.elapsedMs.coerceAtLeast(0L)
            selectedIndex = saved.selectedIndex.takeIf { it in 0..80 } ?: -1
            pencilMode = saved.pencilMode
        }
        for (i in 0 until 81) {
            errors[i] = computeError(i)
        }
        isCompleted = isSolved()
    }

    fun select(index: Int) {
        if (index in 0..80) selectedIndex = index
    }

    fun togglePencil() {
        pencilMode = !pencilMode
    }

    fun inputDigit(digit: Int): IntArray {
        if (digit !in 1..9 || isCompleted) return IntArray(0)
        val index = selectedIndex
        if (index !in 0..80 || given[index]) return IntArray(0)
        return if (pencilMode && values[index] == 0) {
            toggleNote(index, digit)
        } else if (!pencilMode) {
            if (values[index] == digit) clearCell(index) else placeValue(index, digit)
        } else {
            IntArray(0)
        }
    }

    fun clear(): IntArray {
        val index = selectedIndex
        if (index !in 0..80 || given[index] || isCompleted) return IntArray(0)
        if (values[index] == 0 && notes[index] == 0) return IntArray(0)
        return clearCell(index)
    }

    fun undo(): IntArray {
        val record = undoStack.removeLastOrNull() ?: return IntArray(0)
        for (cell in record.cells) {
            if (given[cell.index]) continue
            values[cell.index] = cell.value
            notes[cell.index] = cell.notesMask
            mark(cell.index)
        }
        hintsRemaining = record.hintsRemaining
        for (i in 0 until 81) updateError(i)
        isCompleted = isSolved()
        if (isCompleted) undoStack.clear()
        return consumeDirty()
    }

    /**
     * Reveals the solution digit for the selected wrong/empty cell, or the
     * first such cell. Does not spend a hint when none remain.
     */
    fun requestHint(): HintOutcome {
        if (isCompleted) return HintOutcome.None
        if (hintsRemaining <= 0) return HintOutcome.NeedsAd
        val target = hintTarget() ?: return HintOutcome.None
        val digit = solutionDigits[target]
        val changed = placeValue(target, digit, spendHint = true)
        return HintOutcome.Applied(changed)
    }

    fun toSavedProgress(): SavedProgress {
        val board = CharArray(81) { index -> ('0'.code + values[index]).toChar() }
        return SavedProgress(
            version = 1,
            elapsedMs = elapsedMs,
            hintsRemaining = hintsRemaining,
            selectedIndex = selectedIndex,
            pencilMode = pencilMode,
            board = String(board),
            notes = notes.copyOf(),
        )
    }

    private fun hintTarget(): Int? {
        val selected = selectedIndex
        if (selected in 0..80 && !given[selected] && values[selected] != solutionDigits[selected]) {
            return selected
        }
        for (i in 0 until 81) {
            if (!given[i] && values[i] != solutionDigits[i]) return i
        }
        return null
    }

    private fun toggleNote(index: Int, digit: Int): IntArray {
        pushUndo(UndoRecord(listOf(snap(index)), hintsRemaining))
        notes[index] = notes[index] xor noteBit(digit)
        mark(index)
        return consumeDirty()
    }

    private fun clearCell(index: Int): IntArray {
        pushUndo(UndoRecord(listOf(snap(index)), hintsRemaining))
        values[index] = 0
        notes[index] = 0
        mark(index)
        refreshErrorsAround(index)
        isCompleted = false
        return consumeDirty()
    }

    private fun placeValue(index: Int, digit: Int, spendHint: Boolean = false): IntArray {
        val touched = ArrayList<CellSnap>(4)
        touched += snap(index)
        val bit = noteBit(digit)
        for (peer in PEERS[index]) {
            if (notes[peer] and bit != 0) touched += snap(peer)
        }
        val hintsBefore = hintsRemaining
        pushUndo(UndoRecord(touched, hintsBefore))
        values[index] = digit
        notes[index] = 0
        mark(index)
        for (peer in PEERS[index]) {
            if (notes[peer] and bit != 0) {
                notes[peer] = notes[peer] and bit.inv()
                mark(peer)
            }
        }
        if (spendHint) hintsRemaining = (hintsRemaining - 1).coerceAtLeast(0)
        refreshErrorsAround(index)
        if (!isCompleted && isSolved()) {
            isCompleted = true
            undoStack.clear()
        }
        return consumeDirty()
    }

    private fun refreshErrorsAround(index: Int) {
        updateError(index)
        for (peer in PEERS[index]) updateError(peer)
    }

    private fun updateError(index: Int) {
        val next = computeError(index)
        if (errors[index] != next) {
            errors[index] = next
            mark(index)
        }
    }

    private fun computeError(index: Int): Boolean {
        val value = values[index]
        if (value == 0) return false
        for (peer in PEERS[index]) {
            if (values[peer] == value) return true
        }
        return false
    }

    private fun isSolved(): Boolean {
        for (i in 0 until 81) {
            if (values[i] == 0 || values[i] != solutionDigits[i]) return false
        }
        return true
    }

    private fun snap(index: Int) = CellSnap(index, values[index], notes[index])

    private fun pushUndo(record: UndoRecord) {
        undoStack.addLast(record)
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
    }

    private fun mark(index: Int) {
        dirty[index] = true
    }

    private fun consumeDirty(): IntArray {
        var count = 0
        for (i in 0 until 81) if (dirty[i]) count++
        val out = IntArray(count)
        var cursor = 0
        for (i in 0 until 81) {
            if (dirty[i]) {
                out[cursor++] = i
                dirty[i] = false
            }
        }
        return out
    }

    companion object {
        private const val MAX_UNDO = 100

        /** Unique row, column, and box neighbors for each cell, excluding itself. */
        val PEERS: Array<IntArray> = Array(81) { index ->
            val set = LinkedHashSet<Int>(24)
            val row = index / 9
            val col = index % 9
            for (i in 0 until 9) {
                set += row * 9 + i
                set += i * 9 + col
            }
            val boxRow = (row / 3) * 3
            val boxCol = (col / 3) * 3
            for (r in boxRow until boxRow + 3) {
                for (c in boxCol until boxCol + 3) {
                    set += r * 9 + c
                }
            }
            set.remove(index)
            set.toIntArray()
        }
    }
}
