package com.example.ingresosegresosapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class ExcelExporter {

    public static void exportarYCompartir(Context context, List<Movimiento> movimientos, String nombreArchivo) {
        try {
            File exportDir = new File(context.getCacheDir(), "excel_exports");
            if (!exportDir.exists()) exportDir.mkdirs();

            File file = new File(exportDir, nombreArchivo + ".xls");
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");

            DecimalFormat fmt = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));

            // Formato HTML estructurado reconocido nativamente por MS Excel
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset=\"UTF-8\"></head><body>");
            html.append("<table border=\"1\">");
            html.append("<tr style=\"background-color:#1E88E5; color:#FFFFFF; font-weight:bold;\">");
            html.append("<th>ID</th><th>Fecha</th><th>Cuenta</th><th>Tipo</th><th>Categoría</th><th>Concepto</th><th>Debe (Ingreso)</th><th>Haber (Egreso)</th><th>Saldo Acumulado</th>");
            html.append("</tr>");

            double saldoAcum = 0.0;
            for (Movimiento m : movimientos) {
                saldoAcum += (m.getDebe() - m.getHaber());
                html.append("<tr>")
                        .append("<td>").append(m.getId()).append("</td>")
                        .append("<td>").append(m.getFecha()).append("</td>")
                        .append("<td>").append(m.getCuenta()).append("</td>")
                        .append("<td>").append(m.getTipoMovimiento()).append("</td>")
                        .append("<td>").append(m.getNombreCategoria()).append("</td>")
                        .append("<td>").append(m.getConcepto()).append("</td>")
                        .append("<td>").append(fmt.format(m.getDebe())).append("</td>")
                        .append("<td>").append(fmt.format(m.getHaber())).append("</td>")
                        .append("<td>").append(fmt.format(saldoAcum)).append("</td>")
                        .append("</tr>");
            }

            html.append("</table></body></html>");

            writer.write(html.toString());
            writer.flush();
            writer.close();

            Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("application/vnd.ms-excel");
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(sendIntent, "Exportar estado de cuenta"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}