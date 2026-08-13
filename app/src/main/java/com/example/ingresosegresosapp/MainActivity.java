package com.example.ingresosegresosapp;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements MovimientoAdapter.OnItemClickListener {

    private DatabaseHelper dbHelper;
    private List<Movimiento> listaMovimientos;
    private List<Categoria> listaCategorias;
    private MovimientoAdapter adapter;

    // Vistas principales de saldos
    private TextView tvSaldoEfectivo, tvSaldoBanco, tvSaldoTotal;

    // Vistas inferiores de resumen de mes
    private TextView tvTotalIngresosMes, tvTotalEgresosMes, tvBalanceMes;

    // Spinners de filtro
    private Spinner spMesFiltro, spAnioFiltro;

    private String fechaSeleccionada = "";
    private int mesSeleccionado = 8;
    private int anioSeleccionado = 2026;

    private DecimalFormat fmt = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Configuración del Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Referencias a los Saldos Generales
        tvSaldoEfectivo = findViewById(R.id.tvSaldoEfectivo);
        tvSaldoBanco = findViewById(R.id.tvSaldoBanco);
        tvSaldoTotal = findViewById(R.id.tvSaldoTotal);

        // Referencias al Resumen Inferior del Mes
        tvTotalIngresosMes = findViewById(R.id.tvTotalIngresosMes);
        tvTotalEgresosMes = findViewById(R.id.tvTotalEgresosMes);
        tvBalanceMes = findViewById(R.id.tvBalanceMes);

        // Spinners de Corte
        spMesFiltro = findViewById(R.id.spFiltroMes);
        spAnioFiltro = findViewById(R.id.spFiltroAnio);

        // RecyclerView de Movimientos
        RecyclerView rvMovimientos = findViewById(R.id.rvMovimientos);
        rvMovimientos.setLayoutManager(new LinearLayoutManager(this));

        fechaSeleccionada = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        setupSpinnersFiltro();

        listaMovimientos = new ArrayList<>();
        adapter = new MovimientoAdapter(listaMovimientos, this);
        rvMovimientos.setAdapter(adapter);

        // Botón Flotante para agregar nuevo registro
        FloatingActionButton fabAgregar = findViewById(R.id.fabAgregar);
        fabAgregar.setOnClickListener(v -> abrirDialogoFormulario(null));

        actualizarTodo();
    }

    private void setupSpinnersFiltro() {
        String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        ArrayAdapter<String> adapterMes = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, meses);
        spMesFiltro.setAdapter(adapterMes);

        Calendar cal = Calendar.getInstance();
        mesSeleccionado = cal.get(Calendar.MONTH) + 1;
        spMesFiltro.setSelection(mesSeleccionado - 1);

        String[] anios = {"2024", "2025", "2026", "2027", "2028"};
        ArrayAdapter<String> adapterAnio = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, anios);
        spAnioFiltro.setAdapter(adapterAnio);
        anioSeleccionado = cal.get(Calendar.YEAR);

        // Seleccionar 2026 por defecto si existe en la lista
        for (int i = 0; i < anios.length; i++) {
            if (anios[i].equals(String.valueOf(anioSeleccionado))) {
                spAnioFiltro.setSelection(i);
                break;
            }
        }

        AdapterView.OnItemSelectedListener filtroListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mesSeleccionado = spMesFiltro.getSelectedItemPosition() + 1;
                anioSeleccionado = Integer.parseInt(spAnioFiltro.getSelectedItem().toString());
                actualizarTablaYTotales();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spMesFiltro.setOnItemSelectedListener(filtroListener);
        spAnioFiltro.setOnItemSelectedListener(filtroListener);
    }

    private void actualizarTodo() {
        actualizarTarjetasSaldo();
        actualizarTablaYTotales();
    }

    private void actualizarTarjetasSaldo() {
        Map<String, Double> saldos = dbHelper.obtenerSaldosPorCuenta();
        tvSaldoEfectivo.setText(fmt.format(saldos.get("EFECTIVO")));
        tvSaldoBanco.setText(fmt.format(saldos.get("BANCO")));
        tvSaldoTotal.setText(fmt.format(saldos.get("TOTAL")));
    }

    private void actualizarTablaYTotales() {
        listaMovimientos.clear();
        listaMovimientos.addAll(dbHelper.obtenerMovimientosPorMes(mesSeleccionado, anioSeleccionado));

        double totalIngresos = 0.0;
        double totalEgresos = 0.0;

        for (Movimiento m : listaMovimientos) {
            totalIngresos += m.getDebe();
            totalEgresos += m.getHaber();
        }

        double balanceMes = totalIngresos - totalEgresos;

        tvTotalIngresosMes.setText("+" + fmt.format(totalIngresos));
        tvTotalEgresosMes.setText("-" + fmt.format(totalEgresos));
        tvBalanceMes.setText(fmt.format(balanceMes));

        tvBalanceMes.setTextColor(balanceMes < 0 ? Color.RED : Color.parseColor("#1565C0"));
        adapter.notifyDataSetChanged();
    }

    private void abrirDialogoFormulario(Movimiento movimientoAEditar) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_formulario_movimiento, null);
        bottomSheetDialog.setContentView(view);

        TextView tvTituloForm = view.findViewById(R.id.tvTituloFormulario);
        TextInputEditText etFecha = view.findViewById(R.id.etFechaForm);
        TextInputEditText etConcepto = view.findViewById(R.id.etConceptoForm);
        TextInputEditText etIngreso = view.findViewById(R.id.etIngresoForm);
        TextInputEditText etEgreso = view.findViewById(R.id.etEgresoForm);
        Spinner spCuenta = view.findViewById(R.id.spCuentaForm);
        Spinner spCategoria = view.findViewById(R.id.spCategoriaForm);
        Button btnGuardar = view.findViewById(R.id.btnGuardarForm);

        // Configurar Spinner de Cuentas
        ArrayAdapter<String> adapterCuenta = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"EFECTIVO", "BANCO"});
        spCuenta.setAdapter(adapterCuenta);

        // Configurar Spinner de Categorías
        listaCategorias = dbHelper.obtenerCategorias();
        ArrayAdapter<Categoria> adapterCat = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listaCategorias);
        spCategoria.setAdapter(adapterCat);

        // Manejo de Fecha
        final String[] fechaForm = {fechaSeleccionada};
        etFecha.setText(fechaForm[0]);
        etFecha.setOnClickListener(v -> {
            String[] partes = fechaForm[0].split("-");
            int y = Integer.parseInt(partes[0]);
            int m = Integer.parseInt(partes[1]) - 1;
            int d = Integer.parseInt(partes[2]);

            DatePickerDialog dialog = new DatePickerDialog(this, (datePicker, year, month, dayOfMonth) -> {
                fechaForm[0] = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                etFecha.setText(fechaForm[0]);
            }, y, m, d);
            dialog.show();
        });

        // Si es edición, precargar los valores
        boolean esEdicion = (movimientoAEditar != null);
        if (esEdicion) {
            tvTituloForm.setText("Editar Movimiento");
            fechaForm[0] = movimientoAEditar.getFecha();
            etFecha.setText(fechaForm[0]);
            etConcepto.setText(movimientoAEditar.getConcepto());
            etIngreso.setText(movimientoAEditar.getDebe() > 0 ? String.valueOf(movimientoAEditar.getDebe()) : "");
            etEgreso.setText(movimientoAEditar.getHaber() > 0 ? String.valueOf(movimientoAEditar.getHaber()) : "");

            spCuenta.setSelection("BANCO".equalsIgnoreCase(movimientoAEditar.getCuenta()) ? 1 : 0);

            for (int i = 0; i < listaCategorias.size(); i++) {
                if (listaCategorias.get(i).getId() == movimientoAEditar.getCategoriaId()) {
                    spCategoria.setSelection(i);
                    break;
                }
            }
            btnGuardar.setText("Actualizar Movimiento");
        }

        btnGuardar.setOnClickListener(v -> {
            String concepto = etConcepto.getText() != null ? etConcepto.getText().toString().trim() : "";
            String ingStr = etIngreso.getText() != null ? etIngreso.getText().toString().trim() : "";
            String egStr = etEgreso.getText() != null ? etEgreso.getText().toString().trim() : "";

            if (concepto.isEmpty()) {
                Toast.makeText(this, "Ingresa un concepto", Toast.LENGTH_SHORT).show();
                return;
            }

            double ingreso = ingStr.isEmpty() ? 0.0 : Double.parseDouble(ingStr);
            double egreso = egStr.isEmpty() ? 0.0 : Double.parseDouble(egStr);

            if (ingreso == 0 && egreso == 0) {
                Toast.makeText(this, "Ingresa un valor en Ingreso o Egreso", Toast.LENGTH_SHORT).show();
                return;
            }

            String cuenta = spCuenta.getSelectedItem().toString();
            Categoria cat = (Categoria) spCategoria.getSelectedItem();
            int catId = cat != null ? cat.getId() : 1;
            String tipoMov = ingreso > 0 ? "INGRESO" : "EGRESO";

            if (esEdicion) {
                dbHelper.actualizarMovimiento(movimientoAEditar.getId(), fechaForm[0], concepto, ingreso, egreso, cuenta, catId, tipoMov);
                Toast.makeText(this, "Movimiento actualizado", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.insertarMovimiento(fechaForm[0], concepto, ingreso, egreso, cuenta, catId, tipoMov);
                Toast.makeText(this, "Movimiento guardado", Toast.LENGTH_SHORT).show();
            }

            bottomSheetDialog.dismiss();
            actualizarTodo();
        });

        bottomSheetDialog.show();
    }

    @Override
    public void onEditarClick(Movimiento movimiento) {
        abrirDialogoFormulario(movimiento);
    }

    @Override
    public void onEliminarClick(Movimiento movimiento) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Movimiento")
                .setMessage("¿Estás seguro de borrar este movimiento?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    dbHelper.eliminarMovimiento(movimiento.getId());
                    actualizarTodo();
                    Toast.makeText(this, "Registro eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Módulos secundarios del Menú
    private void abrirDialogoCategorias() {
        CategoriasDialog dialog = new CategoriasDialog(this, dbHelper, this::actualizarTodo);
        dialog.show();
    }

    private void abrirDialogoTransferencia() {
        TransferenciaDialog dialog = new TransferenciaDialog(this, dbHelper, this::actualizarTodo);
        dialog.show();
    }

    private void abrirGraficaGastos() {
        Map<String, Double> resumen = dbHelper.obtenerResumenGastosPorCategoria(mesSeleccionado, anioSeleccionado);
        String tituloMes = spMesFiltro.getSelectedItem().toString() + " " + anioSeleccionado;
        GraficaGastosDialog dialog = new GraficaGastosDialog(this, resumen, tituloMes);
        dialog.show();
    }

    private void abrirDialogoSaldosIniciales() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Establecer Saldos de Apertura");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etEf = new EditText(this);
        etEf.setHint("Saldo Inicial Efectivo ($)");
        etEf.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etEf.setText(String.valueOf(dbHelper.obtenerSaldoInicial("EFECTIVO")));
        layout.addView(etEf);

        final EditText etBa = new EditText(this);
        etBa.setHint("Saldo Inicial Banco ($)");
        etBa.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etBa.setText(String.valueOf(dbHelper.obtenerSaldoInicial("BANCO")));
        layout.addView(etBa);

        builder.setView(layout);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            double ef = etEf.getText().toString().isEmpty() ? 0.0 : Double.parseDouble(etEf.getText().toString());
            double ba = etBa.getText().toString().isEmpty() ? 0.0 : Double.parseDouble(etBa.getText().toString());

            dbHelper.guardarSaldoInicial("EFECTIVO", ef);
            dbHelper.guardarSaldoInicial("BANCO", ba);
            actualizarTodo();
            Toast.makeText(this, "Saldos iniciales actualizados", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarOpcionesExportacion() {
        String[] opciones = {"Exportar Mes Filtrado (" + spMesFiltro.getSelectedItem() + " " + anioSeleccionado + ")", "Exportar Histórico Completo"};
        new AlertDialog.Builder(this)
                .setTitle("Exportar a Excel (.xls)")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        List<Movimiento> movsMes = dbHelper.obtenerMovimientosPorMes(mesSeleccionado, anioSeleccionado);
                        ExcelExporter.exportarYCompartir(this, movsMes, "Estado_Cuenta_" + spMesFiltro.getSelectedItem() + "_" + anioSeleccionado);
                    } else {
                        List<Movimiento> movsTodos = dbHelper.obtenerTodosMovimientos();
                        ExcelExporter.exportarYCompartir(this, movsTodos, "Estado_Cuenta_Historico_Completo");
                    }
                })
                .show();
    }

    // Menú de opciones Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_grafica) {
            abrirGraficaGastos();
            return true;
        } else if (id == R.id.action_transferir) {
            abrirDialogoTransferencia();
            return true;
        } else if (id == R.id.action_categorias) {
            abrirDialogoCategorias();
            return true;
        } else if (id == R.id.action_saldos_iniciales) {
            abrirDialogoSaldosIniciales();
            return true;
        } else if (id == R.id.action_exportar_excel) {
            mostrarOpcionesExportacion();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}