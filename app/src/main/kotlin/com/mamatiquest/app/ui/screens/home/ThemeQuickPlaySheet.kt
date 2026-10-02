package com.mamatiquest.app.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mamatiquest.app.models.GameMode
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.ui.theme.MultiplayerGradient
import com.mamatiquest.app.ui.theme.TriviaModeGradient
import com.mamatiquest.app.ui.theme.WordSearchModeGradient
import com.mamatiquest.app.ui.theme.brush
import com.mamatiquest.app.ui.theme.sfSymbolToIcon
import androidx.compose.ui.graphics.Brush

sealed class QuickPlayChoice {
    data class Solo(val mode: GameMode) : QuickPlayChoice()
    data object MultiplayerTrivia : QuickPlayChoice()
}

private enum class QuickPlayStep { PLAYER_COUNT, GAME }

/** Android counterpart to iOS's `ThemeQuickPlaySheet`, hosted inside a `ModalBottomSheet` by the caller. */
@Composable
fun ThemeQuickPlaySheetContent(theme: GameTheme, onConfirm: (QuickPlayChoice) -> Unit) {
    var step by remember { mutableStateOf(QuickPlayStep.PLAYER_COUNT) }
    var wantsMultiplayer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step == QuickPlayStep.GAME) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.clickable { step = QuickPlayStep.PLAYER_COUNT },
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(theme.brush(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(sfSymbolToIcon(theme.icon), contentDescription = null, tint = Color.White)
            }
            Column {
                Text(theme.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (step == QuickPlayStep.PLAYER_COUNT) "How do you want to play?" else "Choose a game",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedContent(targetState = step, label = "quickPlayStep") { current ->
            when (current) {
                QuickPlayStep.PLAYER_COUNT -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ChoiceRow(
                        icon = Icons.Filled.Person,
                        title = "Solo",
                        subtitle = "Play at your own pace",
                        gradient = theme.brush(),
                    ) {
                        wantsMultiplayer = false
                        step = QuickPlayStep.GAME
                    }
                    ChoiceRow(
                        icon = Icons.Filled.Group,
                        title = "With Others",
                        subtitle = "Live trivia with nearby friends",
                        gradient = MultiplayerGradient,
                    ) {
                        wantsMultiplayer = true
                        step = QuickPlayStep.GAME
                    }
                }

                QuickPlayStep.GAME -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ChoiceRow(
                        icon = Icons.Filled.GridView,
                        title = "Word Search",
                        subtitle = if (wantsMultiplayer) "Multiplayer coming soon" else "Find hidden words in the grid",
                        gradient = WordSearchModeGradient,
                        disabled = wantsMultiplayer,
                        badge = if (wantsMultiplayer) "SOON" else null,
                    ) {
                        onConfirm(QuickPlayChoice.Solo(GameMode.WORD_SEARCH))
                    }
                    ChoiceRow(
                        icon = Icons.AutoMirrored.Filled.Help,
                        title = "Trivia",
                        subtitle = if (wantsMultiplayer) "Host or join nearby players" else "Test your knowledge, beat the clock",
                        gradient = TriviaModeGradient,
                    ) {
                        onConfirm(if (wantsMultiplayer) QuickPlayChoice.MultiplayerTrivia else QuickPlayChoice.Solo(GameMode.TRIVIA))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: Brush,
    disabled: Boolean = false,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = !disabled,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = if (disabled) {
                    Modifier.size(56.dp).background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                } else {
                    Modifier.size(56.dp).background(gradient, RoundedCornerShape(16.dp))
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (badge != null) {
                        Surface(shape = RoundedCornerShape(50), color = Color.Gray.copy(alpha = 0.25f)) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!disabled) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
