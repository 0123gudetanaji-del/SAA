package com.example.data.sync

import com.example.data.model.PartEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.SupplierEntity
import org.json.JSONArray
import org.json.JSONObject

data class DiscoveredDevice(
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val port: Int = 18889,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val lastSyncTimestamp: Long = 0L,
    val isConnected: Boolean = false,
    val statusText: String = "Discovered"
)

data class SyncPacket(
    val action: String, // SYNC_EXCHANGE, PING, PONG
    val deviceId: String,
    val deviceName: String,
    val pairingKey: String,
    val sinceTimestamp: Long,
    val parts: List<PartEntity> = emptyList(),
    val transactions: List<StockTransactionEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("action", action)
        root.put("deviceId", deviceId)
        root.put("deviceName", deviceName)
        root.put("pairingKey", pairingKey)
        root.put("sinceTimestamp", sinceTimestamp)

        val partsArray = JSONArray()
        for (p in parts) {
            val po = JSONObject()
            po.put("partId", p.partId)
            po.put("partName", p.partName)
            po.put("partNumber", p.partNumber)
            po.put("partDescription", p.partDescription)
            po.put("category", p.category)
            po.put("firm", p.firm)
            po.put("rack", p.rack)
            po.put("rackNumber", p.rackNumber)
            po.put("shelf", p.shelf)
            po.put("location", p.location)
            po.put("minStock", p.minStock)
            po.put("stockSaa", p.stockSaa)
            po.put("stockTvs", p.stockTvs)
            po.put("barcode", p.barcode)
            po.put("supplierCode", p.supplierCode)
            po.put("supplierName", p.supplierName)
            po.put("purchasePrice", p.purchasePrice)
            po.put("sellingPrice", p.sellingPrice)
            po.put("createdTimestamp", p.createdTimestamp)
            po.put("updatedTimestamp", p.updatedTimestamp)
            po.put("deviceId", p.deviceId)
            po.put("version", p.version)
            po.put("isDeleted", p.isDeleted)
            partsArray.put(po)
        }
        root.put("parts", partsArray)

        val txArray = JSONArray()
        for (tx in transactions) {
            val to = JSONObject()
            to.put("transactionId", tx.transactionId)
            to.put("partId", tx.partId)
            to.put("partName", tx.partName)
            to.put("partNumber", tx.partNumber)
            to.put("firm", tx.firm)
            to.put("type", tx.type)
            to.put("quantity", tx.quantity)
            to.put("previousStock", tx.previousStock)
            to.put("newStock", tx.newStock)
            to.put("billNumber", tx.billNumber)
            to.put("supplierName", tx.supplierName)
            to.put("supplierCode", tx.supplierCode)
            to.put("purchasePrice", tx.purchasePrice)
            to.put("reason", tx.reason)
            to.put("date", tx.date)
            to.put("deviceId", tx.deviceId)
            to.put("timestamp", tx.timestamp)
            to.put("version", tx.version)
            to.put("isDeleted", tx.isDeleted)
            txArray.put(to)
        }
        root.put("transactions", txArray)

        val supArray = JSONArray()
        for (s in suppliers) {
            val so = JSONObject()
            so.put("supplierId", s.supplierId)
            so.put("supplierName", s.supplierName)
            so.put("supplierCode", s.supplierCode)
            so.put("mobileNumber", s.mobileNumber)
            so.put("address", s.address)
            so.put("notes", s.notes)
            so.put("createdTimestamp", s.createdTimestamp)
            so.put("updatedTimestamp", s.updatedTimestamp)
            so.put("deviceId", s.deviceId)
            so.put("version", s.version)
            so.put("isDeleted", s.isDeleted)
            supArray.put(so)
        }
        root.put("suppliers", supArray)

        return root.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): SyncPacket {
            val root = JSONObject(jsonStr)
            val action = root.optString("action", "SYNC_EXCHANGE")
            val deviceId = root.optString("deviceId", "")
            val deviceName = root.optString("deviceName", "")
            val pairingKey = root.optString("pairingKey", "")
            val sinceTimestamp = root.optLong("sinceTimestamp", 0L)

            val partsList = mutableListOf<PartEntity>()
            val partsArray = root.optJSONArray("parts")
            if (partsArray != null) {
                for (i in 0 until partsArray.length()) {
                    val po = partsArray.getJSONObject(i)
                    partsList.add(
                        PartEntity(
                            partId = po.optString("partId"),
                            partName = po.optString("partName"),
                            partNumber = po.optString("partNumber"),
                            partDescription = po.optString("partDescription"),
                            category = po.optString("category", "General"),
                            firm = po.optString("firm", "BOTH"),
                            rack = po.optString("rack"),
                            rackNumber = po.optString("rackNumber"),
                            shelf = po.optString("shelf"),
                            location = po.optString("location"),
                            minStock = po.optInt("minStock", 5),
                            stockSaa = po.optInt("stockSaa", 0),
                            stockTvs = po.optInt("stockTvs", 0),
                            barcode = po.optString("barcode"),
                            supplierCode = po.optString("supplierCode"),
                            supplierName = po.optString("supplierName"),
                            purchasePrice = po.optDouble("purchasePrice", 0.0),
                            sellingPrice = po.optDouble("sellingPrice", 0.0),
                            createdTimestamp = po.optLong("createdTimestamp", System.currentTimeMillis()),
                            updatedTimestamp = po.optLong("updatedTimestamp", System.currentTimeMillis()),
                            deviceId = po.optString("deviceId"),
                            version = po.optLong("version", 1L),
                            isDeleted = po.optBoolean("isDeleted", false)
                        )
                    )
                }
            }

            val txList = mutableListOf<StockTransactionEntity>()
            val txArray = root.optJSONArray("transactions")
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val to = txArray.getJSONObject(i)
                    txList.add(
                        StockTransactionEntity(
                            transactionId = to.optString("transactionId"),
                            partId = to.optString("partId"),
                            partName = to.optString("partName"),
                            partNumber = to.optString("partNumber"),
                            firm = to.optString("firm", "SAA"),
                            type = to.optString("type", "STOCK_IN"),
                            quantity = to.optInt("quantity", 0),
                            previousStock = to.optInt("previousStock", 0),
                            newStock = to.optInt("newStock", 0),
                            billNumber = to.optString("billNumber"),
                            supplierName = to.optString("supplierName"),
                            supplierCode = to.optString("supplierCode"),
                            purchasePrice = to.optDouble("purchasePrice", 0.0),
                            reason = to.optString("reason"),
                            date = to.optLong("date", System.currentTimeMillis()),
                            deviceId = to.optString("deviceId"),
                            timestamp = to.optLong("timestamp", System.currentTimeMillis()),
                            version = to.optLong("version", 1L),
                            isDeleted = to.optBoolean("isDeleted", false)
                        )
                    )
                }
            }

            val supList = mutableListOf<SupplierEntity>()
            val supArray = root.optJSONArray("suppliers")
            if (supArray != null) {
                for (i in 0 until supArray.length()) {
                    val so = supArray.getJSONObject(i)
                    supList.add(
                        SupplierEntity(
                            supplierId = so.optString("supplierId"),
                            supplierName = so.optString("supplierName"),
                            supplierCode = so.optString("supplierCode"),
                            mobileNumber = so.optString("mobileNumber"),
                            address = so.optString("address"),
                            notes = so.optString("notes"),
                            createdTimestamp = so.optLong("createdTimestamp", System.currentTimeMillis()),
                            updatedTimestamp = so.optLong("updatedTimestamp", System.currentTimeMillis()),
                            deviceId = so.optString("deviceId"),
                            version = so.optLong("version", 1L),
                            isDeleted = so.optBoolean("isDeleted", false)
                        )
                    )
                }
            }

            return SyncPacket(
                action = action,
                deviceId = deviceId,
                deviceName = deviceName,
                pairingKey = pairingKey,
                sinceTimestamp = sinceTimestamp,
                parts = partsList,
                transactions = txList,
                suppliers = supList
            )
        }
    }
}
