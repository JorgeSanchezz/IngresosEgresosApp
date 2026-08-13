package com.example.ingresosegresosapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class MovimientoAdapter extends RecyclerView.Adapter<MovimientoAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onEditarClick(Movimiento movimiento);
        void onEliminarClick(Movimiento movimiento);
    }

    private final List<Movimiento> listaMovimientos;
    private final OnItemClickListener listener;
    private final DecimalFormat fmt = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));

    public MovimientoAdapter(List<Movimiento> listaMovimientos, OnItemClickListener listener) {
        this.listaMovimientos = listaMovimientos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movimiento, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Movimiento m = listaMovimientos.get(position);

        // Asignación de datos
        holder.tvFecha.setText(m.getFecha());

        // Muestra Cuenta y Categoría (Ej. EFECTIVO | Alimentos)
        // Cambia getCategoriaNombre() por getNombreCategoria()
        String cuentaCat = m.getCuenta() + (m.getNombreCategoria() != null ? " | " + m.getNombreCategoria() : "");
        holder.tvCuentaCategoria.setText(cuentaCat);

        holder.tvConcepto.setText(m.getConcepto());
        holder.tvMontoIngreso.setText("Ingreso: " + fmt.format(m.getDebe()));
        holder.tvMontoEgreso.setText("Egreso: " + fmt.format(m.getHaber()));

        // Cálculo y formato de neto/saldo
        double neto = m.getDebe() - m.getHaber();
        if (neto >= 0) {
            holder.tvNeto.setText("+" + fmt.format(neto));
            holder.tvNeto.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            holder.tvNeto.setText("-" + fmt.format(Math.abs(neto)));
            holder.tvNeto.setTextColor(Color.parseColor("#C62828"));
        }

        // Listeners para los botones de acción
        holder.btnEditar.setOnClickListener(v -> {
            if (listener != null) listener.onEditarClick(m);
        });

        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) listener.onEliminarClick(m);
        });
    }

    @Override
    public int getItemCount() {
        return listaMovimientos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvCuentaCategoria, tvConcepto, tvMontoIngreso, tvMontoEgreso, tvNeto;
        ImageView btnEditar, btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Mapeo exacto con las IDs de tu item_movimiento.xml
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvCuentaCategoria = itemView.findViewById(R.id.tvCuentaCategoriaItem);
            tvConcepto = itemView.findViewById(R.id.tvConceptoItem);
            tvMontoIngreso = itemView.findViewById(R.id.tvMontoIngresoItem);
            tvMontoEgreso = itemView.findViewById(R.id.tvMontoEgresoItem);
            tvNeto = itemView.findViewById(R.id.tvNetoItem);

            btnEditar = itemView.findViewById(R.id.btnEditarItem);
            btnEliminar = itemView.findViewById(R.id.btnEliminarItem);
        }
    }
}