package com.example.ui.screens.barcode

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PartEntity
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.BarcodeUtil
import com.example.util.PrintUtil
import com.example.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeLabelScreen(
    viewModel: InventoryViewModel,
    partId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var part by remember { mutableStateOf<PartEntity?>(null) }
    var copiesCount by remember { mutableStateOf(1) }

    LaunchedEffect(partId) {
        part = viewModel.repository.getPartById(partId)
    }

    val labelBitmap: Bitmap? = remember(part) {
        part?.let { p ->
            val code = p.barcode.ifBlank { p.partNumber }
            BarcodeUtil.generateLabelBitmap(
                shopName = "Shri Amardevi Automobile & Spare Part",
                partName = p.partName,
                partNumber = p.partNumber,
                barcode = code,
                supplierCode = p.supplierCode,
                firm = p.firm,
                width = 650,
                height = 360
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Print Barcode Label", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (part != null && labelBitmap != null) {
                Text(
                    text = "LABEL PREVIEW (AUTOMOBILE SPARE PART)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate600
                )

                // Label Card Preview
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Image(
                        bitmap = labelBitmap.asImageBitmap(),
                        contentDescription = "Printable Barcode Label",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }

                // Copies Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Print Copies:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 5, 10).forEach { count ->
                                OutlinedButton(
                                    onClick = { copiesCount = count },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (copiesCount == count) AutoNavy else Color.Transparent,
                                        contentColor = if (copiesCount == count) Color.White else AutoNavyDark
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("$count", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Print Button
                Button(
                    onClick = {
                        PrintUtil.printLabel(context, labelBitmap, "SAA_Label_${part!!.partNumber}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutoNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_print_label")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Print $copiesCount Label(s)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // Share Details Button
                OutlinedButton(
                    onClick = {
                        ShareUtil.sharePartDetails(context, part!!)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Part & Barcode via WhatsApp")
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading label details...", fontSize = 14.sp, color = Slate600)
                }
            }
        }
    }
}
