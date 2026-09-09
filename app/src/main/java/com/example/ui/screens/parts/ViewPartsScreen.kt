package com.example.ui.screens.parts

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Firm
import com.example.data.model.PartEntity
import com.example.ui.components.PartItemCard
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TvsRed
import com.example.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewPartsScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddPart: () -> Unit,
    onNavigateToEditPart: (String) -> Unit,
    onNavigateToStockIn: (String) -> Unit,
    onNavigateToStockOut: (String) -> Unit,
    onNavigateToBarcodeLabel: (String) -> Unit,
    onNavigateToPartHistory: (String) -> Unit
) {
    val parts by viewModel.partsList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFirm by viewModel.selectedFirmFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedStockStatus by viewModel.selectedStockStatusFilter.collectAsStateWithLifecycle()

    var selectedPartForDialog by remember { mutableStateOf<PartEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Spare Parts Inventory (${parts.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                onClick = onNavigateToAddPart,
                containerColor = AutoNavy,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_part")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Part")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Search by name, part no, barcode, rack, shelf...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_parts_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal Filter Chips Row 1: Firm & Stock Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Firm Filters
                        FilterChip(
                            selected = selectedFirm == "ALL",
                            onClick = { viewModel.selectedFirmFilter.value = "ALL" },
                            label = { Text("All Firms") },
                            modifier = Modifier.testTag("filter_firm_all")
                        )
                        FilterChip(
                            selected = selectedFirm == Firm.SAA.code,
                            onClick = { viewModel.selectedFirmFilter.value = Firm.SAA.code },
                            label = { Text("SAA Parts") },
                            modifier = Modifier.testTag("filter_firm_saa")
                        )
                        FilterChip(
                            selected = selectedFirm == Firm.TVS.code,
                            onClick = { viewModel.selectedFirmFilter.value = Firm.TVS.code },
                            label = { Text("TVS Parts") },
                            modifier = Modifier.testTag("filter_firm_tvs")
                        )

                        // Stock Status Filters
                        FilterChip(
                            selected = selectedStockStatus == "LOW",
                            onClick = {
                                viewModel.selectedStockStatusFilter.value = if (selectedStockStatus == "LOW") "ALL" else "LOW"
                            },
                            label = { Text("⚡ Low Stock") },
                            modifier = Modifier.testTag("filter_low_stock")
                        )
                        FilterChip(
                            selected = selectedStockStatus == "OUT",
                            onClick = {
                                viewModel.selectedStockStatusFilter.value = if (selectedStockStatus == "OUT") "ALL" else "OUT"
                            },
                            label = { Text("⚠️ Out of Stock") },
                            modifier = Modifier.testTag("filter_out_of_stock")
                        )
                    }

                    // Categories Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == "ALL",
                            onClick = { viewModel.selectedCategoryFilter.value = "ALL" },
                            label = { Text("All Categories") }
                        )
                        AUTOMOBILE_CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    viewModel.selectedCategoryFilter.value = if (selectedCategory == cat) "ALL" else cat
                                },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            // Parts List
            if (parts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No spare parts found", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate600)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Try clearing your search or add a new part.", fontSize = 13.sp, color = Slate600)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(parts, key = { it.partId }) { part ->
                        PartItemCard(
                            part = part,
                            onClick = { selectedPartForDialog = part },
                            onStockIn = { onNavigateToStockIn(part.partId) },
                            onStockOut = { onNavigateToStockOut(part.partId) }
                        )
                    }
                }
            }
        }
    }

    // Full detail modal dialog
    selectedPartForDialog?.let { part ->
        PartDetailDialog(
            part = part,
            onDismiss = { selectedPartForDialog = null },
            onEdit = {
                val id = part.partId
                selectedPartForDialog = null
                onNavigateToEditPart(id)
            },
            onStockIn = {
                val id = part.partId
                selectedPartForDialog = null
                onNavigateToStockIn(id)
            },
            onStockOut = {
                val id = part.partId
                selectedPartForDialog = null
                onNavigateToStockOut(id)
            },
            onPrintLabel = {
                val id = part.partId
                selectedPartForDialog = null
                onNavigateToBarcodeLabel(id)
            },
            onViewHistory = {
                val id = part.partId
                selectedPartForDialog = null
                onNavigateToPartHistory(id)
            },
            onDelete = {
                viewModel.deletePart(part.partId)
                selectedPartForDialog = null
            }
        )
    }
}
