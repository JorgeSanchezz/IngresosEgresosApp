package com.example.ingresosegresoscx

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.*
import com.example.ingresosegresoscx.models.Movimiento
import com.example.ingresosegresoscx.ui.*
import com.example.ingresosegresoscx.ui.theme.IngresosEgresosTheme
import com.example.ingresosegresoscx.utils.ExcelExporter
import com.example.ingresosegresoscx.utils.JsonBackupUtils
import com.example.ingresosegresoscx.utils.ZipBackupHelper
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var createDocumentLauncher: ActivityResultLauncher<String>
    private lateinit var openDocumentLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var createPdfLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLaunchers()

        setContent {
            IngresosEgresosTheme {
                val activeDialog by viewModel.activeDialog
                val movimientoAEditar by viewModel.movimientoAEditar

                MainScreen(
                    viewModel = viewModel,
                    onOpenDialog = { dialogName ->
                        viewModel.setActiveDialog(dialogName)
                    },
                    onEditMovimiento = { mov ->
                        viewModel.setMovimientoAEditar(mov)
                        viewModel.setActiveDialog("Formulario")
                    },
                    onAddMovimiento = {
                        viewModel.setMovimientoAEditar(null)
                        viewModel.setActiveDialog("Formulario")
                    }
                )

                // Dialog Handling
                when (activeDialog) {
                    "Formulario" -> {
                        MovimientoFormDialog(
                            movimiento = movimientoAEditar,
                            dbHelper = viewModel.getDbHelper(),
                            onDismiss = { viewModel.setActiveDialog(null) },
                            onSave = { viewModel.actualizarTodo() }
                        )
                    }
                    "Menu" -> {
                        OptionsBottomSheet(
                            onDismiss = { viewModel.setActiveDialog(null) },
                            onOptionClick = { which ->
                                when (which) {
                                    0 -> viewModel.setActiveDialog("Transferencia")
                                    1 -> viewModel.setActiveDialog("Categorias")
                                    2 -> viewModel.setActiveDialog("Saldos")
                                    3 -> {
                                        viewModel.setActiveDialog(null)
                                        mostrarOpcionesExportacion()
                                    }
                                    4 -> {
                                        viewModel.setActiveDialog(null)
                                        createDocumentLauncher.launch("IngresosEgresosCX_Backup.zip")
                                    }
                                    5 -> {
                                        viewModel.setActiveDialog(null)
                                        openDocumentLauncher.launch(arrayOf("application/zip", "application/json", "application/octet-stream"))
                                    }
                                    6 -> {
                                        viewModel.setActiveDialog(null)
                                        val meses = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                                            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
                                        val currentMonthName = meses[viewModel.mesSeleccionado.value - 1]
                                        createPdfLauncher.launch("Reporte_${currentMonthName}_${viewModel.anioSeleccionado.value}.pdf")
                                    }
                                }
                            }
                        )
                    }
                    "Grafica" -> {
                        val resumen = viewModel.getDbHelper().obtenerResumenGastosPorCategoria(viewModel.mesSeleccionado.value, viewModel.anioSeleccionado.value)
                        val titulo = "Distribución de Gastos - ${viewModel.mesSeleccionado.value}/${viewModel.anioSeleccionado.value}"
                        GraficaGastosDialog(
                            resumen = resumen,
                            titulo = titulo,
                            onDismiss = { viewModel.setActiveDialog(null) }
                        )
                    }
                    "Transferencia" -> {
                        TransferenciaDialog(
                            dbHelper = viewModel.getDbHelper(),
                            onDismiss = { viewModel.setActiveDialog(null) },
                            onSave = { viewModel.actualizarTodo() }
                        )
                    }
                    "Categorias" -> {
                        CategoriasDialog(
                            dbHelper = viewModel.getDbHelper(),
                            onDismiss = { viewModel.setActiveDialog(null) },
                            onChanged = { viewModel.actualizarTodo() }
                        )
                    }
                    "Saldos" -> {
                        SaldosInicialesDialog(
                            dbHelper = viewModel.getDbHelper(),
                            onDismiss = { viewModel.setActiveDialog(null) },
                            onSave = { viewModel.actualizarTodo() }
                        )
                    }
                }
            }
        }
    }

    private fun setupLaunchers() {
        createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let { exportarBackupAUri(it) }
        }

        openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importarBackupDesdeUri(it) }
        }

        createPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            uri?.let { exportarPdfAUri(it) }
        }
    }

    private fun mostrarOpcionesExportacion() {
        val meses = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        val currentMonthName = meses[viewModel.mesSeleccionado.value - 1]
        
        val opciones = arrayOf(
            "Exportar Mes Filtrado ($currentMonthName ${viewModel.anioSeleccionado.value})",
            "Exportar Histórico Completo"
        )
        AlertDialog.Builder(this)
            .setTitle("Exportar a Excel (.xls)")
            .setItems(opciones) { _, which ->
                if (which == 0) {
                    val movsMes = viewModel.getDbHelper().obtenerMovimientosPorMes(viewModel.mesSeleccionado.value, viewModel.anioSeleccionado.value)
                    ExcelExporter.exportarYCompartir(this, movsMes, "Estado_Cuenta_${currentMonthName}_${viewModel.anioSeleccionado.value}")
                } else {
                    val movsTodos = viewModel.getDbHelper().obtenerTodosMovimientos()
                    ExcelExporter.exportarYCompartir(this, movsTodos, "Estado_Cuenta_Historico_Completo")
                }
            }
            .show()
    }

    private fun exportarPdfAUri(uri: Uri) {
        try {
            val meses = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
            val monthName = meses[viewModel.mesSeleccionado.value - 1]
            val titulo = "$monthName ${viewModel.anioSeleccionado.value}"
            
            val movs = viewModel.getDbHelper().obtenerMovimientosPorMes(viewModel.mesSeleccionado.value, viewModel.anioSeleccionado.value)
            val resumen = viewModel.resumenMes.value
            val totales = mapOf(
                "INGRESOS" to resumen.totalIngresos,
                "EGRESOS" to resumen.totalEgresos,
                "BALANCE" to resumen.balance
            )

            contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                com.example.ingresosegresoscx.utils.PdfExporter.generarReportePdf(this, pfd, movs, titulo, totales)
                Toast.makeText(this, "Reporte PDF generado con éxito", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportarBackupAUri(uri: Uri) {
        try {
            if (ZipBackupHelper.exportarBackupZip(this, uri, viewModel.getDbHelper())) {
                Toast.makeText(this, "Respaldo exportado con éxito (.zip)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al exportar el respaldo", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importarBackupDesdeUri(uri: Uri) {
        try {
            if (ZipBackupHelper.importarBackupZip(this, uri, viewModel.getDbHelper())) {
                viewModel.actualizarTodo()
                Toast.makeText(this, "Respaldo importado con éxito", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al importar el archivo", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al importar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
