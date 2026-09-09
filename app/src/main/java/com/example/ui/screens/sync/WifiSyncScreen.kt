package com.example.ui.screens.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SyncConflictEntity
import com.example.data.model.SyncStatus
import com.example.data.sync.DiscoveredDevice
import com.example.ui.components.SyncStatusPill
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldDark
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.StatusYellowBg
import com.example.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiSyncScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val localIp by viewModel.localIpAddress.collectAsStateWithLifecycle()
    val isAutoSync by viewModel.isAutoSyncEnabled.collectAsStateWithLifecycle()
    val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()
    val pairingKey by viewModel.pairingKey.collectAsStateWithLifecycle()
    val conflicts by viewModel.syncConflicts.collectAsStateWithLifecycle()

    var manualIp by remember { mutableStateOf("") }
    var editDeviceName by remember { mutableStateOf(deviceName) }
    var isEditingName by remember { mutableStateOf(false) }

    var isSimulating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Wi-Fi / Hotspot Sync", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshNetworkState() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AutoNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Status Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = AutoNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT SYNC STATUS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (syncStatus) {
                                    SyncStatus.SYNCED -> "All records in sync"
                                    SyncStatus.SYNCING -> "Synchronizing with peer..."
                                    SyncStatus.WAITING_FOR_DEVICE -> "Broadcasting on local network..."
                                    SyncStatus.CONFLICT -> "Sync complete (conflicts resolved)"
                                    SyncStatus.FAILED -> "Peer connection offline"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        SyncStatusPill(
                            status = syncStatus,
                            lastSyncTimestamp = lastSyncTime,
                            onClick = {}
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Last sync: ${if (lastSyncTime > 0) SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(Date(lastSyncTime)) else "Never"}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Auto-Sync Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Automatic Wi-Fi / Hotspot Sync", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                        Text(
                            text = "Syncs automatically in background when connected to same Wi-Fi or mobile hotspot",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }

                    Switch(
                        checked = isAutoSync,
                        onCheckedChange = { viewModel.setAutoSync(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AutoNavy),
                        modifier = Modifier.testTag("switch_auto_sync")
                    )
                }
            }

            // Local Device Network Info
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("This Device (Local Node)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Device Name:", fontSize = 12.sp, color = Slate600)
                        if (isEditingName) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = editDeviceName,
                                    onValueChange = { editDeviceName = it },
                                    singleLine = true,
                                    modifier = Modifier.width(160.dp)
                                )
                                IconButton(onClick = {
                                    viewModel.setDeviceName(editDeviceName)
                                    isEditingName = false
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = StatusGreen)
                                }
                            }
                        } else {
                            Text(
                                text = "$deviceName ✎",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AutoNavyDark,
                                modifier = Modifier.clickable { isEditingName = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Local IP Address:", fontSize = 12.sp, color = Slate600)
                        Text(localIp, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sync Port:", fontSize = 12.sp, color = Slate600)
                        Text("18889 (TCP) & 18888 (UDP)", fontSize = 12.sp, color = AutoNavyDark)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Shop Pairing Key:", fontSize = 12.sp, color = Slate600)
                        Text(pairingKey, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberGoldDark)
                    }
                }
            }

            // Nearby Discovered Devices
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Nearby SAA Devices (${discoveredDevices.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                        Text("Port: 18889", fontSize = 11.sp, color = Slate600)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (discoveredDevices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Searching for nearby phones on same Wi-Fi / Hotspot...",
                                fontSize = 12.sp,
                                color = Slate600
                            )
                        }
                    } else {
                        discoveredDevices.forEach { dev ->
                            DeviceRow(
                                device = dev,
                                onSync = { viewModel.syncWithDevice(dev.ipAddress, dev.port) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

            // Direct Connect by IP
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Manual IP Connect", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connect directly if Wi-Fi router disables broadcast discovery (e.g. 192.168.43.1 for Hotspot)",
                        fontSize = 11.sp,
                        color = Slate600
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            placeholder = { Text("192.168.43.1") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_manual_peer_ip")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (manualIp.isNotBlank()) {
                                    viewModel.syncWithDevice(manualIp.trim())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AutoNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_connect_ip")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync")
                        }
                    }
                }
            }

            // Dual-Phone Simulation Action Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberGoldDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = AmberGoldDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Dual-Phone Wi-Fi Sync", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Simulates 'Phone B (TVS Counter)' connecting over local hotspot, modifying a spare part stock and purchase bill, and tests automatic record exchange and conflict resolution on a single device.",
                        fontSize = 12.sp,
                        color = Slate600
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            isSimulating = true
                            viewModel.simulatePeerSync {
                                isSimulating = false
                            }
                        },
                        enabled = !isSimulating,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGoldDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_simulate_sync")
                    ) {
                        if (isSimulating) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulating Sync Protocol...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Dual-Device Sync Simulation", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Conflict Resolution Audit Log
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Conflict Resolution Log (${conflicts.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                        Icon(Icons.Default.Security, contentDescription = null, tint = AutoNavy, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tracks concurrent edits on separate phones. Protected by timestamp versioning.",
                        fontSize = 11.sp,
                        color = Slate600
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (conflicts.isEmpty()) {
                        Text("No conflicts encountered. All records cleanly merged.", fontSize = 12.sp, color = StatusGreen)
                    } else {
                        conflicts.take(5).forEach { conflict ->
                            ConflictItem(conflict)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: DiscoveredDevice,
    onSync: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.deviceName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AutoNavyDark)
                Text("IP: ${device.ipAddress} • ${device.statusText}", fontSize = 11.sp, color = Slate600)
            }

            Button(
                onClick = onSync,
                colors = ButtonDefaults.buttonColors(containerColor = AutoNavy),
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_sync_device_${device.deviceId}")
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sync Now", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ConflictItem(conflict: SyncConflictEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(conflict.entityTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AutoNavyDark)
                Text(conflict.resolution, fontSize = 10.sp, color = AmberGoldDark, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(conflict.description, fontSize = 11.sp, color = Slate600)
        }
    }
}
