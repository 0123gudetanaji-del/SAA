package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

object BarcodeUtil {

    // Code 128 Pattern Table (Patterns for Subset B, standard 1D barcode format)
    private val CODE128_PATTERNS = arrayOf(
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
        "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
        "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
        "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
        "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
        "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
        "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
        "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
        "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
        "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
        "114131", "311141", "411131", "211412", "211214", "211232", "2331112" // 106 = Stop
    )

    private const val START_B = 104
    private const val STOP = 106

    /**
     * Encodes alphanumeric string into boolean array representing bars (true) and spaces (false)
     */
    fun encodeCode128(text: String): BooleanArray {
        val safeText = text.filter { it.code in 32..126 }.ifEmpty { "123456" }
        val values = mutableListOf<Int>()
        values.add(START_B)

        var checksum = START_B
        for (i in safeText.indices) {
            val charCode = safeText[i].code - 32
            values.add(charCode)
            checksum += charCode * (i + 1)
        }
        val checkDigit = checksum % 103
        values.add(checkDigit)
        values.add(STOP)

        val patternBuilder = StringBuilder()
        for (v in values) {
            val p = CODE128_PATTERNS[v.coerceIn(0, CODE128_PATTERNS.lastIndex)]
            patternBuilder.append(p)
        }

        // Convert widths to boolean array
        val bars = mutableListOf<Boolean>()
        // Quiet zone at start
        repeat(10) { bars.add(false) }

        var isBar = true
        for (c in patternBuilder.toString()) {
            val width = c.digitToIntOrNull() ?: 1
            repeat(width) { bars.add(isBar) }
            isBar = !isBar
        }

        // Quiet zone at end
        repeat(10) { bars.add(false) }

        return bars.toBooleanArray()
    }

    /**
     * Generates a high quality bitmap of 1D barcode with human-readable text below
     */
    fun generateBarcodeBitmap(
        text: String,
        width: Int = 480,
        height: Int = 160,
        showText: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val encoded = encodeCode128(text)
        val barPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val barcodeHeight = if (showText) height - 36f else height.toFloat()
        val barWidth = width.toFloat() / encoded.size.toFloat()

        for (i in encoded.indices) {
            if (encoded[i]) {
                val left = i * barWidth
                val right = (i + 1) * barWidth
                canvas.drawRect(left, 10f, right, barcodeHeight, barPaint)
            }
        }

        if (showText) {
            canvas.drawText(text, width / 2f, height - 8f, textPaint)
        }

        return bitmap
    }

    /**
     * Generates a complete printable automobile spare part label bitmap
     */
    fun generateLabelBitmap(
        shopName: String = "Shri Amardevi Automobile & Spare Part",
        partName: String,
        partNumber: String,
        barcode: String,
        supplierCode: String,
        firm: String,
        width: Int = 600,
        height: Int = 360
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(6f, 6f, width - 6f, height - 6f, borderPaint)

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(shopName.uppercase(), width / 2f, 36f, titlePaint)

        // Horizontal divider line
        val linePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 1.5f
        }
        canvas.drawLine(16f, 48f, width - 16f, 48f, linePaint)

        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }

        val subPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            typeface = Typeface.MONOSPACE
        }

        canvas.drawText(partName.take(30), 20f, 80f, labelPaint)
        canvas.drawText("PART NO: $partNumber", 20f, 110f, subPaint)

        val firmText = "FIRM: $firm"
        val supText = if (supplierCode.isNotBlank()) "SUP: $supplierCode" else ""
        canvas.drawText("$firmText   $supText", 20f, 138f, subPaint)

        // Draw Barcode inside label
        val codeToRender = barcode.ifBlank { partNumber }
        val barcodeBm = generateBarcodeBitmap(codeToRender, width = width - 40, height = 180, showText = true)
        canvas.drawBitmap(barcodeBm, 20f, 150f, null)

        return bitmap
    }
}
