package com.example.ingresosegresosapp;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements MovimientoAdapter.OnItemClickListener {

    private DatabaseHelper dbHelper;
    private List<Movimiento> listaMovimientos;
    private MovimientoAdapter adapter;
    private EditText etConcepto, etDebe, etHaber;
    private Button btnAgregar;
    private TextView tvTotalDebe, tvTotalHaber, tvSaldoActual;

    // Control para saber si estamos editando un registro existente
    private boolean modoEdicion = false;
    private int idMovimientoEnEdicion = -1;
    private String fechaOriginal = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTotalDebe = findViewById(R.id.tvTotalDebe);
        tvTotalHaber = findViewById(R.id.tvTotalHaber);
        tvSaldoActual = findViewById(R.id.tvSaldoActual);

        dbHelper = new DatabaseHelper(this);

        etConcepto = findViewById(R.id.etConcepto);
        etDebe = findViewById(R.id.etDebe);
        etHaber = findViewById(R.id.etHaber);
        btnAgregar = findViewById(R.id.btnAgregar);
        RecyclerView rvTabla = findViewById(R.id.rvTabla);

        listaMovimientos = dbHelper.obtenerMovimientos();

        adapter = new MovimientoAdapter(listaMovimientos, this);
        rvTabla.setLayoutManager(new LinearLayoutManager(this));
        rvTabla.setAdapter(adapter);

        btnAgregar.setOnClickListener(v -> guardarOActualizarMovimiento());

        // Calcula saldos, totales del footer y notifica al adapter
        recalcularSaldosYTotales();
    }

    private void guardarOActualizarMovimiento() {
        String concepto = etConcepto.getText().toString().trim();
        String debeStr = etDebe.getText().toString().trim();
        String haberStr = etHaber.getText().toString().trim();

        if (concepto.isEmpty()) {
            Toast.makeText(MainActivity.this, "Ingresa un concepto", Toast.LENGTH_SHORT).show();
            return;
        }

        double debe = debeStr.isEmpty() ? 0.0 : Double.parseDouble(debeStr);
        double haber = haberStr.isEmpty() ? 0.0 : Double.parseDouble(haberStr);

        if (modoEdicion) {
            // Actualizar en base de datos conservando la fecha original
            dbHelper.actualizarMovimiento(idMovimientoEnEdicion, fechaOriginal, concepto, debe, haber);
            Toast.makeText(MainActivity.this, "Registro actualizado", Toast.LENGTH_SHORT).show();
        } else {
            // Insertar nuevo registro con la fecha de hoy
            String fechaActual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            dbHelper.insertarMovimiento(fechaActual, concepto, debe, haber);
            Toast.makeText(MainActivity.this, "Movimiento guardado", Toast.LENGTH_SHORT).show();
        }

        limpiarCampos();
        actualizarListaYVista();
    }

    private void actualizarListaYVista() {
        listaMovimientos.clear();
        listaMovimientos.addAll(dbHelper.obtenerMovimientos());
        recalcularSaldosYTotales(); // Actualiza tanto la lista como las sumas inferiores
    }

    private void limpiarCampos() {
        etConcepto.setText("");
        etDebe.setText("");
        etHaber.setText("");
        etConcepto.clearFocus();

        // Resetea el estado de edición al limpiar
        modoEdicion = false;
        idMovimientoEnEdicion = -1;
        fechaOriginal = "";
        btnAgregar.setText("Agregar Movimiento");
    }

    @Override
    public void onEditarClick(Movimiento movimiento) {
        modoEdicion = true;
        idMovimientoEnEdicion = movimiento.getId();
        fechaOriginal = movimiento.getFecha();

        etConcepto.setText(movimiento.getConcepto());
        etDebe.setText(movimiento.getDebe() > 0 ? String.valueOf(movimiento.getDebe()) : "");
        etHaber.setText(movimiento.getHaber() > 0 ? String.valueOf(movimiento.getHaber()) : "");

        btnAgregar.setText("Actualizar Movimiento");
        etConcepto.requestFocus();
        Toast.makeText(this, "Modo Edición Activado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEliminarClick(Movimiento movimiento) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Movimiento")
                .setMessage("¿Estás seguro de eliminar este registro?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    dbHelper.eliminarMovimiento(movimiento.getId());

                    // Si estaba editando justo este registro, limpia los campos y cancela el modo edición
                    if (modoEdicion && idMovimientoEnEdicion == movimiento.getId()) {
                        limpiarCampos();
                    }

                    actualizarListaYVista();
                    Toast.makeText(MainActivity.this, "Registro eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void recalcularSaldosYTotales() {
        double saldoAcumulado = 0.0;
        double totalDebe = 0.0;
        double totalHaber = 0.0;

        if (listaMovimientos != null) {
            for (Movimiento m : listaMovimientos) {
                totalDebe += m.getDebe();
                totalHaber += m.getHaber();

                // Saldo acumulado renglón por renglón
                saldoAcumulado += (m.getDebe() - m.getHaber());
                m.setSaldo(saldoAcumulado);
            }
        }

        // Formato con punto decimal y comas
        DecimalFormat df = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));

        tvTotalDebe.setText(df.format(totalDebe));
        tvTotalHaber.setText(df.format(totalHaber));
        tvSaldoActual.setText(df.format(saldoAcumulado));

        // Cambiar color del Saldo Actual según sea positivo o negativo
        if (saldoAcumulado < 0) {
            tvSaldoActual.setTextColor(Color.RED);
        } else {
            tvSaldoActual.setTextColor(Color.parseColor("#008000")); // Verde
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}