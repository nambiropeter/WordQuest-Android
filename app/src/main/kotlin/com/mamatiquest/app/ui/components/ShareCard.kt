package com.mamatiquest.app.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mamatiquest.app.models.GameMode
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.models.PlayerScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

/**
 * What gets drawn on the shareable result image. Android counterpart to iOS's
 * `ShareCardView` — same 4:5 layout, so a card shared from either platform
 * looks like the same game.
 */
sealed interface ShareCard {
    val caption: String

    data class Level(
        val score: Int,
        val stars: Int,
        val level: Int,
        val theme: GameTheme,
        val mode: GameMode,
    ) : ShareCard {
        override val caption: String
            get() = "I just scored $score points on Level $level (${theme.name}) in ${mode.displayName} on WordQuest! 🧩"
    }

    data class Multiplayer(
        val rank: Int,
        val scores: List<PlayerScore>,
        val myId: String,
    ) : ShareCard {
        val myScore: Int get() = scores.firstOrNull { it.id == myId }?.score ?: 0
        override val caption: String
            get() = "I just finished #$rank of ${scores.size} with $myScore points in a WordQuest local multiplayer trivia game! 🏆"
    }
}

/**
 * Returns a callback that renders [ShareCard] to a PNG and opens the system
 * share sheet with it (plus the caption as text, for apps that take both).
 * [heroIcon] is the glyph drawn in the circle above the headline.
 */
@Composable
fun rememberShareCardSharer(heroIcon: ImageVector): (ShareCard) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer(cacheSize = 64)
    val painters = ShareCardPainters(
        hero = rememberVectorPainter(heroIcon),
        star = rememberVectorPainter(Icons.Filled.Star),
        grid = rememberVectorPainter(Icons.Filled.GridView),
    )
    return remember(context, textMeasurer, painters) {
        { card ->
            val bitmap = renderShareCard(card, textMeasurer, painters)
            scope.launch {
                val file = withContext(Dispatchers.IO) { writePng(context, bitmap) }
                shareImage(context, file, card.caption)
            }
        }
    }
}

private data class ShareCardPainters(val hero: Painter, val star: Painter, val grid: Painter)

/** Design units; the bitmap is rendered at [SCALE]x (1080 × 1350 px). */
private const val CARD_W = 360f
private const val CARD_H = 450f
private const val SCALE = 3f

private val StarGold = Color(0xFFFFD166)
private val MultiplayerColors = listOf(Color(0xFF118AB2), Color(0xFF073B4C))

private fun renderShareCard(card: ShareCard, tm: TextMeasurer, painters: ShareCardPainters): ImageBitmap {
    val bitmap = ImageBitmap((CARD_W * SCALE).toInt(), (CARD_H * SCALE).toInt())
    CanvasDrawScope().draw(
        density = Density(SCALE, fontScale = 1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(bitmap),
        size = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
    ) {
        val colors = when (card) {
            is ShareCard.Level -> listOf(card.theme.primaryColor, card.theme.secondaryColor)
            is ShareCard.Multiplayer -> MultiplayerColors
        }
        drawRect(Brush.linearGradient(colors, start = Offset.Zero, end = Offset(size.width, size.height)))
        drawLetterGrid(tm, seed = card.hashCode())
        // Soft glow behind the score.
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(u(180f), u(250f)),
                radius = u(170f),
            ),
            radius = u(170f),
            center = Offset(u(180f), u(250f)),
        )

        drawBrandPill(tm, painters.grid)
        drawHeroIcon(painters.hero, centerY = 98f)

        when (card) {
            is ShareCard.Level -> drawLevelBody(card, tm, painters.star)
            is ShareCard.Multiplayer -> drawMultiplayerBody(card, tm)
        }

        drawCentered(tm, "Can you beat my score?", y = 398f, size = 17.sp, weight = FontWeight.ExtraBold)
        drawCentered(
            tm, "Play WordQuest · free on iOS & Android", y = 421f, size = 11.sp,
            weight = FontWeight.Medium, color = Color.White.copy(alpha = 0.75f),
        )
    }
    return bitmap
}

