package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.PartEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.SyncChangeLogEntity
import com.example.data.model.SyncConflictEntity
import com.example.data.model.TrustedDeviceEntity

@Database(
    entities = [
        PartEntity::class,
        StockTransactionEntity::class,
        SupplierEntity::class,
        SyncChangeLogEntity::class,
        SyncConflictEntity::class,
        AuditLogEntity::class,
        TrustedDeviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partDao(): PartDao
    abstract fun stockTransactionDao(): StockTransactionDao
    abstract fun supplierDao(): SupplierDao
    abstract fun syncDao(): SyncDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "saa_inventory.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
