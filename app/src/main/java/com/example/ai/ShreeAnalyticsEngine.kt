package com.example.ai

import com.example.data.model.Firm
import com.example.data.model.PartEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.SupplierEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PendingUrgency(val title: String, val badgeColorHex: Long) {
    CRITICAL_ZERO("Zero Stock - Urgent", 0xFFD32F2F),
    HIGH_DEMAND_LOW_STOCK("Fast Moving - Shortage", 0xFFE65100),
    BELOW_MIN_THRESHOLD("Below Min Stock", 0xFFF57C00),
    HEALTHY("Stock Adequate", 0xFF388E3C)
}

data class PendingPartItem(
    val partId: String,
    val partName: String,
    val partNumber: String,
    val category: String,
    val firm: String,
    val rackLocation: String,
    val currentStock: Int,
    val stockSaa: Int,
    val stockTvs: Int,
    val minStock: Int,
    val shortageUnits: Int,
    val recommendedOrderQty: Int,
    val unitPurchasePrice: Double,
    val estimatedOrderCost: Double,
    val preferredSupplierName: String,
    val preferredSupplierCode: String,
    val supplierMobile: String,
    val totalOutward30Days: Int,
    val totalInward30Days: Int,
    val lastStockOutTimestamp: Long?,
    val lastPurchaseTimestamp: Long?,
    val urgency: PendingUrgency
)

data class SupplierOrderGroup(
    val supplierName: String,
    val supplierMobile: String,
    val itemCount: Int,
    val totalEstimatedCost: Double,
    val parts: List<PendingPartItem>
)

data class PendingPartsReport(
    val pendingItems: List<PendingPartItem>,
    val totalPendingCount: Int,
    val criticalZeroStockCount: Int,
    val belowMinStockCount: Int,
    val fastMovingShortageCount: Int,
    val totalEstimatedReorderCost: Double,
    val saaPendingCount: Int,
    val tvsPendingCount: Int,
    val supplierGroups: List<SupplierOrderGroup>,
    val todayPurchaseCount: Int,
    val todayStockOutCount: Int,
    val generatedTimestamp: Long = System.currentTimeMillis()
)

object ShreeAnalyticsEngine {

    private const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L