private fun DrawScope.drawLevelBody(card: ShareCard.Level, tm: TextMeasurer, star: Painter) {
    drawCentered(tm, "LEVEL COMPLETE!", y = 140f, size = 24.sp, weight = FontWeight.Black, spacing = 0.04.em)

    // Stars, with the middle one raised like the in-game result.
    val starSize = 38f
    val gap = 12f
    val startX = (CARD_W - (starSize * 3 + gap * 2)) / 2
    repeat(3) { i ->
        val lift = if (i == 1) -8f else 0f
        translate(u(startX + i * (starSize + gap)), u(184f + lift)) {
            with(star) {
                draw(
                    Size(u(starSize), u(starSize)),
                    colorFilter = ColorFilter.tint(if (i < card.stars) StarGold else Color.White.copy(alpha = 0.28f)),
                )
            }
        }
    }

    drawCentered(tm, "%,d".format(card.score), y = 236f, size = 80.sp, weight = FontWeight.Black)
    drawCentered(
        tm, "POINTS", y = 326f, size = 12.sp, weight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.75f), spacing = 0.3.em,
    )
    drawChip(tm, "LEVEL ${card.level}  ·  ${card.theme.name.uppercase()}  ·  ${card.mode.displayName.uppercase()}", y = 350f)
}

private fun DrawScope.drawMultiplayerBody(card: ShareCard.Multiplayer, tm: TextMeasurer) {
    val headline = if (card.rank == 1) "WINNER!" else "FINISHED #${card.rank}"
    drawCentered(tm, headline, y = 140f, size = 24.sp, weight = FontWeight.Black, spacing = 0.04.em)
    drawCentered(
        tm, "of ${card.scores.size} players · Local Trivia", y = 171f, size = 12.sp,
        weight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.75f),
    )
    drawCentered(tm, "%,d".format(card.myScore), y = 188f, size = 64.sp, weight = FontWeight.Black)
    drawCentered(
        tm, "POINTS", y = 262f, size = 12.sp, weight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.75f), spacing = 0.3.em,
    )

    // Mini podium: top three, with the sharer's row highlighted (swapped in for #3 if they placed lower).
    val ranked = card.scores.withIndex().toList()
    val rows = ranked.take(3).let { top ->
        if (top.any { it.value.id == card.myId }) top
        else top.take(2) + listOfNotNull(ranked.firstOrNull { it.value.id == card.myId })
    }
    val boxX = 48f
    val boxW = CARD_W - boxX * 2
    val rowH = 26f
    val boxY = 286f
    drawRoundRect(
        Color.White.copy(alpha = 0.14f),
        topLeft = Offset(u(boxX), u(boxY)),
        size = Size(u(boxW), u(rows.size * rowH + 12f)),
        cornerRadius = CornerRadius(u(16f)),
    )
    val medals = listOf(StarGold, Color(0xFFD9D9D9), Color(0xFFD4A373))
    rows.forEachIndexed { i, (rankIndex, entry) ->
        val y = boxY + 6f + i * rowH
        val mine = entry.id == card.myId
        if (mine) {
            drawRoundRect(
                Color.White.copy(alpha = 0.18f),
                topLeft = Offset(u(boxX + 6f), u(y)),
                size = Size(u(boxW - 12f), u(rowH)),
                cornerRadius = CornerRadius(u(10f)),
            )
        }
        val weight = if (mine) FontWeight.Black else FontWeight.SemiBold
        drawCircle(medals.getOrElse(rankIndex) { Color.White.copy(alpha = 0.6f) }, radius = u(9f), center = Offset(u(boxX + 24f), u(y + rowH / 2)))
        drawTextAt(tm, "${rankIndex + 1}", x = boxX + 24f, y = y + rowH / 2, size = 11.sp, weight = FontWeight.Black, color = Color(0xFF073B4C), center = true)
        drawTextAt(tm, entry.name, x = boxX + 42f, y = y + rowH / 2, size = 14.sp, weight = weight, maxWidth = boxW - 120f)
        drawTextAt(tm, "${entry.score}", x = boxX + boxW - 16f, y = y + rowH / 2, size = 14.sp, weight = FontWeight.Black, alignEnd = true)
    }
}

