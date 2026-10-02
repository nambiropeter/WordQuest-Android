package com.mamatiquest.app.ui.screens.multiplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.services.LocalMultiplayerService
import com.mamatiquest.app.viewmodels.MultiplayerPhase
import com.mamatiquest.app.viewmodels.MultiplayerViewModel

/**
 * Android counterpart to iOS's `MultiplayerLobbyView` — swaps between
 * role-select, browsing, room, in-game, and results content by [vm.phase],
 * the same state machine the Swift original drives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerLobbyScreen(initialTheme: GameTheme?, onBack: () -> Unit) {
    val vm: MultiplayerViewModel = viewModel()
    var appliedInitialTheme by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialTheme) {
        if (!appliedInitialTheme && initialTheme != null) {
            appliedInitialTheme = true
            vm.selectedTheme = initialTheme
        }
    }

    fun leaveAndPop() {
        vm.leaveGame()
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose { if (vm.phase != MultiplayerPhase.ROLE_SELECT) vm.leaveGame() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Multiplayer", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { leaveAndPop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (vm.phase != MultiplayerPhase.ROLE_SELECT) {
                        androidx.compose.material3.TextButton(onClick = { leaveAndPop() }) {
                            Text("Leave", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (vm.phase) {
                MultiplayerPhase.ROLE_SELECT -> RoleSelectContent(vm)
                MultiplayerPhase.BROWSING -> BrowsingContent(vm)
                MultiplayerPhase.LOBBY -> MultiplayerRoomScreen(vm)
                MultiplayerPhase.PLAYING -> MultiplayerGameScreen(vm)
                MultiplayerPhase.RESULTS -> MultiplayerResultsScreen(vm, onLeave = { leaveAndPop() })
            }
        }
    }
}

@Composable
private fun RoleSelectContent(vm: MultiplayerViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            Icons.Filled.SettingsInputAntenna,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(46.dp),
        )
        Text(
            "Play trivia with friends nearby",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            "No internet needed — everyone joins the same Wi-Fi network (or one phone's hotspot). " +
                "Works between iPhone and Android. Up to ${LocalMultiplayerService.MAX_PEERS} players per game.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("YOUR NAME", style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = vm.playerName,
                onValueChange = { vm.updatePlayerName(it) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(
                onClick = { vm.startHosting() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.WifiTethering, contentDescription = null)
                Spacer(modifier = Modifier.height(0.dp).size(8.dp))
                Text("Host a Game", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { vm.startBrowsing() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Spacer(modifier = Modifier.height(0.dp).size(8.dp))
                Text("Join a Game", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BrowsingContent(vm: MultiplayerViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Searching for nearby games…",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (vm.discoveredHosts.isEmpty()) {
            Text(
                "Make sure a friend has tapped \"Host a Game\" and that you're both on the same Wi-Fi network or hotspot.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(vm.discoveredHosts) { host ->
                    Surface(
                        onClick = { vm.joinHost(host) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Filled.WifiTethering, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                host.name.substringBefore("#"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            if (vm.connectingToHostId == host.endpointId) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
