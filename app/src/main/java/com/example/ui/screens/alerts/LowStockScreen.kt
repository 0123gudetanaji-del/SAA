package com.example.ui.screens.alerts

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Firm
import com.example.data.model.PartEntity
import com.example.ui.components.FirmBadge
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.AmberGoldDark
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.StatusYellowBg
import com.example.ui.theme.TvsRed
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LowStockScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStockIn: (String) -> Unit
) {
    val context = LocalContext.current
    val lowStockList by viewModel.lowStockParts.collectAsStateWithLifecycle()
    var selectedFirmFilter by remember { mutableStateOf("ALL") }

    val filteredList = remember(lowStockList, selectedFirmFilter) {
        if (selectedFirmFilter == "ALL") lowStockList else {
            lowStockList.filter { it.firm == selectedFirmFilter || it.firm == Firm.BOTH.code }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Low Stock Reorder Alert (${filteredList.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { ShareUtil.shareLowStockList(context, filteredList) },
                        modifier = Modifier.testTag("btn_share_low_stock")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share on WhatsApp")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmberGoldDark,
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
        ) {
            // Summary Banner & Share WhatsApp CTA
            Surface(
                color = AmberGoldDark.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberGoldDark, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${filteredList.size} parts below minimum stock",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AutoNavyDark
                            )
                        }

                        Button(
                            onClick = { ShareUtil.shareLowStockList(context, filteredList) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share via WhatsApp", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFirmFilter == "ALL",
                            onClick = { selectedFirmFilter = "ALL" },
                            label = { Text("All Firms") }
                        )
                        FilterChip(
                            selected = selectedFirmFilter == Firm.SAA.code,
                            onClick = { selectedFirmFilter = Firm.SAA.code },
                            label = { Text("SAA Low Stock") }
                        )
                        FilterChip(
                            selected = selectedFirmFilter == Firm.TVS.code,
                            onClick = { selectedFirmFilter = Firm.TVS.code },
                            label = { Text("TVS Low Stock") }
                        )
                    }
                }
            }

            // List of items
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All stock levels healthy!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                        Text("No spare parts are currently below minimum stock threshold.", fontSize = 12.sp, color = Slate600)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.partId }) { part ->
                        LowStockItemCard(
                            part = part,
                            onStockIn = { onNavigateToStockIn(part.partId) },
                            onShare = { ShareUtil.sharePartDetails(context, part) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockItemCard(
    part: PartEntity,
    onStockIn: () -> Unit,
    onShare: () -> Unit
) {
    val isZero = part.totalStock <= 0
    val suggestedOrder = (part.minStock * 2 - part.totalStock).coerceAtLeast(part.minStock)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("low_stock_card_${part.partNumber}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(part.partName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AutoNavyDark)
                    Text("Part No: ${part.partNumber}", fontSize = 12.sp, color = Slate600)
                }
                FirmBadge(firm = part.firm)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quantities Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("Current Total", fontSize = 10.sp, color = Slate600)
                        Text(
                            text = "${part.totalStock} units",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isZero) StatusRed else AmberGoldDark
                        )
                    }

                    Column {
                        Text("Min Alert", fontSize = 10.sp, color = Slate600)
                        Text("${part.minStock} units", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate600)
                    }

                    Column {
                        Text("Suggested Order", fontSize = 10.sp, color = Slate600)
                        Text("$suggestedOrder units", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = StatusGreen)
                    }
                }

                StockStatusBadge(part = part)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SAA vs TVS breakdown & location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stock: SAA: ${part.stockSaa} | TVS: ${part.stockTvs} • Rack ${part.rack}-${part.rackNumber}",
                    fontSize = 11.sp,
                    color = Slate600
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = AutoNavy, modifier = Modifier.size(16.dp))
                    }

                    Button(
                        onClick = onStockIn,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("reorder_stock_in_${part.partNumber}")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Order / In", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
