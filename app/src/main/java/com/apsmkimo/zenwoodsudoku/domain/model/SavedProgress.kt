package com.apsmkimo.zenwoodsudoku.domain.model

/**
 * In-progress snapshot stored in `sudoku_levels.saved_progress`.
 *
 * Schema (version 1), one JSON object, no whitespace:
 * ```
 * {
 *   "v": 1,
 *   "elapsedMs": 0,
 *   "hintsRemaining": 10,
 *   "selected": -1,
 *   "pencil": false,
 *   "board": "<81 digits, 0 = empty>",
 *   "notes": [<81 ints, bit (digit-1) set when that pencil mark is on>]
 * }
 * ```
 * Given clues are not stored again; they always come from the puzzle string.
 * `notes` masks use bits 0..8 for digits 1..9.
 */
class SavedProgress(
    val version: Int,
    val elapsedMs: Long,
    val hintsRemaining: Int,
    val selectedIndex: Int,
    val pencilMode: Boolean,
    val board: String,
    val notes: IntArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedProgress) return false
        return version == other.version &&
            elapsedMs == other.elapsedMs &&
            hintsRemaining == other.hintsRemaining &&
            selectedIndex == other.selectedIndex &&
            pencilMode == other.pencilMode &&
            board == other.board &&
            notes.contentEquals(other.notes)
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + elapsedMs.hashCode()
        result = 31 * result + hintsRemaining
        result = 31 * result + selectedIndex
        result = 31 * result + pencilMode.hashCode()
        result = 31 * result + board.hashCode()
        result = 31 * result + notes.contentHashCode()
        return result
    }
}

object SavedProgressCodec {
    private val pattern = Regex(
        """\{"v":(\d+),"elapsedMs":(\d+),"hintsRemaining":(\d+),"selected":(-?\d+),"pencil":(true|false),"board":"([0-9]{81})","notes":\[([0-9,]*)]\}""",
    )

    fun encode(progress: SavedProgress): String = buildString(160 + progress.notes.size * 3) {
        append("{\"v\":")
        append(progress.version)
        append(",\"elapsedMs\":")
        append(progress.elapsedMs)
        append(",\"hintsRemaining\":")
        append(progress.hintsRemaining)
        append(",\"selected\":")
        append(progress.selectedIndex)
        append(",\"pencil\":")
        append(if (progress.pencilMode) "true" else "false")
        append(",\"board\":\"")
        append(progress.board)
        append("\",\"notes\":[")
        for (i in progress.notes.indices) {
            if (i > 0) append(',')
            append(progress.notes[i])
        }
        append("]}")
    }

    fun decode(raw: String?): SavedProgress? {
        if (raw.isNullOrBlank()) return null
        val match = pattern.matchEntire(raw) ?: return null
        val notes = match.groupValues[7].split(',').mapNotNull { token ->
            if (token.isEmpty()) null else token.toIntOrNull()
        }
        if (notes.size != 81) return null
        return SavedProgress(
            version = match.groupValues[1].toInt(),
            elapsedMs = match.groupValues[2].toLong(),
            hintsRemaining = match.groupValues[3].toInt(),
            selectedIndex = match.groupValues[4].toInt(),
            pencilMode = match.groupValues[5] == "true",
            board = match.groupValues[6],
            notes = notes.toIntArray(),
        )
    }
}
