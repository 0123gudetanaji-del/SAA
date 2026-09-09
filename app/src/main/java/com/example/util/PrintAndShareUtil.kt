package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.PartEntity
import com.example.data.model.StockTransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareUtil {

    fun shareLowStockList(context: Context, lowStockParts: List<PartEntity>) {
        if (lowStockParts.isEmpty()) {
            Toast.makeText(context, "No low-stock parts to share!", Toast.LENGTH_SHORT).show()
            return
        }

        val df = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val dateStr = df.format(Date())

        val sb = StringBuilder()
        sb.append("📋 *PURCHASE ORDER / LOW STOCK LIST*\n")
        sb.append("🏬 *Shri Amardevi Automobile & Spare Part*\n")
        sb.append("📅 Date: $dateStr\n\n")

        lowStockParts.forEachIndexed { index, part ->
            val status = if (part.totalStock <= 0) "⚠️ OUT OF STOCK" else "⚡ LOW STOCK"
            sb.append("${index + 1}. *${part.partName}*\n")
            sb.append("   - Part No: ${part.partNumber}\n")
            sb.append("   - Firm: ${part.firm}\n")
            sb.append("   - Current Stock: ${part.totalStock} (SAA: ${part.stockSaa}, TVS: ${part.stockTvs})\n")
            sb.append("   - Min Required: ${part.minStock}\n")
            val orderQty = (part.minStock * 2 - part.totalStock).coerceAtLeast(part.minStock)
            sb.append("   - *Suggested Order Qty: $orderQty*\n")
            if (part.supplierCode.isNotBlank()) {
                sb.append("   - Supplier: ${part.supplierName.ifBlank { part.supplierCode }}\n")
            }
            sb.append("   - Status: $status\n\n")
        }

        sb.append("Please send quotes / dispatch as soon as possible.")

        shareText(context, sb.toString(), "Share Low Stock List via WhatsApp")
    }

    fun sharePartDetails(context: Context, part: PartEntity) {
        val sb = StringBuilder()
        sb.append("⚙️ *AUTOMOBILE PART DETAILS*\n")
        sb.append("🏬 *Shri Amardevi Automobile & Spare Part*\n\n")
        sb.append("📦 *Part Name:* ${part.partName}\n")
        sb.append("🔢 *Part Number:* ${part.partNumber}\n")
        sb.append("🏢 *Firm:* ${part.firm}\n")
        sb.append("🏷️ *Category:* ${part.category}\n")
        sb.append("📍 *Location:* ${part.location.ifBlank { "Rack ${part.rack} ${part.rackNumber} / Shelf ${part.shelf}" }}\n\n")
        sb.append("📊 *CURRENT STOCK:*\n")
        sb.append("   - SAA Stock: ${part.stockSaa}\n")
        sb.append("   - TVS Stock: ${part.stockTvs}\n")
        sb.append("   - Total Stock: ${part.totalStock}\n")
        sb.append("   - Minimum Stock: ${part.minStock}\n\n")
        if (part.sellingPrice > 0) {
            sb.append("💰 *MRP / Price:* ₹${part.sellingPrice}\n")
        }
        if (part.barcode.isNotBlank()) {
            sb.append("🔲 *Barcode:* ${part.barcode}\n")
        }

        shareText(context, sb.toString(), "Share Part Details via WhatsApp")
    }

    fun shareTransactionReceipt(context: Context, tx: StockTransactionEntity) {
        val df = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val dateStr = df.format(Date(tx.date))

        val sb = StringBuilder()
        val typeHeader = if (tx.type == "STOCK_IN") "📥 PURCHASE / STOCK IN" else "📤 STOCK OUT / ISSUE"
        sb.append("*$typeHeader*\n")
        sb.append("🏬 *Shri Amardevi Automobile & Spare Part*\n")
        sb.append("📅 Date: $dateStr\n\n")
        sb.append("⚙️ *Part:* ${tx.partName} (${tx.partNumber})\n")
        sb.append("🏢 *Firm:* ${tx.firm}\n")
        sb.append("🔢 *Quantity:* ${tx.quantity}\n")
        if (tx.billNumber.isNotBlank()) {
            sb.append("🧾 *Bill Number:* ${tx.billNumber}\n")
        }
        if (tx.supplierName.isNotBlank()) {
            sb.append("🚚 *Supplier:* ${tx.supplierName}\n")
        }
        if (tx.purchasePrice > 0) {
            sb.append("💰 *Unit Rate:* ₹${tx.purchasePrice}\n")
            sb.append("💵 *Total Amount:* ₹${tx.purchasePrice * tx.quantity}\n")
        }
        sb.append("📦 *Stock Balance:* ${tx.previousStock} ➔ ${tx.newStock}\n")
        if (tx.reason.isNotBlank()) {
            sb.append("📝 *Remarks:* ${tx.reason}\n")
        }

        shareText(context, sb.toString(), "Share Stock Receipt via WhatsApp")
    }

    fun shareText(context: Context, text: String, chooserTitle: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
    }
}

object PrintUtil {

    fun printLabel(context: Context, labelBitmap: Bitmap, jobName: String = "SAA_Barcode_Label") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "Print service not available on this device", Toast.LENGTH_SHORT).show()
                return
            }

            printManager.print(
                jobName,
                object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder(jobName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            val outputStream = FileOutputStream(destination?.fileDescriptor)
                            val pdfDocument = android.graphics.pdf.PdfDocument()
                            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                                labelBitmap.width,
                                labelBitmap.height,
                                1
                            ).create()
                            val page = pdfDocument.startPage(pageInfo)
                            page.canvas.drawBitmap(labelBitmap, 0f, 0f, null)
                            pdfDocument.finishPage(page)
                            pdfDocument.writeTo(outputStream)
                            pdfDocument.close()
                            outputStream.close()
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                },
                PrintAttributes.Builder()
                    .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                    .build()
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
