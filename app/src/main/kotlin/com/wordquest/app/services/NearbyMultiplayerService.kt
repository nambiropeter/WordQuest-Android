package com.wordquest.app.services

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.wordquest.app.models.MultiplayerMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Android counterpart to iOS's `MultiplayerService`. Thin transport layer
 * over the Nearby Connections API — knows nothing about trivia rules, just
 * discovery, connection, and sending/receiving serialized [MultiplayerMessage]
 * payloads. `Strategy.P2P_STAR` mirrors the host/guest topology this app
 * actually uses (one advertiser = the host, up to [MAX_PEERS] discoverers
 * connect to it — a discoverer can only ever be connected to one advertiser
 * at a time), the same shape MultipeerConnectivity's session gives iOS,
 * though Nearby Connections models it as an explicit constraint rather than
 * an implicit one. Nearby Connections transparently uses Bluetooth, BLE, or
 * local Wi-Fi, whichever link is available between the devices — the app
 * doesn't choose a transport itself, matching iOS's MultipeerConnectivity.
 */
class NearbyMultiplayerService(context: Context, private val localEndpointName: String) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context.applicationContext)
    private val json = Json { ignoreUnknownKeys = true }
    private val endpointNames = mutableMapOf<String, String>()

    private val _connectedEndpointIds = MutableStateFlow<List<String>>(emptyList())
    val connectedEndpointIds: StateFlow<List<String>> = _connectedEndpointIds.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts.asStateFlow()

    var onReceive: ((MultiplayerMessage, endpointId: String) -> Unit)? = null
    var onPeerConnected: ((endpointId: String, name: String) -> Unit)? = null
    var onPeerDisconnected: ((endpointId: String) -> Unit)? = null

    data class DiscoveredHost(val endpointId: String, val name: String)

    fun startHosting() {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.startAdvertising(localEndpointName, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnFailureListener { Log.w(TAG, "startAdvertising failed: ${it.message}") }
    }

    fun stopHosting() {
        connectionsClient.stopAdvertising()
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnFailureListener { Log.w(TAG, "startDiscovery failed: ${it.message}") }
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _discoveredHosts.value = emptyList()
    }

    fun requestConnection(endpointId: String) {
        connectionsClient.requestConnection(localEndpointName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { Log.w(TAG, "requestConnection failed: ${it.message}") }
    }

    fun send(message: MultiplayerMessage, to: List<String>? = null) {
        val targets = to ?: _connectedEndpointIds.value
        if (targets.isEmpty()) return
        val bytes = json.encodeToString(MultiplayerMessage.serializer(), message).encodeToByteArray()
        connectionsClient.sendPayload(targets, Payload.fromBytes(bytes))
    }

    fun disconnect() {
        connectionsClient.stopAllEndpoints()
        stopAdvertisingAndDiscovery()
        _connectedEndpointIds.value = emptyList()
        endpointNames.clear()
    }

    private fun stopAdvertisingAndDiscovery() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        _discoveredHosts.value = emptyList()
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            endpointNames[endpointId] = info.endpointName
            // +1 accounts for the host itself, which isn't in connectedEndpointIds —
            // mirrors iOS's `connectedPeers.count + 1 < maxPeers` room check.
            val hasRoom = _connectedEndpointIds.value.size + 1 < MAX_PEERS
            if (hasRoom) {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            } else {
                connectionsClient.rejectConnection(endpointId)
            }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                if (!_connectedEndpointIds.value.contains(endpointId)) {
                    _connectedEndpointIds.value = _connectedEndpointIds.value + endpointId
                }
                onPeerConnected?.invoke(endpointId, endpointNames[endpointId] ?: endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectedEndpointIds.value = _connectedEndpointIds.value - endpointId
            onPeerDisconnected?.invoke(endpointId)
        }
    }

    private val endpointDiscoveryCallback = object : com.google.android.gms.nearby.connection.EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            endpointNames[endpointId] = info.endpointName
            if (_discoveredHosts.value.none { it.endpointId == endpointId }) {
                _discoveredHosts.value = _discoveredHosts.value + DiscoveredHost(endpointId, info.endpointName)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            _discoveredHosts.value = _discoveredHosts.value.filterNot { it.endpointId == endpointId }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val bytes = payload.asBytes() ?: return
            val message = runCatching {
                json.decodeFromString(MultiplayerMessage.serializer(), bytes.decodeToString())
            }.getOrNull() ?: return
            onReceive?.invoke(message, endpointId)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    companion object {
        private const val TAG = "NearbyMultiplayer"
        const val SERVICE_ID = "com.wordquest.app.trivia"

        /** Reliable ceiling for a P2P_STAR session (host + guests) — mirrors iOS's MultipeerConnectivity guidance. */
        const val MAX_PEERS = 8
    }
}
