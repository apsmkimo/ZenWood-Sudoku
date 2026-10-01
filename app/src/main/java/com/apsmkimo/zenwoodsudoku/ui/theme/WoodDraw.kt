package com.apsmkimo.zenwoodsudoku.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.sin

fun DrawScope.drawWoodGrain() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                ColorWoodLight,
                ColorWoodMid,
                ColorWoodDark,
                ColorWoodMid,
            ),
        ),
    )
    val bands = 26
    for (index in 0 until bands) {
        val t = index / bands.toFloat()
        val y = size.height * t
        val wave = sin(t * 14f) * 12f
        drawLine(
            color = ZenColors.OnWood.copy(alpha = if (index % 2 == 0) 0.06f else 0.035f),
            start = Offset(0f, y + wave),
            end = Offset(size.width, y - wave * 0.6f),
            strokeWidth = if (index % 5 == 0) 7f else 2.5f,
        )
    }
}

fun DrawScope.drawPaper() {
    drawRect(ZenColors.Paper)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                ZenColors.OnWood.copy(alpha = 0.18f),
                ZenColors.Paper.copy(alpha = 0f),
                ZenColors.Ink.copy(alpha = 0.08f),
            ),
            center = Offset(size.width * 0.5f, size.height * 0.42f),
            radius = size.maxDimension * 0.72f,
        ),
    )
    val fibers = 8
    for (index in 0 until fibers) {
        val y = size.height * (index + 1) / (fibers + 1f)
        drawLine(
            color = ZenColors.PaperDeep.copy(alpha = 0.45f),
            start = Offset(0f, y),
            end = Offset(size.width, y + 1.5f),
            strokeWidth = 1f,
        )
    }
}

fun DrawScope.drawSudokuGrid() {
    val thick = 3.dp.toPx()
    val thin = 1.dp.toPx()
    val inset = thick / 2f
    val span = size.width - thick
    val step = span / 9f
    for (index in 0..9) {
        val major = index % 3 == 0
        val stroke = if (major) thick else thin
        val color = if (major) ZenColors.GridThick else ZenColors.GridThin
        val position = inset + index * step
        drawLine(color, Offset(position, inset), Offset(position, inset + span), stroke)
        drawLine(color, Offset(inset, position), Offset(inset + span, position), stroke)
    }
}

fun DrawScope.drawSelection(index: Int) {
    if (index !in 0..80) return
    val thick = 3.dp.toPx()
    val inset = thick / 2f
    val span = size.width - thick
    val step = span / 9f
    val row = index / 9
    val col = index % 9
    drawRect(
        color = ZenColors.Selection,
        topLeft = Offset(inset + col * step, inset + row * step),
        size = Size(step, step),
    )
}

@Composable
fun WoodBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawWoodGrain() },
        content = content,
    )
}

fun Modifier.paperBoard(): Modifier = this
    .drawBehind { drawPaper() }
    .drawWithContent {
        drawContent()
        drawSudokuGrid()
    }

private val ColorWoodLight = ZenColors.WoodLight
private val ColorWoodMid = ZenColors.WoodMid
private val ColorWoodDark = ZenColors.WoodDark
