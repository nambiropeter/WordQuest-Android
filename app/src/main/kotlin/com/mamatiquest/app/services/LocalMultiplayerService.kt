package com.mamatiquest.app.services

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.mamatiquest.app.models.MultiplayerMessage
import com.mamatiquest.app.models.WireFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Android counterpart to iOS's `MultiplayerService`. Thin transport layer —
 * knows nothing about trivia rules, just discovery, connection, and
 * sending/receiving [MultiplayerMessage]s.
 *
 * Both platforms speak the same open protocol so iPhones and Android phones
 * can play together: the host advertises a Bonjour/DNS-SD service of type
 * [SERVICE_TYPE] (via [NsdManager] here, `NWListener` on iOS) and accepts plain
 * TCP connections; every message is one line of UTF-8 JSON terminated by `\n`.
 * Players therefore need to be on the same Wi-Fi network (or one phone's
 * hotspot) — the platform-specific Bluetooth stacks (Nearby Connections,
 * MultipeerConnectivity) can't talk to each other.
 */
class LocalMultiplayerService(context: Context, private val localName: String) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val connections = ConcurrentHashMap<String, PeerConnection>()
    private val foundServices = ConcurrentHashMap<String, NsdServiceInfo>()
    private var serverSocket: ServerSocket? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _connectedEndpointIds = MutableStateFlow<List<String>>(emptyList())
    val connectedEndpointIds: StateFlow<List<String>> = _connectedEndpointIds.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts.asStateFlow()

    var onReceive: ((MultiplayerMessage, endpointId: String) -> Unit)? = null
    var onPeerConnected: ((endpointId: String) -> Unit)? = null
    var onPeerDisconnected: ((endpointId: String) -> Unit)? = null

    data class DiscoveredHost(val endpointId: String, val name: String)

    private inner class PeerConnection(val id: String, val socket: Socket) {
        private val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

        fun startReading() = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    val message = runCatching { WireFormat.json.decodeFromString(MultiplayerMessage.serializer(), line) }
                        .onFailure { Log.w(TAG, "Dropping undecodable message: ${it.message}") }
                        .getOrNull() ?: continue
                    mainHandler.post { onReceive?.invoke(message, id) }
                }
            } catch (e: Exception) {
                Log.i(TAG, "Connection $id closed: ${e.message}")
            }
            close()
        }

        fun write(line: String) {
            try {
                synchronized(writer) {
                    writer.write(line)
                    writer.write("\n")
                    writer.flush()
                }
            } catch (e: Exception) {
                Log.w(TAG, "send to $id failed: ${e.message}")
                close()
            }
        }

        fun close() {
            runCatching { socket.close() }
            if (connections.remove(id) != null) {
                _connectedEndpointIds.value = connections.keys.toList()
                mainHandler.post { onPeerDisconnected?.invoke(id) }
            }
        }
    }

    // region Host

    fun startHosting() {
        acquireMulticastLock()
        scope.launch {
            try {
                val server = ServerSocket(0)
                serverSocket = server
                mainHandler.post { registerService(server.localPort) }
                while (!server.isClosed) {
                    val socket = server.accept()
                    // +1 accounts for the host itself — mirrors iOS's room check.
                    if (connections.size + 1 >= MAX_PEERS) {
                        runCatching { socket.close() }
                        continue
                    }
                    addConnection(UUID.randomUUID().toString(), socket)
                }
            } catch (e: Exception) {
                Log.i(TAG, "Host socket closed: ${e.message}")
            }
        }
    }

    private fun registerService(port: Int) {
        val info = NsdServiceInfo().apply {
            serviceName = localName
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD registration failed: $errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
        }
        registrationListener = listener
        nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stopHosting() {
        registrationListener?.let { runCatching { nsdManager.unregisterService(it) } }
        registrationListener = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    // endregion

    // region Guest

    fun startDiscovery() {
        acquireMulticastLock()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "NSD discovery failed to start: $errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit

            override fun onServiceFound(info: NsdServiceInfo) {
                val name = unescapeServiceName(info.serviceName)
                foundServices[name] = info
                if (_discoveredHosts.value.none { it.endpointId == name }) {
                    _discoveredHosts.value = _discoveredHosts.value + DiscoveredHost(name, name)
                }
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                val name = unescapeServiceName(info.serviceName)
                foundServices.remove(name)
                _discoveredHosts.value = _discoveredHosts.value.filterNot { it.endpointId == name }
            }
        }
        discoveryListener = listener
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stopDiscovery() {
        discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        discoveryListener = null
        foundServices.clear()
        _discoveredHosts.value = emptyList()
    }

    /** Resolves the host's address and port, then opens the TCP connection. */
    @Suppress("DEPRECATION")
    fun requestConnection(endpointId: String) {
        val info = foundServices[endpointId] ?: return
        nsdManager.resolveService(info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD resolve failed: $errorCode")
            }

            override fun onServiceResolved(resolved: NsdServiceInfo) {
                val address: InetAddress? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    resolved.hostAddresses.firstOrNull()
                } else {
                    resolved.host
                }
                if (address == null) {
                    Log.w(TAG, "Resolved $endpointId without an address")
                    return
                }
                scope.launch {
                    try {
                        addConnection(endpointId, Socket(address, resolved.port))
                    } catch (e: Exception) {
                        Log.w(TAG, "Connecting to $endpointId failed: ${e.message}")
                    }
                }
            }
        })
    }

    // endregion

    private fun addConnection(id: String, socket: Socket) {
        socket.tcpNoDelay = true
        val connection = PeerConnection(id, socket)
        connections[id] = connection
        _connectedEndpointIds.value = connections.keys.toList()
        connection.startReading()
        mainHandler.post { onPeerConnected?.invoke(id) }
    }

    fun send(message: MultiplayerMessage, to: List<String>? = null) {
        val targets = (to ?: connections.keys.toList()).mapNotNull { connections[it] }
        if (targets.isEmpty()) return
        val line = WireFormat.json.encodeToString(MultiplayerMessage.serializer(), message)
        scope.launch { targets.forEach { it.write(line) } }
    }

    fun disconnect() {
        stopHosting()
        stopDiscovery()
        connections.values.toList().forEach { runCatching { it.socket.close() } }
        connections.clear()
        _connectedEndpointIds.value = emptyList()
        multicastLock?.let { if (it.isHeld) it.release() }
        multicastLock = null
        scope.cancel()
    }

    /** Many Wi-Fi drivers drop multicast (and therefore mDNS) packets unless an app holds this lock. */
    private fun acquireMulticastLock() {
        if (multicastLock != null) return
        multicastLock = wifiManager.createMulticastLock("wq-trivia").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    /** Some Android versions return DNS-SD names with decimal escapes, e.g. `Pixel\0328#ab12` for a space. */
    private fun unescapeServiceName(name: String): String =
        Regex("""\\(\d{3})""").replace(name) { it.groupValues[1].toInt().toChar().toString() }

    companion object {
        private const val TAG = "LocalMultiplayer"

        /** Must match `MultiplayerService.serviceType` on iOS and `NSBonjourServices` in its Info.plist. */
        const val SERVICE_TYPE = "_wq-trivia._tcp"

        /** Host + guests — mirrors iOS's `MultiplayerService.maxPeers`. */
        const val MAX_PEERS = 8
    }
}
