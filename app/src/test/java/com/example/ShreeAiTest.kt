package com.example

import com.example.ai.PendingUrgency
import com.example.ai.ShreeAiEngine
import com.example.ai.ShreeAnalyticsEngine
import com.example.ai.ShreeIntent
import com.example.data.model.Firm
import com.example.data.model.PartEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.SupplierEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShreeAiTest {

    private val sampleParts = listOf(
        PartEntity(
            partId = "P001",
            partName = "Spark Plug NGK CPR8EA",
            partNumber = "CPR8EA-9",
            category = "Engine",
            firm = Firm.SAA.code,
            rack = "A",
            rackNumber = "12",
            shelf = "3",
            stockSaa = 0,
            stockTvs = 0,
            minStock = 5,
            purchasePrice = 120.0,
            sellingPrice = 180.0,
            supplierName = "NGK Spark Plugs India"
        ),
        PartEntity(
            partId = "P002",
            partName = "Brake Shoe Set Apache",
            partNumber = "N9111380",
            category = "Brakes",
            firm = Firm.TVS.code,
            rack = "B",
            rackNumber = "04",
            shelf = "1",
            stockSaa = 0,
            stockTvs = 2,
            minStock = 8,
            purchasePrice = 280.0,
            sellingPrice = 380.0,
            supplierName = "TVS Genuine Spares"
        ),
        PartEntity(
            partId = "P003",
            partName = "Engine Oil 4T 10W30 900ml",
            partNumber = "OIL-10W30",
            category = "Lubricants",
            firm = Firm.BOTH.code,
            rack = "E",
            rackNumber = "01",
            shelf = "Floor",
            stockSaa = 20,
            stockTvs = 15,
            minStock = 10,
            purchasePrice = 320.0,
            sellingPrice = 420.0,
            supplierName = "Castrol India"
        )
    )

    private val sampleSuppliers = listOf(
        SupplierEntity(
            supplierId = "SUP01",
            supplierName = "NGK Spark Plugs India",
            supplierCode = "NGK-IND",
            mobileNumber = "9823011223",
            address = "Pune"
        ),
        SupplierEntity(
            supplierId = "SUP02",
            supplierName = "TVS Genuine Spares",
            supplierCode = "TVS-DIST",
            mobileNumber = "9823099887",
            address = "Nashik"
        )
    )

    private val sampleTransactions = listOf(
        StockTransactionEntity(
            transactionId = "TX01",
            partId = "P001",
            partName = "Spark Plug NGK CPR8EA",
            partNumber = "CPR8EA-9",
            firm = Firm.SAA.code,
            type = "STOCK_IN",
            quantity = 10,
            previousStock = 0,
            newStock = 10,
            billNumber = "INV-2026-01",
            supplierName = "NGK Spark Plugs India",
            timestamp = System.currentTimeMillis() - 1000000L
        ),
        StockTransactionEntity(
            transactionId = "TX02",
            partId = "P001",
            partName = "Spark Plug NGK CPR8EA",
            partNumber = "CPR8EA-9",
            firm = Firm.SAA.code,
            type = "STOCK_OUT",
            quantity = 10,
            previousStock = 10,
            newStock = 0,
            reason = "Mechanic Issue",
            timestamp = System.currentTimeMillis() - 500000L
        )
    )

    @Test
    fun testPendingPartsAnalysis_identifiesZeroAndLowStock() {
        val report = ShreeAnalyticsEngine.analyzeInventory(sampleParts, sampleTransactions, sampleSuppliers)

        assertEquals(2, report.totalPendingCount)
        assertEquals(1, report.criticalZeroStockCount) // P001 has 0 stock
        assertEquals(1, report.belowMinStockCount) // P002 has 2 stock < min 8

        val zeroItem = report.pendingItems.first { it.partId == "P001" }
        assertEquals(PendingUrgency.CRITICAL_ZERO, zeroItem.urgency)
        assertTrue("Recommended order should replenish shortage", zeroItem.recommendedOrderQty >= 5)
        assertEquals("NGK Spark Plugs India", zeroItem.preferredSupplierName)
        assertEquals("9823011223", zeroItem.supplierMobile)
        assertEquals("Rack A-12, Shelf 3", zeroItem.rackLocation)

        val lowStockItem = report.pendingItems.first { it.partId == "P002" }
        assertEquals(PendingUrgency.BELOW_MIN_THRESHOLD, lowStockItem.urgency)
        assertEquals("TVS Genuine Spares", lowStockItem.preferredSupplierName)
    }

    @Test
    fun testWhatsAppOrderMessageGeneration() {
        val report = ShreeAnalyticsEngine.analyzeInventory(sampleParts, sampleTransactions, sampleSuppliers)
        val message = ShreeAnalyticsEngine.buildWhatsAppOrderMessage(report)

        assertTrue(message.contains("SHRI AMARDEVI AUTOMOBILE"))
        assertTrue(message.contains("Spark Plug NGK CPR8EA"))
        assertTrue(message.contains("Brake Shoe Set Apache"))
        assertTrue(message.contains("Rack A-12, Shelf 3"))
        assertTrue(message.contains("Shree AI"))
    }

    @Test
    fun testShreeAiEngine_processesPendingPartsQuery() {
        val response = ShreeAiEngine.processQuery("pending parts list dikhao", sampleParts, sampleTransactions, sampleSuppliers)

        assertEquals(ShreeIntent.PENDING_PARTS_LIST, response.intent)
        assertNotNull(response.pendingReport)
        assertTrue(response.speechText.contains("पेंडिंग पार्ट्स"))
        assertFalse("Speech text should not have raw markdown asterisks", response.speechText.contains("**"))
    }

    @Test
    fun testShreeAiEngine_processesRackSearchQuery() {
        val response = ShreeAiEngine.processQuery("spark plug kaha rakha hai", sampleParts, sampleTransactions, sampleSuppliers)

        assertEquals(ShreeIntent.RACK_LOCATION_SEARCH, response.intent)
        assertTrue(response.matchingParts.isNotEmpty())
        assertTrue(response.displayText.contains("Rack A"))
        assertTrue(response.speechText.contains("रैक A"))
    }

    @Test
    fun testShreeAiEngine_processesWifiSyncHelp() {
        val response = ShreeAiEngine.processQuery("wifi sync kaise kare", sampleParts, sampleTransactions, sampleSuppliers)

        assertEquals(ShreeIntent.WIFI_SYNC_HELP, response.intent)
        assertTrue(response.displayText.contains("SAA-AMARDEVI-2026"))
        assertTrue(response.speechText.contains("हॉटस्पॉट"))
    }

    @Test
    fun testShreeAiEngine_processesZeroStockAlert() {
        val response = ShreeAiEngine.processQuery("zero stock parts", sampleParts, sampleTransactions, sampleSuppliers)

        assertEquals(ShreeIntent.ZERO_STOCK_ALERT, response.intent)
        assertTrue(response.displayText.contains("CPR8EA-9"))
    }
}
