package com.mamatiquest.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val StarYellow = Color(0xFFFFC107)
private val StarEmpty = Color(0xFF9E9E9E)

/** Android counterpart to iOS's `StarsView` — a static 1-of-3 rating row. */
@Composable
fun StarsView(filled: Int, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.semantics { contentDescription = "$filled out of 3 stars" },
    ) {
        repeat(3) { i ->
            Icon(
                imageVector = if (i < filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (i < filled) StarYellow else StarEmpty.copy(alpha = 0.4f),
                modifier = Modifier.size(size),
            )
        }
    }
}

/** Android counterpart to iOS's `AnimatedStarsView` — staggered pop-in on the result screen. */
@Composable
fun AnimatedStarsView(filled: Int, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.semantics { contentDescription = "$filled out of 3 stars" },
    ) {
        repeat(3) { i ->
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(i * 150L)
                appeared = true
            }
            val scale by animateFloatAsState(
                targetValue = if (appeared) 1f else 0.1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                label = "starScale",
            )
            val rotation by animateFloatAsState(
                targetValue = if (appeared) 0f else -45f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                label = "starRotation",
            )
            Icon(
                imageVector = if (i < filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (i < filled) StarYellow else StarEmpty.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(size)
                    .scale(scale)
                    .rotate(rotation),
            )
        }
    }
}
