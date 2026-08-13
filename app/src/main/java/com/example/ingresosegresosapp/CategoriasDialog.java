package com.example.ingresosegresosapp;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.List;

public class CategoriasDialog extends Dialog {

    private DatabaseHelper dbHelper;
    private RecyclerView rvCategorias;
    private CategoriaAdapter adapter;
    private EditText etNombreCat;
    private Spinner spTipoCat;
    private Button btnGuardarCat;
    private List<Categoria> listaCat;
    private Runnable onDismissCallback;

    private boolean modoEdicion = false;
    private int idEdicion = -1;

    public CategoriasDialog(@NonNull Context context, DatabaseHelper dbHelper, Runnable onDismissCallback) {
        super(context);
        this.dbHelper = dbHelper;
        this.onDismissCallback = onDismissCallback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_categorias);
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        etNombreCat = findViewById(R.id.etNombreCat);
        spTipoCat = findViewById(R.id.spTipoCat);
        btnGuardarCat = findViewById(R.id.btnGuardarCat);
        rvCategorias = findViewById(R.id.rvCategorias);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, new String[]{"EGRESO", "INGRESO", "AMBOS"});
        spTipoCat.setAdapter(spinnerAdapter);

        rvCategorias.setLayoutManager(new LinearLayoutManager(getContext()));
        cargarCategorias();

        btnGuardarCat.setOnClickListener(v -> {
            String nombre = etNombreCat.getText().toString().trim();
            String tipo = spTipoCat.getSelectedItem().toString();

            if (nombre.isEmpty()) {
                Toast.makeText(getContext(), "Ingresa un nombre para la categoría", Toast.LENGTH_SHORT).show();
                return;
            }

            if (modoEdicion) {
                dbHelper.actualizarCategoria(idEdicion, nombre, tipo);
                Toast.makeText(getContext(), "Categoría actualizada", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.insertarCategoria(nombre, tipo);
                Toast.makeText(getContext(), "Categoría creada", Toast.LENGTH_SHORT).show();
            }

            limpiar();
            cargarCategorias();
        });

        setOnDismissListener(dialog -> {
            if (onDismissCallback != null) onDismissCallback.run();
        });
    }

    private void cargarCategorias() {
        listaCat = dbHelper.obtenerCategorias();
        adapter = new CategoriaAdapter(listaCat, new CategoriaAdapter.OnCategoriaListener() {
            @Override
            public void onEditar(Categoria categoria) {
                modoEdicion = true;
                idEdicion = categoria.getId();
                etNombreCat.setText(categoria.getNombre());
                spTipoCat.setSelection(categoria.getTipo().equals("INGRESO") ? 1 : (categoria.getTipo().equals("AMBOS") ? 2 : 0));
                btnGuardarCat.setText("Actualizar");
            }

            @Override
            public void onEliminar(Categoria categoria) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Eliminar Categoría")
                        .setMessage("¿Deseas eliminar '" + categoria.getNombre() + "'? Los movimientos vinculados pasarán a 'Sin Categoría'.")
                        .setPositiveButton("Eliminar", (d, w) -> {
                            dbHelper.eliminarCategoria(categoria.getId());
                            cargarCategorias();
                            Toast.makeText(getContext(), "Categoría eliminada", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
        rvCategorias.setAdapter(adapter);
    }

    private void limpiar() {
        etNombreCat.setText("");
        modoEdicion = false;
        idEdicion = -1;
        btnGuardarCat.setText("Guardar");
    }
}