package com.wordquest.app.ui.screens.trivia

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordquest.app.models.GameMode
import com.wordquest.app.models.GameTheme
import com.wordquest.app.ui.components.ResultOverlay
import com.wordquest.app.ui.theme.brush
import com.wordquest.app.viewmodels.AnswerState
import com.wordquest.app.viewmodels.TriviaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriviaScreen(
    theme: GameTheme,
    level: Int,
    onBack: () -> Unit,
    onReplay: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val vm: TriviaViewModel = viewModel(
        key = "trivia-${theme.id}-$level",
        factory = TriviaViewModel.factory(application, theme, level),
    )

    DisposableEffect(vm) {
        vm.startQuestionTimer()
        onDispose { vm.stopTimer() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = theme.primaryColor.copy(alpha = 0.06f),
            topBar = {
                TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Level $level", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(theme.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                TriviaTopBar(vm = vm, theme = theme)
                TimerBar(vm = vm, theme = theme)
                ProgressDots(vm = vm, theme = theme)
                QuestionCard(vm = vm)

                if (vm.answerState != AnswerState.UNANSWERED) {
                    Button(
                        onClick = { vm.advance() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(theme.brush(), RoundedCornerShape(16.dp))
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (vm.isLastQuestion) "Finish" else "Next Question",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        if (vm.isComplete) {
            ResultOverlay(
                stars = vm.computeStars(),
                score = vm.score,
                theme = theme,
                level = level,
                mode = GameMode.TRIVIA,
                onReplay = onReplay,
                onNext = onNext,
                onHome = onBack,
            )
        }
    }
}

@Composable
private fun TriviaTopBar(vm: TriviaViewModel, theme: GameTheme) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(18.dp))
            Text("${vm.score}", fontWeight = FontWeight.Bold, color = theme.primaryColor)
        }

        if (vm.streak > 1) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Whatshot, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                Text("${vm.streak}", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = { vm.useFiftyFifty() },
            enabled = !vm.fiftyFiftyUsed && vm.answerState == AnswerState.UNANSWERED,
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                containerColor = if (vm.fiftyFiftyUsed) Color.Gray.copy(alpha = 0.2f) else theme.primaryColor.copy(alpha = 0.2f),
                contentColor = if (vm.fiftyFiftyUsed) MaterialTheme.colorScheme.onSurfaceVariant else theme.primaryColor,
            ),
            shape = RoundedCornerShape(50),
        ) {
            Text("50:50", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TimerBar(vm: TriviaViewModel, theme: GameTheme) {
    val fraction = vm.timeRemaining.toFloat() / maxOf(vm.triviaLevel.timePerQuestion, 1).toFloat()
    val animatedFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(900), label = "timerFraction")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Gray.copy(alpha = 0.15f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                .height(8.dp)
                .background(if (vm.timeRemaining <= 3) Color.Red else theme.primaryColor, RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun ProgressDots(vm: TriviaViewModel, theme: GameTheme) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(vm.totalQuestions) { i ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        when {
                            i < vm.currentIndex -> theme.primaryColor
                            i == vm.currentIndex -> theme.secondaryColor
                            else -> Color.Gray.copy(alpha = 0.2f)
                        },
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun QuestionCard(vm: TriviaViewModel) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Question ${vm.currentIndex + 1} of ${vm.totalQuestions}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                vm.currentQuestion.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                vm.currentQuestion.options.forEachIndexed { index, option ->
                    OptionButton(vm = vm, index = index, option = option)
                }
            }
        }
    }
}

@Composable
private fun OptionButton(vm: TriviaViewModel, index: Int, option: String) {
    val isEliminated = vm.eliminatedOptions.contains(index)
    val isSelected = vm.selectedIndex == index
    val isCorrectAnswer = index == vm.currentQuestion.correctIndex
    val answered = vm.answerState != AnswerState.UNANSWERED

    val background = when {
        !answered -> MaterialTheme.colorScheme.surface
        isCorrectAnswer -> Color(0xFF4CAF50).copy(alpha = 0.25f)
        isSelected -> Color(0xFFF44336).copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when {
        !answered -> Color.Transparent
        isCorrectAnswer -> Color(0xFF4CAF50)
        isSelected -> Color(0xFFF44336)
        else -> Color.Transparent
    }

    Surface(
        onClick = { vm.selectAnswer(index) },
        enabled = !answered && !isEliminated,
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = androidx.compose.foundation.BorderStroke(2.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                option,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                color = if (isEliminated) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
            )
            if (answered && isCorrectAnswer) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
            } else if (answered && isSelected) {
                Icon(Icons.Filled.Cancel, contentDescription = null, tint = Color(0xFFF44336))
            }
        }
    }
}
