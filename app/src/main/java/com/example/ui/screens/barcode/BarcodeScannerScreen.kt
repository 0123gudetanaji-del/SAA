package com.example.ui.screens.barcode

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PartEntity
import com.example.ui.components.PartItemCard
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPartDetail: (String) -> Unit,
    onNavigateToAddNewPartWithBarcode: (String) -> Unit,
    onNavigateToStockIn: (String) -> Unit,
    onNavigateToStockOut: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var manualBarcode by remember { mutableStateOf("") }
    var matchedPart by remember { mutableStateOf<PartEntity?>(null) }
    var searchPerformed by remember { mutableStateOf(false) }

    // Laser scanning animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    fun performLookup(code: String) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) return
        searchPerformed = true
        // Search in repository by barcode or part number
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val byBarcode = viewModel.repository.getPartByBarcode(trimmed)
            val byNumber = if (byBarcode == null) viewModel.repository.getPartByPartNumber(trimmed) else null
            matchedPart = byBarcode ?: byNumber
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("1D Barcode Scanner", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scanner Viewfinder Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Viewfinder reticle
                    Box(
                        modifier = Modifier
                            .size(width = 240.dp, height = 140.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    ) {
                        // Animated Red Laser Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .padding(top = (laserOffset % 130).dp)
                                .background(Color.Red)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (hasCameraPermission) "Align 1D barcode inside the target box" else "Camera access required for live scanning",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )

                        if (!hasCameraPermission) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = AutoNavy),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Enable Camera", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Manual Barcode / USB Barcode Scanner Input
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Manual Entry or Barcode Gun", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualBarcode,
                            onValueChange = {
                                manualBarcode = it
                                if (it.length >= 6) {
                                    performLookup(it)
                                }
                            },
                            placeholder = { Text("Type code or scan with gun...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_barcode_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { performLookup(manualBarcode) },
                            colors = ButtonDefaults.buttonColors(containerColor = AutoNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_lookup_barcode")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Test Barcodes (for easy emulator evaluation)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Test samples:", fontSize = 10.sp, color = Slate600)
                        OutlinedButton(
                            onClick = {
                                manualBarcode = "8901234001015"
                                performLookup("8901234001015")
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Spark Plug", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                manualBarcode = "8901234001046"
                                performLookup("8901234001046")
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Engine Oil", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Results Section
            if (matchedPart != null) {
                Text(
                    text = "PART FOUND IN INVENTORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusGreen
                )

                PartItemCard(
                    part = matchedPart!!,
                    onClick = { onNavigateToPartDetail(matchedPart!!.partId) },
                    onStockIn = { onNavigateToStockIn(matchedPart!!.partId) },
                    onStockOut = { onNavigateToStockOut(matchedPart!!.partId) }
                )
            } else if (searchPerformed && manualBarcode.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Barcode '$manualBarcode' not in inventory",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AutoNavyDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Would you like to register this as a new spare part?",
                            fontSize = 12.sp,
                            color = Slate600
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onNavigateToAddNewPartWithBarcode(manualBarcode) },
                            colors = ButtonDefaults.buttonColors(containerColor = AutoNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_register_new_barcode")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create New Part with this Barcode", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
