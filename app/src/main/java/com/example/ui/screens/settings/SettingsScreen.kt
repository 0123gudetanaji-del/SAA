package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AmberGoldDark
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TvsRed
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.ShareUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var allowNegativeStock by remember { mutableStateOf(viewModel.isAllowNegativeStock()) }
    var defaultMinStockText by remember { mutableStateOf(viewModel.getDefaultMinStock().toString()) }

    var pairingKeyText by remember { mutableStateOf(viewModel.pairingKey.value) }
    var deviceNameText by remember { mutableStateOf(viewModel.deviceName.value) }

    val auditLogs by viewModel.repository.getRecentAuditLogs().collectAsStateWithLifecycle(emptyList())

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }
    var showAuditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings & Backup", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AutoNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
            // Business Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Business Entity", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Shri Amardevi Automobile & Spare Part",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AutoNavyDark
                    )
                    Text(
                        text = "Multi-Firm: SAA (Shri Amardevi Automobile) & TVS (Genuine Motor Parts)",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }
            }

            // Inventory & Stock Rules
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Inventory Rules", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block Negative Stock", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                            Text(
                                text = "Prevents issuing stock when quantity available is zero or insufficient",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }

                        Switch(
                            checked = !allowNegativeStock,
                            onCheckedChange = {
                                allowNegativeStock = !it
                                viewModel.setAllowNegativeStock(!it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AutoNavy),
                            modifier = Modifier.testTag("switch_block_negative")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = defaultMinStockText,
                            onValueChange = {
                                defaultMinStockText = it.filter { c -> c.isDigit() }
                                val v = defaultMinStockText.toIntOrNull()
                                if (v != null) viewModel.setDefaultMinStock(v)
                            },
                            label = { Text("Default Minimum Reorder Threshold") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sync Security Credentials
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Wi-Fi Sync Security", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deviceNameText,
                        onValueChange = {
                            deviceNameText = it
                            viewModel.setDeviceName(it)
                        },
                        label = { Text("Device Name in Wi-Fi Network") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pairingKeyText,
                        onValueChange = {
                            pairingKeyText = it
                            viewModel.setPairingKey(it)
                        },
                        label = { Text("Pairing Security Key (Must match across all phones)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Data Backup & Export
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Local Data Backup & Restore", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Backup your entire catalog, transactions, and suppliers into standard JSON format.",
                        fontSize = 11.sp,
                        color = Slate600
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val json = viewModel.repository.exportDatabaseJson()
                                    ShareUtil.shareText(context, json, "Export SAA Inventory Database Backup")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AutoNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_backup")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Backup", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_restore_backup")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore Backup", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Audit Trail
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
                        Text("Audit Trail History (${auditLogs.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                        TextButton(onClick = { showAuditDialog = true }) {
                            Text("View All")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Every inventory change and peer synchronization is permanently logged.",
                        fontSize = 11.sp,
                        color = Slate600
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    auditLogs.take(4).forEach { log ->
                        val dateStr = SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${log.operationType}: ${log.newValue.take(28)}",
                                fontSize = 11.sp,
                                color = AutoNavyDark,
                                fontWeight = FontWeight.Medium
                            )
                            Text(text = dateStr, fontSize = 10.sp, color = Slate600)
                        }
                    }
                }
            }
        }
    }

    // Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Inventory Backup") },
            text = {
                Column {
                    Text("Paste JSON backup string to restore records into local database:", fontSize = 12.sp, color = Slate600)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        placeholder = { Text("{\"appName\": ...}") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val res = viewModel.repository.restoreDatabaseJson(restoreJsonText)
                            if (res.isSuccess) {
                                viewModel.showMessage("Successfully restored ${res.getOrNull()} parts!")
                                showRestoreDialog = false
                            } else {
                                viewModel.showMessage("Invalid JSON backup: ${res.exceptionOrNull()?.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutoNavy)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Audit Dialog
    if (showAuditDialog) {
        AlertDialog(
            onDismissRequest = { showAuditDialog = false },
            title = { Text("Audit Trail Logs") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    auditLogs.forEach { log ->
                        val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate100),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(log.operationType, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                                    Text(dateStr, fontSize = 10.sp, color = Slate600)
                                }
                                Text("Field: ${log.fieldName} • Device: ${log.deviceId.take(8)}", fontSize = 10.sp, color = Slate600)
                                Text("New: ${log.newValue}", fontSize = 11.sp, color = AutoNavyDark)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAuditDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
