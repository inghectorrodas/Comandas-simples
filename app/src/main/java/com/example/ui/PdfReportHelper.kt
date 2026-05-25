package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.ui.RestaurantViewModel.ProductStat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportHelper {

    /**
     * Generates a beautifully formatted PDF report of a cash closure (active or historical)
     * and saves it to the app's cache directory.
     */
    fun generateClosureReport(
        context: Context,
        restaurantName: String,
        restaurantAddress: String,
        restaurantPhone: String,
        restaurantLogoBase64: String?,
        reportTitle: String, // e.g., "Reporte de Cierre Abierto (Pre-Corte)" or "Reporte de Cierre de Caja #12"
        dateRange: String,
        ordersCount: Int,
        totalSales: Double,
        totalCost: Double,
        paymentsBreakdown: Map<String, Double>,
        orderTypesBreakdown: Map<String, Int>,
        productStats: List<ProductStat>
    ): File? {
        val pdfDocument = PdfDocument()
        
        // Setup paint configurations
        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintSecondaryText = Paint().apply {
            color = Color.rgb(100, 116, 139) // Slate Gray
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintBoldText = Paint().apply {
            color = Color.rgb(30, 41, 59) // Deep Navy/Slate
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintTitle = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintSubtitle = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintSectionHeading = Paint().apply {
            color = Color.rgb(198, 40, 40) // Crimson red
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Keep track of our multi-page builders
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create() // A4 Size in Points (72 pts/inch)
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = 25f

        fun drawHeaderAndBacking() {
            // Top red header banner (height 100)
            val bannerPaint = Paint().apply { color = Color.rgb(198, 40, 40) }
            canvas.drawRect(0f, 0f, 595f, 100f, bannerPaint)

            // Accent light banner under it
            val bannerAccentPaint = Paint().apply { color = Color.rgb(245, 225, 200) } // beige accent line
            canvas.drawRect(0f, 100f, 595f, 106f, bannerAccentPaint)

            // Try drawing the custom business logo icon if present
            var logoOffset = 40f
            if (!restaurantLogoBase64.isNullOrBlank()) {
                try {
                    val decoded = Base64.decode(restaurantLogoBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    if (bitmap != null) {
                        val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
                        canvas.drawBitmap(scaled, 35f, 18f, null)
                        logoOffset = 115f
                    }
                } catch (t: Throwable) {
                    // Fail silently and keep standard offsets
                }
            }

            // Draw header text
            canvas.drawText(restaurantName.uppercase(Locale.getDefault()), logoOffset, 42f, paintTitle)
            canvas.drawText("Dirección: $restaurantAddress  |  Tel: $restaurantPhone", logoOffset, 62f, paintSubtitle)
            canvas.drawText("Sabor y Gestión - Cocinando con pasión todos los días", logoOffset, 77f, paintSubtitle)

            // Content starts below the banner
            y = 135f
        }

        fun drawFooter(currentPage: Int) {
            val paintFoot = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }
            // Draw fine separator line at bottom
            canvas.drawLine(40f, 800f, 555f, 800f, Paint().apply { color = Color.LTGRAY; strokeWidth = 1f })
            val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            canvas.drawText("Sabor y Gestión • Reporte oficial PDF generado en tiempo real: $timestamp", 40f, 814f, paintFoot)
            canvas.drawText("Página $currentPage", 515f, 814f, paintFoot)
        }

        fun checkPageOverflow(currY: Float, heightNeeded: Float): Float {
            if (currY + heightNeeded > 780f) {
                // Draw footer on current page & finish
                drawFooter(pageNum)
                pdfDocument.finishPage(page)

                // Start new page
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                // Small banner at the top of subsequent pages
                val barPaint = Paint().apply { color = Color.rgb(198, 40, 40) }
                canvas.drawRect(0f, 0f, 595f, 40f, barPaint)
                val paintShortTitle = Paint().apply {
                    color = Color.WHITE
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("$restaurantName — ANEXO DE REPORTE CONTINUACIÓN", 40f, 25f, paintShortTitle)
                return 70f
            }
            return currY
        }

        // Draw Cover/First page header
        drawHeaderAndBacking()

        // Report Title & Date info
        canvas.drawText(reportTitle, 40f, y, paintSectionHeading)
        y += 18f
        canvas.drawText("Período / Rango de Fecha: $dateRange", 40f, y, paintBoldText)
        y += 24f

        // --- KEY METRICS PANEL (Grip with cards) ---
        val cardPaint = Paint().apply {
            color = Color.rgb(248, 250, 252) // off-white card bg
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240) // light gray border
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Draw 3 metric boxes: Ventas, Costos, Utilidad Estimada
        val colWidth = 165f
        val gap = 10f
        val startX = 40f

        val metrics = listOf(
            Triple("Ventas Totales", String.format(Locale.US, "$%.2f", totalSales), Color.rgb(30, 41, 59)),
            Triple("Costo de Insumos", String.format(Locale.US, "$%.2f", totalCost), Color.rgb(100, 116, 139)),
            Triple("Ganancia Neta", String.format(Locale.US, "$%.2f", totalSales - totalCost), Color.rgb(46, 125, 50)) // Green
        )

        for (i in metrics.indices) {
            val (lbl, valStr, valColor) = metrics[i]
            val l = startX + i * (colWidth + gap)
            val r = l + colWidth
            
            // Draw card bg and border
            canvas.drawRoundRect(l, y, r, y + 55f, 8f, 8f, cardPaint)
            canvas.drawRoundRect(l, y, r, y + 55f, 8f, 8f, cardBorderPaint)

            // Text inside
            val paintLbl = Paint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val paintVal = Paint().apply {
                color = valColor
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(lbl, l + 12f, y + 20f, paintLbl)
            canvas.drawText(valStr, l + 12f, y + 42f, paintVal)
        }
        y += 75f

        // --- SECONDARY METRICS: PAYMENT METHODS & ORDER TYPES ---
        y = checkPageOverflow(y, 110f)
        
        // Left Column: Payment Methods
        val middleX = 290f
        canvas.drawText("MÉTODOS DE PAGO", 40f, y, paintBoldText)
        canvas.drawText("CANALES DE ENVÍO / COMANDA", middleX, y, paintBoldText)
        
        canvas.drawLine(40f, y + 4f, 260f, y + 4f, Paint().apply { color = Color.rgb(198, 40, 40); strokeWidth = 1f })
        canvas.drawLine(middleX, y + 4f, 545f, y + 4f, Paint().apply { color = Color.rgb(198, 40, 40); strokeWidth = 1f })
        y += 20f

        val boldSmallValue = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Write Payment totals
        val payKeys = listOf("Efectivo", "Tarjeta", "Transferencia")
        var pY = y
        for (m in payKeys) {
            val amt = paymentsBreakdown[m] ?: 0.0
            val lineStr = "• $m:"
            canvas.drawText(lineStr, 40f, pY, paintText)
            canvas.drawText(String.format(Locale.US, "$%.2f", amt), 150f, pY, boldSmallValue)
            pY += 15f
        }
        
        // Write order types
        val typeKeys = listOf(
            "COMER_AQUI" to "Mesa / Comer Aquí",
            "LLEVAR" to "Verificado Para Llevar",
            "DOMICILIO" to "Entrega Domicilio"
        )
        var tY = y
        for ((key, desc) in typeKeys) {
            val count = orderTypesBreakdown[key] ?: 0
            val lineStr = "• $desc:"
            canvas.drawText(lineStr, middleX, tY, paintText)
            canvas.drawText("$count comandas", middleX + 160f, tY, boldSmallValue)
            tY += 15f
        }

        y = maxOf(pY, tY) + 15f

        // --- SECTION 3: PRODUCT STATISTICS (TABLE) ---
        y = checkPageOverflow(y, 60f)
        
        canvas.drawText("DESGLOSE DE ARTÍCULOS VENDIDOS", 40f, y, paintSectionHeading)
        y += 12f
        canvas.drawText("Resumen y volumen de ventas detallado por platillo:", 40f, y, paintSecondaryText)
        y += 15f

        // Draw Table Header
        y = checkPageOverflow(y, 40f)
        val headerBgPaint = Paint().apply { color = Color.rgb(30, 41, 59) }
        canvas.drawRect(40f, y, 555f, y + 22f, headerBgPaint)

        val tableHeaderPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Alignments: Product (45f), Qty (240f), P.Unit (290f), Revenue (350f), Cost (420f), Profit (490f)
        canvas.drawText("Nombre de Platillo", 48f, y + 14f, tableHeaderPaint)
        canvas.drawText("Cant", 240f, y + 14f, tableHeaderPaint)
        canvas.drawText("P. Unit", 280f, y + 14f, tableHeaderPaint)
        canvas.drawText("Ingresos", 340f, y + 14f, tableHeaderPaint)
        canvas.drawText("Costo", 410f, y + 14f, tableHeaderPaint)
        canvas.drawText("Utilidad", 480f, y + 14f, tableHeaderPaint)
        y += 22f

        val rowEvenBg = Paint().apply { color = Color.rgb(248, 250, 252) }
        val rowOddBg = Paint().apply { color = Color.WHITE }
        val borderRowPaint = Paint().apply { color = Color.rgb(241, 245, 249); strokeWidth = 1f }

        val rowTextPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 9f
            isAntiAlias = true
        }
        val rowBoldTextGreen = Paint().apply {
            color = Color.rgb(46, 125, 50)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        productStats.forEachIndexed { idx, stat ->
            y = checkPageOverflow(y, 20f)

            // Draw row background
            val bg = if (idx % 2 == 0) rowEvenBg else rowOddBg
            canvas.drawRect(40f, y, 555f, y + 18f, bg)
            canvas.drawLine(40f, y + 18f, 555f, y + 18f, borderRowPaint)

            // Determine unit price preview
            val unitPrice = if (stat.quantitySold > 0) stat.revenue / stat.quantitySold else 0.0

            // Draw text values
            val shortenedName = if (stat.name.length > 25) stat.name.substring(0, 23) + ".." else stat.name
            canvas.drawText(shortenedName, 48f, y + 12f, rowTextPaint)
            canvas.drawText(stat.quantitySold.toString(), 245f, y + 12f, Paint(rowTextPaint).apply { typeface = Typeface.DEFAULT_BOLD })
            canvas.drawText(String.format(Locale.US, "$%.2f", unitPrice), 280f, y + 12f, rowTextPaint)
            canvas.drawText(String.format(Locale.US, "$%.2f", stat.revenue), 340f, y + 12f, rowTextPaint)
            canvas.drawText(String.format(Locale.US, "$%.2f", stat.cost), 410f, y + 12f, rowTextPaint)
            canvas.drawText(String.format(Locale.US, "$%.2f", stat.profit), 480f, y + 12f, rowBoldTextGreen)

            y += 18f
        }

        // Draw summary end note
        y = checkPageOverflow(y, 60f)
        y += 15f
        canvas.drawRoundRect(40f, y, 555f, y + 50f, 6f, 6f, cardPaint)
        canvas.drawRoundRect(40f, y, 555f, y + 50f, 6f, 6f, cardBorderPaint)

        val noteTextPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("DECLARACIÓN DE INTEGRIDAD: Este documento constituye un extracto oficial seguro de transacciones", 48f, y + 18f, noteTextPaint)
        canvas.drawText("almacenadas localmente en la base de datos de Sabor y Gestión. Todo valor expresado en dólares estadounidenses (USD).", 48f, y + 33f, noteTextPaint)

        // Draw last page footer before finishing
        drawFooter(pageNum)
        pdfDocument.finishPage(page)

        // Write to cache file
        return try {
            val reportFile = File(context.cacheDir, "reporte_cierre_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(reportFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            reportFile
        } catch (t: Throwable) {
            t.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Direct Share via general Action Chooser
     */
    fun shareDocument(context: Context, file: File, subject: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Estimado, adjunto el PDF del $subject generado desde Sabor y Gestión.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val ch = Intent.createChooser(intent, "Compartir Reporte PDF vía...")
            ch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(ch)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo compartir el archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Direct share via WhatsApp
     */
    fun shareToWhatsApp(context: Context, file: File, subject: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                `package` = "com.whatsapp"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Estimado, adjunto el PDF del $subject correspondiente a Panes con Gallina Benítez.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback for WhatsApp Business if personal is not installed
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intentBus = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    `package` = "com.whatsapp.w4b" // WhatsApp Business package
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Estimado, adjunto el PDF del $subject correspondiente a Panes con Gallina Benítez.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                intentBus.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intentBus)
            } catch (ex: Exception) {
                // If neither is installed, offer standard sharing popup of Android safely
                Toast.makeText(context, "WhatsApp no instalado. Abriendo menú alternativo...", Toast.LENGTH_SHORT).show()
                shareDocument(context, file, subject)
            }
        }
    }

    /**
     * Direct share via Email client
     */
    fun shareToEmail(context: Context, file: File, subject: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822" // specific MIME for mail clients to trigger them
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$subject — Panes con Gallina Benítez")
                putExtra(Intent.EXTRA_TEXT, "Estimado,\n\nAdjunto en este mensaje el reporte oficial PDF del $subject generado automáticamente por el sistema de gestión del restaurante.\n\nAtentamente,\nAdministración.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val ch = Intent.createChooser(intent, "Enviar correo usando...")
            ch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(ch)
        } catch (e: Exception) {
            // Standard fallback
            shareDocument(context, file, subject)
        }
    }
}
