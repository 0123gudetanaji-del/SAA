package com.example.ai

import com.example.data.model.Firm
import com.example.data.model.PartEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.SupplierEntity
import java.util.Locale

data class ShreeResponse(
    val displayText: String,
    val speechText: String,
    val intent: ShreeIntent,
    val pendingReport: PendingPartsReport? = null,
    val matchingParts: List<PartEntity> = emptyList(),
    val suggestedActions: List<String> = emptyList()
)

enum class ShreeIntent {
    PENDING_PARTS_LIST,
    PURCHASE_ORDER_TRACKING,
    ZERO_STOCK_ALERT,
    RACK_LOCATION_SEARCH,
    PART_STOCK_INQUIRY,
    FIRM_STOCK_SUMMARY,
    WIFI_SYNC_HELP,
    BARCODE_HELP,
    STOCK_IN_OUT_HELP,
    SUPPLIER_HELP,
    GENERAL_APP_GUIDE,
    GREETING,
    UNKNOWN_QUERY
}

object ShreeAiEngine {

    val SYSTEM_KNOWLEDGE = """
        श्री AI (Shree AI Assistant) - Shri Amardevi Automobile & Spare Part
        
        • नाम: श्री (Shree)
        • विशेषता: ऑटोमोबाइल स्पेयर पार्ट्स इन्वेंटरी, परचेस व सेल्स ट्रैकिंग और पेंडिंग रीऑर्डर लिस्ट विशेषज्ञ।
        • फर्म संरचना: 
          1) SAA (Shri Amardevi Automobile): सामान्य स्पेयर पार्ट्स (Hero, Bajaj, Honda, Yamaha आदि)
          2) TVS: टीवीएस अधिकृत स्पेयर पार्ट्स (Apache, Jupiter, XL100, Raider आदि)
        • मुख्य फीचर्स:
          - परचेस एंट्री (Stock In): सप्लायर बिल, खरीद दर और रैक आवंटन।
          - स्टॉक आउट (Stock Out): काउंटर सेल या मैकेनिक इशू।
          - पेंडिंग पार्ट्स ट्रैकिंग: स्टॉक व बिक्री की गति देखकर रीऑर्डर सूची तैयार करना।
          - कोड 128 बारकोड: कैमरा स्कैनर और शेल्फ लेबल प्रिंटिंग।
          - वाई-फाई/हॉटस्पॉट सिंक: बिना इंटरनेट दो मोबाइलों में डेटा सिंक (Pairing Key: SAA-AMARDEVI-2026)।
          - रैक सिस्टम: Rack A, B, C... Shelf 1, 2, 3... दुकान में सामान तुरंत खोजने के लिए।
    """.trimIndent()

    /**
     * Processes user text query completely offline using intelligent heuristic NLP & live DB inspection.
     */
    fun processQuery(
        query: String,
        parts: List<PartEntity>,
        transactions: List<StockTransactionEntity>,
        suppliers: List<SupplierEntity>
    ): ShreeResponse {
        val q = query.trim().lowercase(Locale.ROOT)
        val pendingReport = ShreeAnalyticsEngine.analyzeInventory(parts, transactions, suppliers)

        return when {
            // 1. Pending parts list / shortage / what to order
            matchesAny(q, "pending", "order list", "reorder", "shortage", "khatam", "kya mangwana", "mangao", "purchase list", "order karna hai", "kam stock", "low stock", "पेंडिंग", "कमी") -> {
                handlePendingPartsQuery(pendingReport)
            }

            // 2. Purchase and order tracking / Today's activity / Inward vs Outward
            matchesAny(q, "purchase track", "order track", "today purchase", "today sales", "inward", "outward", "aaj kitna", "aaj ki purchase", "entry track", "bikri", "khareed", "tracking", "ट्रांसैक्शन") -> {
                handlePurchaseOrderTrackingQuery(pendingReport, transactions)
            }

            // 3. Out of stock / Zero stock items
            matchesAny(q, "zero stock", "out of stock", "nil stock", "khatam part", "shunya", "खत्म", "शून्य") -> {
                handleZeroStockQuery(pendingReport)
            }

            // 4. Rack / Shelf location query
            matchesAny(q, "rack", "shelf", "kaha rakha", "location", "almirah", "kaha hai", "रैक", "कहाँ") -> {
                handleRackLocationQuery(q, parts)
            }

            // 5. Wi-Fi / Hotspot local sync help
            matchesAny(q, "wifi sync", "hotspot", "sync kaise", "peer sync", "do phone", "pairing key", "सिंक") -> {
                handleWifiSyncHelp()
            }

            // 6. Barcode scanning or label printing help
            matchesAny(q, "barcode", "label", "scanner", "print", "बारकोड", "प्रिंट") -> {
                handleBarcodeHelp()
            }

            // 7. Stock In / Stock Out help
            matchesAny(q, "stock in", "stock out", "bill entry", "kaise kare", "parcha", "invoice") -> {
                handleStockInOutHelp()
            }

            // 8. Firm comparison (SAA vs TVS)
            matchesAny(q, "tvs", "saa", "firm", "company", "dono firm") -> {
                handleFirmStockQuery(parts, pendingReport)
            }

            // 9. Suppliers query
            matchesAny(q, "supplier", "vendor", "dealer", "distributor", "सप्लायर", "व्यापारी") -> {
                handleSupplierQuery(suppliers, pendingReport)
            }

            // 10. Greetings
            matchesAny(q, "hello", "hi", "namaste", "namaskar", "shree", "shri", "kaun ho", "who are you", "नमस्ते") -> {
                handleGreeting(pendingReport)
            }

            // 11. Specific part search by name or number
            else -> {
                handleSpecificPartOrFallback(q, parts, pendingReport)
            }
        }
    }

