package com.wordquest.app.ui.screens.levelmap

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordquest.app.data.ProgressStore
import com.wordquest.app.models.GameMode
import com.wordquest.app.models.GameTheme
import com.wordquest.app.models.LevelResult
import com.wordquest.app.services.HapticsManager
import com.wordquest.app.ui.components.StarsView
import com.wordquest.app.ui.theme.brush
import com.wordquest.app.ui.theme.sfSymbolToIcon
import kotlinx.coroutines.launch

/** Android counterpart to iOS's `LevelMapView` — a scrollable 1..1000 level path split into 10 difficulty tiers. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LevelMapScreen(
    mode: GameMode,
    theme: GameTheme,
    onBack: () -> Unit,
    onLevelSelected: (Int) -> Unit,
) {
    val context = LocalContext.current
    val progressStore = remember { ProgressStore.getInstance(context) }
    val wordSearchResults by progressStore.wordSearchResults.collectAsState()
    val triviaResults by progressStore.triviaResults.collectAsState()
    val results = if (mode == GameMode.WORD_SEARCH) wordSearchResults else triviaResults
    val prefix = "${theme.id}#"
    val completed = results.keys.count { it.startsWith(prefix) }
    val stars = results.filterKeys { it.startsWith(prefix) }.values.sumOf { it.stars }

    val bandCount = GameMode.TIER_COUNT
    val bandSize = GameMode.TIER_SIZE
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Item layout: [0] header, [1] tier-jump bar, then per band: 1 sticky header + 20 rows of 5 levels.
    val rowsPerBand = bandSize / 5
    fun bandHeaderIndex(band: Int) = 2 + band * (1 + rowsPerBand)

    val resumeLevel = remember(results) { progressStore.furthestReachedLevel(mode, theme) }
    val resumeBand = (resumeLevel - 1) / bandSize

    LaunchedEffect(theme.id, mode) {
        listState.scrollToItem(bandHeaderIndex(resumeBand))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(theme.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.padding(padding)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(theme.brush(), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(sfSymbolToIcon(theme.icon), contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("${mode.displayName} · ${theme.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$stars stars · $completed/${GameMode.LEVEL_COUNT} complete",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        "JUMP TO A DIFFICULTY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(bandCount) { band ->
                            Surface(
                                onClick = {
                                    scope.launch { listState.animateScrollToItem(bandHeaderIndex(band)) }
                                },
                                color = theme.primaryColor.copy(alpha = 0.15f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                            ) {
                                Text(
                                    "Tier ${band + 1}",
                                    color = theme.primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            for (band in 0 until bandCount) {
                val startLevel = band * bandSize + 1
                val endLevel = minOf(startLevel + bandSize - 1, GameMode.LEVEL_COUNT)

                stickyHeader {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Difficulty Tier ${band + 1}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "$startLevel–$endLevel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                val levels = (startLevel..endLevel).toList()
                items(levels.chunked(5)) { rowLevels ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowLevels.forEach { level ->
                            val isUnlocked = progressStore.isUnlocked(level, mode, theme)
                            val result = results["${theme.id}#$level"]
                            Box(modifier = Modifier.weight(1f)) {
                                LevelButton(
                                    level = level,
                                    theme = theme,
                                    isUnlocked = isUnlocked,
                                    result = result,
                                    onClick = {
                                        HapticsManager.light()
                                        onLevelSelected(level)
                                    },
                                )
                            }
                        }
                        repeat(5 - rowLevels.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun LevelButton(
    level: Int,
    theme: GameTheme,
    isUnlocked: Boolean,
    result: LevelResult?,
    onClick: () -> Unit,
) {
    val label = if (isUnlocked) {
        result?.let { "Level $level, ${it.stars} out of 3 stars" } ?: "Level $level, not yet played"
    } else {
        "Level $level, locked"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isUnlocked) theme.brush() else androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.25f)),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                onClick = onClick,
                enabled = isUnlocked,
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isUnlocked) {
                        Text("$level", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (result != null) {
            StarsView(filled = result.stars, size = 8.dp)
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
