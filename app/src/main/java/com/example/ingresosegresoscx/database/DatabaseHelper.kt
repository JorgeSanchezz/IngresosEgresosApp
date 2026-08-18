package com.example.ingresosegresoscx.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.ingresosegresoscx.models.Categoria
import com.example.ingresosegresoscx.models.Movimiento
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ingresos_egresos.db"
        private const val DATABASE_VERSION = 3

        // Tabla Movimientos
        const val TABLE_MOVIMIENTOS = "movimientos"
        const val COLUMN_ID = "id"
        const val COLUMN_FECHA = "fecha" // Formato YYYY-MM-DD
        const val COLUMN_CONCEPTO = "concepto"
        const val COLUMN_DEBE = "debe"
        const val COLUMN_HABER = "haber"
        const val COLUMN_CUENTA = "cuenta" // "EFECTIVO" o "BANCO"
        const val COLUMN_CATEGORIA_ID = "categoria_id"
        const val COLUMN_TIPO_MOVIMIENTO = "tipo_movimiento" // "INGRESO", "EGRESO", "TRANSFERENCIA"
        const val COLUMN_IMAGEN_URI = "imagen_uri"

        // Tabla Categorias
        const val TABLE_CATEGORIAS = "categorias"
        const val COLUMN_CAT_ID = "id"
        const val COLUMN_CAT_NOMBRE = "nombre"
        const val COLUMN_CAT_TIPO = "tipo" // "INGRESO", "EGRESO", "AMBOS"

        // Tabla Saldos Iniciales
        const val TABLE_SALDOS_INICIALES = "saldos_iniciales"
        const val COLUMN_SALDO_CUENTA = "cuenta"
        const val COLUMN_SALDO_MONTO = "monto"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear tabla Categorias
        val createCategoriasTable = ("CREATE TABLE $TABLE_CATEGORIAS (" +
                "$COLUMN_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_CAT_NOMBRE TEXT, " +
                "$COLUMN_CAT_TIPO TEXT)")
        db.execSQL(createCategoriasTable)

        // Crear tabla Saldos Iniciales
        val createSaldosTable = ("CREATE TABLE $TABLE_SALDOS_INICIALES (" +
                "$COLUMN_SALDO_CUENTA TEXT PRIMARY KEY, " +
                "$COLUMN_SALDO_MONTO REAL)")
        db.execSQL(createSaldosTable)

        // Crear tabla Movimientos
        val createMovimientosTable = ("CREATE TABLE $TABLE_MOVIMIENTOS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_FECHA TEXT, " +
                "$COLUMN_CONCEPTO TEXT, " +
                "$COLUMN_DEBE REAL, " +
                "$COLUMN_HABER REAL, " +
                "$COLUMN_CUENTA TEXT DEFAULT 'EFECTIVO', " +
                "$COLUMN_CATEGORIA_ID INTEGER DEFAULT 1, " +
                "$COLUMN_TIPO_MOVIMIENTO TEXT DEFAULT 'INGRESO', " +
                "$COLUMN_IMAGEN_URI TEXT, " +
                "FOREIGN KEY($COLUMN_CATEGORIA_ID) REFERENCES $TABLE_CATEGORIAS($COLUMN_CAT_ID))")
        db.execSQL(createMovimientosTable)

        // Insertar categoría predeterminada
        val cvCat = ContentValues().apply {
            put(COLUMN_CAT_ID, 1)
            put(COLUMN_CAT_NOMBRE, "Sin Categoría")
            put(COLUMN_CAT_TIPO, "AMBOS")
        }
        db.insert(TABLE_CATEGORIAS, null, cvCat)

        // Insertar categorías iniciales comunes
        insertarCategoriaDefault(db, "Sueldo / Salario", "INGRESO")
        insertarCategoriaDefault(db, "Alimentos / Viveres", "EGRESO")
        insertarCategoriaDefault(db, "Servicios (Agua/Luz/Gas)", "EGRESO")
        insertarCategoriaDefault(db, "Transporte", "EGRESO")

        // Saldos Iniciales en 0.0 por defecto
        val cvEf = ContentValues().apply {
            put(COLUMN_SALDO_CUENTA, "EFECTIVO")
            put(COLUMN_SALDO_MONTO, 0.0)
        }
        db.insert(TABLE_SALDOS_INICIALES, null, cvEf)

        val cvBa = ContentValues().apply {
            put(COLUMN_SALDO_CUENTA, "BANCO")
            put(COLUMN_SALDO_MONTO, 0.0)
        }
        db.insert(TABLE_SALDOS_INICIALES, null, cvBa)
    }

    private fun insertarCategoriaDefault(db: SQLiteDatabase, nombre: String, tipo: String) {
        val cv = ContentValues().apply {
            put(COLUMN_CAT_NOMBRE, nombre)
            put(COLUMN_CAT_TIPO, tipo)
        }
        db.insert(TABLE_CATEGORIAS, null, cv)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIAS (" +
                    "$COLUMN_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_CAT_NOMBRE TEXT, " +
                    "$COLUMN_CAT_TIPO TEXT)")

            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SALDOS_INICIALES (" +
                    "$COLUMN_SALDO_CUENTA TEXT PRIMARY KEY, " +
                    "$COLUMN_SALDO_MONTO REAL)")

            // Categoría por defecto
            db.rawQuery("SELECT * FROM $TABLE_CATEGORIAS WHERE $COLUMN_CAT_ID = 1", null).use { cursor ->
                if (!cursor.moveToFirst()) {
                    val cvCat = ContentValues().apply {
                        put(COLUMN_CAT_ID, 1)
                        put(COLUMN_CAT_NOMBRE, "Sin Categoría")
                        put(COLUMN_CAT_TIPO, "AMBOS")
                    }
                    db.insert(TABLE_CATEGORIAS, null, cvCat)
                }
            }

            // Migrar tabla movimientos agregando columnas
            try {
                db.execSQL("ALTER TABLE $TABLE_MOVIMIENTOS ADD COLUMN $COLUMN_CUENTA TEXT DEFAULT 'EFECTIVO'")
                db.execSQL("ALTER TABLE $TABLE_MOVIMIENTOS ADD COLUMN $COLUMN_CATEGORIA_ID INTEGER DEFAULT 1")
                db.execSQL("ALTER TABLE $TABLE_MOVIMIENTOS ADD COLUMN $COLUMN_TIPO_MOVIMIENTO TEXT DEFAULT 'INGRESO'")
            } catch (ignored: Exception) {
            }

            // Inicializar saldos
            val cvEf = ContentValues().apply {
                put(COLUMN_SALDO_CUENTA, "EFECTIVO")
                put(COLUMN_SALDO_MONTO, 0.0)
            }
            db.insertWithOnConflict(TABLE_SALDOS_INICIALES, null, cvEf, SQLiteDatabase.CONFLICT_IGNORE)

            val cvBa = ContentValues().apply {
                put(COLUMN_SALDO_CUENTA, "BANCO")
                put(COLUMN_SALDO_MONTO, 0.0)
            }
            db.insertWithOnConflict(TABLE_SALDOS_INICIALES, null, cvBa, SQLiteDatabase.CONFLICT_IGNORE)
        }

        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_MOVIMIENTOS ADD COLUMN $COLUMN_IMAGEN_URI TEXT")
            } catch (ignored: Exception) {
            }
        }
    }

    // ==================== SALDOS INICIALES ====================
    fun vaciarTablas() {
        val db = this.writableDatabase
        db.delete(TABLE_MOVIMIENTOS, null, null)
        db.delete(TABLE_CATEGORIAS, null, null)
        db.delete(TABLE_SALDOS_INICIALES, null, null)
    }

    fun insertarCategoriaConId(id: Int, nombre: String, tipo: String): Long {
        val db = this.writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_CAT_ID, id)
            put(COLUMN_CAT_NOMBRE, nombre)
            put(COLUMN_CAT_TIPO, tipo)
        }
        return db.insert(TABLE_CATEGORIAS, null, cv)
    }

    fun insertarMovimientoConId(id: Int, fecha: String, concepto: String, debe: Double, haber: Double,
                                cuenta: String, categoriaId: Int, tipoMovimiento: String, imagenUri: String? = null): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, id)
            put(COLUMN_FECHA, fecha)
            put(COLUMN_CONCEPTO, concepto)
            put(COLUMN_DEBE, debe)
            put(COLUMN_HABER, haber)
            put(COLUMN_CUENTA, cuenta)
            put(COLUMN_CATEGORIA_ID, categoriaId)
            put(COLUMN_TIPO_MOVIMIENTO, tipoMovimiento)
            put(COLUMN_IMAGEN_URI, imagenUri)
        }
        val result = db.insert(TABLE_MOVIMIENTOS, null, values)
        return result != -1L
    }

    fun guardarSaldoInicial(cuenta: String, monto: Double): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_SALDO_CUENTA, cuenta)
            put(COLUMN_SALDO_MONTO, monto)
        }
        val result = db.insertWithOnConflict(TABLE_SALDOS_INICIALES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        return result != -1L
    }

    fun obtenerSaldoInicial(cuenta: String): Double {
        val db = this.readableDatabase
        var monto = 0.0
        db.rawQuery("SELECT $COLUMN_SALDO_MONTO FROM $TABLE_SALDOS_INICIALES WHERE $COLUMN_SALDO_CUENTA = ?", arrayOf(cuenta)).use { cursor ->
            if (cursor.moveToFirst()) {
                monto = cursor.getDouble(0)
            }
        }
        return monto
    }

    fun obtenerSaldosPorCuenta(): Map<String, Double> {
        val saldos = mutableMapOf<String, Double>()
        var saldoEfectivo = obtenerSaldoInicial("EFECTIVO")
        var saldoBanco = obtenerSaldoInicial("BANCO")

        val db = this.readableDatabase
        db.rawQuery("SELECT $COLUMN_CUENTA, SUM($COLUMN_DEBE), SUM($COLUMN_HABER) FROM $TABLE_MOVIMIENTOS GROUP BY $COLUMN_CUENTA", null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val cuenta = cursor.getString(0)
                    val totalDebe = cursor.getDouble(1)
                    val totalHaber = cursor.getDouble(2)
                    val neto = totalDebe - totalHaber

                    if ("EFECTIVO".equals(cuenta, ignoreCase = true)) {
                        saldoEfectivo += neto
                    } else if ("BANCO".equals(cuenta, ignoreCase = true)) {
                        saldoBanco += neto
                    }
                } while (cursor.moveToNext())
            }
        }

        saldos["EFECTIVO"] = saldoEfectivo
        saldos["BANCO"] = saldoBanco
        saldos["TOTAL"] = saldoEfectivo + saldoBanco
        return saldos
    }

    // ==================== CATEGORÍAS ====================
    fun insertarCategoria(nombre: String, tipo: String): Long {
        val db = this.writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_CAT_NOMBRE, nombre)
            put(COLUMN_CAT_TIPO, tipo)
        }
        return db.insert(TABLE_CATEGORIAS, null, cv)
    }

    fun actualizarCategoria(id: Int, nombre: String, tipo: String): Boolean {
        if (id == 1) return false // Protección para "Sin Categoría"
        val db = this.writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_CAT_NOMBRE, nombre)
            put(COLUMN_CAT_TIPO, tipo)
        }
        return db.update(TABLE_CATEGORIAS, cv, "$COLUMN_CAT_ID = ?", arrayOf(id.toString())) > 0
    }

    fun eliminarCategoria(id: Int): Boolean {
        if (id == 1) return false // No se puede eliminar "Sin Categoría"
        val db = this.writableDatabase
        db.beginTransaction()
        return try {
            // Reasignar movimientos a "Sin Categoría" (ID 1)
            val cv = ContentValues().apply {
                put(COLUMN_CATEGORIA_ID, 1)
            }
            db.update(TABLE_MOVIMIENTOS, cv, "$COLUMN_CATEGORIA_ID = ?", arrayOf(id.toString()))

            // Eliminar categoría
            db.delete(TABLE_CATEGORIAS, "$COLUMN_CAT_ID = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    fun obtenerCategorias(): List<Categoria> {
        val lista = mutableListOf<Categoria>()
        val db = this.readableDatabase
        db.rawQuery("SELECT * FROM $TABLE_CATEGORIAS ORDER BY $COLUMN_CAT_ID ASC", null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAT_ID))
                    val nombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_NOMBRE))
                    val tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_TIPO))
                    lista.add(Categoria(id, nombre, tipo))
                } while (cursor.moveToNext())
            }
        }
        return lista
    }

    fun obtenerNombreCategoria(categoriaId: Int): String {
        val db = this.readableDatabase
        var nombre = "Sin Categoría"
        db.rawQuery("SELECT $COLUMN_CAT_NOMBRE FROM $TABLE_CATEGORIAS WHERE $COLUMN_CAT_ID = ?", arrayOf(categoriaId.toString())).use { cursor ->
            if (cursor.moveToFirst()) {
                nombre = cursor.getString(0)
            }
        }
        return nombre
    }

    // ==================== MOVIMIENTOS ====================
    fun insertarMovimiento(fecha: String, concepto: String, debe: Double, haber: Double,
                           cuenta: String, categoriaId: Int, tipoMovimiento: String, imagenUri: String? = null): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FECHA, fecha)
            put(COLUMN_CONCEPTO, concepto)
            put(COLUMN_DEBE, debe)
            put(COLUMN_HABER, haber)
            put(COLUMN_CUENTA, cuenta)
            put(COLUMN_CATEGORIA_ID, categoriaId)
            put(COLUMN_TIPO_MOVIMIENTO, tipoMovimiento)
            put(COLUMN_IMAGEN_URI, imagenUri)
        }
        val result = db.insert(TABLE_MOVIMIENTOS, null, values)
        return result != -1L
    }

    fun registrarTransferencia(fecha: String, monto: Double, origen: String, destino: String, concepto: String): Boolean {
        val db = this.writableDatabase
        db.beginTransaction()
        return try {
            val descOrigen = "TRANSFERENCIA A $destino: $concepto"
            val descDestino = "TRANSFERENCIA DESDE $origen: $concepto"

            // Salida de la cuenta Origen (Haber)
            val cvOrigen = ContentValues().apply {
                put(COLUMN_FECHA, fecha)
                put(COLUMN_CONCEPTO, descOrigen)
                put(COLUMN_DEBE, 0.0)
                put(COLUMN_HABER, monto)
                put(COLUMN_CUENTA, origen)
                put(COLUMN_CATEGORIA_ID, 1)
                put(COLUMN_TIPO_MOVIMIENTO, "TRANSFERENCIA")
            }
            db.insert(TABLE_MOVIMIENTOS, null, cvOrigen)

            // Entrada a la cuenta Destino (Debe)
            val cvDestino = ContentValues().apply {
                put(COLUMN_FECHA, fecha)
                put(COLUMN_CONCEPTO, descDestino)
                put(COLUMN_DEBE, monto)
                put(COLUMN_HABER, 0.0)
                put(COLUMN_CUENTA, destino)
                put(COLUMN_CATEGORIA_ID, 1)
                put(COLUMN_TIPO_MOVIMIENTO, "TRANSFERENCIA")
            }
            db.insert(TABLE_MOVIMIENTOS, null, cvDestino)

            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            false
        } finally {
            db.endTransaction()
        }
    }

    fun actualizarMovimiento(id: Int, fecha: String, concepto: String, debe: Double, haber: Double,
                             cuenta: String, categoriaId: Int, tipoMovimiento: String, imagenUri: String? = null): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FECHA, fecha)
            put(COLUMN_CONCEPTO, concepto)
            put(COLUMN_DEBE, debe)
            put(COLUMN_HABER, haber)
            put(COLUMN_CUENTA, cuenta)
            put(COLUMN_CATEGORIA_ID, categoriaId)
            put(COLUMN_TIPO_MOVIMIENTO, tipoMovimiento)
            put(COLUMN_IMAGEN_URI, imagenUri)
        }
        val rows = db.update(TABLE_MOVIMIENTOS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    fun eliminarMovimiento(id: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_MOVIMIENTOS, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun obtenerMovimientosPorMes(mes: Int, anio: Int): List<Movimiento> {
        val lista = mutableListOf<Movimiento>()
        val db = this.readableDatabase

        val strMes = String.format(Locale.US, "%02d", mes)
        val filtroFecha = "$anio-$strMes-%"

        val query = "SELECT m.*, c.$COLUMN_CAT_NOMBRE FROM $TABLE_MOVIMIENTOS m " +
                "LEFT JOIN $TABLE_CATEGORIAS c ON m.$COLUMN_CATEGORIA_ID = c.$COLUMN_CAT_ID " +
                "WHERE m.$COLUMN_FECHA LIKE ? ORDER BY m.$COLUMN_FECHA ASC, m.$COLUMN_ID ASC"

        db.rawQuery(query, arrayOf(filtroFecha)).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                    val fecha = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA))
                    val concepto = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONCEPTO))
                    val debe = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DEBE))
                    val haber = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HABER))
                    val cuenta = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUENTA))
                    val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORIA_ID))
                    val tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIPO_MOVIMIENTO))
                    val imagenUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGEN_URI))
                    val catNombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_NOMBRE))

                    val mov = Movimiento(id, fecha, concepto, debe, haber, cuenta, catId, tipo)
                    mov.nombreCategoria = catNombre ?: "Sin Categoría"
                    mov.imagenUri = imagenUri
                    lista.add(mov)
                } while (cursor.moveToNext())
            }
        }
        return lista
    }

    fun obtenerTodosMovimientos(): List<Movimiento> {
        val lista = mutableListOf<Movimiento>()
        val db = this.readableDatabase

        val query = "SELECT m.*, c.$COLUMN_CAT_NOMBRE FROM $TABLE_MOVIMIENTOS m " +
                "LEFT JOIN $TABLE_CATEGORIAS c ON m.$COLUMN_CATEGORIA_ID = c.$COLUMN_CAT_ID " +
                "ORDER BY m.$COLUMN_FECHA ASC, m.$COLUMN_ID ASC"

        db.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                    val fecha = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA))
                    val concepto = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONCEPTO))
                    val debe = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DEBE))
                    val haber = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HABER))
                    val cuenta = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUENTA))
                    val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORIA_ID))
                    val tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIPO_MOVIMIENTO))
                    val imagenUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGEN_URI))
                    val catNombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_NOMBRE))

                    val mov = Movimiento(id, fecha, concepto, debe, haber, cuenta, catId, tipo)
                    mov.nombreCategoria = catNombre ?: "Sin Categoría"
                    mov.imagenUri = imagenUri
                    lista.add(mov)
                } while (cursor.moveToNext())
            }
        }
        return lista
    }

    fun obtenerResumenGastosPorCategoria(mes: Int, anio: Int): Map<String, Double> {
        val resumen = mutableMapOf<String, Double>()
        val db = this.readableDatabase

        val strMes = String.format(Locale.US, "%02d", mes)
        val filtroFecha = "$anio-$strMes-%"

        val query = "SELECT c.$COLUMN_CAT_NOMBRE, SUM(m.$COLUMN_HABER) FROM $TABLE_MOVIMIENTOS m " +
                "JOIN $TABLE_CATEGORIAS c ON m.$COLUMN_CATEGORIA_ID = c.$COLUMN_CAT_ID " +
                "WHERE m.$COLUMN_FECHA LIKE ? AND m.$COLUMN_HABER > 0 AND m.$COLUMN_TIPO_MOVIMIENTO != 'TRANSFERENCIA' " +
                "GROUP BY c.$COLUMN_CAT_ID"

        db.rawQuery(query, arrayOf(filtroFecha)).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val catNombre = cursor.getString(0)
                    val total = cursor.getDouble(1)
                    resumen[catNombre] = total
                } while (cursor.moveToNext())
            }
        }
        return resumen
    }

    // ==================== FILTROS DINÁMICOS ====================
    fun obtenerAniosDisponibles(): List<Int> {
        val lista = mutableListOf<Int>()
        val db = this.readableDatabase
        val query = "SELECT DISTINCT SUBSTR($COLUMN_FECHA, 1, 4) FROM $TABLE_MOVIMIENTOS ORDER BY 1 DESC"
        db.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    lista.add(cursor.getString(0).toInt())
                } while (cursor.moveToNext())
            }
        }
        return lista
    }

    fun obtenerMesesDisponiblesPorAnio(anio: Int): List<Int> {
        val lista = mutableListOf<Int>()
        val db = this.readableDatabase
        val query = "SELECT DISTINCT SUBSTR($COLUMN_FECHA, 6, 2) FROM $TABLE_MOVIMIENTOS WHERE $COLUMN_FECHA LIKE ? ORDER BY 1 ASC"
        db.rawQuery(query, arrayOf("$anio-%")).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    lista.add(cursor.getString(0).toInt())
                } while (cursor.moveToNext())
            }
        }
        return lista
    }
}
