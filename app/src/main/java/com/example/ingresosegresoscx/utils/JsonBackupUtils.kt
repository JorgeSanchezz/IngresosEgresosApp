package com.example.ingresosegresoscx.utils

import android.content.Context
import com.example.ingresosegresoscx.database.DatabaseHelper
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object JsonBackupUtils {

    @JvmStatic
    fun exportarDatos(context: Context, dbHelper: DatabaseHelper): String? {
        return try {
            val root = JSONObject()

            // Exportar Categorías
            val arrayCategorias = JSONArray()
            val categorias = dbHelper.obtenerCategorias()
            for (c in categorias) {
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("nombre", c.nombre)
                obj.put("tipo", c.tipo)
                arrayCategorias.put(obj)
            }
            root.put("categorias", arrayCategorias)

            // Exportar Movimientos
            val arrayMovimientos = JSONArray()
            val movimientos = dbHelper.obtenerTodosMovimientos()
            for (m in movimientos) {
                val obj = JSONObject()
                obj.put("id", m.id)
                obj.put("fecha", m.fecha)
                obj.put("concepto", m.concepto)
                obj.put("debe", m.debe)
                obj.put("haber", m.haber)
                obj.put("cuenta", m.cuenta)
                obj.put("categoria_id", m.categoriaId)
                obj.put("tipo_movimiento", m.tipoMovimiento)
                arrayMovimientos.put(obj)
            }
            root.put("movimientos", arrayMovimientos)

            // Exportar Saldos Iniciales
            val arraySaldos = JSONArray()
            val cuentas = arrayOf("EFECTIVO", "BANCO")
            for (cuenta in cuentas) {
                val obj = JSONObject()
                obj.put("cuenta", cuenta)
                obj.put("monto", dbHelper.obtenerSaldoInicial(cuenta))
                arraySaldos.put(obj)
            }
            root.put("saldos_iniciales", arraySaldos)

            root.toString(4) // Indentado para legibilidad
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun importarDatos(context: Context, jsonStr: String, dbHelper: DatabaseHelper): Boolean {
        return try {
            val root = JSONObject(jsonStr)

            // 1. Vaciar tablas actuales
            dbHelper.vaciarTablas()

            // 2. Importar Categorías
            if (root.has("categorias")) {
                val arrayCat = root.getJSONArray("categorias")
                for (i in 0 until arrayCat.length()) {
                    val obj = arrayCat.getJSONObject(i)
                    dbHelper.insertarCategoriaConId(
                        obj.getInt("id"),
                        obj.getString("nombre"),
                        obj.getString("tipo")
                    )
                }
            }

            // 3. Importar Saldos Iniciales
            if (root.has("saldos_iniciales")) {
                val arraySaldos = root.getJSONArray("saldos_iniciales")
                for (i in 0 until arraySaldos.length()) {
                    val obj = arraySaldos.getJSONObject(i)
                    dbHelper.guardarSaldoInicial(
                        obj.getString("cuenta"),
                        obj.getDouble("monto")
                    )
                }
            }

            // 4. Importar Movimientos
            if (root.has("movimientos")) {
                val arrayMov = root.getJSONArray("movimientos")
                for (i in 0 until arrayMov.length()) {
                    val obj = arrayMov.getJSONObject(i)
                    dbHelper.insertarMovimientoConId(
                        obj.getInt("id"),
                        obj.getString("fecha"),
                        obj.getString("concepto"),
                        obj.getDouble("debe"),
                        obj.getDouble("haber"),
                        obj.getString("cuenta"),
                        obj.getInt("categoria_id"),
                        obj.getString("tipo_movimiento")
                    )
                }
            }

            true
        } catch (e: JSONException) {
            e.printStackTrace()
            false
        }
    }
}
