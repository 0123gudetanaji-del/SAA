package com.example.ui.screens.stock

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
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.StockTransactionEntity
import com.example.ui.components.FirmBadge
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.TvsRed
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.ShareUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockHistoryScreen(
    viewModel: InventoryViewModel,
    filterPartId: String? = null,
    isPurchasesOnly: Boolean = false,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    var typeFilter by remember { mutableStateOf(if (isPurchasesOnly) "STOCK_IN" else "ALL") }
    var firmFilter by remember { mutableStateOf("ALL") }
    var searchKeyword by remember { mutableStateOf("") }

    val filteredList = remember(allTransactions, typeFilter, firmFilter, searchKeyword, filterPartId) {
        allTransactions.filter { tx ->
            val matchesPart = if (!filterPartId.isNullOrBlank()) tx.partId == filterPartId else true
            val matchesType = when (typeFilter) {
                "STOCK_IN" -> tx.type == "STOCK_IN"
                "STOCK_OUT" -> tx.type == "STOCK_OUT"
                else -> true
            }
            val matchesFirm = when (firmFilter) {
                Firm.SAA.code -> tx.firm == Firm.SAA.code
                Firm.TVS.code -> tx.firm == Firm.TVS.code
                else -> true
            }
            val matchesSearch = if (searchKeyword.isBlank()) true else {
                tx.partName.contains(searchKeyword, ignoreCase = true) ||
                tx.partNumber.contains(searchKeyword, ignoreCase = true) ||
                tx.billNumber.contains(searchKeyword, ignoreCase = true) ||
                tx.supplierName.contains(searchKeyword, ignoreCase = true)
            }
            matchesPart && matchesType && matchesFirm && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isPurchasesOnly) "Purchase Bills History" else "Stock Movement History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        ) {
            // Filter Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        placeholder = { Text("Search by part name, part no, bill no, supplier...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!isPurchasesOnly) {
                            FilterChip(
                                selected = typeFilter == "ALL",
                                onClick = { typeFilter = "ALL" },
                                label = { Text("All Types") }
                            )
                            FilterChip(
                                selected = typeFilter == "STOCK_IN",
                                onClick = { typeFilter = "STOCK_IN" },
                                label = { Text("📥 Stock In") }
                            )
                            FilterChip(
                                selected = typeFilter == "STOCK_OUT",
                                onClick = { typeFilter = "STOCK_OUT" },
                                label = { Text("📤 Stock Out") }
                            )
                        }

                        FilterChip(
                            selected = firmFilter == "ALL",
                            onClick = { firmFilter = "ALL" },
                            label = { Text("All Firms") }
                        )
                        FilterChip(
                            selected = firmFilter == Firm.SAA.code,
                            onClick = { firmFilter = Firm.SAA.code },
                            label = { Text("SAA") }
                        )
                        FilterChip(
                            selected = firmFilter == Firm.TVS.code,
                            onClick = { firmFilter = Firm.TVS.code },
                            label = { Text("TVS") }
                        )
                    }
                }
            }

            // Transaction List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions found", fontSize = 15.sp, color = Slate600)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.transactionId }) { tx ->
                        TransactionCard(
                            tx = tx,
                            onShare = { ShareUtil.shareTransactionReceipt(context, tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    tx: StockTransactionEntity,
    onShare: () -> Unit
) {
    val isIn = tx.type == "STOCK_IN"
    val color = if (isIn) StatusGreen else StatusRed
    val bgColor = if (isIn) StatusGreenBg else StatusRedBg
    val icon = if (isIn) Icons.Default.AddShoppingCart else Icons.Default.RemoveShoppingCart
    val sign = if (isIn) "+" else "-"

    val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_item_${tx.transactionId}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tx.partName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AutoNavyDark
                        )
                        Text(
                            text = "Part No: ${tx.partNumber}",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }

                FirmBadge(firm = tx.firm)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$sign${tx.quantity} units ($sign${tx.quantity} ${tx.firm})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )
                    Text(
                        text = "Balance: ${tx.previousStock} ➔ ${tx.newStock}",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }

                if (tx.billNumber.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Bill: ${tx.billNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AutoNavyDark
                        )
                        if (tx.supplierName.isNotBlank()) {
                            Text(
                                text = tx.supplierName,
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }
                } else if (tx.reason.isNotBlank()) {
                    Text(
                        text = tx.reason,
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Date & WhatsApp Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dateStr • ${tx.deviceId.take(8)}",
                    fontSize = 10.sp,
                    color = Slate600
                )

                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = AutoNavy, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
