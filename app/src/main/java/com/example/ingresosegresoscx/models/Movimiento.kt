package com.example.ingresosegresoscx.models

data class Movimiento(
    val id: Int,
    var fecha: String, // YYYY-MM-DD
    var concepto: String,
    var debe: Double,
    var haber: Double,
    var cuenta: String, // "EFECTIVO" o "BANCO"
    var categoriaId: Int,
    var tipoMovimiento: String, // "INGRESO", "EGRESO", "TRANSFERENCIA"
    var saldo: Double = 0.0,
    var nombreCategoria: String = "Sin Categoría",
    var imagenUri: String? = null
)