    private fun handlePendingPartsQuery(report: PendingPartsReport): ShreeResponse {
        val count = report.totalPendingCount
        val zeroCount = report.criticalZeroStockCount
        val estCost = report.totalEstimatedReorderCost.toInt()

        val speech = if (count == 0) {
            "नमस्ते! बहुत बढ़िया, आपकी इन्वेंटरी में अभी कोई पेंडिंग पार्ट नहीं है। सभी स्पेयर पार्ट्स का स्टॉक पर्याप्त है।"
        } else {
            "नमस्ते! मैंने आपकी परचेस और ऑर्डर हिस्ट्री को ट्रैक कर के $count पेंडिंग पार्ट्स की रीऑर्डर लिस्ट तैयार कर ली है। इसमें से $zeroCount पार्ट्स का स्टॉक शून्य है। अनुमानित आर्डर राशि लगभग $estCost रुपये है।"
        }

        val display = buildString {
            append("📋 **पेंडिंग पार्ट्स रीऑर्डर रिपोर्ट (Shree AI)**\n\n")
            append("मैंने परचेस (Stock In) और बिक्री/ऑर्डर्स (Stock Out) को ट्रैक करके यह लिस्ट तैयार की है:\n\n")
            append("• **कुल पेंडिंग आइटम्स:** $count\n")
            append("• **शून्य स्टॉक (Critical):** $zeroCount आइटम्स\n")
            append("• **कम स्टॉक (Below Min):** ${report.belowMinStockCount} आइटम्स\n")
            append("• **अनुमानित आर्डर मूल्य:** ₹$estCost\n")
            append("• **SAA फर्म पेंडिंग:** ${report.saaPendingCount} | **TVS फर्म पेंडिंग:** ${report.tvsPendingCount}\n\n")

            if (report.pendingItems.isNotEmpty()) {
                append("━━━━━━━━━━━━━━━━━━━━\n")
                append("**प्रमुख पेंडिंग पार्ट्स (Reorder Recommendations):**\n\n")
                report.pendingItems.take(5).forEachIndexed { i, item ->
                    val statusEmoji = if (item.currentStock == 0) "🚨" else "⚠️"
                    append("$statusEmoji **${i + 1}. ${item.partName}** (${item.partNumber})\n")
                    append("   • फर्म: `${item.firm}` | रैक: `${item.rackLocation}`\n")
                    append("   • वर्तमान स्टॉक: **${item.currentStock}** (न्यूनतम सीमा: ${item.minStock})\n")
                    append("   • **सुझावित आर्डर:** **${item.recommendedOrderQty} pcs**")
                    if (item.unitPurchasePrice > 0.0) {
                        append(" (~₹${item.estimatedOrderCost.toInt()})")
                    }
                    append("\n")
                    if (item.preferredSupplierName.isNotBlank()) {
                        append("   • सप्लायर: ${item.preferredSupplierName}")
                        if (item.supplierMobile.isNotBlank()) append(" (📞 ${item.supplierMobile})")
                        append("\n")
                    }
                    append("\n")
                }
                if (count > 5) {
                    append("_...और ${count - 5} अन्य पेंडिंग पार्ट्स सूची में उपलब्ध हैं।_\n\n")
                }
                append("👉 _आप नीचे दिए गए बटन से सीधे सप्लायर को व्हाट्सएप पर आर्डर भेज सकते हैं या 1-टैप में स्टॉक इन कर सकते हैं।_")
            }
        }

        return ShreeResponse(
            displayText = display,
            speechText = speech,
            intent = ShreeIntent.PENDING_PARTS_LIST,
            pendingReport = report,
            suggestedActions = listOf("WhatsApp Order Sheet", "Today's Purchases", "Zero Stock Parts", "Wi-Fi Sync")
        )
    }

