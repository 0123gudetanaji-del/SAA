package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class Firm(val code: String, val displayName: String) {
    SAA("SAA", "SAA"),
    TVS("TVS", "TVS"),
    BOTH("BOTH", "Both (SAA & TVS)")
}

enum class TransactionType(val label: String) {
    STOCK_IN("Stock In / Purchase"),
    STOCK_OUT("Stock Out"),
    ADJUSTMENT("Adjustment"),
    SYNC_UPDATE("Sync Update")
}

enum class SyncStatus {
    SYNCED,
    SYNCING,
    WAITING_FOR_DEVICE,
    CONFLICT,
    FAILED
}

@Entity(
    tableName = "parts",
    indices = [
        Index(value = ["partNumber"], unique = false),
        Index(value = ["barcode"], unique = false),
        Index(value = ["partName"]),
        Index(value = ["supplierCode"]),
        Index(value = ["category"])
    ]
)
data class PartEntity(
    @PrimaryKey val partId: String = UUID.randomUUID().toString(),
    val partName: String,
    val partNumber: String,
    val partDescription: String = "",
    val category: String = "General",
    val firm: String = Firm.BOTH.code, // SAA, TVS, BOTH
    val rack: String = "",
    val rackNumber: String = "",
    val shelf: String = "",
    val location: String = "",
    val minStock: Int = 5,
    val stockSaa: Int = 0,
    val stockTvs: Int = 0,
    val barcode: String = "",
    val supplierCode: String = "",
    val supplierName: String = "",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    val totalStock: Int get() = stockSaa + stockTvs

    fun isLowStock(): Boolean = totalStock > 0 && totalStock <= minStock
    fun isOutOfStock(): Boolean = totalStock <= 0
}

@Entity(
    tableName = "stock_transactions",
    indices = [
        Index(value = ["partId"]),
        Index(value = ["billNumber"]),
        Index(value = ["timestamp"])
    ]
)
data class StockTransactionEntity(
    @PrimaryKey val transactionId: String = UUID.randomUUID().toString(),
    val partId: String,
    val partName: String,
    val partNumber: String,
    val firm: String, // SAA or TVS
    val type: String, // STOCK_IN, STOCK_OUT, ADJUSTMENT
    val quantity: Int,
    val previousStock: Int,
    val newStock: Int,
    val billNumber: String = "",
    val supplierName: String = "",
    val supplierCode: String = "",
    val purchasePrice: Double = 0.0,
    val reason: String = "",
    val date: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["supplierCode"], unique = false),
        Index(value = ["supplierName"])
    ]
)
data class SupplierEntity(
    @PrimaryKey val supplierId: String = UUID.randomUUID().toString(),
    val supplierName: String,
    val supplierCode: String = "",
    val mobileNumber: String = "",
    val address: String = "",
    val notes: String = "",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val version: Long = 1L,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "sync_change_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["isSynced"])
    ]
)
data class SyncChangeLogEntity(
    @PrimaryKey val changeId: String = UUID.randomUUID().toString(),
    val entityType: String, // PART, TRANSACTION, SUPPLIER
    val entityId: String,
    val operation: String, // CREATE, UPDATE, DELETE, STOCK_IN, STOCK_OUT
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val version: Long = 1L,
    val isSynced: Boolean = false
)

@Entity(tableName = "sync_conflicts")
data class SyncConflictEntity(
    @PrimaryKey val conflictId: String = UUID.randomUUID().toString(),
    val entityType: String,
    val entityId: String,
    val entityTitle: String,
    val localTimestamp: Long,
    val remoteTimestamp: Long,
    val remoteDeviceId: String,
    val resolution: String, // LATEST_TIMESTAMP_WINS
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["timestamp"])]
)
data class AuditLogEntity(
    @PrimaryKey val auditId: String = UUID.randomUUID().toString(),
    val entityType: String,
    val entityId: String,
    val operationType: String,
    val fieldName: String,
    val oldValue: String,
    val newValue: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String = ""
)

@Entity(tableName = "trusted_devices")
data class TrustedDeviceEntity(
    @PrimaryKey val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val port: Int = 18889,
    val lastSyncTimestamp: Long = 0L,
    val isTrusted: Boolean = true,
    val pairingKey: String = "SAA-DEFAULT"
)