    /**
     * Core intelligence function: Analyzes inventory stock vs purchases vs order/counter outflows
     * to identify pending parts that need to be reordered.
     */
    fun analyzeInventory(
        parts: List<PartEntity>,
        transactions: List<StockTransactionEntity>,
        suppliers: List<SupplierEntity>
    ): PendingPartsReport {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - THIRTY_DAYS_MS
        val startOfToday = getStartOfToday()

        // Index suppliers by name (lowercase) and code
        val supplierByName = suppliers.associateBy { it.supplierName.trim().lowercase(Locale.ROOT) }
        val supplierByCode = suppliers.filter { it.supplierCode.isNotBlank() }
            .associateBy { it.supplierCode.trim().lowercase(Locale.ROOT) }

        // Group transactions by partId
        val transactionsByPart = transactions.groupBy { it.partId }

        // Today metrics
        var todayPurchases = 0
        var todayStockOuts = 0
        transactions.forEach { tx ->
            if (tx.timestamp >= startOfToday) {
                if (tx.type == "STOCK_IN") todayPurchases++
                if (tx.type == "STOCK_OUT") todayStockOuts++
            }
        }

        val pendingList = mutableListOf<PendingPartItem>()

        for (part in parts) {
            val partTransactions = transactionsByPart[part.partId] ?: emptyList()

            // Calculate 30-day velocity
            var outward30Days = 0
            var inward30Days = 0
            var lastOutwardTime: Long? = null
            var lastInwardTime: Long? = null
            var lastInwardSupplier = ""

            for (tx in partTransactions) {
                if (tx.type == "STOCK_OUT") {
                    if (lastOutwardTime == null || tx.timestamp > lastOutwardTime) {
                        lastOutwardTime = tx.timestamp
                    }
                    if (tx.timestamp >= thirtyDaysAgo) {
                        outward30Days += tx.quantity
                    }
                } else if (tx.type == "STOCK_IN") {
                    if (lastInwardTime == null || tx.timestamp > lastInwardTime) {
                        lastInwardTime = tx.timestamp
                        lastInwardSupplier = tx.supplierName
                    }
                    if (tx.timestamp >= thirtyDaysAgo) {
                        inward30Days += tx.quantity
                    }
                }
            }

            val currentStock = part.totalStock
            val isZero = currentStock <= 0
            val isBelowMin = currentStock <= part.minStock
            val isFastMovingShortage = outward30Days > 0 && currentStock <= (outward30Days / 2).coerceAtLeast(part.minStock)

            // Determine if pending reorder is needed
            if (isZero || isBelowMin || isFastMovingShortage) {
                val urgency = when {
                    isZero -> PendingUrgency.CRITICAL_ZERO
                    isFastMovingShortage && currentStock <= part.minStock -> PendingUrgency.HIGH_DEMAND_LOW_STOCK
                    isBelowMin -> PendingUrgency.BELOW_MIN_THRESHOLD
                    else -> PendingUrgency.HEALTHY
                }

                val shortage = (part.minStock - currentStock).coerceAtLeast(0)

                // Smart Recommended Reorder Quantity:
                // Base target: double minimum stock minus current stock.
                // If fast moving, account for expected 30-day demand.
                val targetBuffer = part.minStock * 2
                val velocityNeed = if (outward30Days > 0) (outward30Days * 1.3).toInt() else 0
                val calculatedQty = maxOf(targetBuffer - currentStock, velocityNeed)
                val recommendedQty = maxOf(5, calculatedQty) // minimum 5 pcs standard pack

                val unitPrice = if (part.purchasePrice > 0.0) part.purchasePrice else (part.sellingPrice * 0.70)
                val estimatedCost = recommendedQty * unitPrice

                // Resolve supplier contact details
                val supplierCode = part.supplierCode.trim()
                val candidateSupplierName = when {
                    part.supplierName.isNotBlank() -> part.supplierName.trim()
                    lastInwardSupplier.isNotBlank() -> lastInwardSupplier.trim()
                    else -> ""
                }

                val supplierEntity = supplierByCode[supplierCode.lowercase(Locale.ROOT)]
                    ?: supplierByName[candidateSupplierName.lowercase(Locale.ROOT)]

                val supplierPhone = supplierEntity?.mobileNumber ?: ""
                val finalSupplierName = supplierEntity?.supplierName ?: candidateSupplierName.ifBlank { "General Distributor" }

                val rackStr = buildString {
                    if (part.rack.isNotBlank()) append("Rack ${part.rack}")
                    if (part.rackNumber.isNotBlank()) append("-${part.rackNumber}")
                    if (part.shelf.isNotBlank()) append(", Shelf ${part.shelf}")
                }.ifBlank { "Unassigned Rack" }

                pendingList.add(
                    PendingPartItem(
                        partId = part.partId,
                        partName = part.partName,
                        partNumber = part.partNumber,
                        category = part.category,
                        firm = part.firm,
                        rackLocation = rackStr,
                        currentStock = currentStock,
                        stockSaa = part.stockSaa,
                        stockTvs = part.stockTvs,
                        minStock = part.minStock,
                        shortageUnits = shortage,
                        recommendedOrderQty = recommendedQty,
                        unitPurchasePrice = unitPrice,
                        estimatedOrderCost = estimatedCost,
                        preferredSupplierName = finalSupplierName,
                        preferredSupplierCode = supplierCode,
                        supplierMobile = supplierPhone,
                        totalOutward30Days = outward30Days,
                        totalInward30Days = inward30Days,
                        lastStockOutTimestamp = lastOutwardTime,
                        lastPurchaseTimestamp = lastInwardTime,
                        urgency = urgency
                    )
                )
            }
        }

        // Sort by urgency: CRITICAL_ZERO first, then HIGH_DEMAND, then BELOW_MIN
        pendingList.sortWith(
            compareBy<PendingPartItem> { it.urgency.ordinal }
                .thenBy { it.currentStock }
                .thenByDescending { it.totalOutward30Days }
        )

        val criticalZero = pendingList.count { it.urgency == PendingUrgency.CRITICAL_ZERO }
        val belowMin = pendingList.count { it.urgency == PendingUrgency.BELOW_MIN_THRESHOLD }
        val fastMoving = pendingList.count { it.urgency == PendingUrgency.HIGH_DEMAND_LOW_STOCK }
        val totalCost = pendingList.sumOf { it.estimatedOrderCost }
        val saaPending = pendingList.count { it.firm == Firm.SAA.code || it.firm == Firm.BOTH.code }
        val tvsPending = pendingList.count { it.firm == Firm.TVS.code || it.firm == Firm.BOTH.code }

        // Group by supplier
        val supplierGroups = pendingList.groupBy { it.preferredSupplierName }
            .map { (supplier, items) ->
                val mobile = items.firstOrNull { it.supplierMobile.isNotBlank() }?.supplierMobile ?: ""
                SupplierOrderGroup(
                    supplierName = supplier,
                    supplierMobile = mobile,
                    itemCount = items.size,
                    totalEstimatedCost = items.sumOf { it.estimatedOrderCost },
                    parts = items
                )
            }.sortedByDescending { it.itemCount }

        return PendingPartsReport(
            pendingItems = pendingList,
            totalPendingCount = pendingList.size,
            criticalZeroStockCount = criticalZero,
            belowMinStockCount = belowMin,
            fastMovingShortageCount = fastMoving,
            totalEstimatedReorderCost = totalCost,
            saaPendingCount = saaPending,
            tvsPendingCount = tvsPending,
            supplierGroups = supplierGroups,
            todayPurchaseCount = todayPurchases,
            todayStockOutCount = todayStockOuts,
            generatedTimestamp = now
        )
    }