    private fun handlePurchaseOrderTrackingQuery(
        report: PendingPartsReport,
        transactions: List<StockTransactionEntity>
    ): ShreeResponse {
        val todayIn = report.todayPurchaseCount
        val todayOut = report.todayStockOutCount

        val speech = "आज आपके स्टोर में $todayIn परचेस बिल्स इनवर्ड हुए हैं और $todayOut काउंटर ऑर्डर्स स्टॉक आउट हुए हैं। कुल ${report.totalPendingCount} पार्ट्स रीऑर्डर के लिए पेंडिंग हैं।"

        val display = buildString {
            append("📊 **परचेस एवं काउंटर ऑर्डर ट्रैकिंग रिपोर्ट**\n\n")
            append("• **आज के परचेस बिल (Stock In):** $todayIn एंट्री\n")
            append("• **आज का स्टॉक आउट (Counter Issue):** $todayOut एंट्री\n")
            append("• **पेंडिंग रीऑर्डर पार्ट्स:** ${report.totalPendingCount} आइटम्स\n")
            append("• **अनुमानित परचेस बजट:** ₹${report.totalEstimatedReorderCost.toInt()}\n\n")

            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("**हाल के 5 ट्रांज़ैक्शन:**\n")
            val recentTx = transactions.sortedByDescending { it.timestamp }.take(5)
            if (recentTx.isEmpty()) {
                append("_कोई हालिया ट्रांज़ैक्शन उपलब्ध नहीं है।_\n")
            } else {
                recentTx.forEach { tx ->
                    val sign = if (tx.type == "STOCK_IN") "📥 [IN]" else "📤 [OUT]"
                    append("$sign **${tx.partName}** (${tx.firm}): ${tx.quantity} pcs")
                    if (tx.supplierName.isNotBlank()) append(" - ${tx.supplierName}")
                    if (tx.reason.isNotBlank()) append(" (${tx.reason})")
                    append("\n")
                }
            }
            append("\n💡 _श्री टिप: नियमित स्टॉक आउट एंट्री करने से पेंडिंग पार्ट्स का रीऑर्डर समय पर होता है।_")
        }

        return ShreeResponse(
            displayText = display,
            speechText = speech,
            intent = ShreeIntent.PURCHASE_ORDER_TRACKING,
            pendingReport = report,
            suggestedActions = listOf("Pending Parts List", "Zero Stock Alert", "Add Purchase (Stock In)", "Stock Out")
        )
    }

    private fun handleZeroStockQuery(report: PendingPartsReport): ShreeResponse {
        val zeroItems = report.pendingItems.filter { it.currentStock <= 0 }
        val count = zeroItems.size

        val speech = if (count == 0) {
            "बहुत अच्छी बात है! आपकी दुकान में कोई भी पार्ट शून्य स्टॉक पर नहीं है।"
        } else {
            "अलर्ट! आपकी दुकान में $count पार्ट्स का स्टॉक पूरी तरह खत्म हो चुका है। इन्हें तुरंत मंगाना जरूरी है।"
        }

        val display = buildString {
            append("🚨 **शून्य स्टॉक चेतावनी (Out of Stock Alert)**\n\n")
            if (count == 0) {
                append("✅ दुकान में सभी स्पेयर पार्ट्स का कुछ न कुछ स्टॉक मौजूद है।\n")
            } else {
                append("निम्नलिखित **$count स्पेयर पार्ट्स** का स्टॉक **0** हो चुका है:\n\n")
                zeroItems.forEachIndexed { idx, item ->
                    append("${idx + 1}. **${item.partName}** (${item.partNumber})\n")
                    append("   • फर्म: `${item.firm}` | रैक: `${item.rackLocation}`\n")
                    append("   • रीऑर्डर सुझाव: **${item.recommendedOrderQty} pcs** | सप्लायर: ${item.preferredSupplierName}\n\n")
                }
            }
        }

        return ShreeResponse(
            displayText = display,
            speechText = speech,
            intent = ShreeIntent.ZERO_STOCK_ALERT,
            pendingReport = report,
            suggestedActions = listOf("Pending Parts List", "WhatsApp Order Sheet", "Stock In")
        )
    }

