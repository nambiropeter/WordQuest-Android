package com.wordquest.app.ui.screens.multiplayer

import android.Manifest
import android.os.Build

/**
 * Permissions Nearby Connections needs to advertise/discover, split by API
 * level the same way the manifest declares them: Bluetooth permissions are
 * the API 31+ path, location covers API 23-30 (BLE scanning implicitly
 * requires it there even though the app never reads location), and
 * NEARBY_WIFI_DEVICES is the API 33+ Wi-Fi equivalent.
 */
fun requiredNearbyPermissions(): Array<String> = buildList {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        else -> {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}.toTypedArray()
