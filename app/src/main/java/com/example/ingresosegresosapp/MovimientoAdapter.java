package com.example.ingresosegresosapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class MovimientoAdapter extends RecyclerView.Adapter<MovimientoAdapter.ViewHolder> {

    private List<Movimiento> listaMovimientos;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEditarClick(Movimiento movimiento);
        void onEliminarClick(Movimiento movimiento);
    }

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
        Movimiento mov = listaMovimientos.get(position);

        holder.tvFecha.setText(mov.getFecha());
        holder.tvConcepto.setText(mov.getConcepto());
        holder.tvDebe.setText(String.format(Locale.US, "%.2f", mov.getDebe()));
        holder.tvHaber.setText(String.format(Locale.US, "%.2f", mov.getHaber()));
        holder.tvSaldo.setText(String.format(Locale.US, "%.2f", mov.getSaldo()));

        holder.btnEditar.setOnClickListener(v -> listener.onEditarClick(mov));
        holder.btnEliminar.setOnClickListener(v -> listener.onEliminarClick(mov));
    }

    @Override
    public int getItemCount() {
        return listaMovimientos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvConcepto, tvDebe, tvHaber, tvSaldo;
        ImageButton btnEditar, btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvConcepto = itemView.findViewById(R.id.tvConcepto);
            tvDebe = itemView.findViewById(R.id.tvDebe);
            tvHaber = itemView.findViewById(R.id.tvHaber);
            tvSaldo = itemView.findViewById(R.id.tvSaldo);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}