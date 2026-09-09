package com.example.ui.screens.stock

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Firm
import com.example.data.model.PartEntity
import com.example.ui.components.FirmBadge
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.TvsRed
import com.example.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockInScreen(
    viewModel: InventoryViewModel,
    preselectedPartId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToBarcodeScan: () -> Unit
) {
    val allParts by viewModel.partsList.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()

    var selectedPart by remember { mutableStateOf<PartEntity?>(null) }
    var firm by remember { mutableStateOf(Firm.SAA.code) }
    var quantityText by remember { mutableStateOf("1") }
    var billNumber by remember { mutableStateOf("") }
    var supplierName by remember { mutableStateOf("") }
    var supplierCode by remember { mutableStateOf("") }
    var purchasePriceText by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    var partDropdownExpanded by remember { mutableStateOf(false) }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    var billError by remember { mutableStateOf<String?>(null) }
    var partError by remember { mutableStateOf<String?>(null) }
    var qtyError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(preselectedPartId, allParts) {
        if (!preselectedPartId.isNullOrBlank() && selectedPart == null) {
            val matched = allParts.firstOrNull { it.partId == preselectedPartId }
                ?: viewModel.repository.getPartById(preselectedPartId)
            if (matched != null) {
                selectedPart = matched
                firm = if (matched.firm == Firm.TVS.code) Firm.TVS.code else Firm.SAA.code
                if (matched.supplierName.isNotBlank()) supplierName = matched.supplierName
                if (matched.supplierCode.isNotBlank()) supplierCode = matched.supplierCode
                if (matched.purchasePrice > 0) purchasePriceText = matched.purchasePrice.toString()
            }
        }
    }

    val quantity = quantityText.toIntOrNull() ?: 0
    val currentFirmStock = if (selectedPart != null) {
        if (firm == Firm.SAA.code) selectedPart!!.stockSaa else selectedPart!!.stockTvs
    } else 0
    val projectedStock = currentFirmStock + quantity

    val todayDateStr = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Stock In / Purchase Entry", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StatusGreen,
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
            // Purchase Header Card: Bill Number & Date (Mandatory)
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
                        Text("Purchase Bill Details", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                        Text("Date: $todayDateStr", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = billNumber,
                        onValueChange = {
                            billNumber = it
                            if (it.isNotBlank()) billError = null
                        },
                        label = { Text("Bill / Invoice Number * (Mandatory)") },
                        isError = billError != null,
                        supportingText = billError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_stock_in_bill_number")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Supplier Selection
                    ExposedDropdownMenuBox(
                        expanded = supplierDropdownExpanded,
                        onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = supplierName,
                            onValueChange = { supplierName = it },
                            label = { Text("Supplier Name (e.g. Bosch India Ltd)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("input_stock_in_supplier")
                        )
                        ExposedDropdownMenu(
                            expanded = supplierDropdownExpanded,
                            onDismissRequest = { supplierDropdownExpanded = false }
                        ) {
                            suppliers.forEach { sup ->
                                DropdownMenuItem(
                                    text = { Text("${sup.supplierName} (${sup.supplierCode})") },
                                    onClick = {
                                        supplierName = sup.supplierName
                                        supplierCode = sup.supplierCode
                                        supplierDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Firm Selection: SAA vs TVS
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Select Destination Firm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = firm == Firm.SAA.code,
                            onClick = { firm = Firm.SAA.code },
                            label = { Text("SAA Stock (+)") },
                            modifier = Modifier.testTag("stock_in_firm_saa")
                        )
                        FilterChip(
                            selected = firm == Firm.TVS.code,
                            onClick = { firm = Firm.TVS.code },
                            label = { Text("TVS Stock (+)") },
                            modifier = Modifier.testTag("stock_in_firm_tvs")
                        )
                    }
                }
            }

            // Part Selection (Dropdown or Scanned)
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
                                .testTag("select_part_dropdown")
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
                                            Text("Part No: ${p.partNumber} • Stock (SAA: ${p.stockSaa}, TVS: ${p.stockTvs})", fontSize = 11.sp, color = Slate600)
                                        }
                                    },
                                    onClick = {
                                        selectedPart = p
                                        partError = null
                                        if (p.supplierName.isNotBlank() && supplierName.isBlank()) {
                                            supplierName = p.supplierName
                                        }
                                        if (p.purchasePrice > 0 && purchasePriceText.isBlank()) {
                                            purchasePriceText = p.purchasePrice.toString()
                                        }
                                        partDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quantity & Purchase Price
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Inward Quantity & Rate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = {
                                quantityText = it.filter { c -> c.isDigit() }
                                if (quantityText.isNotBlank()) qtyError = null
                            },
                            label = { Text("Quantity *") },
                            isError = qtyError != null,
                            supportingText = qtyError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_stock_in_quantity")
                        )

                        OutlinedTextField(
                            value = purchasePriceText,
                            onValueChange = { purchasePriceText = it },
                            label = { Text("Unit Price (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks (Batch, rack placement, notes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Live Stock Preview Calculation Card
            if (selectedPart != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusGreenBg),
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
                        Column {
                            Text(
                                text = "STOCK PREVIEW FOR $firm",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Current: $currentFirmStock units  ➔  New: $projectedStock units",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AutoNavyDark
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Save Transaction Button
            Button(
                onClick = {
                    var error = false
                    if (billNumber.isBlank()) {
                        billError = "Bill Number is mandatory!"
                        error = true
                    }
                    if (selectedPart == null) {
                        partError = "Please select a spare part"
                        error = true
                    }
                    if (quantity <= 0) {
                        qtyError = "Quantity must be greater than 0"
                        error = true
                    }
                    if (error) return@Button

                    viewModel.recordStockIn(
                        partId = selectedPart!!.partId,
                        firm = firm,
                        quantity = quantity,
                        billNumber = billNumber,
                        supplierName = supplierName,
                        supplierCode = supplierCode,
                        purchasePrice = purchasePriceText.toDoubleOrNull() ?: 0.0,
                        remarks = remarks
                    ) { success, _ ->
                        if (success) {
                            onNavigateBack()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_stock_in")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm & Record Stock In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
