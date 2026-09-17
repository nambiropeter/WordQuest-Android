package com.wordquest.app.ui.screens.multiplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordquest.app.data.ContentStore
import com.wordquest.app.models.GameTheme
import com.wordquest.app.ui.theme.MultiplayerGradient
import com.wordquest.app.ui.theme.brush
import com.wordquest.app.ui.theme.sfSymbolToIcon
import com.wordquest.app.viewmodels.MultiplayerViewModel

/** Android counterpart to iOS's `MultiplayerRoomView` — host controls (theme/question count/start) or a waiting state for guests, plus the shared player list. */
@Composable
fun MultiplayerRoomScreen(vm: MultiplayerViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val contentStore = androidx.compose.runtime.remember { ContentStore.getInstance(context) }
    val questionCountOptions = listOf(5, 8, 12)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (vm.isHost) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("THEME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ThemeChip(selected = vm.selectedTheme == null, label = "Mixed", icon = null, gradient = MultiplayerGradient) {
                        vm.selectedTheme = null
                    }
                    contentStore.themes.forEach { theme ->
                        ThemeChip(
                            selected = vm.selectedTheme?.id == theme.id,
                            label = theme.name,
                            icon = theme.icon,
                            gradient = theme.brush(),
                        ) { vm.selectedTheme = theme }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("QUESTIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        questionCountOptions.forEachIndexed { index, count ->
                            SegmentedButton(
                                selected = vm.questionCount == count,
                                onClick = { vm.questionCount = count },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = questionCountOptions.size),
                            ) { Text("$count") }
                        }
                    }
                }

                Button(
                    onClick = { vm.startGame() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(" Start Game", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    "Waiting for the host to start the game…",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        PlayerList(vm)
    }
}

@Composable
private fun ThemeChip(selected: Boolean, label: String, icon: String?, gradient: androidx.compose.ui.graphics.Brush, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(width = 68.dp, height = 64.dp).let {
            if (selected) it.background(gradient, RoundedCornerShape(14.dp)) else it
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon?.let { sfSymbolToIcon(it) } ?: Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PlayerList(vm: MultiplayerViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "PLAYERS (${vm.players.size}/${com.wordquest.app.services.NearbyMultiplayerService.MAX_PEERS})",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        if (vm.isHost && vm.players.size >= com.wordquest.app.services.NearbyMultiplayerService.MAX_PEERS) {
            Text(
                "Session is full — this game supports up to ${com.wordquest.app.services.NearbyMultiplayerService.MAX_PEERS} players.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF9800),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Column {
                vm.players.forEachIndexed { index, player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(player.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        if (player.id == vm.myId) {
                            Text("(You)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (index < vm.players.size - 1) {
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(start = 44.dp))
                    }
                }
            }
        }
    }
}