    /**
     * Formats pending parts into an official WhatsApp Purchase Order text message.
     */
    fun buildWhatsAppOrderMessage(
        report: PendingPartsReport,
        supplierFilter: String? = null
    ): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val filteredItems = if (!supplierFilter.isNullOrBlank()) {
            report.pendingItems.filter { it.preferredSupplierName.equals(supplierFilter, ignoreCase = true) }
        } else {
            report.pendingItems
        }

        return buildString {
            append("📦 *PURCHASE ORDER - SHRI AMARDEVI AUTOMOBILE*\n")
            append("📅 *Date:* $dateStr\n")
            if (!supplierFilter.isNullOrBlank()) {
                append("🏢 *Supplier:* $supplierFilter\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("*पेंडिंग पार्ट्स रीऑर्डर लिस्ट:*\n\n")

            if (filteredItems.isEmpty()) {
                append("वर्तमान में कोई पेंडिंग पार्ट नहीं है।\n")
            } else {
                filteredItems.forEachIndexed { index, item ->
                    val statusTag = if (item.currentStock == 0) "[🚨 OUT OF STOCK]" else "[Current: ${item.currentStock}]"
                    append("${index + 1}. *${item.partName}*\n")
                    append("   • Part No: ${item.partNumber} (${item.firm})\n")
                    append("   • *Order Qty: ${item.recommendedOrderQty} pcs* $statusTag\n")
                    append("   • Location: ${item.rackLocation}\n")
                    if (item.unitPurchasePrice > 0.0) {
                        append("   • Est Rate: ₹${item.unitPurchasePrice.toInt()}\n")
                    }
                    append("\n")
                }
            }

            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("📊 कुल आइटम्स: ${filteredItems.size}\n")
            val totalCost = filteredItems.sumOf { it.estimatedOrderCost }
            if (totalCost > 0.0) {
                append("💰 अनुमानित आर्डर राशि: ₹${totalCost.toInt()}\n")
            }
            append("🏪 *Shri Amardevi Automobile & Spare Part*\n")
            append("✨ _Generated via Shree AI Assistant_")
        }
    }

    private fun getStartOfToday(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
