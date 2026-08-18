package com.example.ingresosegresoscx.models

data class Categoria(
    val id: Int,
    var nombre: String,
    var tipo: String // INGRESO, EGRESO, AMBOS
) {
    override fun toString(): String {
        return nombre
    }
}
