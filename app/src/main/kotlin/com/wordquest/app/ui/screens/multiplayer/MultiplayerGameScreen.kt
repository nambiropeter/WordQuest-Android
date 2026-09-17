package com.wordquest.app.ui.screens.multiplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wordquest.app.models.NetworkQuestion
import com.wordquest.app.models.PlayerScore
import com.wordquest.app.viewmodels.MultiplayerViewModel

/** Android counterpart to iOS's `MultiplayerGameView`. */
@Composable
fun MultiplayerGameScreen(vm: MultiplayerViewModel) {
    val theme = vm.themeForGame ?: MultiplayerViewModel.MIXED_THEME

    Box(modifier = Modifier.fillMaxSize().background(theme.primaryColor.copy(alpha = 0.06f))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LeaderboardStrip(vm, theme.primaryColor)
            TimerBar(vm, theme.primaryColor)
            Text(
                "Question ${vm.currentIndex + 1} of ${vm.totalQuestions} · ${theme.name}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val question = vm.currentQuestion
            if (question != null) {
                QuestionCard(vm, question)
            } else {
                CircularProgressIndicator()
            }

            if (vm.hasAnswered && vm.revealedCorrectIndex == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                    Text(
                        "Waiting for other players…",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardStrip(vm: MultiplayerViewModel, tint: Color) {
    val topScores = if (vm.scores.isEmpty()) {
        vm.players.map { PlayerScore(it.id, it.name, 0, 0) }
    } else {
        vm.scores
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        topScores.forEach { entry ->
            Surface(
                shape = RoundedCornerShape(50),
                color = if (entry.id == vm.myId) tint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(entry.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${entry.score}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = tint)
                }
            }
        }
    }
}

@Composable
private fun TimerBar(vm: MultiplayerViewModel, tint: Color) {
    val fraction = vm.timeRemaining.toFloat() / maxOf(vm.timePerQuestion, 1).toFloat()
    Box(
        modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(50)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .background(if (vm.timeRemaining <= 3) Color.Red else tint, RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun QuestionCard(vm: MultiplayerViewModel, question: NetworkQuestion) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                question.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                question.options.forEachIndexed { index, option ->
                    OptionButton(vm, index, option)
                }
            }
        }
    }
}

@Composable
private fun OptionButton(vm: MultiplayerViewModel, index: Int, option: String) {
    val isSelected = vm.selectedAnswer == index
    val revealed = vm.revealedCorrectIndex != null
    val isCorrectAnswer = revealed && index == vm.revealedCorrectIndex

    val background = when {
        !revealed -> if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        isCorrectAnswer -> Color(0xFF4CAF50).copy(alpha = 0.25f)
        isSelected -> Color(0xFFF44336).copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        onClick = { vm.selectAnswer(index) },
        enabled = !vm.hasAnswered,
        shape = RoundedCornerShape(14.dp),
        color = background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(option, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (revealed && isCorrectAnswer) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
            } else if (revealed && isSelected) {
                Icon(Icons.Filled.Cancel, contentDescription = null, tint = Color(0xFFF44336))
            }
        }
    }
}
