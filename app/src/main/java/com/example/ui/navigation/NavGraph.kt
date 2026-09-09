package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.ai.ShreeAiScreen
import com.example.ui.screens.alerts.LowStockScreen
import com.example.ui.screens.barcode.BarcodeLabelScreen
import com.example.ui.screens.barcode.BarcodeScannerScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.parts.AddEditPartScreen
import com.example.ui.screens.parts.ViewPartsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.stock.StockHistoryScreen
import com.example.ui.screens.stock.StockInScreen
import com.example.ui.screens.stock.StockOutScreen
import com.example.ui.screens.suppliers.SuppliersScreen
import com.example.ui.screens.sync.WifiSyncScreen
import com.example.ui.viewmodel.InventoryViewModel

object Destinations {
    const val HOME = "home"
    const val ADD_PART = "add_part"
    const val VIEW_PARTS = "view_parts"
    const val STOCK_IN = "stock_in"
    const val STOCK_OUT = "stock_out"
    const val LOW_STOCK = "low_stock"
    const val BARCODE_SCANNER = "barcode_scanner"
    const val BARCODE_LABEL = "barcode_label"
    const val SUPPLIERS = "suppliers"
    const val PURCHASE_HISTORY = "purchase_history"
    const val STOCK_HISTORY = "stock_history"
    const val WIFI_SYNC = "wifi_sync"
    const val SETTINGS = "settings"
    const val SHREE_AI = "shree_ai"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: InventoryViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToAddPart = { navController.navigate(Destinations.ADD_PART) },
                onNavigateToViewParts = { navController.navigate(Destinations.VIEW_PARTS) },
                onNavigateToStockIn = { navController.navigate(Destinations.STOCK_IN) },
                onNavigateToStockOut = { navController.navigate(Destinations.STOCK_OUT) },
                onNavigateToLowStock = { navController.navigate(Destinations.LOW_STOCK) },
                onNavigateToBarcodeScanner = { navController.navigate(Destinations.BARCODE_SCANNER) },
                onNavigateToSuppliers = { navController.navigate(Destinations.SUPPLIERS) },
                onNavigateToPurchaseHistory = { navController.navigate(Destinations.PURCHASE_HISTORY) },
                onNavigateToStockHistory = { navController.navigate(Destinations.STOCK_HISTORY) },
                onNavigateToWifiSync = { navController.navigate(Destinations.WIFI_SYNC) },
                onNavigateToSettings = { navController.navigate(Destinations.SETTINGS) },
                onNavigateToShreeAi = { navController.navigate(Destinations.SHREE_AI) }
            )
        }

        composable(
            route = "${Destinations.ADD_PART}?editId={editId}&barcode={barcode}",
            arguments = listOf(
                navArgument("editId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("barcode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val editId = backStackEntry.arguments?.getString("editId")
            val barcode = backStackEntry.arguments?.getString("barcode")
            AddEditPartScreen(
                viewModel = viewModel,
                partIdToEdit = editId,
                scannedBarcode = barcode,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBarcodeScan = { navController.navigate(Destinations.BARCODE_SCANNER) }
            )
        }

        composable(Destinations.VIEW_PARTS) {
            ViewPartsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddPart = { navController.navigate(Destinations.ADD_PART) },
                onNavigateToEditPart = { partId ->
                    navController.navigate("${Destinations.ADD_PART}?editId=$partId")
                },
                onNavigateToStockIn = { partId ->
                    navController.navigate("${Destinations.STOCK_IN}?partId=$partId")
                },
                onNavigateToStockOut = { partId ->
                    navController.navigate("${Destinations.STOCK_OUT}?partId=$partId")
                },
                onNavigateToBarcodeLabel = { partId ->
                    navController.navigate("${Destinations.BARCODE_LABEL}/$partId")
                },
                onNavigateToPartHistory = { partId ->
                    navController.navigate("${Destinations.STOCK_HISTORY}?partId=$partId")
                }
            )
        }

        composable(
            route = "${Destinations.STOCK_IN}?partId={partId}",
            arguments = listOf(
                navArgument("partId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val partId = backStackEntry.arguments?.getString("partId")
            StockInScreen(
                viewModel = viewModel,
                preselectedPartId = partId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBarcodeScan = { navController.navigate(Destinations.BARCODE_SCANNER) }
            )
        }

        composable(
            route = "${Destinations.STOCK_OUT}?partId={partId}",
            arguments = listOf(
                navArgument("partId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val partId = backStackEntry.arguments?.getString("partId")
            StockOutScreen(
                viewModel = viewModel,
                preselectedPartId = partId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBarcodeScan = { navController.navigate(Destinations.BARCODE_SCANNER) }
            )
        }

        composable(Destinations.LOW_STOCK) {
            LowStockScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStockIn = { partId ->
                    navController.navigate("${Destinations.STOCK_IN}?partId=$partId")
                }
            )
        }

        composable(Destinations.BARCODE_SCANNER) {
            BarcodeScannerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPartDetail = { partId ->
                    navController.navigate("${Destinations.STOCK_HISTORY}?partId=$partId")
                },
                onNavigateToAddNewPartWithBarcode = { barcode ->
                    navController.navigate("${Destinations.ADD_PART}?barcode=$barcode")
                },
                onNavigateToStockIn = { partId ->
                    navController.navigate("${Destinations.STOCK_IN}?partId=$partId")
                },
                onNavigateToStockOut = { partId ->
                    navController.navigate("${Destinations.STOCK_OUT}?partId=$partId")
                }
            )
        }

        composable(
            route = "${Destinations.BARCODE_LABEL}/{partId}",
            arguments = listOf(
                navArgument("partId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val partId = backStackEntry.arguments?.getString("partId") ?: ""
            BarcodeLabelScreen(
                viewModel = viewModel,
                partId = partId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.SUPPLIERS) {
            SuppliersScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.PURCHASE_HISTORY) {
            StockHistoryScreen(
                viewModel = viewModel,
                isPurchasesOnly = true,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Destinations.STOCK_HISTORY}?partId={partId}",
            arguments = listOf(
                navArgument("partId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val partId = backStackEntry.arguments?.getString("partId")
            StockHistoryScreen(
                viewModel = viewModel,
                filterPartId = partId,
                isPurchasesOnly = false,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.WIFI_SYNC) {
            WifiSyncScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.SHREE_AI) {
            ShreeAiScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStockIn = { partId ->
                    navController.navigate("${Destinations.STOCK_IN}?partId=$partId")
                }
            )
        }
    }
}
