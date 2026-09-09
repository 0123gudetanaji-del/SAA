package com.example.ui.screens.parts

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.ui.theme.TvsRed
import com.example.util.BarcodeUtil
import com.example.util.ShareUtil

@Composable
fun PartDetailDialog(
    part: PartEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onStockIn: () -> Unit,
    onStockOut: () -> Unit,
    onPrintLabel: () -> Unit,
    onViewHistory: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val barcodeBitmap = remember(part.barcode, part.partNumber) {
        val code = part.barcode.ifBlank { part.partNumber }
        try {
            BarcodeUtil.generateBarcodeBitmap(code, width = 450, height = 140, showText = true)
        } catch (e: Exception) {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = part.partName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AutoNavyDark
                        )
                        Text(
                            text = "Part No: ${part.partNumber}",
                            fontSize = 13.sp,
                            color = Slate600
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FirmBadge(firm = part.firm)
                    StockStatusBadge(part = part)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stock Breakdown Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SAA Stock", fontSize = 11.sp, color = Slate600)
                            Text(
                                text = "${part.stockSaa}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SaaBlue
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TVS Stock", fontSize = 11.sp, color = Slate600)
                            Text(
                                text = "${part.stockTvs}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TvsRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Stock", fontSize = 11.sp, color = Slate600)
                            Text(
                                text = "${part.totalStock}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AutoNavyDark
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Min Stock", fontSize = 11.sp, color = Slate600)
                            Text(
                                text = "${part.minStock}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate600
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Specifications
                DetailItem(label = "Category", value = part.category)
                DetailItem(label = "Location", value = part.location.ifBlank { "N/A" })
                DetailItem(label = "Rack / Shelf", value = "Rack ${part.rack} (${part.rackNumber}) / Shelf ${part.shelf}")
                if (part.supplierName.isNotBlank() || part.supplierCode.isNotBlank()) {
                    DetailItem(label = "Supplier", value = "${part.supplierName} (${part.supplierCode})")
                }
                if (part.purchasePrice > 0) {
                    DetailItem(label = "Purchase Price", value = "₹${part.purchasePrice}")
                }
                if (part.sellingPrice > 0) {
                    DetailItem(label = "Selling Price", value = "₹${part.sellingPrice}")
                }
                if (part.partDescription.isNotBlank()) {
                    DetailItem(label = "Description", value = part.partDescription)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barcode Preview Box
                if (barcodeBitmap != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate100, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Barcode: ${part.barcode.ifBlank { part.partNumber }}",
                            fontSize = 11.sp,
                            color = Slate600,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Image(
                            bitmap = barcodeBitmap.asImageBitmap(),
                            contentDescription = "Barcode",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStockIn,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_stock_in")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stock In", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onStockOut,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_stock_out")
                    ) {
                        Icon(Icons.Default.RemoveShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stock Out", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPrintLabel,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_print_label")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Label", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { ShareUtil.sharePartDetails(context, part) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_share")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onViewHistory,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_history")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("History", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("dialog_edit")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Part")
                    }

                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = StatusRed),
                        modifier = Modifier.testTag("dialog_delete")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Part?") },
            text = { Text("Are you sure you want to delete '${part.partName}'? This deletion will be synchronized across all devices.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Slate600)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AutoNavyDark
        )
    }
}
