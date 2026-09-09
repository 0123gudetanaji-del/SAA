package com.example.data.sync

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.PartEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.SyncConflictEntity
import com.example.data.model.SyncStatus
import com.example.data.model.TrustedDeviceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

class LocalNetworkSyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "SAASync"
        const val BROADCAST_PORT = 18888
        const val SYNC_PORT = 18889
        const val DEFAULT_PAIRING_KEY = "SAA-AMARDEVI-2026"
    }

    private val prefs = context.getSharedPreferences("saa_sync_prefs", Context.MODE_PRIVATE)

    val deviceId: String = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("device_id", it).apply()
    }

    private val _deviceName = MutableStateFlow(
        prefs.getString("device_name", "SAA-Mobile-" + deviceId.take(4).uppercase()) ?: "SAA-Mobile"
    )
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _pairingKey = MutableStateFlow(
        prefs.getString("pairing_key", DEFAULT_PAIRING_KEY) ?: DEFAULT_PAIRING_KEY
    )
    val pairingKey: StateFlow<String> = _pairingKey.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.WAITING_FOR_DEVICE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(prefs.getLong("last_sync_time", 0L))
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _localIpAddress = MutableStateFlow("127.0.0.1")
    val localIpAddress: StateFlow<String> = _localIpAddress.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(prefs.getBoolean("auto_sync_enabled", true))
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private var serverJob: Job? = null
    private var discoveryListenerJob: Job? = null
    private var beaconSenderJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    init {
        updateLocalIp()
        startSyncService()
    }

    fun setDeviceName(name: String) {
        _deviceName.value = name
        prefs.edit().putString("device_name", name).apply()
    }

    fun setPairingKey(key: String) {
        _pairingKey.value = key
        prefs.edit().putString("pairing_key", key).apply()
    }

    fun setAutoSync(enabled: Boolean) {
        _isAutoSyncEnabled.value = enabled
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
        if (enabled) {
            startSyncService()
        }
    }

    fun updateLocalIp(): String {
        val ip = getDeviceLocalIp()
        _localIpAddress.value = ip
        return ip
    }

    private fun startSyncService() {
        acquireMulticastLock()
        startTcpServer()
        startUdpDiscoveryListener()
        startBeaconSender()
    }

    private fun acquireMulticastLock() {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("saa_multicast_lock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Multicast lock error: ${e.message}")
        }
    }

    private fun startTcpServer() {
        serverJob?.cancel()
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket(SYNC_PORT)
                Log.d(TAG, "TCP Sync Server listening on port $SYNC_PORT")

                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch(Dispatchers.IO) {
                            handleClientConnection(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                        Log.w(TAG, "Server accept error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start TCP server: ${e.message}")
            }
        }
    }

    private suspend fun handleClientConnection(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            val requestJson = reader.readLine()
            if (requestJson != null) {
                val incomingPacket = SyncPacket.fromJson(requestJson)
                
                // Validate pairing key
                if (incomingPacket.pairingKey != _pairingKey.value && incomingPacket.pairingKey != DEFAULT_PAIRING_KEY) {
                    val rej = JSONObject().apply {
                        put("status", "REJECTED_UNAUTHORIZED")
                        put("message", "Pairing key mismatch")
                    }
                    writer.println(rej.toString())
                    socket.close()
                    return
                }

                // Process incoming updates
                _syncStatus.value = SyncStatus.SYNCING
                val conflictsCount = reconcileIncomingData(incomingPacket)

                // Fetch local records modified since remote device's lastSyncTimestamp
                val localChanges = getLocalChangesSince(incomingPacket.sinceTimestamp)
                val responsePacket = SyncPacket(
                    action = "SYNC_RESPONSE",
                    deviceId = deviceId,
                    deviceName = _deviceName.value,
                    pairingKey = _pairingKey.value,
                    sinceTimestamp = System.currentTimeMillis(),
                    parts = localChanges.parts,
                    transactions = localChanges.transactions,
                    suppliers = localChanges.suppliers
                )

                writer.println(responsePacket.toJson())

                val now = System.currentTimeMillis()
                _lastSyncTimestamp.value = now
                prefs.edit().putLong("last_sync_time", now).apply()

                if (conflictsCount > 0) {
                    _syncStatus.value = SyncStatus.CONFLICT
                } else {
                    _syncStatus.value = SyncStatus.SYNCED
                }

                // Update trusted device table
                database.syncDao().upsertTrustedDevice(
                    TrustedDeviceEntity(
                        deviceId = incomingPacket.deviceId,
                        deviceName = incomingPacket.deviceName,
                        ipAddress = socket.inetAddress.hostAddress ?: "",
                        port = SYNC_PORT,
                        lastSyncTimestamp = now,
                        isTrusted = true,
                        pairingKey = incomingPacket.pairingKey
                    )
                )

                // Update discovered device status
                updateDeviceSyncState(incomingPacket.deviceId, true, now)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Handle client error: ${e.message}")
            _syncStatus.value = SyncStatus.FAILED
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    private fun startUdpDiscoveryListener() {
        discoveryListenerJob?.cancel()
        discoveryListenerJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(BROADCAST_PORT))
                }
                val buffer = ByteArray(2048)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val jsonStr = String(packet.data, 0, packet.length)
                    val senderIp = packet.address.hostAddress ?: continue

                    // Ignore self broadcasts
                    if (senderIp == _localIpAddress.value) continue

                    try {
                        val obj = JSONObject(jsonStr)
                        if (obj.optString("magic") == "SAA_INVENTORY") {
                            val remoteDeviceId = obj.optString("deviceId")
                            if (remoteDeviceId == deviceId) continue

                            val remoteDeviceName = obj.optString("deviceName")
                            val remotePort = obj.optInt("port", SYNC_PORT)

                            addOrUpdateDiscoveredDevice(
                                DiscoveredDevice(
                                    deviceId = remoteDeviceId,
                                    deviceName = remoteDeviceName,
                                    ipAddress = senderIp,
                                    port = remotePort,
                                    lastSeenTimestamp = System.currentTimeMillis(),
                                    isConnected = true,
                                    statusText = "Ready to Sync"
                                )
                            )

                            // Trigger auto-sync if enabled and not currently syncing
                            if (_isAutoSyncEnabled.value && _syncStatus.value != SyncStatus.SYNCING) {
                                launch(Dispatchers.IO) {
                                    syncWithDevice(senderIp, remotePort)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing discovery packet: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                if (isActive) Log.w(TAG, "UDP listener closed or failed: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    private fun startBeaconSender() {
        beaconSenderJob?.cancel()
        beaconSenderJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    updateLocalIp()
                    sendDiscoveryBeacon()
                } catch (e: Exception) {
                    Log.w(TAG, "Error sending beacon: ${e.message}")
                }
                delay(12000L) // Broadcast beacon every 12 seconds
            }
        }
    }

    private fun sendDiscoveryBeacon() {
        try {
            val beaconJson = JSONObject().apply {
                put("magic", "SAA_INVENTORY")
                put("deviceId", deviceId)
                put("deviceName", _deviceName.value)
                put("ip", _localIpAddress.value)
                put("port", SYNC_PORT)
                put("timestamp", System.currentTimeMillis())
            }.toString()

            val bytes = beaconJson.toByteArray()
            val socket = DatagramSocket()
            socket.broadcast = true

            // Send to global broadcast
            val globalBroadcast = InetAddress.getByName("255.255.255.255")
            socket.send(DatagramPacket(bytes, bytes.size, globalBroadcast, BROADCAST_PORT))

            // Also send to current subnet broadcast e.g. 192.168.43.255 or 192.168.1.255
            val localIp = _localIpAddress.value
            if (localIp != "127.0.0.1" && localIp.contains(".")) {
                val parts = localIp.split(".")
                if (parts.size == 4) {
                    val subnetBroadcastIp = "${parts[0]}.${parts[1]}.${parts[2]}.255"
                    try {
                        val subnetBroadcast = InetAddress.getByName(subnetBroadcastIp)
                        socket.send(DatagramPacket(bytes, bytes.size, subnetBroadcast, BROADCAST_PORT))
                    } catch (ignored: Exception) {}
                }
            }
            socket.close()
        } catch (e: Exception) {
            Log.w(TAG, "Send discovery beacon error: ${e.message}")
        }
    }

    suspend fun syncWithDevice(targetIp: String, port: Int = SYNC_PORT): Boolean = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.SYNCING
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(targetIp, port), 4000)

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Gather local changes
            val lastSync = _lastSyncTimestamp.value
            val localChanges = getLocalChangesSince(lastSync)

            val packet = SyncPacket(
                action = "SYNC_REQUEST",
                deviceId = deviceId,
                deviceName = _deviceName.value,
                pairingKey = _pairingKey.value,
                sinceTimestamp = lastSync,
                parts = localChanges.parts,
                transactions = localChanges.transactions,
                suppliers = localChanges.suppliers
            )

            writer.println(packet.toJson())

            val responseLine = reader.readLine()
            if (responseLine != null) {
                val responsePacket = SyncPacket.fromJson(responseLine)
                val conflictCount = reconcileIncomingData(responsePacket)

                val now = System.currentTimeMillis()
                _lastSyncTimestamp.value = now
                prefs.edit().putLong("last_sync_time", now).apply()

                if (conflictCount > 0) {
                    _syncStatus.value = SyncStatus.CONFLICT
                } else {
                    _syncStatus.value = SyncStatus.SYNCED
                }

                // Update trusted devices
                database.syncDao().upsertTrustedDevice(
                    TrustedDeviceEntity(
                        deviceId = responsePacket.deviceId,
                        deviceName = responsePacket.deviceName,
                        ipAddress = targetIp,
                        port = port,
                        lastSyncTimestamp = now,
                        isTrusted = true,
                        pairingKey = responsePacket.pairingKey
                    )
                )

                updateDeviceSyncState(responsePacket.deviceId, true, now)
                return@withContext true
            } else {
                _syncStatus.value = SyncStatus.FAILED
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync with $targetIp failed: ${e.message}")
            _syncStatus.value = SyncStatus.FAILED
            return@withContext false
        } finally {
            try { socket?.close() } catch (ignored: Exception) {}
        }
    }

    private data class ChangesContainer(
        val parts: List<PartEntity>,
        val transactions: List<StockTransactionEntity>,
        val suppliers: List<SupplierEntity>
    )

    private suspend fun getLocalChangesSince(sinceTimestamp: Long): ChangesContainer {
        val parts = database.partDao().getPartsModifiedSince(sinceTimestamp)
        val transactions = database.stockTransactionDao().getTransactionsSince(sinceTimestamp)
        val suppliers = database.supplierDao().getSuppliersModifiedSince(sinceTimestamp)
        return ChangesContainer(parts, transactions, suppliers)
    }

    private suspend fun reconcileIncomingData(packet: SyncPacket): Int {
        var conflicts = 0

        // 1. Reconcile Parts
        for (remotePart in packet.parts) {
            val localPart = database.partDao().getPartById(remotePart.partId)
            if (localPart == null) {
                // Completely new part from peer
                database.partDao().insertOrUpdatePart(remotePart)
                database.auditDao().insertAuditLog(
                    AuditLogEntity(
                        entityType = "PART",
                        entityId = remotePart.partId,
                        operationType = "SYNC_INSERT",
                        fieldName = "partName",
                        oldValue = "",
                        newValue = remotePart.partName,
                        deviceId = packet.deviceId
                    )
                )
            } else {
                // Record exists locally. Compare timestamps
                if (remotePart.updatedTimestamp > localPart.updatedTimestamp) {
                    // Check if local record was also updated since last sync (concurrent edit conflict)
                    if (localPart.updatedTimestamp > _lastSyncTimestamp.value && _lastSyncTimestamp.value > 0) {
                        conflicts++
                        database.syncDao().insertConflict(
                            SyncConflictEntity(
                                entityType = "PART",
                                entityId = remotePart.partId,
                                entityTitle = remotePart.partName,
                                localTimestamp = localPart.updatedTimestamp,
                                remoteTimestamp = remotePart.updatedTimestamp,
                                remoteDeviceId = packet.deviceId,
                                resolution = "LATEST_TIMESTAMP_APPLIED",
                                description = "Part '${remotePart.partName}' edited concurrently. Local stock (SAA: ${localPart.stockSaa}, TVS: ${localPart.stockTvs}), Remote stock (SAA: ${remotePart.stockSaa}, TVS: ${remotePart.stockTvs}). Updated to latest remote version."
                            )
                        )
                    }
                    database.partDao().insertOrUpdatePart(remotePart)
                    database.auditDao().insertAuditLog(
                        AuditLogEntity(
                            entityType = "PART",
                            entityId = remotePart.partId,
                            operationType = "SYNC_UPDATE",
                            fieldName = "all",
                            oldValue = "v${localPart.version}",
                            newValue = "v${remotePart.version}",
                            deviceId = packet.deviceId
                        )
                    )
                }
            }
        }

        // 2. Reconcile Transactions (Idempotent UUID insert)
        for (tx in packet.transactions) {
            database.stockTransactionDao().insertTransaction(tx)
        }

        // 3. Reconcile Suppliers
        for (remoteSup in packet.suppliers) {
            val localSup = database.supplierDao().getSupplierById(remoteSup.supplierId)
            if (localSup == null || remoteSup.updatedTimestamp > localSup.updatedTimestamp) {
                database.supplierDao().insertOrUpdateSupplier(remoteSup)
            }
        }

        return conflicts
    }

    private fun addOrUpdateDiscoveredDevice(device: DiscoveredDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst { it.deviceId == device.deviceId || it.ipAddress == device.ipAddress }
        if (index >= 0) {
            current[index] = device.copy(lastSyncTimestamp = current[index].lastSyncTimestamp)
        } else {
            current.add(device)
        }
        _discoveredDevices.value = current
    }

    private fun updateDeviceSyncState(deviceId: String, isConnected: Boolean, syncTime: Long) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst { it.deviceId == deviceId }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(
                isConnected = isConnected,
                lastSyncTimestamp = syncTime,
                statusText = "Synced at " + formatSimpleTime(syncTime)
            )
            _discoveredDevices.value = current
        }
    }

    /**
     * Simulation tool for testing device-to-device synchronization on a single phone / emulator!
     * Creates virtual peer "Phone B (TVS Counter)", syncs records, simulates changes & conflict resolution.
     */
    suspend fun simulatePeerSync(): String = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.SYNCING
        delay(1000L) // Realism delay

        val virtualPeerId = "SAA-SIM-DEV-B"
        val virtualPeerName = "Phone B (TVS Counter)"
        val virtualIp = "192.168.43.50"

        // Virtual peer creates/modifies a spare part
        val simPartId = "sim-part-spark-plug-1"
        val existing = database.partDao().getPartById(simPartId)
        val now = System.currentTimeMillis()

        val simPart = PartEntity(
            partId = simPartId,
            partName = "Bosch Spark Plug UR4AC",
            partNumber = "BSH-UR4AC",
            partDescription = "Genuine Bosch Spark Plug for 2-Wheelers",
            category = "Electrical",
            firm = "SAA",
            rack = "Rack-B",
            rackNumber = "04",
            shelf = "S2",
            location = "Counter 1",
            minStock = 10,
            stockSaa = if (existing != null) existing.stockSaa + 5 else 25,
            stockTvs = if (existing != null) existing.stockTvs + 5 else 15,
            barcode = "8901234567890",
            supplierCode = "SUP-01",
            supplierName = "Bosch Auto Spares Ltd",
            purchasePrice = 85.0,
            sellingPrice = 120.0,
            createdTimestamp = existing?.createdTimestamp ?: (now - 100000L),
            updatedTimestamp = now,
            deviceId = virtualPeerId,
            version = (existing?.version ?: 0L) + 1L
        )

        val simTx = StockTransactionEntity(
            transactionId = UUID.randomUUID().toString(),
            partId = simPartId,
            partName = simPart.partName,
            partNumber = simPart.partNumber,
            firm = "SAA",
            type = "STOCK_IN",
            quantity = 5,
            previousStock = (existing?.stockSaa ?: 20),
            newStock = simPart.stockSaa,
            billNumber = "BILL-PEER-7749",
            supplierName = "Bosch Auto Spares Ltd",
            supplierCode = "SUP-01",
            purchasePrice = 85.0,
            reason = "Peer Phone B Purchase Entry",
            date = now,
            deviceId = virtualPeerId,
            timestamp = now
        )

        val packet = SyncPacket(
            action = "SYNC_EXCHANGE",
            deviceId = virtualPeerId,
            deviceName = virtualPeerName,
            pairingKey = _pairingKey.value,
            sinceTimestamp = 0L,
            parts = listOf(simPart),
            transactions = listOf(simTx),
            suppliers = emptyList()
        )

        val conflicts = reconcileIncomingData(packet)

        _lastSyncTimestamp.value = now
        prefs.edit().putLong("last_sync_time", now).apply()

        // Upsert device in nearby list
        addOrUpdateDiscoveredDevice(
            DiscoveredDevice(
                deviceId = virtualPeerId,
                deviceName = virtualPeerName,
                ipAddress = virtualIp,
                port = SYNC_PORT,
                lastSeenTimestamp = now,
                lastSyncTimestamp = now,
                isConnected = true,
                statusText = "Synced at " + formatSimpleTime(now)
            )
        )

        database.syncDao().upsertTrustedDevice(
            TrustedDeviceEntity(
                deviceId = virtualPeerId,
                deviceName = virtualPeerName,
                ipAddress = virtualIp,
                port = SYNC_PORT,
                lastSyncTimestamp = now,
                isTrusted = true,
                pairingKey = _pairingKey.value
            )
        )

        if (conflicts > 0) {
            _syncStatus.value = SyncStatus.CONFLICT
            "Simulated sync with Phone B completed with 1 conflict resolved!"
        } else {
            _syncStatus.value = SyncStatus.SYNCED
            "Simulated sync with Phone B succeeded! 1 Part & 1 Transaction synced."
        }
    }

    private fun getDeviceLocalIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress != null && addr.hostAddress!!.indexOf(':') < 0) {
                        return addr.hostAddress!!
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting local IP: ${e.message}")
        }
        return "127.0.0.1"
    }

    private fun formatSimpleTime(millis: Long): String {
        if (millis <= 0L) return "Never"
        val sdf = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }

    fun cleanUp() {
        serverJob?.cancel()
        discoveryListenerJob?.cancel()
        beaconSenderJob?.cancel()
        try { serverSocket?.close() } catch (ignored: Exception) {}
        try { multicastLock?.release() } catch (ignored: Exception) {}
    }
}