    private fun handleRackLocationQuery(query: String, parts: List<PartEntity>): ShreeResponse {
        // Find if user mentioned any keyword from partName or number
        val matching = parts.filter { p ->
            val n = p.partName.lowercase(Locale.ROOT)
            val no = p.partNumber.lowercase(Locale.ROOT)
            val r = p.rack.lowercase(Locale.ROOT)
            val rn = p.rackNumber.lowercase(Locale.ROOT)
            val cleanQuery = query.replace("rack", "").replace("kaha", "").replace("hai", "").trim()

            (cleanQuery.isNotBlank() && (n.contains(cleanQuery) || no.contains(cleanQuery))) ||
                    (r.isNotBlank() && query.contains(r)) ||
                    (rn.isNotBlank() && query.contains(rn))
        }

        if (matching.isNotEmpty()) {
            val first = matching.first()
            val rackStr = "रैक ${first.rack} नंबर ${first.rackNumber}, शेल्फ ${first.shelf}"
            val speech = "${first.partName} का स्थान $rackStr में है। वर्तमान में ${first.totalStock} पीस उपलब्ध हैं।"

            val display = buildString {
                append("📍 **रैक और शेल्फ स्थान जानकारी:**\n\n")
                matching.take(5).forEach { p ->
                    append("• **${p.partName}** (${p.partNumber})\n")
                    append("  👉 स्थान: **Rack ${p.rack.ifBlank { "-" }}**, No: **${p.rackNumber.ifBlank { "-" }}**, Shelf: **${p.shelf.ifBlank { "-" }}**\n")
                    append("  👉 स्टॉक: SAA: ${p.stockSaa} | TVS: ${p.stockTvs} (कुल: ${p.totalStock})\n\n")
                }
            }

            return ShreeResponse(
                displayText = display,
                speechText = speech,
                intent = ShreeIntent.RACK_LOCATION_SEARCH,
                matchingParts = matching,
                suggestedActions = listOf("Pending Parts List", "Barcode Label", "View Parts")
            )
        } else {
            val speech = "आप किसी भी पार्ट का नाम या नंबर बोलकर उसका रैक और शेल्फ स्थान जान सकते हैं। हमारे यहाँ रैक ए, बी, सी और शेल्फ नंबर के अनुसार वर्गीकरण है।"
            val display = buildString {
                append("📍 **रैक लोकेशन सिस्टम गाइड:**\n\n")
                append("श्री अमरदेवी ऑटोमोबाइल में स्पेयर पार्ट्स को रैक में व्यवस्थित रखा गया है:\n")
                append("• **Rack A**: इंजन पार्ट्स, स्पार्क प्लग, फिल्टर, वाल्व\n")
                append("• **Rack B**: ब्रेक पैड, डिस्क प्लेट, ड्रम शूज़, केबल्स\n")
                append("• **Rack C**: चेन स्प्रोकेट, क्लच प्लेट, बेयरिंग\n")
                append("• **Rack D**: इलेक्ट्रिकल्स, कॉइल, रिले, बल्ब्स, इंडिकेटर\n")
                append("• **Rack E / Floor**: इंजन ऑयल्स, सस्पेंशन, टायर्स\n\n")
                append("💡 किसी खास पार्ट का रैक जानने के लिए पूछें: _'Spark plug ka rack kaha hai?'_")
            }
            return ShreeResponse(
                displayText = display,
                speechText = speech,
                intent = ShreeIntent.RACK_LOCATION_SEARCH,
                suggestedActions = listOf("Pending Parts List", "View Parts")
            )
        }
    }

