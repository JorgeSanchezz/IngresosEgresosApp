package com.example.ingresosegresosapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoriaAdapter extends RecyclerView.Adapter<CategoriaAdapter.ViewHolder> {

    private List<Categoria> listaCategorias;
    private OnCategoriaListener listener;

    public interface OnCategoriaListener {
        void onEditar(Categoria categoria);
        void onEliminar(Categoria categoria);
    }

    public CategoriaAdapter(List<Categoria> listaCategorias, OnCategoriaListener listener) {
        this.listaCategorias = listaCategorias;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_categoria, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Categoria cat = listaCategorias.get(position);
        holder.tvNombre.setText(cat.getNombre());
        holder.tvTipo.setText(cat.getTipo());

        if (cat.getId() == 1) { // Sin Categoría es protegida
            holder.btnEditar.setVisibility(View.GONE);
            holder.btnEliminar.setVisibility(View.GONE);
        } else {
            holder.btnEditar.setVisibility(View.VISIBLE);
            holder.btnEliminar.setVisibility(View.VISIBLE);
            holder.btnEditar.setOnClickListener(v -> listener.onEditar(cat));
            holder.btnEliminar.setOnClickListener(v -> listener.onEliminar(cat));
        }
    }

    @Override
    public int getItemCount() { return listaCategorias.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvTipo;
        ImageButton btnEditar, btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvCatNombreItem);
            tvTipo = itemView.findViewById(R.id.tvCatTipoItem);
            btnEditar = itemView.findViewById(R.id.btnEditarCat);
            btnEliminar = itemView.findViewById(R.id.btnEliminarCat);
        }
    }
}