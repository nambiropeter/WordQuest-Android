package com.mamatiquest.app.ui.screens.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mamatiquest.app.data.ContentStore
import com.mamatiquest.app.data.ProgressStore
import com.mamatiquest.app.models.GameMode
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.services.HapticsManager
import com.mamatiquest.app.ui.theme.brush
import com.mamatiquest.app.ui.theme.sfSymbolToIcon

/** Android counterpart to iOS's `ThemeSelectView`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectScreen(
    mode: GameMode,
    onBack: () -> Unit,
    onThemeSelected: (GameTheme) -> Unit,
) {
    val context = LocalContext.current
    val contentStore = remember { ContentStore.getInstance(context) }
    val progressStore = remember { ProgressStore.getInstance(context) }
    val wordSearchResults by progressStore.wordSearchResults.collectAsState()
    val triviaResults by progressStore.triviaResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose a Theme", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding),
        ) {
            items(contentStore.themes) { theme ->
                val results = if (mode == GameMode.WORD_SEARCH) wordSearchResults else triviaResults
                val prefix = "${theme.id}#"
                val completed = results.keys.count { it.startsWith(prefix) }
                val stars = results.filterKeys { it.startsWith(prefix) }.values.sumOf { it.stars }

                ThemeCard(
                    theme = theme,
                    completed = completed,
                    stars = stars,
                    onClick = {
                        HapticsManager.light()
                        onThemeSelected(theme)
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: GameTheme, completed: Int, stars: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(theme.brush(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(sfSymbolToIcon(theme.icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Text(theme.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            if (completed > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                    Text(
                        "$stars · $completed/${GameMode.LEVEL_COUNT}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    "Start playing",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