    private fun handleWifiSyncHelp(): ShreeResponse {
        val speech = "वाई-फाई पीयर सिंक पूरी तरह ऑफलाइन काम करता है। बस एक फोन में हॉटस्पॉट चालू करें और दूसरे फोन को उससे कनेक्ट करें। फिर दोनों फोन में पेयरिंग की एसएए-अमरदेवी-2026 दर्ज करके सिंक बटन दबाएं।"
        val display = buildString {
            append("📶 **ऑफलाइन वाई-फाई / हॉटस्पॉट पीयर सिंक गाइड:**\n\n")
            append("बिना इंटरनेट के दो मोबाइल फोनों में डेटा सिंक करने का तरीका:\n\n")
            append("1. **हॉटस्पॉट चालू करें:** फोन A का पोर्टेबल हॉटस्पॉट ऑन करें।\n")
            append("2. **वाई-फाई कनेक्ट करें:** फोन B को फोन A के वाई-फाई से जोड़ें। (मोबाइल डेटा बंद भी रख सकते हैं)\n")
            append("3. **ऐप में Wi-Fi Sync खोलें:** दोनों फोन में 'Wi-Fi Sync' स्क्रीन खोलें।\n")
            append("4. **पेयरिंग की (Pairing Key):** `SAA-AMARDEVI-2026` सुनिश्चित करें।\n")
            append("5. **Sync दबाएं:** अपने आप दोनों फोन एक दूसरे का डेटा सुरक्षित रूप से मिला लेंगे।\n\n")
            append("🔒 _डेटा पूरी तरह निजी और सुरक्षित रहता है।_")
        }
        return ShreeResponse(
            displayText = display,
            speechText = speech,
            intent = ShreeIntent.WIFI_SYNC_HELP,
            suggestedActions = listOf("Open Wi-Fi Sync", "Pending Parts List")
        )
    }

    private fun handleBarcodeHelp(): ShreeResponse {
        val speech = "ऐप में कोड 128 बारकोड सिस्टम है। आप कैमरा से 1 डी बारकोड स्कैन कर सकते हैं और पार्ट्स के लिए कस्टम शेल्फ लेबल भी प्रिंट कर सकते हैं।"
        val display = buildString {
            append("🏷️ **कोड 128 बारकोड और स्कैनर गाइड:**\n\n")
            append("• **बारकोड स्कैन करना:** होम स्क्रीन पर 'Barcode Scanner' दबाएं। स्पेयर पार्ट के डिब्बे का बारकोड कैमरे के सामने रखें। पार्ट तुरंत खुल जाएगा।\n")
            append("• **शेल्फ लेबल बनाना:** किसी भी पार्ट के विवरण में जाकर 'Barcode Label' दबाएं।\n")
            append("• **लेबल में शामिल जानकारी:** पार्ट का नाम, पार्ट नंबर, फर्म (SAA/TVS), रैक लोकेशन, और एमआरपी।\n")
            append("• **प्रिंटर सपोर्ट:** आप थर्मल बारकोड प्रिंटर या सामान्य प्रिंटर से लेबल सीधे प्रिंट या शेयर कर सकते हैं।\n")
        }
        return ShreeResponse(
            displayText = display,
            speechText = speech,
            intent = ShreeIntent.BARCODE_HELP,
            suggestedActions = listOf("Barcode Scanner", "Pending Parts List")
        )
    }

    private fun handleStockInOutHelp(): ShreeResponse {
        val speech = "स्टॉक इन का उपयोग सप्लायर से नया सामान आने पर बिल एंट्री के लिए करें। स्टॉक आउट का उपयोग काउंटर पर पार्ट्स बेचने या मैकेनिक को देने पर करें।"
        val display = buildString {
            append("📥📤 **स्टॉक इन और स्टॉक आउट कैसे करें?**\n\n")
            append("• **Stock In (परचेस बिल):**\n")
            append("  1. 'Stock In / Purchase' बटन दबाएं।\n")
            append("  2. पार्ट चुनें, फर्म (SAA/TVS) चुनें, बिल नंबर, मात्रा और खरीद दर डालें।\n")
            append("  3. सेव करते ही स्टॉक बढ़ जाएगा और परचेस हिस्ट्री में रिकॉर्ड हो जाएगा।\n\n")
            append("• **Stock Out (बिक्री / इशू):**\n")
            append("  1. 'Stock Out' बटन दबाएं या बारकोड स्कैन करें।\n")
            append("  2. मात्रा और कारण (Counter Sale, Mechanic Issue आदि) दर्ज करें।\n")
            append("  3. तुरंत स्टॉक घट जाएगा।\n")
        }
        return ShreeResponse(
            displayText = display,
            speechText = speech,
            intent = ShreeIntent.STOCK_IN_OUT_HELP,
            suggestedActions = listOf("Stock In", "Stock Out", "Pending Parts List")
        )
    }

