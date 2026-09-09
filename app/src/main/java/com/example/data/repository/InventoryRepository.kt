package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.Firm
import com.example.data.model.PartEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.SyncChangeLogEntity
import com.example.data.sync.LocalNetworkSyncManager
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class InventoryRepository(
    private val database: AppDatabase,
    private val syncManager: LocalNetworkSyncManager,
    context: Context
) {
    private val prefs = context.getSharedPreferences("saa_inventory_settings", Context.MODE_PRIVATE)

    fun isAllowNegativeStock(): Boolean = prefs.getBoolean("allow_negative_stock", false)
    fun setAllowNegativeStock(allow: Boolean) = prefs.edit().putBoolean("allow_negative_stock", allow).apply()

    fun getDefaultMinStock(): Int = prefs.getInt("default_min_stock", 5)
    fun setDefaultMinStock(value: Int) = prefs.edit().putInt("default_min_stock", value).apply()

    // Parts Queries
    fun getAllParts(): Flow<List<PartEntity>> = database.partDao().getAllActiveParts()
    fun getLowStockParts(): Flow<List<PartEntity>> = database.partDao().getLowStockParts()
    fun searchParts(query: String): Flow<List<PartEntity>> = database.partDao().searchParts(query)

    suspend fun getPartById(id: String): PartEntity? = database.partDao().getPartById(id)
    suspend fun getPartByPartNumber(partNumber: String): PartEntity? = database.partDao().getPartByPartNumber(partNumber.trim())
    suspend fun getPartByBarcode(barcode: String): PartEntity? = database.partDao().getPartByBarcode(barcode.trim())

    // Dashboard metrics
    fun getTotalPartsCount(): Flow<Int> = database.partDao().getTotalPartsCount()
    fun getTotalStockUnitsCount(): Flow<Int> = database.partDao().getTotalStockUnitsCount()
    fun getLowStockCount(): Flow<Int> = database.partDao().getLowStockCount()

    fun getTodayPurchases(): Flow<List<StockTransactionEntity>> {
        val startOfDay = getStartOfDayMillis()
        return database.stockTransactionDao().getTodayPurchases(startOfDay)
    }

    fun getTodayStockOuts(): Flow<List<StockTransactionEntity>> {
        val startOfDay = getStartOfDayMillis()
        return database.stockTransactionDao().getTodayStockOuts(startOfDay)
    }

    // Transactions Queries
    fun getAllTransactions(): Flow<List<StockTransactionEntity>> = database.stockTransactionDao().getAllTransactions()
    fun getTransactionsForPart(partId: String): Flow<List<StockTransactionEntity>> = database.stockTransactionDao().getTransactionsForPart(partId)
    fun getAllPurchases(): Flow<List<StockTransactionEntity>> = database.stockTransactionDao().getAllPurchases()

    // Suppliers Queries
    fun getAllSuppliers(): Flow<List<SupplierEntity>> = database.supplierDao().getAllSuppliers()
    suspend fun getSupplierById(id: String): SupplierEntity? = database.supplierDao().getSupplierById(id)

    // Audit & Conflicts
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>> = database.auditDao().getRecentAuditLogs()
    fun getSyncConflicts() = database.syncDao().getAllConflicts()
    fun getTrustedDevices() = database.syncDao().getAllTrustedDevices()

    suspend fun savePart(part: PartEntity, isNew: Boolean): Result<Unit> {
        val now = System.currentTimeMillis()
        val existingWithNumber = getPartByPartNumber(part.partNumber)
        if (existingWithNumber != null && existingWithNumber.partId != part.partId) {
            return Result.failure(Exception("Part Number '${part.partNumber}' already exists for '${existingWithNumber.partName}'!"))
        }

        if (part.barcode.isNotBlank()) {
            val existingWithBarcode = getPartByBarcode(part.barcode)
            if (existingWithBarcode != null && existingWithBarcode.partId != part.partId) {
                return Result.failure(Exception("Barcode '${part.barcode}' already assigned to '${existingWithBarcode.partName}'!"))
            }
        }

        val updatedPart = part.copy(
            updatedTimestamp = now,
            deviceId = syncManager.deviceId,
            version = if (isNew) 1L else (part.version + 1L)
        )

        database.partDao().insertOrUpdatePart(updatedPart)

        // Record Change Log for peer sync
        database.syncDao().insertChangeLog(
            SyncChangeLogEntity(
                entityType = "PART",
                entityId = updatedPart.partId,
                operation = if (isNew) "CREATE" else "UPDATE",
                payloadJson = JSONObject().apply {
                    put("partName", updatedPart.partName)
                    put("partNumber", updatedPart.partNumber)
                    put("stockSaa", updatedPart.stockSaa)
                    put("stockTvs", updatedPart.stockTvs)
                }.toString(),
                timestamp = now,
                deviceId = syncManager.deviceId,
                version = updatedPart.version
            )
        )

        // Audit Trail
        database.auditDao().insertAuditLog(
            AuditLogEntity(
                entityType = "PART",
                entityId = updatedPart.partId,
                operationType = if (isNew) "CREATE_PART" else "EDIT_PART",
                fieldName = "all",
                oldValue = if (isNew) "" else "v${part.version}",
                newValue = "v${updatedPart.version} - ${updatedPart.partName}",
                timestamp = now,
                deviceId = syncManager.deviceId
            )
        )

        return Result.success(Unit)
    }

    suspend fun deletePart(partId: String): Result<Unit> {
        val part = getPartById(partId) ?: return Result.failure(Exception("Part not found"))
        val now = System.currentTimeMillis()
        val tombstone = part.copy(
            isDeleted = true,
            updatedTimestamp = now,
            deviceId = syncManager.deviceId,
            version = part.version + 1L
        )
        database.partDao().insertOrUpdatePart(tombstone)

        database.syncDao().insertChangeLog(
            SyncChangeLogEntity(
                entityType = "PART",
                entityId = partId,
                operation = "DELETE",
                payloadJson = "{}",
                timestamp = now,
                deviceId = syncManager.deviceId,
                version = tombstone.version
            )
        )

        database.auditDao().insertAuditLog(
            AuditLogEntity(
                entityType = "PART",
                entityId = partId,
                operationType = "DELETE_PART",
                fieldName = "isDeleted",
                oldValue = "false",
                newValue = "true",
                timestamp = now,
                deviceId = syncManager.deviceId
            )
        )

        return Result.success(Unit)
    }

    /**
     * Stock In / Purchase transaction (Atomic)
     * Mandatory: Bill Number, Supplier Name, Purchase Date, Firm (SAA / TVS), Part, Quantity
     */
    suspend fun recordStockIn(
        partId: String,
        firm: String, // SAA or TVS
        quantity: Int,
        billNumber: String,
        supplierName: String,
        supplierCode: String = "",
        purchasePrice: Double = 0.0,
        remarks: String = ""
    ): Result<Unit> {
        if (billNumber.isBlank()) {
            return Result.failure(Exception("Bill Number is mandatory for Stock In / Purchase!"))
        }
        if (quantity <= 0) {
            return Result.failure(Exception("Quantity must be greater than 0!"))
        }
        if (firm != Firm.SAA.code && firm != Firm.TVS.code) {
            return Result.failure(Exception("Please specify Firm (SAA or TVS)!"))
        }

        val part = getPartById(partId) ?: return Result.failure(Exception("Part not found!"))
        val now = System.currentTimeMillis()

        val prevStock = if (firm == Firm.SAA.code) part.stockSaa else part.stockTvs
        val newStock = prevStock + quantity

        val updatedPart = if (firm == Firm.SAA.code) {
            part.copy(
                stockSaa = newStock,
                updatedTimestamp = now,
                deviceId = syncManager.deviceId,
                version = part.version + 1L,
                purchasePrice = if (purchasePrice > 0.0) purchasePrice else part.purchasePrice
            )
        } else {
            part.copy(
                stockTvs = newStock,
                updatedTimestamp = now,
                deviceId = syncManager.deviceId,
                version = part.version + 1L,
                purchasePrice = if (purchasePrice > 0.0) purchasePrice else part.purchasePrice
            )
        }

        val transaction = StockTransactionEntity(
            transactionId = UUID.randomUUID().toString(),
            partId = part.partId,
            partName = part.partName,
            partNumber = part.partNumber,
            firm = firm,
            type = "STOCK_IN",
            quantity = quantity,
            previousStock = prevStock,
            newStock = newStock,
            billNumber = billNumber.trim(),
            supplierName = supplierName.trim(),
            supplierCode = supplierCode.trim(),
            purchasePrice = purchasePrice,
            reason = remarks.trim().ifEmpty { "Purchase Bill: $billNumber" },
            date = now,
            deviceId = syncManager.deviceId,
            timestamp = now,
            version = 1L
        )

        database.withTransaction {
            database.partDao().insertOrUpdatePart(updatedPart)
            database.stockTransactionDao().insertTransaction(transaction)

            database.syncDao().insertChangeLog(
                SyncChangeLogEntity(
                    entityType = "TRANSACTION",
                    entityId = transaction.transactionId,
                    operation = "STOCK_IN",
                    payloadJson = JSONObject().apply {
                        put("partId", part.partId)
                        put("firm", firm)
                        put("quantity", quantity)
                        put("billNumber", billNumber)
                    }.toString(),
                    timestamp = now,
                    deviceId = syncManager.deviceId
                )
            )

            database.auditDao().insertAuditLog(
                AuditLogEntity(
                    entityType = "STOCK",
                    entityId = part.partId,
                    operationType = "STOCK_IN",
                    fieldName = "stock$firm",
                    oldValue = prevStock.toString(),
                    newValue = "$newStock (+$quantity, Bill: $billNumber)",
                    timestamp = now,
                    deviceId = syncManager.deviceId
                )
            )
        }

        return Result.success(Unit)
    }

    /**
     * Stock Out transaction (Atomic)
     * Mandatory: Part, Firm, Quantity, Reason
     */
    suspend fun recordStockOut(
        partId: String,
        firm: String,
        quantity: Int,
        reason: String
    ): Result<Unit> {
        if (quantity <= 0) {
            return Result.failure(Exception("Quantity must be greater than 0!"))
        }
        val part = getPartById(partId) ?: return Result.failure(Exception("Part not found!"))

        val prevStock = if (firm == Firm.SAA.code) part.stockSaa else part.stockTvs
        if (prevStock < quantity && !isAllowNegativeStock()) {
            return Result.failure(Exception("Insufficient stock for $firm! Available: $prevStock, Requested: $quantity. Enable negative stock in Settings if needed."))
        }

        val newStock = prevStock - quantity
        val now = System.currentTimeMillis()

        val updatedPart = if (firm == Firm.SAA.code) {
            part.copy(
                stockSaa = newStock,
                updatedTimestamp = now,
                deviceId = syncManager.deviceId,
                version = part.version + 1L
            )
        } else {
            part.copy(
                stockTvs = newStock,
                updatedTimestamp = now,
                deviceId = syncManager.deviceId,
                version = part.version + 1L
            )
        }

        val transaction = StockTransactionEntity(
            transactionId = UUID.randomUUID().toString(),
            partId = part.partId,
            partName = part.partName,
            partNumber = part.partNumber,
            firm = firm,
            type = "STOCK_OUT",
            quantity = quantity,
            previousStock = prevStock,
            newStock = newStock,
            billNumber = "",
            supplierName = "",
            supplierCode = "",
            reason = reason.trim().ifEmpty { "Counter Stock Out" },
            date = now,
            deviceId = syncManager.deviceId,
            timestamp = now,
            version = 1L
        )

        database.withTransaction {
            database.partDao().insertOrUpdatePart(updatedPart)
            database.stockTransactionDao().insertTransaction(transaction)

            database.syncDao().insertChangeLog(
                SyncChangeLogEntity(
                    entityType = "TRANSACTION",
                    entityId = transaction.transactionId,
                    operation = "STOCK_OUT",
                    payloadJson = JSONObject().apply {
                        put("partId", part.partId)
                        put("firm", firm)
                        put("quantity", quantity)
                        put("reason", reason)
                    }.toString(),
                    timestamp = now,
                    deviceId = syncManager.deviceId
                )
            )

            database.auditDao().insertAuditLog(
                AuditLogEntity(
                    entityType = "STOCK",
                    entityId = part.partId,
                    operationType = "STOCK_OUT",
                    fieldName = "stock$firm",
                    oldValue = prevStock.toString(),
                    newValue = "$newStock (-$quantity, Reason: $reason)",
                    timestamp = now,
                    deviceId = syncManager.deviceId
                )
            )
        }

        return Result.success(Unit)
    }

    suspend fun saveSupplier(supplier: SupplierEntity, isNew: Boolean): Result<Unit> {
        val now = System.currentTimeMillis()
        val updated = supplier.copy(
            updatedTimestamp = now,
            deviceId = syncManager.deviceId,
            version = if (isNew) 1L else (supplier.version + 1L)
        )
        database.supplierDao().insertOrUpdateSupplier(updated)
        return Result.success(Unit)
    }

    /**
     * Backup whole database to JSON string
     */
    suspend fun exportDatabaseJson(): String {
        val parts = database.partDao().getAllPartsRaw()
        val txs = database.stockTransactionDao().getAllTransactionsRaw()
        val sups = database.supplierDao().getAllSuppliersRaw()

        val root = JSONObject()
        root.put("appName", "SAA INVENTORY APP")
        root.put("shopName", "Shri Amardevi Automobile & Spare Part")
        root.put("exportTime", System.currentTimeMillis())
        root.put("deviceId", syncManager.deviceId)

        val partsArr = JSONArray()
        for (p in parts) {
            partsArr.put(JSONObject().apply {
                put("partId", p.partId)
                put("partName", p.partName)
                put("partNumber", p.partNumber)
                put("partDescription", p.partDescription)
                put("category", p.category)
                put("firm", p.firm)
                put("rack", p.rack)
                put("rackNumber", p.rackNumber)
                put("shelf", p.shelf)
                put("location", p.location)
                put("minStock", p.minStock)
                put("stockSaa", p.stockSaa)
                put("stockTvs", p.stockTvs)
                put("barcode", p.barcode)
                put("supplierCode", p.supplierCode)
                put("supplierName", p.supplierName)
                put("purchasePrice", p.purchasePrice)
                put("sellingPrice", p.sellingPrice)
                put("createdTimestamp", p.createdTimestamp)
                put("updatedTimestamp", p.updatedTimestamp)
                put("deviceId", p.deviceId)
                put("version", p.version)
                put("isDeleted", p.isDeleted)
            })
        }
        root.put("parts", partsArr)

        val txArr = JSONArray()
        for (t in txs) {
            txArr.put(JSONObject().apply {
                put("transactionId", t.transactionId)
                put("partId", t.partId)
                put("partName", t.partName)
                put("partNumber", t.partNumber)
                put("firm", t.firm)
                put("type", t.type)
                put("quantity", t.quantity)
                put("previousStock", t.previousStock)
                put("newStock", t.newStock)
                put("billNumber", t.billNumber)
                put("supplierName", t.supplierName)
                put("purchasePrice", t.purchasePrice)
                put("reason", t.reason)
                put("date", t.date)
                put("deviceId", t.deviceId)
                put("timestamp", t.timestamp)
            })
        }
        root.put("transactions", txArr)

        val supArr = JSONArray()
        for (s in sups) {
            supArr.put(JSONObject().apply {
                put("supplierId", s.supplierId)
                put("supplierName", s.supplierName)
                put("supplierCode", s.supplierCode)
                put("mobileNumber", s.mobileNumber)
                put("address", s.address)
                put("notes", s.notes)
            })
        }
        root.put("suppliers", supArr)

        return root.toString(2)
    }

    /**
     * Restore database from JSON with safety check
     */
    suspend fun restoreDatabaseJson(jsonStr: String): Result<Int> {
        return try {
            val root = JSONObject(jsonStr)
            val partsArr = root.optJSONArray("parts") ?: JSONArray()
            val txArr = root.optJSONArray("transactions") ?: JSONArray()
            val supArr = root.optJSONArray("suppliers") ?: JSONArray()

            val partsList = mutableListOf<PartEntity>()
            for (i in 0 until partsArr.length()) {
                val o = partsArr.getJSONObject(i)
                partsList.add(
                    PartEntity(
                        partId = o.optString("partId", UUID.randomUUID().toString()),
                        partName = o.optString("partName"),
                        partNumber = o.optString("partNumber"),
                        partDescription = o.optString("partDescription"),
                        category = o.optString("category", "General"),
                        firm = o.optString("firm", "BOTH"),
                        rack = o.optString("rack"),
                        rackNumber = o.optString("rackNumber"),
                        shelf = o.optString("shelf"),
                        location = o.optString("location"),
                        minStock = o.optInt("minStock", 5),
                        stockSaa = o.optInt("stockSaa", 0),
                        stockTvs = o.optInt("stockTvs", 0),
                        barcode = o.optString("barcode"),
                        supplierCode = o.optString("supplierCode"),
                        supplierName = o.optString("supplierName"),
                        purchasePrice = o.optDouble("purchasePrice", 0.0),
                        sellingPrice = o.optDouble("sellingPrice", 0.0),
                        createdTimestamp = o.optLong("createdTimestamp", System.currentTimeMillis()),
                        updatedTimestamp = o.optLong("updatedTimestamp", System.currentTimeMillis()),
                        deviceId = o.optString("deviceId"),
                        version = o.optLong("version", 1L),
                        isDeleted = o.optBoolean("isDeleted", false)
                    )
                )
            }

            database.withTransaction {
                database.partDao().insertOrUpdateParts(partsList)
            }
            Result.success(partsList.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Preload realistic Indian Automobile spare parts for Shri Amardevi Automobile & Spare Part
     */
    suspend fun seedAutomobileSampleData() {
        val count = database.partDao().getAllPartsRaw().size
        if (count > 0) return // Already seeded

        val now = System.currentTimeMillis()
        val devId = syncManager.deviceId

        val sampleSuppliers = listOf(
            SupplierEntity("sup-1", "Bosch India Automotive Ltd", "BOSCH-01", "+91 98220 12345", "Pune Auto Cluster, MH", "Primary supplier for electricals and spark plugs"),
            SupplierEntity("sup-2", "TVS Genuine Motor Parts", "TVS-MH-10", "+91 94230 67890", "Hosur Depot / Pune Distribution", "Authorized TVS genuine 2-wheeler parts"),
            SupplierEntity("sup-3", "Minda Corporation Ltd", "MINDA-03", "+91 98900 11223", "Chakan Industrial Zone", "Switches, horns, locks, wiring harness"),
            SupplierEntity("sup-4", "Castrol Automotive Lubricants", "CAS-77", "+91 91580 44556", "Viman Nagar Depot", "Engine oils, fork oils, chain lube")
        )
        database.supplierDao().insertOrUpdateSuppliers(sampleSuppliers)

        val sampleParts = listOf(
            PartEntity(
                partId = "part-1",
                partName = "Spark Plug UR4AC",
                partNumber = "BSH-UR4AC-01",
                partDescription = "Bosch Nickel spark plug for Splendor, HF Deluxe, TVS Star City",
                category = "Electrical",
                firm = Firm.BOTH.code,
                rack = "Rack-A",
                rackNumber = "01",
                shelf = "S1",
                location = "Front Counter",
                minStock = 15,
                stockSaa = 20,
                stockTvs = 10,
                barcode = "8901234001015",
                supplierCode = "BOSCH-01",
                supplierName = "Bosch India Automotive Ltd",
                purchasePrice = 75.0,
                sellingPrice = 110.0,
                createdTimestamp = now,
                updatedTimestamp = now,
                deviceId = devId
            ),
            PartEntity(
                partId = "part-2",
                partName = "Front Disc Brake Pad Set",
                partNumber = "TVS-AP-FB02",
                partDescription = "Genuine ceramic brake pads for Apache RTR 160/180/200",
                category = "Brakes",
                firm = Firm.TVS.code,
                rack = "Rack-B",
                rackNumber = "03",
                shelf = "S2",
                location = "Brake Section",
                minStock = 8,
                stockSaa = 5,
                stockTvs = 12,
                barcode = "8901234001022",
                supplierCode = "TVS-MH-10",
                supplierName = "TVS Genuine Motor Parts",
                purchasePrice = 220.0,
                sellingPrice = 320.0,
                createdTimestamp = now,
                updatedTimestamp = now,
                deviceId = devId
            ),
            PartEntity(
                partId = "part-3",
                partName = "Air Filter Element Foam",
                partNumber = "SAA-AF-201",
                partDescription = "High flow polyurethane air filter for Hero & Honda 100/110cc",
                category = "Filters",
                firm = Firm.SAA.code,
                rack = "Rack-C",
                rackNumber = "02",
                shelf = "S3",
                location = "Filter Rack Upper",
                minStock = 12,
                stockSaa = 25,
                stockTvs = 8,
                barcode = "8901234001039",
                supplierCode = "BOSCH-01",
                supplierName = "Bosch India Automotive Ltd",
                purchasePrice = 90.0,
                sellingPrice = 140.0,
                createdTimestamp = now,
                updatedTimestamp = now,
                deviceId = devId
            ),
            PartEntity(
                partId = "part-4",
                partName = "Engine Oil 4T 10W-30 (900ml)",
                partNumber = "CAS-4T-900",
                partDescription = "Castrol Activ semi-synthetic 4T motorcycle engine oil",
                category = "Lubricants",
                firm = Firm.BOTH.code,
                rack = "Rack-E",
                rackNumber = "01",
                shelf = "Ground",
                location = "Oil Pallet Bay",
                minStock = 20,
                stockSaa = 35,
                stockTvs = 20,
                barcode = "8901234001046",
                supplierCode = "CAS-77",
                supplierName = "Castrol Automotive Lubricants",
                purchasePrice = 340.0,
                sellingPrice = 425.0,
                createdTimestamp = now,
                updatedTimestamp = now,
                deviceId = devId
            ),
            PartEntity(
                partId = "part-5",
                partName = "Drive Chain & Sprocket Kit",
                partNumber = "ROL-CS-428",
                partDescription = "Heavy duty chain & sprocket kit 428-112L for Pulsar / Apache",
                category = "Transmission",
                firm = Firm.TVS.code,
                rack = "Rack-D",
                rackNumber = "04",
                shelf = "S1",
                location = "Transmission Bay",
                minStock = 6,
                stockSaa = 2,
                stockTvs = 7,
                barcode = "8901234001053",
                supplierCode = "TVS-MH-10",
                supplierName = "TVS Genuine Motor Parts",
                purchasePrice = 780.0,
                sellingPrice = 1150.0,
                createdTimestamp = now,
                updatedTimestamp = now,
                deviceId = devId
            ),
            PartEntity(
                partId = "part-6",
                partName = "Halogen Headlamp Bulb 12V 35/35W",
                partNumber = "MIN-HL-35W",
                partDescription = "Clear quartz halogen HS1 bulb for 2-wheeler headlights",
                category = "Electrical",
                firm = Firm.SAA.code,
                rack = "Rack-A",
                rackNumber = "02",
                shelf = "S1",
                location = "Front Counter Glass",
                minStock = 10,
                stockSaa = 3, // LOW STOCK!
                stockTvs = 1,
                barcode = "8901234001060",
                supplierCode = "MINDA-03",
                supplierName = "Minda Corporation Ltd",
                purchasePrice = 65.0,
                sellingPrice = 110.0,
                createdTimestamp = now,
                updatedTimestamp = now,
                deviceId = devId
            ),
            PartEntity(
                partId = "part-7",
                partName = "Clutch Cable Assembly",
                partNumber = "SAA-CC-TVS-JUP",
                partDescription = "Teflon lined low-friction clutch wire for TVS Jupiter / Wego",
                category = "General",
                firm = Firm.TVS.code,
                rack = "Rack-C",
                rackNumber = "05",
                shelf = "Hanger",
                location = "Cable Wall Hook #3",
                minStock = 5,
                stockSaa = 0, // OUT OF STOCK!
                stockTvs = 2,
                barcode = "8901234001077",
                supplierCode = "TVS-MH-10",
                supplierName = "TVS Genuine Motor Parts",
                purchasePrice = 85.0,
                sellingPrice = 140.0,
                createdTimestamp = now,
                updatedTimestamp = now,
                deviceId = devId
            )
        )
        database.partDao().insertOrUpdateParts(sampleParts)

        // Seed initial sample transactions
        val sampleTxs = listOf(
            StockTransactionEntity(
                transactionId = "tx-init-1",
                partId = "part-1",
                partName = "Spark Plug UR4AC",
                partNumber = "BSH-UR4AC-01",
                firm = Firm.SAA.code,
                type = "STOCK_IN",
                quantity = 20,
                previousStock = 0,
                newStock = 20,
                billNumber = "BILL-SAA-101",
                supplierName = "Bosch India Automotive Ltd",
                supplierCode = "BOSCH-01",
                purchasePrice = 75.0,
                reason = "Initial Stock Inward",
                date = now - 86400000L,
                deviceId = devId,
                timestamp = now - 86400000L
            ),
            StockTransactionEntity(
                transactionId = "tx-init-2",
                partId = "part-1",
                partName = "Spark Plug UR4AC",
                partNumber = "BSH-UR4AC-01",
                firm = Firm.TVS.code,
                type = "STOCK_IN",
                quantity = 10,
                previousStock = 0,
                newStock = 10,
                billNumber = "BILL-TVS-509",
                supplierName = "TVS Genuine Motor Parts",
                supplierCode = "TVS-MH-10",
                purchasePrice = 75.0,
                reason = "TVS Inward Batch",
                date = now - 86400000L,
                deviceId = devId,
                timestamp = now - 86400000L
            ),
            StockTransactionEntity(
                transactionId = "tx-init-3",
                partId = "part-6",
                partName = "Halogen Headlamp Bulb 12V 35/35W",
                partNumber = "MIN-HL-35W",
                firm = Firm.SAA.code,
                type = "STOCK_OUT",
                quantity = 4,
                previousStock = 7,
                newStock = 3,
                billNumber = "",
                supplierName = "",
                reason = "Workshop Repair Installation",
                date = now,
                deviceId = devId,
                timestamp = now
            )
        )
        database.stockTransactionDao().insertTransactions(sampleTxs)
    }

    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
