package com.example.ingresosegresosapp;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity implements MovimientoAdapter.OnItemClickListener {

    private DatabaseHelper dbHelper;
    private List<Movimiento> listaMovimientos;
    private List<Categoria> listaCategorias;
    private MovimientoAdapter adapter;

    private TextView tvSaldoEfectivo, tvSaldoBanco, tvSaldoTotal;
    private TextView tvTotalDebe, tvTotalHaber, tvSaldoActual;
    private EditText etConcepto, etDebe, etHaber;
    private Button btnAgregar, btnSeleccionarFecha, btnTransferirMain, btnGestionCategorias, btnAperturaSaldos, btnVerGrafica, btnExportarExcel;
    private Spinner spMesFiltro, spAnioFiltro, spCuentaInput, spCategoriaInput;

    private String fechaSeleccionada = "";
    private int mesSeleccionado = 8;
    private int anioSeleccionado = 2026;

    private boolean modoEdicion = false;
    private int idMovimientoEnEdicion = -1;

    private DecimalFormat fmt = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Referencias
        tvSaldoEfectivo = findViewById(R.id.tvSaldoEfectivo);
        tvSaldoBanco = findViewById(R.id.tvSaldoBanco);
        tvSaldoTotal = findViewById(R.id.tvSaldoTotal);
        tvTotalDebe = findViewById(R.id.tvTotalDebe);
        tvTotalHaber = findViewById(R.id.tvTotalHaber);
        tvSaldoActual = findViewById(R.id.tvSaldoActual);

        etConcepto = findViewById(R.id.etConcepto);
        etDebe = findViewById(R.id.etDebe);
        etHaber = findViewById(R.id.etHaber);
        btnAgregar = findViewById(R.id.btnAgregar);
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha);
        btnTransferirMain = findViewById(R.id.btnTransferirMain);
        btnGestionCategorias = findViewById(R.id.btnGestionCategorias);
        btnAperturaSaldos = findViewById(R.id.btnAperturaSaldos);
        btnVerGrafica = findViewById(R.id.btnVerGrafica);
        btnExportarExcel = findViewById(R.id.btnExportarExcel);

        spMesFiltro = findViewById(R.id.spMesFiltro);
        spAnioFiltro = findViewById(R.id.spAnioFiltro);
        spCuentaInput = findViewById(R.id.spCuentaInput);
        spCategoriaInput = findViewById(R.id.spCategoriaInput);

        RecyclerView rvTabla = findViewById(R.id.rvTabla);
        rvTabla.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar fecha
        fechaSeleccionada = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        btnSeleccionarFecha.setText(fechaSeleccionada);

        btnSeleccionarFecha.setOnClickListener(v -> abrirDatePicker());

        setupSpinners();

        listaMovimientos = new ArrayList<>();
        adapter = new MovimientoAdapter(listaMovimientos, this);
        rvTabla.setAdapter(adapter);

        btnAgregar.setOnClickListener(v -> guardarOActualizarMovimiento());
        btnTransferirMain.setOnClickListener(v -> abrirDialogoTransferencia());
        btnGestionCategorias.setOnClickListener(v -> abrirDialogoCategorias());
        btnAperturaSaldos.setOnClickListener(v -> abrirDialogoSaldosIniciales());
        btnVerGrafica.setOnClickListener(v -> abrirGraficaGastos());
        btnExportarExcel.setOnClickListener(v -> mostrarOpcionesExportacion());

        actualizarTodo();
    }

    private void setupSpinners() {
        // Cuentas Input
        ArrayAdapter<String> adapterCuenta = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"EFECTIVO", "BANCO"});
        spCuentaInput.setAdapter(adapterCuenta);

        // Categorías Input
        cargarCategoriasEnSpinner();

        // Filtro Meses
        String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        ArrayAdapter<String> adapterMes = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, meses);
        spMesFiltro.setAdapter(adapterMes);

        Calendar cal = Calendar.getInstance();
        mesSeleccionado = cal.get(Calendar.MONTH) + 1;
        spMesFiltro.setSelection(mesSeleccionado - 1);

        // Filtro Años
        String[] anios = {"2024", "2025", "2026", "2027", "2028"};
        ArrayAdapter<String> adapterAnio = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, anios);
        spAnioFiltro.setAdapter(adapterAnio);
        anioSeleccionado = cal.get(Calendar.YEAR);
        spAnioFiltro.setSelection(2); // 2026 por defecto

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

    private void cargarCategoriasEnSpinner() {
        listaCategorias = dbHelper.obtenerCategorias();
        ArrayAdapter<Categoria> adapterCat = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listaCategorias);
        spCategoriaInput.setAdapter(adapterCat);
    }

    private void abrirDatePicker() {
        String[] partes = fechaSeleccionada.split("-");
        int y = Integer.parseInt(partes[0]);
        int m = Integer.parseInt(partes[1]) - 1;
        int d = Integer.parseInt(partes[2]);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            fechaSeleccionada = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            btnSeleccionarFecha.setText(fechaSeleccionada);
        }, y, m, d);
        dialog.show();
    }

    private void guardarOActualizarMovimiento() {
        String concepto = etConcepto.getText().toString().trim();
        String debeStr = etDebe.getText().toString().trim();
        String haberStr = etHaber.getText().toString().trim();

        if (concepto.isEmpty()) {
            Toast.makeText(this, "Ingresa un concepto", Toast.LENGTH_SHORT).show();
            return;
        }

        double debe = debeStr.isEmpty() ? 0.0 : Double.parseDouble(debeStr);
        double haber = haberStr.isEmpty() ? 0.0 : Double.parseDouble(haberStr);

        if (debe == 0 && haber == 0) {
            Toast.makeText(this, "Ingresa un valor en Ingreso o Egreso", Toast.LENGTH_SHORT).show();
            return;
        }

        String cuenta = spCuentaInput.getSelectedItem().toString();
        Categoria cat = (Categoria) spCategoriaInput.getSelectedItem();
        int catId = cat != null ? cat.getId() : 1;
        String tipoMov = debe > 0 ? "INGRESO" : "EGRESO";

        if (modoEdicion) {
            dbHelper.actualizarMovimiento(idMovimientoEnEdicion, fechaSeleccionada, concepto, debe, haber, cuenta, catId, tipoMov);
            Toast.makeText(this, "Movimiento actualizado", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.insertarMovimiento(fechaSeleccionada, concepto, debe, haber, cuenta, catId, tipoMov);
            Toast.makeText(this, "Movimiento guardado", Toast.LENGTH_SHORT).show();
        }

        limpiarCampos();
        actualizarTodo();
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

        double totalDebe = 0.0;
        double totalHaber = 0.0;
        double saldoAcum = 0.0;

        for (Movimiento m : listaMovimientos) {
            totalDebe += m.getDebe();
            totalHaber += m.getHaber();
            saldoAcum += (m.getDebe() - m.getHaber());
            m.setSaldo(saldoAcum);
        }

        tvTotalDebe.setText(fmt.format(totalDebe));
        tvTotalHaber.setText(fmt.format(totalHaber));
        tvSaldoActual.setText(fmt.format(saldoAcum));

        tvSaldoActual.setTextColor(saldoAcum < 0 ? Color.RED : Color.parseColor("#008000"));
        adapter.notifyDataSetChanged();
    }

    private void limpiarCampos() {
        etConcepto.setText("");
        etDebe.setText("");
        etHaber.setText("");
        etConcepto.clearFocus();

        modoEdicion = false;
        idMovimientoEnEdicion = -1;
        btnAgregar.setText("Guardar Movimiento");
    }

    @Override
    public void onEditarClick(Movimiento movimiento) {
        modoEdicion = true;
        idMovimientoEnEdicion = movimiento.getId();

        fechaSeleccionada = movimiento.getFecha();
        btnSeleccionarFecha.setText(fechaSeleccionada);

        etConcepto.setText(movimiento.getConcepto());
        etDebe.setText(movimiento.getDebe() > 0 ? String.valueOf(movimiento.getDebe()) : "");
        etHaber.setText(movimiento.getHaber() > 0 ? String.valueOf(movimiento.getHaber()) : "");

        spCuentaInput.setSelection("BANCO".equalsIgnoreCase(movimiento.getCuenta()) ? 1 : 0);

        for (int i = 0; i < listaCategorias.size(); i++) {
            if (listaCategorias.get(i).getId() == movimiento.getCategoriaId()) {
                spCategoriaInput.setSelection(i);
                break;
            }
        }

        btnAgregar.setText("Actualizar Movimiento");
    }

    @Override
    public void onEliminarClick(Movimiento movimiento) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Movimiento")
                .setMessage("¿Estás seguro de borrar este movimiento?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    dbHelper.eliminarMovimiento(movimiento.getId());
                    if (modoEdicion && idMovimientoEnEdicion == movimiento.getId()) {
                        limpiarCampos();
                    }
                    actualizarTodo();
                    Toast.makeText(this, "Registro eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void abrirDialogoCategorias() {
        CategoriasDialog dialog = new CategoriasDialog(this, dbHelper, () -> {
            cargarCategoriasEnSpinner();
            actualizarTablaYTotales();
        });
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

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_transferencia, null); // Reutilizamos contenedor o construimos vista simple
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

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
}