package com.example.ingresosegresoscx.ui

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.ingresosegresoscx.database.DatabaseHelper
import com.example.ingresosegresoscx.models.Movimiento
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dbHelper = DatabaseHelper(application)

    private val _mesSeleccionado = mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val mesSeleccionado: State<Int> = _mesSeleccionado

    private val _anioSeleccionado = mutableStateOf(Calendar.getInstance().get(Calendar.YEAR))
    val anioSeleccionado: State<Int> = _anioSeleccionado

    private val _movimientos = mutableStateOf<List<Movimiento>>(emptyList())
    val movimientos: State<List<Movimiento>> = _movimientos

    private val _saldos = mutableStateOf<Map<String, Double>>(emptyMap())
    val saldos: State<Map<String, Double>> = _saldos

    private val _resumenMes = mutableStateOf(ResumenMes())
    val resumenMes: State<ResumenMes> = _resumenMes

    private val _aniosDisponibles = mutableStateOf<List<Int>>(emptyList())
    val aniosDisponibles: State<List<Int>> = _aniosDisponibles

    private val _mesesDisponibles = mutableStateOf<List<Int>>(emptyList())
    val mesesDisponibles: State<List<Int>> = _mesesDisponibles

    private val _activeDialog = mutableStateOf<String?>(null)
    val activeDialog: State<String?> = _activeDialog

    private val _movimientoAEditar = mutableStateOf<Movimiento?>(null)
    val movimientoAEditar: State<Movimiento?> = _movimientoAEditar

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    data class ResumenMes(
        val totalIngresos: Double = 0.0,
        val totalEgresos: Double = 0.0,
        val balance: Double = 0.0
    )

    init {
        actualizarTodo()
    }

    fun setActiveDialog(name: String?) {
        _activeDialog.value = name
    }

    fun setMovimientoAEditar(mov: Movimiento?) {
        _movimientoAEditar.value = mov
    }

    fun setMes(mes: Int) {
        _mesSeleccionado.value = mes
        actualizarTablaYTotales()
    }

    fun setAnio(anio: Int) {
        _anioSeleccionado.value = anio
        actualizarMesesDisponibles(anio)
        actualizarTablaYTotales()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        actualizarTablaYTotales()
    }

    fun actualizarTodo() {
        actualizarAniosDisponibles()
        actualizarTarjetasSaldo()
        actualizarTablaYTotales()
    }

    private fun actualizarAniosDisponibles() {
        val anios = dbHelper.obtenerAniosDisponibles()
        _aniosDisponibles.value = anios
        if (anios.isNotEmpty()) {
            if (!_aniosDisponibles.value.contains(_anioSeleccionado.value)) {
                _anioSeleccionado.value = anios.first()
            }
            actualizarMesesDisponibles(_anioSeleccionado.value)
        } else {
            _mesesDisponibles.value = emptyList()
        }
    }

    private fun actualizarMesesDisponibles(anio: Int) {
        val meses = dbHelper.obtenerMesesDisponiblesPorAnio(anio)
        _mesesDisponibles.value = meses
        if (meses.isNotEmpty() && !meses.contains(_mesSeleccionado.value)) {
            _mesSeleccionado.value = meses.last() // Ir al último mes con datos por defecto
        }
    }

    private fun actualizarTarjetasSaldo() {
        _saldos.value = dbHelper.obtenerSaldosPorCuenta()
    }

    private fun actualizarTablaYTotales() {
        if (_searchQuery.value.isNotBlank()) {
            val lista = dbHelper.buscarMovimientosPorConcepto(_searchQuery.value)
            _movimientos.value = lista
            _resumenMes.value = ResumenMes() // Opcional: podrías calcular el resumen de la búsqueda
            return
        }

        // Solo actualizar si hay datos
        if (_aniosDisponibles.value.isEmpty()) {
            _movimientos.value = emptyList()
            _resumenMes.value = ResumenMes()
            return
        }

        val lista = dbHelper.obtenerMovimientosPorMes(_mesSeleccionado.value, _anioSeleccionado.value)
        _movimientos.value = lista

        var ingresos = 0.0
        var egresos = 0.0
        for (m in lista) {
            ingresos += m.debe
            egresos += m.haber
        }
        _resumenMes.value = ResumenMes(ingresos, egresos, ingresos - egresos)
    }

    fun eliminarMovimiento(id: Int) {
        dbHelper.eliminarMovimiento(id)
        actualizarTodo()
    }

    fun getDbHelper() = dbHelper
}
