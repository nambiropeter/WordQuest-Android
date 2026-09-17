package com.wordquest.app.ui.screens.multiplayer

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordquest.app.viewmodels.MultiplayerViewModel

/** Android counterpart to iOS's `MultiplayerResultsView`. */
@Composable
fun MultiplayerResultsScreen(vm: MultiplayerViewModel, onLeave: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.height(46.dp))
            Text("Game Over", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            vm.myRank?.let { rank ->
                Text(
                    "You finished #$rank of ${vm.scores.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            vm.myScore?.let { myScore ->
                TextButton(onClick = {
                    val rank = vm.myRank ?: 1
                    val text = "I just finished #$rank of ${vm.scores.size} with ${myScore.score} points in a " +
                        "WordQuest local multiplayer trivia game! 🏆"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.height(16.dp))
                    Text(" Share Result", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Column {
                vm.scores.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RankBadge(index + 1)
                        Text(entry.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        if (entry.id == vm.myId) {
                            Text("(You)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${entry.score}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                    }
                    if (index < vm.scores.size - 1) HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (vm.isHost) {
                Button(onClick = { vm.startGame() }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Filled.Replay, contentDescription = null)
                    Text(" Play Again", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    "Waiting for the host to start a new round…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onLeave, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text(" Leave", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val color = when (rank) {
        1 -> Color(0xFFFFC107)
        2 -> Color(0xFF9E9E9E)
        3 -> Color(0xFF8D6E63)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text("$rank", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
}
