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
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransferenciaDialog extends Dialog {

    private DatabaseHelper dbHelper;
    private Runnable onComplete;
    private Spinner spOrigen, spDestino;
    private EditText etMonto, etConcepto;
    private Button btnTransferir;

    public TransferenciaDialog(@NonNull Context context, DatabaseHelper dbHelper, Runnable onComplete) {
        super(context);
        this.dbHelper = dbHelper;
        this.onComplete = onComplete;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_transferencia);
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        spOrigen = findViewById(R.id.spOrigenTransf);
        spDestino = findViewById(R.id.spDestinoTransf);
        etMonto = findViewById(R.id.etMontoTransf);
        etConcepto = findViewById(R.id.etConceptoTransf);
        btnTransferir = findViewById(R.id.btnEjecutarTransf);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, new String[]{"BANCO", "EFECTIVO"});
        spOrigen.setAdapter(adapter);

        ArrayAdapter<String> adapterDestino = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, new String[]{"EFECTIVO", "BANCO"});
        spDestino.setAdapter(adapterDestino);

        btnTransferir.setOnClickListener(v -> {
            String origen = spOrigen.getSelectedItem().toString();
            String destino = spDestino.getSelectedItem().toString();
            String montoStr = etMonto.getText().toString().trim();
            String concepto = etConcepto.getText().toString().trim();

            if (origen.equalsIgnoreCase(destino)) {
                Toast.makeText(getContext(), "Origen y destino deben ser distintos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (montoStr.isEmpty()) {
                Toast.makeText(getContext(), "Ingresa un monto válido", Toast.LENGTH_SHORT).show();
                return;
            }

            double monto = Double.parseDouble(montoStr);
            if (monto <= 0) {
                Toast.makeText(getContext(), "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return;
            }

            String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            boolean ok = dbHelper.registrarTransferencia(fechaActual, monto, origen, destino, concepto.isEmpty() ? "Traspaso interno" : concepto);

            if (ok) {
                Toast.makeText(getContext(), "Transferencia realizada con éxito", Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
                dismiss();
            } else {
                Toast.makeText(getContext(), "Error al realizar transferencia", Toast.LENGTH_SHORT).show();
            }
        });
    }
}