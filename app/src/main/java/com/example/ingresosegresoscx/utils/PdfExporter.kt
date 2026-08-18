package com.example.ingresosegresoscx.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.ParcelFileDescriptor
import com.example.ingresosegresoscx.models.Movimiento
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    private val fmt = DecimalFormat("$#,##0.00")

    fun generarReportePdf(
        context: Context,
        pfd: ParcelFileDescriptor,
        movimientos: List<Movimiento>,
        tituloReporte: String,
        totales: Map<String, Double>
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        // Configuración de página (A4 aprox 595 x 842)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var yPos = 40f

        // --- ENCABEZADO ---
        titlePaint.color = Color.parseColor("#3F51B5") // IndigoPrimary
        titlePaint.textSize = 24f
        titlePaint.isFakeBoldText = true
        canvas.drawText("IngresosEgresosCX", 40f, yPos, titlePaint)

        yPos += 30f
        paint.textSize = 12f
        paint.color = Color.GRAY
        canvas.drawText("Reporte de Movimientos", 40f, yPos, paint)
        canvas.drawText("Fecha Gen: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 380f, yPos, paint)

        yPos += 15f
        canvas.drawText("Periodo: $tituloReporte", 40f, yPos, paint)

        yPos += 20f
        paint.color = Color.BLACK
        canvas.drawLine(40f, yPos, 555f, yPos, paint)

        // --- RESUMEN DE TOTALES ---
        yPos += 40f
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Resumen del Periodo", 40f, yPos, paint)

        yPos += 30f
        paint.isFakeBoldText = false
        paint.textSize = 14f
        
        // Ingresos
        paint.color = Color.parseColor("#2ECC71") // EmeraldIncome
        canvas.drawText("Total Ingresos:", 60f, yPos, paint)
        canvas.drawText("+" + fmt.format(totales["INGRESOS"] ?: 0.0), 200f, yPos, paint)

        // Egresos
        yPos += 25f
        paint.color = Color.parseColor("#FF6F61") // CoralExpense
        canvas.drawText("Total Egresos:", 60f, yPos, paint)
        canvas.drawText("-" + fmt.format(totales["EGRESOS"] ?: 0.0), 200f, yPos, paint)

        // Balance
        yPos += 25f
        val balance = totales["BALANCE"] ?: 0.0
        paint.color = if (balance < 0) Color.parseColor("#FF6F61") else Color.parseColor("#3F51B5")
        paint.isFakeBoldText = true
        canvas.drawText("Balance Neto:", 60f, yPos, paint)
        canvas.drawText(fmt.format(balance), 200f, yPos, paint)

        // --- TABLA DE MOVIMIENTOS ---
        yPos += 60f
        paint.color = Color.parseColor("#3F51B5")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        
        // Cabecera Tabla
        canvas.drawText("Fecha", 40f, yPos, paint)
        canvas.drawText("Concepto", 120f, yPos, paint)
        canvas.drawText("Categoría", 320f, yPos, paint)
        canvas.drawText("Monto", 500f, yPos, paint)

        yPos += 10f
        paint.isFakeBoldText = false
        canvas.drawLine(40f, yPos, 555f, yPos, paint)

        yPos += 20f
        paint.textSize = 10f
        paint.color = Color.BLACK

        for (mov in movimientos) {
            // Verificar si necesitamos nueva página
            if (yPos > 780) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50f
                
                // Repetir cabecera
                paint.color = Color.parseColor("#3F51B5")
                paint.isFakeBoldText = true
                canvas.drawText("Fecha", 40f, yPos, paint)
                canvas.drawText("Concepto", 120f, yPos, paint)
                canvas.drawText("Categoría", 320f, yPos, paint)
                canvas.drawText("Monto", 500f, yPos, paint)
                yPos += 15f
                paint.isFakeBoldText = false
                paint.color = Color.BLACK
            }

            canvas.drawText(mov.fecha, 40f, yPos, paint)
            
            // Concepto (truncar si es largo)
            val conc = if (mov.concepto.length > 30) mov.concepto.substring(0, 27) + "..." else mov.concepto
            canvas.drawText(conc, 120f, yPos, paint)
            
            canvas.drawText(mov.nombreCategoria, 320f, yPos, paint)

            // Monto con color
            val monto = if (mov.debe > 0) mov.debe else mov.haber
            val textMonto = (if (mov.debe > 0) "+" else "-") + fmt.format(monto)
            val colorMonto = if (mov.debe > 0) Color.parseColor("#2ECC71") else Color.parseColor("#FF6F61")
            
            val originalColor = paint.color
            paint.color = colorMonto
            paint.isFakeBoldText = true
            canvas.drawText(textMonto, 500f, yPos, paint)
            
            paint.color = originalColor
            paint.isFakeBoldText = false

            yPos += 20f
            // Línea divisoria muy suave
            val grayColor = Color.parseColor("#F0F0F0")
            val p = Paint()
            p.color = grayColor
            canvas.drawLine(40f, yPos - 5f, 555f, yPos - 5f, p)
        }

        // --- PIE DE PÁGINA ---
        paint.textSize = 9f
        paint.color = Color.LTGRAY
        canvas.drawText("Generado por IngresosEgresosCX - Reporte Financiero Profesional", 40f, 820f, paint)

        pdfDocument.finishPage(page)

        try {
            FileOutputStream(pfd.fileDescriptor).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