/** Faint word-search letters filling the background so the card reads as WordQuest at a glance. */
private fun DrawScope.drawLetterGrid(tm: TextMeasurer, seed: Int) {
    val random = Random(seed)
    val cols = 8
    val rows = 10
    val cell = CARD_W / cols
    val style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.07f))
    for (r in 0 until rows) for (c in 0 until cols) {
        val letter = ('A' + random.nextInt(26)).toString()
        val layout = tm.measure(letter, style)
        drawText(
            layout,
            topLeft = Offset(
                u(c * cell + cell / 2) - layout.size.width / 2f,
                u(r * cell + cell / 2) - layout.size.height / 2f,
            ),
        )
    }
}

private fun DrawScope.drawBrandPill(tm: TextMeasurer, grid: Painter) {
    val layout = tm.measure(
        "WORDQUEST",
        TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 0.18.em),
    )
    val iconSize = u(15f)
    val gap = u(6f)
    val padH = u(14f)
    val pillW = padH * 2 + iconSize + gap + layout.size.width
    val pillH = u(30f)
    val left = (size.width - pillW) / 2
    val top = u(24f)
    drawRoundRect(
        Color.White.copy(alpha = 0.18f),
        topLeft = Offset(left, top),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(pillH / 2),
    )
    translate(left + padH, top + (pillH - iconSize) / 2) {
        with(grid) { draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(Color.White)) }
    }
    drawText(layout, topLeft = Offset(left + padH + iconSize + gap, top + (pillH - layout.size.height) / 2))
}

private fun DrawScope.drawHeroIcon(hero: Painter, centerY: Float) {
    val center = Offset(u(CARD_W / 2), u(centerY))
    drawCircle(Color.White.copy(alpha = 0.22f), radius = u(28f), center = center)
    val iconSize = u(28f)
    translate(center.x - iconSize / 2, center.y - iconSize / 2) {
        with(hero) { draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(Color.White)) }
    }
}

private fun DrawScope.drawChip(tm: TextMeasurer, text: String, y: Float) {
    val layout = tm.measure(
        text,
        TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.06.em),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = u(CARD_W - 64f).toInt()),
    )
    val padH = u(14f)
    val chipH = u(28f)
    val chipW = layout.size.width + padH * 2
    val left = (size.width - chipW) / 2
    drawRoundRect(
        Color.White.copy(alpha = 0.16f),
        topLeft = Offset(left, u(y)),
        size = Size(chipW, chipH),
        cornerRadius = CornerRadius(chipH / 2),
    )
    drawText(layout, topLeft = Offset(left + padH, u(y) + (chipH - layout.size.height) / 2))
}

/** Draws [text] horizontally centered with its top at design-unit [y]. */
private fun DrawScope.drawCentered(
    tm: TextMeasurer,
    text: String,
    y: Float,
    size: TextUnit,
    weight: FontWeight,
    color: Color = Color.White,
    spacing: TextUnit = TextUnit.Unspecified,
) {
    val layout = tm.measure(text, TextStyle(fontSize = size, fontWeight = weight, color = color, letterSpacing = spacing))
    drawText(layout, topLeft = Offset((this.size.width - layout.size.width) / 2, u(y)))
}

/** Draws single-line [text] vertically centered on design-unit [y], anchored at [x] by its start, center or end. */
private fun DrawScope.drawTextAt(
    tm: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    size: TextUnit,
    weight: FontWeight,
    color: Color = Color.White,
    maxWidth: Float? = null,
    center: Boolean = false,
    alignEnd: Boolean = false,
) {
    val layout = tm.measure(
        text,
        TextStyle(fontSize = size, fontWeight = weight, color = color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        constraints = maxWidth?.let { Constraints(maxWidth = u(it).toInt()) } ?: Constraints(),
    )
    val left = when {
        center -> u(x) - layout.size.width / 2f
        alignEnd -> u(x) - layout.size.width
        else -> u(x)
    }
    drawText(layout, topLeft = Offset(left, u(y) - layout.size.height / 2f))
}

/** Design units → pixels. */
private fun DrawScope.u(value: Float): Float = value.dp.toPx()

private fun writePng(context: Context, bitmap: ImageBitmap): File {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "wordquest-result.png")
    file.outputStream().use { bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    return file
}

private fun shareImage(context: Context, file: File, caption: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        // ClipData lets the chooser show the image as its preview thumbnail.
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Result"))
}