    private fun handleFirmStockQuery(parts: List<PartEntity>, report: PendingPartsReport): ShreeResponse {
        val saaTotalUnits = parts.sumOf { it.stockSaa }
        val tvsTotalUnits = parts.sumOf { it.stockTvs }

        val speech = "एसएए फर्म में कुल $saaTotalUnits यूनिट्स हैं और टीवीएस फर्म में $tvsTotalUnits यूनिट्स हैं। एसएए में ${report.saaPendingCount} और टीवीएस में ${report.tvsPendingCount} पार्ट्स पेंडिंग हैं।"

        val display = buildString {
            append("🏢 **फर्म विभाजन (SAA vs TVS):**\n\n")
            append("• **SAA (Shri Amardevi Automobile):**\n")
            append("  - कुल स्टॉक यूनिट्स: **$saaTotalUnits**\n")
            append("  - पेंडिंग रीऑर्डर पार्ट्स: **${report.saaPendingCount}**\n\n")
            append("• **TVS (TVS Genuine Spares):**\n")
            append("  - कुल स्टॉक यूनिट्स: **$tvsTotalUnits**\n")
            append("  - पेंडिंग रीऑर्डर पार्ट्स: **${report.tvsPendingCount}**\n\n")
            append("💡 दोनों फर्मों का स्टॉक अलग-अलग ट्रैक होता है ताकि हिसाब-किताब में कोई गड़बड़ी न हो।")
        }

        return ShreeResponse(
            displayText = display,
            speechText = speech,
            intent = ShreeIntent.FIRM_STOCK_SUMMARY,
            pendingReport = report,
            suggestedActions = listOf("Pending Parts List", "View Parts")
        )
    }

    private fun handleSupplierQuery(suppliers: List<SupplierEntity>, report: PendingPartsReport): ShreeResponse {
        val speech = "आपके पास ${suppliers.size} सप्लायर्स रजिस्टर्ड हैं। आप पेंडिंग पार्ट्स का ऑर्डर सीधे व्हाट्सएप के माध्यम से सप्लायर को भेज सकते हैं।"
        val display = buildString {
            append("👥 **सप्लायर व वेंडर लिस्ट:**\n\n")
            if (suppliers.isEmpty()) {
                append("_अभी कोई सप्लायर नहीं जोड़ा गया है। 'Suppliers' स्क्रीन से जोड़ें।_\n")
            } else {
                suppliers.forEachIndexed { i, s ->
                    append("${i + 1}. **${s.supplierName}** (${s.supplierCode})\n")
                    if (s.mobileNumber.isNotBlank()) append("   • मोबाइल: 📞 ${s.mobileNumber}\n")
                    if (s.address.isNotBlank()) append("   • पता: ${s.address}\n")
                    append("\n")
                }
            }
            append("📋 _पेंडिंग ऑर्डर्स में सप्लायर के अनुसार अलग-अलग लिस्ट तैयार की जा सकती है।_")
        }

        return ShreeResponse(
            displayText = display,
            speechText = speech,
            intent = ShreeIntent.SUPPLIER_HELP,
            pendingReport = report,
            suggestedActions = listOf("Pending Parts List", "WhatsApp Order Sheet")
        )
    }

    private fun handleGreeting(report: PendingPartsReport): ShreeResponse {
        val speech = "नमस्ते! मैं श्री हूँ, श्री अमरदेवी ऑटोमोबाइल की स्मार्ट इन्वेंटरी सहायक। मैं परचेस और ऑर्डर्स ट्रैक करके पेंडिंग पार्ट्स की लिस्ट बनाती हूँ। आज मैं आपकी क्या मदद करूँ?"
        val display = buildString {
            append("👋 **नमस्ते! मैं श्री (Shree AI) हूँ।**\n\n")
            append("श्री अमरदेवी ऑटोमोबाइल & स्पेयर पार्ट की एआई सहायक।\n\n")
            append("✨ **मैं आपकी इन चीज़ों में मदद कर सकती हूँ:**\n")
            append("1. 📋 **पेंडिंग पार्ट्स लिस्ट:** परचेस व आर्डर ट्रैक कर के जो पार्ट्स कम हैं उनकी लिस्ट बनाना।\n")
            append("2. 📱 **व्हाट्सएप ऑर्डर:** सप्लायर को भेजने के लिए 1-टैप ऑर्डर शीट।\n")
            append("3. 📍 **रैक और शेल्फ खोज:** कोई भी स्पेयर पार्ट किस रैक में रखा है तुरंत बताना।\n")
            append("4. 🎙️ **बोलकर सुनना:** सभी जवाबों को हिंदी या इंग्लिश में आवाज़ में सुनना।\n")
            append("5. 📶 **ऑफलाइन सपोर्ट:** बिना इंटरनेट पूरी जानकारी और डेटा का विश्लेषण।\n\n")
            append("💡 _नीचे दिए गए किसी भी बटन पर टैप करें या अपना सवाल पूछें!_")
        }

        return ShreeResponse(
            displayText = display,
            speechText = speech,
            intent = ShreeIntent.GREETING,
            pendingReport = report,
            suggestedActions = listOf("Pending Parts List", "Purchase & Order Tracking", "Zero Stock Alert", "App Guide")
        )
    }

