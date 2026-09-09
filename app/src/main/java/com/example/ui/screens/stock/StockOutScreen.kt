package com.example.ui.screens.stock

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Firm
import com.example.data.model.PartEntity
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.StatusYellowBg
import com.example.ui.theme.TvsRed
import com.example.ui.viewmodel.InventoryViewModel

val STOCK_OUT_REASONS = listOf(
    "Counter Customer Sale",
    "Workshop Repair Job Card",
    "Mechanic Fitting Issue",
    "Warranty Replacement",
    "Damaged / Defective Part",
    "Internal Transfer"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOutScreen(
    viewModel: InventoryViewModel,
    preselectedPartId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToBarcodeScan: () -> Unit
) {
    val allParts by viewModel.partsList.collectAsStateWithLifecycle()

    var selectedPart by remember { mutableStateOf<PartEntity?>(null) }
    var firm by remember { mutableStateOf(Firm.SAA.code) }
    var quantityText by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf(STOCK_OUT_REASONS[0]) }

    var partDropdownExpanded by remember { mutableStateOf(false) }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }

    var partError by remember { mutableStateOf<String?>(null) }
    var qtyError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(preselectedPartId, allParts) {
        if (!preselectedPartId.isNullOrBlank() && selectedPart == null) {
            val matched = allParts.firstOrNull { it.partId == preselectedPartId }
                ?: viewModel.repository.getPartById(preselectedPartId)
            if (matched != null) {
                selectedPart = matched
                firm = if (matched.stockSaa <= 0 && matched.stockTvs > 0) Firm.TVS.code else Firm.SAA.code
            }
        }
    }

    val quantity = quantityText.toIntOrNull() ?: 0
    val currentFirmStock = if (selectedPart != null) {
        if (firm == Firm.SAA.code) selectedPart!!.stockSaa else selectedPart!!.stockTvs
    } else 0
    val projectedStock = currentFirmStock - quantity
    val hasInsufficientStock = projectedStock < 0 && !viewModel.isAllowNegativeStock()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Stock Out / Counter Issue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StatusRed,
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
            // Firm Selection: SAA vs TVS
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Issue Stock From Firm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = firm == Firm.SAA.code,
                            onClick = { firm = Firm.SAA.code },
                            label = { Text("SAA Stock (-)") },
                            modifier = Modifier.testTag("stock_out_firm_saa")
                        )
                        FilterChip(
                            selected = firm == Firm.TVS.code,
                            onClick = { firm = Firm.TVS.code },
                            label = { Text("TVS Stock (-)") },
                            modifier = Modifier.testTag("stock_out_firm_tvs")
                        )
                    }
                }
            }

            // Part Selection
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Spare Part *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                        IconButton(onClick = onNavigateToBarcodeScan, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = partDropdownExpanded,
                        onExpandedChange = { partDropdownExpanded = !partDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedPart?.let { "${it.partName} (${it.partNumber})" } ?: "Tap to choose spare part...",
                            onValueChange = {},
                            readOnly = true,
                            isError = partError != null,
                            supportingText = partError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("select_part_stock_out")
                        )
                        ExposedDropdownMenu(
                            expanded = partDropdownExpanded,
                            onDismissRequest = { partDropdownExpanded = false }
                        ) {
                            allParts.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(p.partName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Part No: ${p.partNumber} • SAA: ${p.stockSaa}, TVS: ${p.stockTvs}", fontSize = 11.sp, color = Slate600)
                                        }
                                    },
                                    onClick = {
                                        selectedPart = p
                                        partError = null
                                        partDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quantity & Reason
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Issue Quantity & Purpose", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it.filter { c -> c.isDigit() }
                            if (quantityText.isNotBlank()) qtyError = null
                        },
                        label = { Text("Quantity to Issue *") },
                        isError = qtyError != null,
                        supportingText = qtyError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_stock_out_quantity")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reason Dropdown
                    ExposedDropdownMenuBox(
                        expanded = reasonDropdownExpanded,
                        onExpandedChange = { reasonDropdownExpanded = !reasonDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Reason / Purpose") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = reasonDropdownExpanded,
                            onDismissRequest = { reasonDropdownExpanded = false }
                        ) {
                            STOCK_OUT_REASONS.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        reason = r
                                        reasonDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Live Stock Balance Card
            if (selectedPart != null) {
                val cardBg = if (hasInsufficientStock) StatusRedBg else StatusYellowBg
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURRENT AVAILABLE ($firm): $currentFirmStock UNITS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasInsufficientStock) StatusRed else AutoNavyDark
                            )

                            Icon(
                                imageVector = if (hasInsufficientStock) Icons.Default.Warning else Icons.Default.RemoveShoppingCart,
                                contentDescription = null,
                                tint = if (hasInsufficientStock) StatusRed else StatusRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Remaining after deduction: $projectedStock units",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasInsufficientStock) StatusRed else Slate600
                        )

                        if (hasInsufficientStock) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ Cannot issue more than available stock ($currentFirmStock units)!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Confirm Button
            Button(
                onClick = {
                    var error = false
                    if (selectedPart == null) {
                        partError = "Please select a spare part"
                        error = true
                    }
                    if (quantity <= 0) {
                        qtyError = "Quantity must be greater than 0"
                        error = true
                    }
                    if (hasInsufficientStock) {
                        qtyError = "Insufficient stock available"
                        error = true
                    }
                    if (error) return@Button

                    viewModel.recordStockOut(
                        partId = selectedPart!!.partId,
                        firm = firm,
                        quantity = quantity,
                        reason = reason
                    ) { success, _ ->
                        if (success) {
                            onNavigateBack()
                        }
                    }
                },
                enabled = !hasInsufficientStock,
                colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_stock_out")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm & Issue Stock Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
