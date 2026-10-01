package com.apsmkimo.zenwoodsudoku.ui.game

import android.graphics.Paint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.apsmkimo.zenwoodsudoku.R
import com.apsmkimo.zenwoodsudoku.ui.theme.ZenColors

@Composable
fun SudokuCell(
    index: Int,
    value: Int,
    notesMask: Int,
    isGiven: Boolean,
    hasError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val row = index / 9 + 1
    val column = index % 9 + 1
    val description = if (value == 0) {
        stringResource(R.string.cell_empty, row, column)
    } else {
        stringResource(R.string.cell_value, row, column, value)
    }
    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .clickable(onClick = onClick)
            .drawBehind { drawPencilNotes(value, notesMask) },
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                color = when {
                    hasError -> ZenColors.Error
                    isGiven -> ZenColors.Ink
                    else -> ZenColors.Pen
                },
                fontFamily = FontFamily.Serif,
                fontWeight = if (isGiven) FontWeight.Bold else FontWeight.Medium,
                fontSize = 20.sp,
            )
        }
    }
}

private fun DrawScope.drawPencilNotes(value: Int, notesMask: Int) {
    if (value != 0 || notesMask == 0) return
    val paint = notePaint
    paint.textSize = size.minDimension * 0.22f
    val canvas = drawContext.canvas.nativeCanvas
    val cellWidth = size.width / 3f
    val cellHeight = size.height / 3f
    for (digit in 1..9) {
        if (notesMask and (1 shl (digit - 1)) == 0) continue
        val column = (digit - 1) % 3
        val row = (digit - 1) / 3
        val x = column * cellWidth + cellWidth / 2f
        val y = row * cellHeight + cellHeight / 2f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(digit.toString(), x, y, paint)
    }
}

private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.parseColor("#8A8175")
    textAlign = Paint.Align.CENTER
}
