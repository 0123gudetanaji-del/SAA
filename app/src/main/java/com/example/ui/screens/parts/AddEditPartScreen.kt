package com.example.ui.screens.parts

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
import androidx.compose.material.icons.filled.AutoFixHigh
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
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TvsRed
import com.example.ui.viewmodel.InventoryViewModel
import java.util.UUID

val AUTOMOBILE_CATEGORIES = listOf(
    "Electrical",
    "Brakes",
    "Engine Parts",
    "Filters",
    "Lubricants",
    "Transmission",
    "Body & Frame",
    "Suspension",
    "Cables & Controls",
    "General"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPartScreen(
    viewModel: InventoryViewModel,
    partIdToEdit: String? = null,
    scannedBarcode: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToBarcodeScan: () -> Unit
) {
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()

    var partName by remember { mutableStateOf("") }
    var partNumber by remember { mutableStateOf("") }
    var partDescription by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(AUTOMOBILE_CATEGORIES[0]) }
    var firm by remember { mutableStateOf(Firm.BOTH.code) }
    var rack by remember { mutableStateOf("Rack-A") }
    var rackNumber by remember { mutableStateOf("01") }
    var shelf by remember { mutableStateOf("S1") }
    var location by remember { mutableStateOf("Front Counter") }
    var minStock by remember { mutableStateOf("5") }
    var stockSaa by remember { mutableStateOf("0") }
    var stockTvs by remember { mutableStateOf("0") }
    var barcode by remember { mutableStateOf(scannedBarcode ?: "") }
    var supplierCode by remember { mutableStateOf("") }
    var supplierName by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    var originalPart by remember { mutableStateOf<PartEntity?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var numberError by remember { mutableStateOf<String?>(null) }

    // Populate fields if editing
    LaunchedEffect(partIdToEdit) {
        if (!partIdToEdit.isNullOrBlank()) {
            val p = viewModel.repository.getPartById(partIdToEdit)
            if (p != null) {
                originalPart = p
                partName = p.partName
                partNumber = p.partNumber
                partDescription = p.partDescription
                category = p.category
                firm = p.firm
                rack = p.rack
                rackNumber = p.rackNumber
                shelf = p.shelf
                location = p.location
                minStock = p.minStock.toString()
                stockSaa = p.stockSaa.toString()
                stockTvs = p.stockTvs.toString()
                barcode = p.barcode
                supplierCode = p.supplierCode
                supplierName = p.supplierName
                purchasePrice = if (p.purchasePrice > 0) p.purchasePrice.toString() else ""
                sellingPrice = if (p.sellingPrice > 0) p.sellingPrice.toString() else ""
            }
        }
    }

    LaunchedEffect(scannedBarcode) {
        if (!scannedBarcode.isNullOrBlank()) {
            barcode = scannedBarcode
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (partIdToEdit != null) "Edit Spare Part" else "Add New Spare Part",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Firm Selection (SAA, TVS, BOTH)
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Firm Classification", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = firm == Firm.SAA.code,
                            onClick = { firm = Firm.SAA.code },
                            label = { Text("SAA Only") },
                            modifier = Modifier.testTag("chip_firm_saa")
                        )
                        FilterChip(
                            selected = firm == Firm.TVS.code,
                            onClick = { firm = Firm.TVS.code },
                            label = { Text("TVS Only") },
                            modifier = Modifier.testTag("chip_firm_tvs")
                        )
                        FilterChip(
                            selected = firm == Firm.BOTH.code,
                            onClick = { firm = Firm.BOTH.code },
                            label = { Text("Both (SAA & TVS)") },
                            modifier = Modifier.testTag("chip_firm_both")
                        )
                    }
                }
            }

            // Part Name (Mandatory)
            OutlinedTextField(
                value = partName,
                onValueChange = {
                    partName = it
                    if (it.isNotBlank()) nameError = null
                },
                label = { Text("Part Name * (e.g. Spark Plug UR4AC)") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_part_name")
            )

            // Part Number (Mandatory & Unique)
            OutlinedTextField(
                value = partNumber,
                onValueChange = {
                    partNumber = it
                    if (it.isNotBlank()) numberError = null
                },
                label = { Text("Part Number * (Unique Code, e.g. BSH-UR4AC)") },
                isError = numberError != null,
                supportingText = numberError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_part_number")
            )

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    AUTOMOBILE_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Stock Details Card (SAA Stock & TVS Stock)
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Stock & Alert Quantities", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = stockSaa,
                            onValueChange = { stockSaa = it.filter { c -> c.isDigit() } },
                            label = { Text("SAA Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_stock_saa")
                        )

                        OutlinedTextField(
                            value = stockTvs,
                            onValueChange = { stockTvs = it.filter { c -> c.isDigit() } },
                            label = { Text("TVS Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_stock_tvs")
                        )

                        OutlinedTextField(
                            value = minStock,
                            onValueChange = { minStock = it.filter { c -> c.isDigit() } },
                            label = { Text("Min Alert") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_min_stock")
                        )
                    }
                }
            }

            // Location: Rack, Rack Number, Shelf, Location
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Physical Shelf Location", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = rack,
                            onValueChange = { rack = it },
                            label = { Text("Rack (e.g. A)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rackNumber,
                            onValueChange = { rackNumber = it },
                            label = { Text("No. (e.g. 01)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = shelf,
                            onValueChange = { shelf = it },
                            label = { Text("Shelf (S1)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location Description (e.g. Front Counter, Oil Bay)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Barcode Entry & Generator
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("1D Barcode Identification", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode Number") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_barcode")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToBarcodeScan,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Barcode", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                // Generate unique barcode based on part number or timestamp
                                barcode = if (partNumber.isNotBlank()) {
                                    partNumber.filter { it.isLetterOrDigit() }.take(12).uppercase()
                                } else {
                                    "SAA" + (System.currentTimeMillis() % 100000000L).toString()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto Generate", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Pricing & Supplier
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Pricing & Supplier Info", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = purchasePrice,
                            onValueChange = { purchasePrice = it },
                            label = { Text("Purchase Rate (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = sellingPrice,
                            onValueChange = { sellingPrice = it },
                            label = { Text("MRP / Sale (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Supplier Quick Selection
                    ExposedDropdownMenuBox(
                        expanded = supplierDropdownExpanded,
                        onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (supplierName.isNotBlank()) "$supplierName ($supplierCode)" else "Select Known Supplier (Optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Supplier") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
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

            // Part Description
            OutlinedTextField(
                value = partDescription,
                onValueChange = { partDescription = it },
                label = { Text("Description & Notes (Vehicle fitment, specifications)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    var hasError = false
                    if (partName.isBlank()) {
                        nameError = "Part name is mandatory"
                        hasError = true
                    }
                    if (partNumber.isBlank()) {
                        numberError = "Part number is mandatory"
                        hasError = true
                    }
                    if (hasError) return@Button

                    val partId = originalPart?.partId ?: UUID.randomUUID().toString()
                    val part = PartEntity(
                        partId = partId,
                        partName = partName.trim(),
                        partNumber = partNumber.trim().uppercase(),
                        partDescription = partDescription.trim(),
                        category = category,
                        firm = firm,
                        rack = rack.trim(),
                        rackNumber = rackNumber.trim(),
                        shelf = shelf.trim(),
                        location = location.trim(),
                        minStock = minStock.toIntOrNull() ?: 5,
                        stockSaa = stockSaa.toIntOrNull() ?: 0,
                        stockTvs = stockTvs.toIntOrNull() ?: 0,
                        barcode = barcode.trim(),
                        supplierCode = supplierCode.trim(),
                        supplierName = supplierName.trim(),
                        purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                        sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                        createdTimestamp = originalPart?.createdTimestamp ?: System.currentTimeMillis(),
                        updatedTimestamp = System.currentTimeMillis(),
                        version = originalPart?.version ?: 1L
                    )

                    viewModel.savePart(part, isNew = (originalPart == null)) { success, _ ->
                        if (success) {
                            onNavigateBack()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AutoNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_part")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (partIdToEdit != null) "Update Spare Part" else "Save Spare Part",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
