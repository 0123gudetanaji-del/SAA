package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.SyncStatusPill
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldDark
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.AutoNavyLight
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.SaaBlueBg
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.StatusYellowBg
import com.example.ui.theme.SteelBlue
import com.example.ui.theme.SteelBlueLight
import com.example.ui.theme.TvsRed
import com.example.ui.theme.TvsRedBg
import com.example.ui.viewmodel.InventoryViewModel

@Composable
fun HomeScreen(
    viewModel: InventoryViewModel,
    onNavigateToAddPart: () -> Unit,
    onNavigateToViewParts: () -> Unit,
    onNavigateToStockIn: () -> Unit,
    onNavigateToStockOut: () -> Unit,
    onNavigateToLowStock: () -> Unit,
    onNavigateToBarcodeScanner: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToPurchaseHistory: () -> Unit,
    onNavigateToStockHistory: () -> Unit,
    onNavigateToWifiSync: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToShreeAi: () -> Unit
) {
    val totalParts by viewModel.totalPartsCount.collectAsStateWithLifecycle()
    val totalStockUnits by viewModel.totalStockUnitsCount.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()
    val todayPurchases by viewModel.todayPurchases.collectAsStateWithLifecycle()
    val todayStockOuts by viewModel.todayStockOuts.collectAsStateWithLifecycle()
    val pendingReport by viewModel.pendingPartsReport.collectAsStateWithLifecycle()

    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Automobile Shop Identity & Wi-Fi Sync Pill
            item(span = { GridItemSpan(2) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AutoNavy),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(AutoNavy, AutoNavyDark)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SHRI AMARDEVI AUTOMOBILE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "& SPARE PART",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "SAA INVENTORY APP",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            // SAA & TVS Badges
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    color = SaaBlue,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SAA",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = TvsRed,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "TVS",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Wi-Fi Sync Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Wi-Fi / Hotspot Peer Sync:",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            SyncStatusPill(
                                status = syncStatus,
                                lastSyncTimestamp = lastSyncTime,
                                onClick = onNavigateToWifiSync
                            )
                        }
                    }
                }
            }

            // Dashboard KPI Summary
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Parts
                    KpiCard(
                        title = "Total Parts",
                        value = "$totalParts",
                        subtitle = "Catalog items",
                        containerColor = SteelBlueLight,
                        contentColor = SteelBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToViewParts
                    )

                    // Total Units
                    KpiCard(
                        title = "Total Stock",
                        value = "$totalStockUnits",
                        subtitle = "Units on shelf",
                        containerColor = Slate100,
                        contentColor = AutoNavyDark,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToViewParts
                    )

                    // Low Stock Alert
                    KpiCard(
                        title = "Low Stock",
                        value = "$lowStockCount",
                        subtitle = "Need reorder",
                        containerColor = if (lowStockCount > 0) StatusRedBg else StatusGreenBg,
                        contentColor = if (lowStockCount > 0) StatusRed else StatusGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToLowStock
                    )
                }
            }

            // Today's Activity Strip
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateToPurchaseHistory)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreenBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Today's Purchases", fontSize = 11.sp, color = Slate600)
                                Text("${todayPurchases.size} bills", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateToStockHistory)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(StatusRedBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.RemoveShoppingCart, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Today's Stock Out", fontSize = 11.sp, color = Slate600)
                                Text("${todayStockOuts.size} items", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                            }
                        }
                    }
                }
            }

            // Shree AI Assistant - Purchase & Pending Tracker Banner
            item(span = { GridItemSpan(2) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AutoNavy),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToShreeAi)
                        .testTag("card_shree_ai_banner")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(AutoNavyDark, AutoNavy)
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AmberGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = AutoNavyDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "श्री AI ASSISTANT",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = AmberGold,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Voice + Offline",
                                                color = AutoNavyDark,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Purchase & Pending Parts Intelligence",
                                        fontSize = 11.sp,
                                        color = AmberGoldLight
                                    )
                                }
                            }

                            Surface(
                                color = if (pendingReport.totalPendingCount > 0) StatusRedBg else StatusGreenBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${pendingReport.totalPendingCount} Pending",
                                    color = if (pendingReport.totalPendingCount > 0) StatusRed else StatusGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "परचेस व ऑर्डर्स ट्रैक कर के श्री ने ${pendingReport.totalPendingCount} पेंडिंग पार्ट्स की रीऑर्डर लिस्ट बनाई है। आवाज़ में सुनें व सीधे व्हाट्सएप ऑर्डर भेजें।",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToShreeAi,
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Icon(Icons.Default.Inventory, contentDescription = null, tint = AutoNavyDark, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("पेंडिंग लिस्ट देखें", color = AutoNavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onNavigateToShreeAi,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = AmberGold, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("श्री से बात करें", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section Header
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "MAIN OPERATIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate600,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }

            // 11 Main Functional Buttons
            item {
                MenuActionButton(
                    title = "Add Part",
                    subtitle = "New spare part entry",
                    icon = Icons.Default.AddBox,
                    accentColor = AutoNavy,
                    testTag = "btn_add_part",
                    onClick = onNavigateToAddPart
                )
            }

            item {
                MenuActionButton(
                    title = "View Parts",
                    subtitle = "Search & inventory list",
                    icon = Icons.Default.Inventory,
                    accentColor = SteelBlue,
                    testTag = "btn_view_parts",
                    onClick = onNavigateToViewParts
                )
            }

            item {
                MenuActionButton(
                    title = "Stock In / Purchase",
                    subtitle = "Bill entry & add stock",
                    icon = Icons.Default.AddShoppingCart,
                    accentColor = StatusGreen,
                    testTag = "btn_stock_in",
                    onClick = onNavigateToStockIn
                )
            }

            item {
                MenuActionButton(
                    title = "Stock Out",
                    subtitle = "Counter issue & sales",
                    icon = Icons.Default.RemoveShoppingCart,
                    accentColor = StatusRed,
                    testTag = "btn_stock_out",
                    onClick = onNavigateToStockOut
                )
            }

            item {
                MenuActionButton(
                    title = "Low Stock Alert",
                    subtitle = "$lowStockCount items need reorder",
                    icon = Icons.Default.Warning,
                    accentColor = AmberGoldDark,
                    testTag = "btn_low_stock",
                    onClick = onNavigateToLowStock
                )
            }

            item {
                MenuActionButton(
                    title = "Barcode Scanner",
                    subtitle = "1D barcode camera lookup",
                    icon = Icons.Default.QrCodeScanner,
                    accentColor = AutoNavyLight,
                    testTag = "btn_barcode_scanner",
                    onClick = onNavigateToBarcodeScanner
                )
            }

            item {
                MenuActionButton(
                    title = "Suppliers",
                    subtitle = "Vendor list & history",
                    icon = Icons.Default.People,
                    accentColor = SaaBlue,
                    testTag = "btn_suppliers",
                    onClick = onNavigateToSuppliers
                )
            }

            item {
                MenuActionButton(
                    title = "Purchase History",
                    subtitle = "Bills, rates & inward log",
                    icon = Icons.Default.Receipt,
                    accentColor = SteelBlue,
                    testTag = "btn_purchase_history",
                    onClick = onNavigateToPurchaseHistory
                )
            }

            item {
                MenuActionButton(
                    title = "Stock History",
                    subtitle = "All in/out transactions",
                    icon = Icons.Default.History,
                    accentColor = AutoNavyDark,
                    testTag = "btn_stock_history",
                    onClick = onNavigateToStockHistory
                )
            }

            item {
                MenuActionButton(
                    title = "Wi-Fi Sync",
                    subtitle = "Peer-to-peer hotspot sync",
                    icon = Icons.Default.Wifi,
                    accentColor = AmberGoldDark,
                    testTag = "btn_wifi_sync",
                    onClick = onNavigateToWifiSync
                )
            }

            item {
                MenuActionButton(
                    title = "श्री AI Assistant",
                    subtitle = "Pending parts & voice audit",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = AmberGold,
                    testTag = "btn_shree_ai",
                    onClick = onNavigateToShreeAi
                )
            }

            item(span = { GridItemSpan(2) }) {
                MenuActionButton(
                    title = "Settings & Data Backup",
                    subtitle = "Database export/import, sync config, shop info",
                    icon = Icons.Default.Settings,
                    accentColor = Slate800,
                    testTag = "btn_settings",
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(text = title, fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = contentColor)
            Text(text = subtitle, fontSize = 9.sp, color = Slate600)
        }
    }
}

@Composable
private fun MenuActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AutoNavyDark
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Slate600,
                    maxLines = 1
                )
            }
        }
    }
}
