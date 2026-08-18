package com.example.ingresosegresoscx.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.ingresosegresoscx.models.Movimiento
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object ExcelExporter {

    @JvmStatic
    fun exportarYCompartir(context: Context, movimientos: List<Movimiento>, nombreArchivo: String) {
        try {
            val exportDir = File(context.cacheDir, "excel_exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, "$nombreArchivo.xls")
            val fos = FileOutputStream(file)
            val writer = OutputStreamWriter(fos, StandardCharsets.UTF_8)

            val fmt = DecimalFormat("$#,##0.00", DecimalFormatSymbols(Locale.US))

            // Formato HTML estructurado reconocido nativamente por MS Excel
            val html = StringBuilder()
            html.append("<html><head><meta charset=\"UTF-8\"></head><body>")
            html.append("<table border=\"1\">")
            html.append("<tr style=\"background-color:#1E88E5; color:#FFFFFF; font-weight:bold;\">")
            html.append("<th>ID</th><th>Fecha</th><th>Cuenta</th><th>Tipo</th><th>Categoría</th><th>Concepto</th><th>Debe (Ingreso)</th><th>Haber (Egreso)</th><th>Saldo Acumulado</th>")
            html.append("</tr>")

            var saldoAcum = 0.0
            for (m in movimientos) {
                saldoAcum += (m.debe - m.haber)
                html.append("<tr>")
                    .append("<td>").append(m.id).append("</td>")
                    .append("<td>").append(m.fecha).append("</td>")
                    .append("<td>").append(m.cuenta).append("</td>")
                    .append("<td>").append(m.tipoMovimiento).append("</td>")
                    .append("<td>").append(m.nombreCategoria).append("</td>")
                    .append("<td>").append(m.concepto).append("</td>")
                    .append("<td>").append(fmt.format(m.debe)).append("</td>")
                    .append("<td>").append(fmt.format(m.haber)).append("</td>")
                    .append("<td>").append(fmt.format(saldoAcum)).append("</td>")
                    .append("</tr>")
            }

            html.append("</table></body></html>")

            writer.write(html.toString())
            writer.flush()
            writer.close()

            val fileUri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(sendIntent, "Exportar estado de cuenta"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
