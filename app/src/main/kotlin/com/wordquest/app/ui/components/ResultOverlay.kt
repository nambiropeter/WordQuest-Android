package com.wordquest.app.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordquest.app.models.GameMode
import com.wordquest.app.models.GameTheme
import com.wordquest.app.ui.theme.brush

/**
 * Android counterpart to iOS's `ResultOverlay` — the full-screen scrim shown
 * on level completion with score, stars, and replay/next/home actions.
 */
@Composable
fun ResultOverlay(
    stars: Int,
    score: Int,
    theme: GameTheme,
    level: Int,
    mode: GameMode,
    onReplay: () -> Unit,
    onNext: () -> Unit,
    onHome: () -> Unit,
) {
    val context = LocalContext.current
    val hasNextLevel = level < GameMode.LEVEL_COUNT

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .padding(32.dp)
                .background(Color(0xFF2B2B33).copy(alpha = 0.92f), RoundedCornerShape(28.dp))
                .padding(28.dp),
        ) {
            Text(
                text = if (stars > 0) "Level Complete!" else "Time's Up",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            AnimatedStarsView(filled = stars)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SCORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
            }

            Text(
                text = "Level $level · ${theme.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
            )

            if (stars > 0) {
                Button(
                    onClick = {
                        val shareText = "I just scored $score points on Level $level (${theme.name}) " +
                            "in ${mode.displayName} on WordQuest! 🧩"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Result"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Share Result", color = Color.White.copy(alpha = 0.9f))
                }
            }

            if (hasNextLevel) {
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.brush(), RoundedCornerShape(16.dp))
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("Next Level", color = Color.White, fontWeight = FontWeight.Bold)
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onReplay,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
                    Text(" Replay", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onHome,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = Color.White)
                    Text(" Home", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
