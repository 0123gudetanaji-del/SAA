package com.example

import com.example.data.model.Firm
import com.example.data.model.PartEntity
import com.example.data.sync.SyncPacket
import com.example.util.BarcodeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InventoryBusinessLogicTest {

    @Test
    fun testBarcodePatternGeneration() {
        val pattern = BarcodeUtil.encodeCode128("TEST1234")
        assertNotNull(pattern)
        assertTrue(pattern.isNotEmpty())
        // Start quiet zone has 10 spaces
        assertFalse(pattern[0])
        // First bar starts at index 10
        assertTrue(pattern[10])
    }

    @Test
    fun testPartStockCalculations() {
        val part = PartEntity(
            partId = "part-1",
            partName = "Disc Brake Pad Set",
            partNumber = "BP-001",
            firm = Firm.SAA.code,
            stockSaa = 15,
            stockTvs = 5,
            minStock = 10
        )

        assertEquals(20, part.totalStock)
        assertFalse(part.isLowStock())
        assertFalse(part.isOutOfStock())

        val lowPart = part.copy(stockSaa = 4, stockTvs = 2) // total = 6 <= 10
        assertEquals(6, lowPart.totalStock)
        assertTrue(lowPart.isLowStock())

        val outPart = part.copy(stockSaa = 0, stockTvs = 0)
        assertEquals(0, outPart.totalStock)
        assertTrue(outPart.isOutOfStock())
    }

    @Test
    fun testSyncPacketSerialization() {
        val packet = SyncPacket(
            action = "SYNC_EXCHANGE",
            deviceId = "phone-a-1234",
            deviceName = "Phone A",
            pairingKey = "SAA-AMARDEVI-2026",
            sinceTimestamp = 0L,
            parts = listOf(
                PartEntity(
                    partId = "part-1",
                    partName = "Clutch Cable",
                    partNumber = "CC-450",
                    firm = "SAA",
                    stockSaa = 20,
                    stockTvs = 0
                )
            ),
            transactions = emptyList(),
            suppliers = emptyList()
        )

        val json = packet.toJson()
        assertNotNull(json)
        assertTrue(json.contains("phone-a-1234"))
        assertTrue(json.contains("Clutch Cable"))
        assertTrue(json.contains("SAA-AMARDEVI-2026"))

        val deserialized = SyncPacket.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(packet.deviceId, deserialized?.deviceId)
        assertEquals(packet.parts.size, deserialized?.parts?.size)
        assertEquals("Clutch Cable", deserialized?.parts?.get(0)?.partName)
    }
}
