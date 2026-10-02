package com.mamatiquest.app.ui.screens.wordsearch

import android.app.Activity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mamatiquest.app.services.AdsManager
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mamatiquest.app.models.GameMode
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.ui.components.ResultOverlay
import com.mamatiquest.app.ui.theme.brush
import com.mamatiquest.app.viewmodels.WordSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSearchScreen(
    theme: GameTheme,
    level: Int,
    onBack: () -> Unit,
    onReplay: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val vm: WordSearchViewModel = viewModel(
        key = "wordsearch-${theme.id}-$level",
        factory = WordSearchViewModel.factory(application, theme, level),
    )

    val activity = context as? Activity
    fun afterGame(navigate: () -> Unit) =
        if (activity != null) AdsManager.afterSoloGame(activity, GameMode.WORD_SEARCH, navigate) else navigate()

    DisposableEffect(vm) {
        vm.startTimer()
        onDispose { vm.stopTimer() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            // Composited onto the scheme background rather than left translucent: a
            // translucent container lets the Activity window show through, and that
            // window is always the light platform theme. In dark mode that produced a
            // near-white board under near-white (onSurface) grid letters.
            containerColor = theme.primaryColor.copy(alpha = 0.08f)
                .compositeOver(MaterialTheme.colorScheme.background),
            // Scaffold defaults contentColor to contentColorFor(containerColor), which
            // only resolves exact scheme colors — a custom container yields Unspecified
            // and every descendant that doesn't set its own color falls back to black.
            contentColor = MaterialTheme.colorScheme.onBackground,
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
                    .padding(top = 8.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TopBar(vm = vm, theme = theme)
                WordBank(vm = vm)
                WordSearchGrid(vm = vm, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }

        if (vm.isComplete) {
            ResultOverlay(
                stars = vm.computeStars(),
                score = vm.foundCount * 100,
                theme = theme,
                level = level,
                mode = GameMode.WORD_SEARCH,
                // Every 10th solo game shows an interstitial before the tapped navigation.
                onReplay = { afterGame(onReplay) },
                onNext = { afterGame(onNext) },
                onHome = { afterGame(onBack) },
            )
        }
    }
}

@Composable
private fun TopBar(vm: WordSearchViewModel, theme: GameTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.padding(end = 2.dp))
            Text(timeString(vm.elapsedSeconds), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }

        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(50), color = Color.Transparent) {
            Box(
                modifier = Modifier
                    .background(theme.brush(), androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("${vm.foundCount}/${vm.totalWords}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
        }

        val rewardedAvailable by AdsManager.rewardedAvailable.collectAsState()
        if (vm.hintsRemaining == 0 && rewardedAvailable) {
            // Out of hints: opt-in rewarded ad for one more. The clock pauses while it plays.
            val activity = LocalContext.current as? Activity
            TextButton(onClick = {
                activity ?: return@TextButton
                vm.stopTimer()
                AdsManager.showRewarded(activity, onReward = { vm.grantHint() }, onFinished = { vm.startTimer() })
            }) {
                Icon(Icons.Filled.OndemandVideo, contentDescription = "Watch an ad for a hint", modifier = Modifier.padding(end = 4.dp))
                Text("+1", fontWeight = FontWeight.SemiBold)
            }
        } else {
            TextButton(onClick = { vm.useHint() }, enabled = vm.hintsRemaining > 0) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("${vm.hintsRemaining}", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WordBank(vm: WordSearchViewModel) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(vm.puzzle.placedWords, key = { it.id }) { placed ->
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                color = if (placed.isFound) Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = placed.word,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (placed.isFound) TextDecoration.LineThrough else null,
                    color = if (placed.isFound) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun timeString(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
