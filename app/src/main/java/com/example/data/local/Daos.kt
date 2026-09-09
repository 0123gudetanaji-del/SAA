package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AuditLogEntity
import com.example.data.model.PartEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.SyncChangeLogEntity
import com.example.data.model.SyncConflictEntity
import com.example.data.model.TrustedDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {
    @Query("SELECT * FROM parts WHERE isDeleted = 0 ORDER BY partName ASC")
    fun getAllActiveParts(): Flow<List<PartEntity>>

    @Query("SELECT * FROM parts WHERE isDeleted = 0 AND ((stockSaa + stockTvs) <= minStock) ORDER BY (stockSaa + stockTvs) ASC")
    fun getLowStockParts(): Flow<List<PartEntity>>

    @Query("SELECT * FROM parts WHERE partId = :id")
    suspend fun getPartById(id: String): PartEntity?

    @Query("SELECT * FROM parts WHERE partNumber = :partNumber AND isDeleted = 0 LIMIT 1")
    suspend fun getPartByPartNumber(partNumber: String): PartEntity?

    @Query("SELECT * FROM parts WHERE barcode = :barcode AND isDeleted = 0 LIMIT 1")
    suspend fun getPartByBarcode(barcode: String): PartEntity?

    @Query("""
        SELECT * FROM parts 
        WHERE isDeleted = 0 AND (
            partName LIKE '%' || :query || '%' OR 
            partNumber LIKE '%' || :query || '%' OR 
            barcode LIKE '%' || :query || '%' OR 
            supplierCode LIKE '%' || :query || '%' OR 
            rack LIKE '%' || :query || '%' OR 
            rackNumber LIKE '%' || :query || '%' OR 
            shelf LIKE '%' || :query || '%' OR 
            location LIKE '%' || :query || '%'
        )
        ORDER BY partName ASC
    """)
    fun searchParts(query: String): Flow<List<PartEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePart(part: PartEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateParts(parts: List<PartEntity>)

    @Query("SELECT * FROM parts WHERE updatedTimestamp > :sinceTimestamp")
    suspend fun getPartsModifiedSince(sinceTimestamp: Long): List<PartEntity>

    @Query("SELECT COUNT(*) FROM parts WHERE isDeleted = 0")
    fun getTotalPartsCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(stockSaa + stockTvs), 0) FROM parts WHERE isDeleted = 0")
    fun getTotalStockUnitsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM parts WHERE isDeleted = 0 AND (stockSaa + stockTvs) <= minStock")
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT * FROM parts")
    suspend fun getAllPartsRaw(): List<PartEntity>
}

@Dao
interface StockTransactionDao {
    @Query("SELECT * FROM stock_transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE partId = :partId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsForPart(partId: String): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE type = 'STOCK_IN' AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE timestamp >= :startOfDay AND type = 'STOCK_IN' AND isDeleted = 0")
    fun getTodayPurchases(startOfDay: Long): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE timestamp >= :startOfDay AND type = 'STOCK_OUT' AND isDeleted = 0")
    fun getTodayStockOuts(startOfDay: Long): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE timestamp > :sinceTimestamp")
    suspend fun getTransactionsModifiedSince(sinceTimestamp: Long): List<StockTransactionEntity>

    @Query("SELECT * FROM stock_transactions WHERE timestamp > :sinceTimestamp")
    suspend fun getTransactionsSince(sinceTimestamp: Long): List<StockTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<StockTransactionEntity>)

    @Query("SELECT * FROM stock_transactions")
    suspend fun getAllTransactionsRaw(): List<StockTransactionEntity>
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE isDeleted = 0 ORDER BY supplierName ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE supplierId = :id")
    suspend fun getSupplierById(id: String): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE supplierCode = :code AND isDeleted = 0 LIMIT 1")
    suspend fun getSupplierByCode(code: String): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSupplier(supplier: SupplierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSuppliers(suppliers: List<SupplierEntity>)

    @Query("SELECT * FROM suppliers WHERE updatedTimestamp > :sinceTimestamp")
    suspend fun getSuppliersModifiedSince(sinceTimestamp: Long): List<SupplierEntity>

    @Query("SELECT * FROM suppliers")
    suspend fun getAllSuppliersRaw(): List<SupplierEntity>
}

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChangeLog(log: SyncChangeLogEntity)

    @Query("SELECT * FROM sync_change_logs WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedChanges(): List<SyncChangeLogEntity>

    @Query("UPDATE sync_change_logs SET isSynced = 1 WHERE changeId IN (:changeIds)")
    suspend fun markChangesSynced(changeIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConflict(conflict: SyncConflictEntity)

    @Query("SELECT * FROM sync_conflicts ORDER BY timestamp DESC")
    fun getAllConflicts(): Flow<List<SyncConflictEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrustedDevice(device: TrustedDeviceEntity)

    @Query("SELECT * FROM trusted_devices ORDER BY lastSyncTimestamp DESC")
    fun getAllTrustedDevices(): Flow<List<TrustedDeviceEntity>>

    @Query("SELECT * FROM trusted_devices WHERE deviceId = :deviceId")
    suspend fun getTrustedDevice(deviceId: String): TrustedDeviceEntity?
}

@Dao
interface AuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>
}
