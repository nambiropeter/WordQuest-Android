package com.mamatiquest.app.ui.screens.home

import com.mamatiquest.app.ui.components.NativeAdCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mamatiquest.app.data.ContentStore
import com.mamatiquest.app.data.HouseAdProvider
import com.mamatiquest.app.data.ProgressStore
import com.mamatiquest.app.models.GameMode
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.models.HouseAdAction
import com.mamatiquest.app.services.HapticsManager
import com.mamatiquest.app.services.SettingsStore
import com.mamatiquest.app.ui.theme.MultiplayerGradient
import com.mamatiquest.app.ui.theme.TriviaModeGradient
import com.mamatiquest.app.ui.theme.WordSearchModeGradient
import com.mamatiquest.app.ui.theme.brush
import com.mamatiquest.app.ui.theme.sfSymbolToIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateThemeSelect: (GameMode) -> Unit,
    onNavigateMultiplayer: (GameTheme?) -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateLevelMap: (GameMode, GameTheme) -> Unit,
) {
    val context = LocalContext.current
    val contentStore = remember { ContentStore.getInstance(context) }
    val progressStore = remember { ProgressStore.getInstance(context) }
    val settingsStore = remember { SettingsStore.getInstance(context) }

    val showSuggestions by settingsStore.showSuggestions.collectAsState()

    val wordSearchResults by progressStore.wordSearchResults.collectAsState()
    val triviaResults by progressStore.triviaResults.collectAsState()
    val totalCoins by progressStore.totalCoins.collectAsState()
    val totalStars = wordSearchResults.values.sumOf { it.stars } + triviaResults.values.sumOf { it.stars }
    val levelsCompleted = wordSearchResults.size + triviaResults.size

    var quickPlayTheme by remember { mutableStateOf<GameTheme?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WordQuest", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            StatsHeader(stars = totalStars, coins = totalCoins, levelsCompleted = levelsCompleted)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ModeCard(
                    title = GameMode.WORD_SEARCH.displayName,
                    subtitle = "Find hidden words in a themed grid",
                    icon = sfSymbolToIcon(GameMode.WORD_SEARCH.icon),
                    gradient = WordSearchModeGradient,
                ) {
                    HapticsManager.light()
                    onNavigateThemeSelect(GameMode.WORD_SEARCH)
                }
                ModeCard(
                    title = GameMode.TRIVIA.displayName,
                    subtitle = "Test your knowledge, beat the clock",
                    icon = sfSymbolToIcon(GameMode.TRIVIA.icon),
                    gradient = TriviaModeGradient,
                ) {
                    HapticsManager.light()
                    onNavigateThemeSelect(GameMode.TRIVIA)
                }
                ModeCard(
                    title = "Local Multiplayer",
                    subtitle = "Live trivia with up to 8 nearby friends — no internet needed",
                    icon = Icons.Filled.SettingsInputAntenna,
                    gradient = MultiplayerGradient,
                    showLevelCaption = false,
                ) {
                    HapticsManager.light()
                    onNavigateMultiplayer(null)
                }
            }

            ThemeStrip(
                themes = contentStore.themes,
                onThemeClick = {
                    HapticsManager.light()
                    quickPlayTheme = it
                },
            )

            if (showSuggestions) {
                val houseAd = remember(levelsCompleted) {
                    HouseAdProvider.recommendation(contentStore, progressStore)
                }
                SuggestedCard(
                    houseAd = houseAd,
                    onOpenMultiplayer = { onNavigateMultiplayer(null) },
                    onOpenTheme = { quickPlayTheme = it },
                )
            }

            NativeAdCard()

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    val theme = quickPlayTheme
    if (theme != null) {
        ModalBottomSheet(onDismissRequest = { quickPlayTheme = null }, sheetState = sheetState) {
            ThemeQuickPlaySheetContent(theme = theme) { choice ->
                quickPlayTheme = null
                when (choice) {
                    is QuickPlayChoice.Solo -> onNavigateLevelMap(choice.mode, theme)
                    QuickPlayChoice.MultiplayerTrivia -> onNavigateMultiplayer(theme)
                }
            }
        }
    }
}

@Composable
private fun StatsHeader(stars: Int, coins: Int, levelsCompleted: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatChip(icon = Icons.Filled.Star, tint = Color(0xFFFFC107), value = "$stars")
        StatChip(icon = Icons.Filled.MonetizationOn, tint = Color(0xFFFF9800), value = "$coins")
        StatChip(icon = Icons.Filled.Verified, tint = Color(0xFF4CAF50), value = "$levelsCompleted")
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, value: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), contentColor = MaterialTheme.colorScheme.onSurfaceVariant) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    showLevelCaption: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                if (showLevelCaption) {
                    Text(
                        "8 themes · 1,000 levels each",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun ThemeStrip(themes: List<GameTheme>, onThemeClick: (GameTheme) -> Unit) {
    Column {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Jump Into a Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap one to play solo or with friends nearby",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(themes) { theme ->
                ThemeCard(theme = theme, onClick = { onThemeClick(theme) })
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: GameTheme, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 128.dp, height = 140.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(theme.brush())
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color.White.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(sfSymbolToIcon(theme.icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            Text(
                theme.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Play", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

/** First-party "suggested for you" content card — recommends an unexplored theme or local multiplayer. */
@Composable
private fun SuggestedCard(
    houseAd: com.mamatiquest.app.models.HouseAd,
    onOpenMultiplayer: () -> Unit,
    onOpenTheme: (GameTheme) -> Unit,
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        HouseAdCard(houseAd = houseAd, onOpenMultiplayer = onOpenMultiplayer, onOpenTheme = onOpenTheme)
    }
}

@Composable
private fun HouseAdCard(
    houseAd: com.mamatiquest.app.models.HouseAd,
    onOpenMultiplayer: () -> Unit,
    onOpenTheme: (GameTheme) -> Unit,
) {
    Surface(
        onClick = {
            HapticsManager.light()
            when (val action = houseAd.action) {
                HouseAdAction.OpenMultiplayer -> onOpenMultiplayer()
                is HouseAdAction.OpenTheme -> onOpenTheme(action.theme)
            }
        },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Brush.linearGradient(houseAd.gradientColors), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(sfSymbolToIcon(houseAd.icon), contentDescription = null, tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(houseAd.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        houseAd.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Text(
                    "Suggested",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
    }
}