    private fun handleSpecificPartOrFallback(
        query: String,
        parts: List<PartEntity>,
        report: PendingPartsReport
    ): ShreeResponse {
        // Try searching parts
        val matches = parts.filter { p ->
            p.partName.lowercase(Locale.ROOT).contains(query) ||
                    p.partNumber.lowercase(Locale.ROOT).contains(query) ||
                    p.category.lowercase(Locale.ROOT).contains(query)
        }

        if (matches.isNotEmpty()) {
            val part = matches.first()
            val rackStr = "रैक ${part.rack}-${part.rackNumber}, शेल्फ ${part.shelf}"
            val speech = "${part.partName} का कुल स्टॉक ${part.totalStock} पीस है। यह $rackStr में रखा है। SAA में ${part.stockSaa} और TVS में ${part.stockTvs} हैं।"

            val display = buildString {
                append("🔍 **स्पेयर पार्ट जानकारी:**\n\n")
                matches.take(4).forEach { p ->
                    val status = if (p.isOutOfStock()) "🚨 OUT OF STOCK" else if (p.isLowStock()) "⚠️ LOW STOCK" else "✅ In Stock"
                    append("• **${p.partName}** (`${p.partNumber}`)\n")
                    append("  - श्रेणी: ${p.category} | फर्म: ${p.firm}\n")
                    append("  - स्थान: Rack ${p.rack}-${p.rackNumber}, Shelf ${p.shelf}\n")
                    append("  - स्टॉक स्थिति: **$status**\n")
                    append("  - SAA: ${p.stockSaa} | TVS: ${p.stockTvs} | **कुल: ${p.totalStock}**\n")
                    if (p.purchasePrice > 0) append("  - परचेस दर: ₹${p.purchasePrice.toInt()} | MRP: ₹${p.sellingPrice.toInt()}\n")
                    append("\n")
                }
            }

            return ShreeResponse(
                displayText = display,
                speechText = speech,
                intent = ShreeIntent.PART_STOCK_INQUIRY,
                matchingParts = matches,
                suggestedActions = listOf("Pending Parts List", "Stock In", "Stock Out")
            )
        }

        // Generic friendly response with quick options
        val speech = "मैं श्री हूँ। आप मुझसे पेंडिंग पार्ट्स की लिस्ट, परचेस व आर्डर ट्रैकिंग, किसी पार्ट का रैक स्थान या ऐप के किसी भी फीचर की जानकारी पूछ सकते हैं।"
        val display = buildString {
            append("🤖 **श्री AI सहायक (Shree)**\n\n")
            append("मुझे आपका सवाल समझ आया। आपकी सहायता के लिए प्रमुख विकल्प:\n\n")
            append("• 📋 **पेंडिंग पार्ट्स:** 'Pending parts list' पूछें या बटन दबाएं।\n")
            append("• 📊 **परचेस व बिक्री:** 'Purchase track' या 'Stock Out' पूछें।\n")
            append("• 📍 **रैक स्थान:** पार्ट का नाम लिखें जैसे 'Spark plug kaha hai?'\n")
            append("• 📶 **सिंक जानकारी:** 'Wifi sync kaise kare?' पूछें।\n")
        }

        return ShreeResponse(
            displayText = display,
            speechText = speech,
            intent = ShreeIntent.UNKNOWN_QUERY,
            pendingReport = report,
            suggestedActions = listOf("Pending Parts List", "Purchase & Order Tracking", "Zero Stock Alert", "App Guide")
        )
    }

    private fun matchesAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
}
