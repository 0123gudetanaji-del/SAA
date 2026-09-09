package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Firm
import com.example.data.model.PartEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.SyncConflictEntity
import com.example.data.model.SyncStatus
import com.example.data.repository.InventoryRepository
import com.example.data.sync.DiscoveredDevice
import com.example.data.sync.LocalNetworkSyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val syncManager = LocalNetworkSyncManager(application, database, viewModelScope)
    val repository = InventoryRepository(database, syncManager, application)

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedFirmFilter = MutableStateFlow("ALL") // "ALL", "SAA", "TVS"
    val selectedCategoryFilter = MutableStateFlow("ALL")
    val selectedStockStatusFilter = MutableStateFlow("ALL") // "ALL", "LOW", "OUT"

    // Dashboard flows
    val totalPartsCount: StateFlow<Int> = repository.getTotalPartsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStockUnitsCount: StateFlow<Int> = repository.getTotalStockUnitsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lowStockCount: StateFlow<Int> = repository.getLowStockCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayPurchases: StateFlow<List<StockTransactionEntity>> = repository.getTodayPurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayStockOuts: StateFlow<List<StockTransactionEntity>> = repository.getTodayStockOuts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All parts with search & filter applied
    @OptIn(ExperimentalCoroutinesApi::class)
    val partsList: StateFlow<List<PartEntity>> = combine(
        searchQuery,
        selectedFirmFilter,
        selectedCategoryFilter,
        selectedStockStatusFilter
    ) { query, firm, cat, stockStatus ->
        Triple(query, firm, Pair(cat, stockStatus))
    }.flatMapLatest { (query, firm, filters) ->
        val (cat, stockStatus) = filters
        val baseFlow = if (query.isBlank()) repository.getAllParts() else repository.searchParts(query)
        baseFlow.combine(MutableStateFlow(Unit)) { list, _ ->
            list.filter { part ->
                val matchesFirm = when (firm) {
                    "ALL" -> true
                    Firm.SAA.code -> part.firm == Firm.SAA.code || part.firm == Firm.BOTH.code
                    Firm.TVS.code -> part.firm == Firm.TVS.code || part.firm == Firm.BOTH.code
                    else -> true
                }
                val matchesCategory = if (cat == "ALL") true else part.category.equals(cat, ignoreCase = true)
                val matchesStock = when (stockStatus) {
                    "LOW" -> part.isLowStock()
                    "OUT" -> part.isOutOfStock()
                    else -> true
                }
                matchesFirm && matchesCategory && matchesStock
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Low stock parts specifically
    val lowStockParts: StateFlow<List<PartEntity>> = repository.getLowStockParts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions
    val allTransactions: StateFlow<List<StockTransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<StockTransactionEntity>> = repository.getAllPurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Suppliers
    val suppliersList: StateFlow<List<SupplierEntity>> = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sync States
    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus
    val lastSyncTimestamp: StateFlow<Long> = syncManager.lastSyncTimestamp
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = syncManager.discoveredDevices
    val localIpAddress: StateFlow<String> = syncManager.localIpAddress
    val isAutoSyncEnabled: StateFlow<Boolean> = syncManager.isAutoSyncEnabled
    val deviceName: StateFlow<String> = syncManager.deviceName
    val pairingKey: StateFlow<String> = syncManager.pairingKey
    val syncConflicts: StateFlow<List<SyncConflictEntity>> = repository.getSyncConflicts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Operation Messages (Snackbar / Toast notifications)
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    init {
        // Seed default automobile inventory data on first launch
        viewModelScope.launch {
            repository.seedAutomobileSampleData()
        }
    }

    fun savePart(part: PartEntity, isNew: Boolean, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.savePart(part, isNew)
            if (result.isSuccess) {
                _userMessage.value = if (isNew) "Part '${part.partName}' added successfully!" else "Part updated!"
                onResult(true, null)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to save part"
                _userMessage.value = err
                onResult(false, err)
            }
        }
    }

    fun deletePart(partId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deletePart(partId)
            if (res.isSuccess) {
                _userMessage.value = "Part deleted successfully"
                onComplete()
            } else {
                _userMessage.value = res.exceptionOrNull()?.message ?: "Failed to delete"
            }
        }
    }

    fun recordStockIn(
        partId: String,
        firm: String,
        quantity: Int,
        billNumber: String,
        supplierName: String,
        supplierCode: String,
        purchasePrice: Double,
        remarks: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.recordStockIn(
                partId = partId,
                firm = firm,
                quantity = quantity,
                billNumber = billNumber,
                supplierName = supplierName,
                supplierCode = supplierCode,
                purchasePrice = purchasePrice,
                remarks = remarks
            )
            if (res.isSuccess) {
                _userMessage.value = "Stock In recorded! Bill: $billNumber (+$quantity for $firm)"
                onResult(true, null)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Stock In failed"
                _userMessage.value = err
                onResult(false, err)
            }
        }
    }

    fun recordStockOut(
        partId: String,
        firm: String,
        quantity: Int,
        reason: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.recordStockOut(
                partId = partId,
                firm = firm,
                quantity = quantity,
                reason = reason
            )
            if (res.isSuccess) {
                _userMessage.value = "Stock Out recorded (-$quantity from $firm)"
                onResult(true, null)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Stock Out failed"
                _userMessage.value = err
                onResult(false, err)
            }
        }
    }

    fun saveSupplier(supplier: SupplierEntity, isNew: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.saveSupplier(supplier, isNew)
            if (res.isSuccess) {
                _userMessage.value = "Supplier saved successfully!"
                onResult(true)
            } else {
                _userMessage.value = "Failed to save supplier"
                onResult(false)
            }
        }
    }

    fun syncWithDevice(ip: String, port: Int = LocalNetworkSyncManager.SYNC_PORT) {
        viewModelScope.launch {
            val success = syncManager.syncWithDevice(ip, port)
            _userMessage.value = if (success) "Sync with $ip completed!" else "Sync with $ip failed."
        }
    }

    fun simulatePeerSync(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val result = syncManager.simulatePeerSync()
            _userMessage.value = result
            onComplete(result)
        }
    }

    fun refreshNetworkState() {
        val ip = syncManager.updateLocalIp()
        _userMessage.value = "Local IP updated: $ip"
    }

    fun setAutoSync(enabled: Boolean) {
        syncManager.setAutoSync(enabled)
    }

    fun setDeviceName(name: String) {
        syncManager.setDeviceName(name)
    }

    fun setPairingKey(key: String) {
        syncManager.setPairingKey(key)
    }

    fun setAllowNegativeStock(allow: Boolean) {
        repository.setAllowNegativeStock(allow)
        _userMessage.value = if (allow) "Negative stock allowed" else "Negative stock blocked"
    }

    fun isAllowNegativeStock(): Boolean = repository.isAllowNegativeStock()

    fun setDefaultMinStock(value: Int) {
        repository.setDefaultMinStock(value)
    }

    fun getDefaultMinStock(): Int = repository.getDefaultMinStock()

    // ==========================================
    // SHREE AI ASSISTANT & TTS CAPABILITIES
    // ==========================================

    val ttsManager = com.example.util.ShreeTtsManager(getApplication())
    val isSpeaking = ttsManager.isSpeaking
    val isTtsInitialized = ttsManager.isInitialized
    val autoSpeakEnabled = MutableStateFlow(true)

    private val _shreeChatMessages = MutableStateFlow<List<ShreeChatMessage>>(emptyList())
    val shreeChatMessages: StateFlow<List<ShreeChatMessage>> = _shreeChatMessages.asStateFlow()

    // Continuous Live Tracking of Pending Parts via Purchase & Order Transactions
    val pendingPartsReport: StateFlow<com.example.ai.PendingPartsReport> = combine(
        repository.getAllParts(),
        repository.getAllTransactions(),
        repository.getAllSuppliers()
    ) { parts, transactions, suppliers ->
        com.example.ai.ShreeAnalyticsEngine.analyzeInventory(parts, transactions, suppliers)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.ai.PendingPartsReport(
            pendingItems = emptyList(),
            totalPendingCount = 0,
            criticalZeroStockCount = 0,
            belowMinStockCount = 0,
            fastMovingShortageCount = 0,
            totalEstimatedReorderCost = 0.0,
            saaPendingCount = 0,
            tvsPendingCount = 0,
            supplierGroups = emptyList(),
            todayPurchaseCount = 0,
            todayStockOutCount = 0
        )
    )

    fun initShreeConversation() {
        if (_shreeChatMessages.value.isEmpty()) {
            val welcomeReport = pendingPartsReport.value
            val greetingResponse = com.example.ai.ShreeAiEngine.processQuery(
                query = "hello shree",
                parts = partsList.value,
                transactions = allTransactions.value,
                suppliers = suppliersList.value
            )
            val welcomeMessage = ShreeChatMessage(
                isFromUser = false,
                text = greetingResponse.displayText,
                speechText = greetingResponse.speechText,
                pendingReport = welcomeReport,
                suggestedActions = greetingResponse.suggestedActions,
                intent = greetingResponse.intent
            )
            _shreeChatMessages.value = listOf(welcomeMessage)
            if (autoSpeakEnabled.value) {
                ttsManager.speak(greetingResponse.speechText)
            }
        }
    }

    fun askShree(query: String) {
        if (query.isBlank()) return
        val userMsg = ShreeChatMessage(
            isFromUser = true,
            text = query.trim()
        )
        val currentList = _shreeChatMessages.value.toMutableList()
        currentList.add(userMsg)
        _shreeChatMessages.value = currentList

        viewModelScope.launch {
            val response = com.example.ai.ShreeAiEngine.processQuery(
                query = query,
                parts = partsList.value,
                transactions = allTransactions.value,
                suppliers = suppliersList.value
            )

            val shreeMsg = ShreeChatMessage(
                isFromUser = false,
                text = response.displayText,
                speechText = response.speechText,
                pendingReport = response.pendingReport,
                matchingParts = response.matchingParts,
                suggestedActions = response.suggestedActions,
                intent = response.intent
            )
            val updated = _shreeChatMessages.value.toMutableList()
            updated.add(shreeMsg)
            _shreeChatMessages.value = updated

            if (autoSpeakEnabled.value && response.speechText.isNotBlank()) {
                ttsManager.speak(response.speechText)
            }
        }
    }

    fun speakText(text: String) {
        ttsManager.speak(text)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun toggleAutoSpeak(enabled: Boolean) {
        autoSpeakEnabled.value = enabled
        if (!enabled) {
            ttsManager.stop()
        }
    }

    fun getWhatsAppOrderText(supplierFilter: String? = null): String {
        return com.example.ai.ShreeAnalyticsEngine.buildWhatsAppOrderMessage(
            report = pendingPartsReport.value,
            supplierFilter = supplierFilter
        )
    }

    override fun onCleared() {
        super.onCleared()
        syncManager.cleanUp()
        ttsManager.shutdown()
    }
}

data class ShreeChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isFromUser: Boolean,
    val text: String,
    val speechText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val pendingReport: com.example.ai.PendingPartsReport? = null,
    val matchingParts: List<PartEntity> = emptyList(),
    val suggestedActions: List<String> = emptyList(),
    val intent: com.example.ai.ShreeIntent = com.example.ai.ShreeIntent.GENERAL_APP_GUIDE
)
