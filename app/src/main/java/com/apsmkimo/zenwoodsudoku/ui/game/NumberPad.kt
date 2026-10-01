package com.apsmkimo.zenwoodsudoku.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apsmkimo.zenwoodsudoku.R
import com.apsmkimo.zenwoodsudoku.ui.theme.ZenColors

@Composable
fun NumberPad(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    val pencil by viewModel.pencilMode.collectAsStateWithLifecycle()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DigitRow(digits = intArrayOf(1, 2, 3, 4, 5), onDigit = viewModel::onDigit)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (digit in 6..9) {
                DigitKey(
                    digit = digit,
                    onClick = { viewModel.onDigit(digit) },
                    modifier = Modifier.weight(1f),
                )
            }
            PadKey(onClick = viewModel::onClear, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = stringResource(R.string.erase),
                    tint = ZenColors.Ink,
                )
            }
        }
        PadKey(
            onClick = viewModel::togglePencil,
            selected = pencil,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.pencil),
                tint = if (pencil) ZenColors.Pen else ZenColors.Pencil,
            )
        }
    }
}

@Composable
private fun DigitRow(
    digits: IntArray,
    onDigit: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (digit in digits) {
            DigitKey(
                digit = digit,
                onClick = { onDigit(digit) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DigitKey(
    digit: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PadKey(onClick = onClick, modifier = modifier) {
        Text(
            text = digit.toString(),
            color = ZenColors.Ink,
            fontFamily = FontFamily.Serif,
            fontSize = 22.sp,
        )
    }
}

@Composable
private fun PadKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) ZenColors.PaperDeep else ZenColors.Paper,
        modifier = modifier.height(48.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}
