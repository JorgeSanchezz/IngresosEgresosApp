package com.example.ingresosegresosapp;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Map;
import java.util.Locale;

public class GraficaGastosDialog extends Dialog {

    private Map<String, Double> resumenGastos;
    private String tituloMes;

    public GraficaGastosDialog(@NonNull Context context, Map<String, Double> resumenGastos, String tituloMes) {
        super(context);
        this.resumenGastos = resumenGastos;
        this.tituloMes = tituloMes;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_grafica);
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitulo = findViewById(R.id.tvTituloGrafica);
        LinearLayout containerChart = findViewById(R.id.containerChart);
        LinearLayout containerLeyenda = findViewById(R.id.containerLeyenda);

        tvTitulo.setText("Distribución de Gastos (" + tituloMes + ")");

        double totalGastado = 0.0;
        for (double v : resumenGastos.values()) {
            totalGastado += v;
        }

        if (totalGastado == 0) {
            TextView tvVacio = new TextView(getContext());
            tvVacio.setText("No hay egresos registrados en este periodo.");
            tvVacio.setTextSize(16f);
            tvVacio.setPadding(16, 32, 16, 32);
            containerChart.addView(tvVacio);
            return;
        }

        // Vista de pastel personalizada
        PieChartView pieChart = new PieChartView(getContext(), resumenGastos, totalGastado);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(500, 500);
        params.gravity = android.view.Gravity.CENTER;
        pieChart.setLayoutParams(params);
        containerChart.addView(pieChart);

        // Leyenda
        DecimalFormat fmt = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));
        int[] colores = getColores();
        int idx = 0;

        for (Map.Entry<String, Double> entry : resumenGastos.entrySet()) {
            double porc = (entry.getValue() / totalGastado) * 100;
            TextView tvItem = new TextView(getContext());
            tvItem.setText(String.format(Locale.US, "■ %s: %s (%.1f%%)", entry.getKey(), fmt.format(entry.getValue()), porc));
            tvItem.setTextColor(colores[idx % colores.length]);
            tvItem.setTextSize(15);
            tvItem.setPadding(0, 8, 0, 8);
            containerLeyenda.addView(tvItem);
            idx++;
        }
    }

    private static int[] getColores() {
        return new int[]{
                Color.parseColor("#E53935"), Color.parseColor("#1E88E5"),
                Color.parseColor("#43A047"), Color.parseColor("#FB8C00"),
                Color.parseColor("#8E24AA"), Color.parseColor("#00ACC1"),
                Color.parseColor("#3949AB"), Color.parseColor("#D81B60")
        };
    }

    private static class PieChartView extends View {
        private Map<String, Double> datos;
        private double total;
        private Paint paint;
        private RectF rectF;

        public PieChartView(Context context, Map<String, Double> datos, double total) {
            super(context);
            this.datos = datos;
            this.total = total;
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            rectF = new RectF();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            float radius = Math.min(width, height) / 2f - 20;

            rectF.set(width / 2f - radius, height / 2f - radius, width / 2f + radius, height / 2f + radius);

            float startAngle = 0f;
            int[] colores = getColores();
            int idx = 0;

            for (double valor : datos.values()) {
                float sweepAngle = (float) ((valor / total) * 360f);
                paint.setColor(colores[idx % colores.length]);
                canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
                startAngle += sweepAngle;
                idx++;
            }
        }
    }
}