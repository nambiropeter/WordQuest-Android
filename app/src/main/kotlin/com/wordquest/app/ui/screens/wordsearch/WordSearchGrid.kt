package com.wordquest.app.ui.screens.wordsearch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wordquest.app.models.GridPosition
import com.wordquest.app.viewmodels.WordSearchViewModel

/**
 * Android counterpart to iOS's `WordSearchGridView` — a single Canvas draws
 * the letters and found/in-progress selection lines, with a `pointerInput`
 * drag gesture mirroring the `DragGesture` iOS uses to trace words.
 */
@Composable
fun WordSearchGrid(vm: WordSearchViewModel, modifier: Modifier = Modifier) {
    val puzzle = vm.puzzle
    val gridSize = puzzle.gridSize
    val theme = puzzle.theme
    val letterColor = MaterialTheme.colorScheme.onSurface

    // Compose 1.7.6 (this project's BOM) has no `rememberTextMeasurer()` helper yet,
    // so `TextMeasurer` is built directly from its constructor, the way that helper
    // does internally.
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val textMeasurer = remember(fontFamilyResolver, density, layoutDirection) {
        TextMeasurer(fontFamilyResolver, density, layoutDirection)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(gridSize) {
                var cellSize = size.width.toFloat() / gridSize
                detectDragGestures(
                    onDragStart = { offset ->
                        cellSize = size.width.toFloat() / gridSize
                        vm.beginSelection(gridPositionFor(offset, cellSize, gridSize))
                    },
                    onDrag = { change, _ ->
                        vm.updateSelection(gridPositionFor(change.position, cellSize, gridSize))
                    },
                    onDragEnd = { vm.endSelection() },
                    onDragCancel = { vm.endSelection() },
                )
            },
    ) {
        val cellSize = size.width / gridSize

        // Lines for already-found words.
        vm.foundPaths.forEach { path ->
            drawSelectionLine(path, cellSize, theme.primaryColor.copy(alpha = 0.4f))
        }

        // Line for the selection currently being dragged.
        if (vm.currentSelection.size > 1) {
            drawSelectionLine(vm.currentSelection, cellSize, theme.secondaryColor.copy(alpha = 0.5f))
        }

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val letter = puzzle.letters[row][col].toString()
                val center = Offset(col * cellSize + cellSize / 2, row * cellSize + cellSize / 2)

                if (vm.hintedPosition == GridPosition(row, col)) {
                    drawCircle(color = theme.primaryColor.copy(alpha = 0.55f), radius = cellSize * 0.42f, center = center)
                }

                val measured = textMeasurer.measure(
                    text = letter,
                    style = TextStyle(
                        fontSize = (cellSize.toDp().value * 0.46f).sp,
                        fontWeight = FontWeight.Bold,
                        color = letterColor,
                    ),
                )
                translate(left = center.x - measured.size.width / 2f, top = center.y - measured.size.height / 2f) {
                    drawText(measured)
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSelectionLine(
    path: List<GridPosition>,
    cellSize: Float,
    color: Color,
) {
    val first = path.firstOrNull() ?: return
    val last = path.lastOrNull() ?: return
    val x1 = first.col * cellSize + cellSize / 2
    val y1 = first.row * cellSize + cellSize / 2
    val x2 = last.col * cellSize + cellSize / 2
    val y2 = last.row * cellSize + cellSize / 2

    drawLine(
        color = color,
        start = Offset(x1, y1),
        end = Offset(x2, y2),
        strokeWidth = cellSize * 0.78f,
        cap = StrokeCap.Round,
    )
}

private fun gridPositionFor(offset: Offset, cellSize: Float, gridSize: Int): GridPosition {
    val col = (offset.x / cellSize).toInt().coerceIn(0, gridSize - 1)
    val row = (offset.y / cellSize).toInt().coerceIn(0, gridSize - 1)
    return GridPosition(row, col)
}
