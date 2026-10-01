package com.apsmkimo.zenwoodsudoku.domain

import com.apsmkimo.zenwoodsudoku.domain.game.HintOutcome
import com.apsmkimo.zenwoodsudoku.domain.game.SudokuGame
import com.apsmkimo.zenwoodsudoku.domain.game.noteBit
import com.apsmkimo.zenwoodsudoku.domain.model.SavedProgress
import com.apsmkimo.zenwoodsudoku.domain.model.SavedProgressCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuGameTest {
    @Test
    fun givenCellIgnoresPenAndPencil() {
        val game = SudokuGame()
        game.load(puzzle(0 to '5'), SOLUTION, null)
        game.select(0)
        game.inputDigit(9)
        assertEquals(5, game.values[0])
        game.togglePencil()
        game.inputDigit(3)
        assertEquals(0, game.notes[0])
    }

    @Test
    fun duplicateInRowMarksBothCellsAndClearingFixesThem() {
        val game = fresh()
        game.select(0)
        game.inputDigit(5)
        game.select(1)
        game.inputDigit(5)
        assertTrue(game.errors[0])
        assertTrue(game.errors[1])
        game.clear()
        assertFalse(game.errors[0])
        assertFalse(game.errors[1])
        assertEquals(0, game.values[1])
    }

    @Test
    fun pencilToggleUndoAndPeerNoteCleanup() {
        val game = fresh()
        game.select(0)
        game.togglePencil()
        game.inputDigit(7)
        assertNotEquals(0, game.notes[0] and noteBit(7))
        game.undo()
        assertEquals(0, game.notes[0])

        game.inputDigit(7)
        game.togglePencil()
        game.select(1)
        game.inputDigit(7)
        assertEquals(0, game.notes[0] and noteBit(7))
        assertEquals(7, game.values[1])
        game.undo()
        assertNotEquals(0, game.notes[0] and noteBit(7))
        assertEquals(0, game.values[1])
    }

    @Test
    fun sameDigitClearsUserEntry() {
        val game = fresh()
        game.select(4)
        game.inputDigit(2)
        game.inputDigit(2)
        assertEquals(0, game.values[4])
        game.undo()
        assertEquals(2, game.values[4])
    }

    @Test
    fun hintRevealsSolutionThenRequestsAd() {
        val game = fresh()
        game.select(0)
        repeat(10) {
            val outcome = game.requestHint()
            assertTrue(outcome is HintOutcome.Applied)
        }
        assertEquals(0, game.hintsRemaining)
        assertEquals(SOLUTION[0].digitToInt(), game.values[0])
        assertTrue(game.requestHint() is HintOutcome.NeedsAd)
        game.undo()
        assertEquals(1, game.hintsRemaining)
        assertEquals(0, game.values[9])
    }

    @Test
    fun completingBoardSetsCompletedAndDropsUndo() {
        val puzzle = SOLUTION.replaceRange(80, 81, "0")
        val game = SudokuGame()
        game.load(puzzle, SOLUTION, null)
        game.select(80)
        game.inputDigit(SOLUTION[80].digitToInt())
        assertTrue(game.isCompleted)
        assertFalse(game.canUndo())
        game.inputDigit(1)
        assertEquals(SOLUTION[80].digitToInt(), game.values[80])
    }

    @Test
    fun savedProgressRestoresUserStateWithoutClobberingGivens() {
        val puzzle = SOLUTION.replaceRange(0, 1, "0")
        val board = puzzle.toCharArray()
        board[0] = '1'
        board[1] = '9'
        val notes = IntArray(81)
        notes[0] = noteBit(4)
        val saved = SavedProgress(
            version = 1,
            elapsedMs = 4_200L,
            hintsRemaining = 6,
            selectedIndex = 0,
            pencilMode = true,
            board = String(board),
            notes = notes,
        )
        val game = SudokuGame()
        game.load(puzzle, SOLUTION, saved)
        assertEquals(1, game.values[0])
        assertEquals(SOLUTION[1].digitToInt(), game.values[1])
        assertEquals(noteBit(4), game.notes[0])
        assertEquals(6, game.hintsRemaining)
        assertEquals(4_200L, game.elapsedMs)
        assertEquals(0, game.selectedIndex)
        assertTrue(game.pencilMode)
        val snapshot = game.toSavedProgress()
        assertEquals(snapshot, SavedProgressCodec.decode(SavedProgressCodec.encode(snapshot)))
    }

    @Test
    fun codecRejectsShortPayload() {
        assertNull(SavedProgressCodec.decode("{\"v\":1}"))
        assertNull(SavedProgressCodec.decode(null))
    }

    private fun fresh(): SudokuGame = SudokuGame().also { it.load("0".repeat(81), SOLUTION, null) }

    private fun puzzle(vararg overrides: Pair<Int, Char>): String {
        val chars = "0".repeat(81).toCharArray()
        overrides.forEach { (index, char) -> chars[index] = char }
        return String(chars)
    }

    private companion object {
        const val SOLUTION =
            "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    }
}
