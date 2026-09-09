package com.example.ui.screens.suppliers

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.SupplierEntity
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.InventoryViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Auto Parts Suppliers (${suppliers.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AutoNavy,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_supplier")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Supplier")
            }
        }
    ) { padding ->
        if (suppliers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No suppliers registered yet", fontSize = 15.sp, color = Slate600)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(suppliers, key = { it.supplierId }) { sup ->
                    SupplierCard(
                        supplier = sup,
                        onCall = {
                            if (sup.mobileNumber.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sup.mobileNumber}"))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddSupplierDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, code, phone, addr, notes ->
                val newSup = SupplierEntity(
                    supplierId = UUID.randomUUID().toString(),
                    supplierName = name,
                    supplierCode = code,
                    mobileNumber = phone,
                    address = addr,
                    notes = notes,
                    createdTimestamp = System.currentTimeMillis(),
                    updatedTimestamp = System.currentTimeMillis()
                )
                viewModel.saveSupplier(newSup, isNew = true) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun SupplierCard(
    supplier: SupplierEntity,
    onCall: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = supplier.supplierName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AutoNavyDark
                    )
                    Text(
                        text = "Code: ${supplier.supplierCode}",
                        fontSize = 12.sp,
                        color = SaaBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (supplier.mobileNumber.isNotBlank()) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = StatusGreen)
                    }
                }
            }

            if (supplier.mobileNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "📞 ${supplier.mobileNumber}", fontSize = 12.sp, color = Slate600)
            }

            if (supplier.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "📍 ${supplier.address}", fontSize = 12.sp, color = Slate600)
            }

            if (supplier.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "📝 ${supplier.notes}", fontSize = 11.sp, color = Slate600)
            }
        }
    }
}

@Composable
private fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Auto Spare Parts Supplier", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Supplier / Company Name *") },
                    isError = error != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_supplier_name")
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Supplier Code (e.g. BOSCH-01)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile / Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Depot / Warehouse Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Parts supplied, terms)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Supplier name is required"
                    } else {
                        val safeCode = if (code.isBlank()) name.take(3).uppercase() + "-01" else code.trim()
                        onSave(name.trim(), safeCode, phone.trim(), address.trim(), notes.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AutoNavy),
                modifier = Modifier.testTag("btn_save_supplier")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
