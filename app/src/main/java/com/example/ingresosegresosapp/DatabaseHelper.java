package com.example.ingresosegresosapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ingresos_egresos.db";
    private static final int DATABASE_VERSION = 2;

    // Tabla Movimientos
    public static final String TABLE_MOVIMIENTOS = "movimientos";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_FECHA = "fecha"; // Formato YYYY-MM-DD
    public static final String COLUMN_CONCEPTO = "concepto";
    public static final String COLUMN_DEBE = "debe";
    public static final String COLUMN_HABER = "haber";
    public static final String COLUMN_CUENTA = "cuenta"; // "EFECTIVO" o "BANCO"
    public static final String COLUMN_CATEGORIA_ID = "categoria_id";
    public static final String COLUMN_TIPO_MOVIMIENTO = "tipo_movimiento"; // "INGRESO", "EGRESO", "TRANSFERENCIA"

    // Tabla Categorias
    public static final String TABLE_CATEGORIAS = "categorias";
    public static final String COLUMN_CAT_ID = "id";
    public static final String COLUMN_CAT_NOMBRE = "nombre";
    public static final String COLUMN_CAT_TIPO = "tipo"; // "INGRESO", "EGRESO", "AMBOS"

    // Tabla Saldos Iniciales
    public static final String TABLE_SALDOS_INICIALES = "saldos_iniciales";
    public static final String COLUMN_SALDO_CUENTA = "cuenta";
    public static final String COLUMN_SALDO_MONTO = "monto";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear tabla Categorias
        String CREATE_CATEGORIAS_TABLE = "CREATE TABLE " + TABLE_CATEGORIAS + " (" +
                COLUMN_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CAT_NOMBRE + " TEXT, " +
                COLUMN_CAT_TIPO + " TEXT)";
        db.execSQL(CREATE_CATEGORIAS_TABLE);

        // Crear tabla Saldos Iniciales
        String CREATE_SALDOS_TABLE = "CREATE TABLE " + TABLE_SALDOS_INICIALES + " (" +
                COLUMN_SALDO_CUENTA + " TEXT PRIMARY KEY, " +
                COLUMN_SALDO_MONTO + " REAL)";
        db.execSQL(CREATE_SALDOS_TABLE);

        // Crear tabla Movimientos
        String CREATE_MOVIMIENTOS_TABLE = "CREATE TABLE " + TABLE_MOVIMIENTOS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_FECHA + " TEXT, " +
                COLUMN_CONCEPTO + " TEXT, " +
                COLUMN_DEBE + " REAL, " +
                COLUMN_HABER + " REAL, " +
                COLUMN_CUENTA + " TEXT DEFAULT 'EFECTIVO', " +
                COLUMN_CATEGORIA_ID + " INTEGER DEFAULT 1, " +
                COLUMN_TIPO_MOVIMIENTO + " TEXT DEFAULT 'INGRESO', " +
                "FOREIGN KEY(" + COLUMN_CATEGORIA_ID + ") REFERENCES " + TABLE_CATEGORIAS + "(" + COLUMN_CAT_ID + "))";
        db.execSQL(CREATE_MOVIMIENTOS_TABLE);

        // Insertar categoría predeterminada
        ContentValues cvCat = new ContentValues();
        cvCat.put(COLUMN_CAT_ID, 1);
        cvCat.put(COLUMN_CAT_NOMBRE, "Sin Categoría");
        cvCat.put(COLUMN_CAT_TIPO, "AMBOS");
        db.insert(TABLE_CATEGORIAS, null, cvCat);

        // Insertar categorías iniciales comunes
        insertarCategoriaDefault(db, "Sueldo / Salario", "INGRESO");
        insertarCategoriaDefault(db, "Alimentos / Viveres", "EGRESO");
        insertarCategoriaDefault(db, "Servicios (Agua/Luz/Gas)", "EGRESO");
        insertarCategoriaDefault(db, "Transporte", "EGRESO");

        // Saldos Iniciales en 0.0 por defecto
        ContentValues cvEf = new ContentValues();
        cvEf.put(COLUMN_SALDO_CUENTA, "EFECTIVO");
        cvEf.put(COLUMN_SALDO_MONTO, 0.0);
        db.insert(TABLE_SALDOS_INICIALES, null, cvEf);

        ContentValues cvBa = new ContentValues();
        cvBa.put(COLUMN_SALDO_CUENTA, "BANCO");
        cvBa.put(COLUMN_SALDO_MONTO, 0.0);
        db.insert(TABLE_SALDOS_INICIALES, null, cvBa);
    }

    private void insertarCategoriaDefault(SQLiteDatabase db, String nombre, String tipo) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CAT_NOMBRE, nombre);
        cv.put(COLUMN_CAT_TIPO, tipo);
        db.insert(TABLE_CATEGORIAS, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIAS + " (" +
                    COLUMN_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CAT_NOMBRE + " TEXT, " +
                    COLUMN_CAT_TIPO + " TEXT)");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SALDOS_INICIALES + " (" +
                    COLUMN_SALDO_CUENTA + " TEXT PRIMARY KEY, " +
                    COLUMN_SALDO_MONTO + " REAL)");

            // Categoría por defecto
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIAS + " WHERE " + COLUMN_CAT_ID + " = 1", null);
            if (!cursor.moveToFirst()) {
                ContentValues cvCat = new ContentValues();
                cvCat.put(COLUMN_CAT_ID, 1);
                cvCat.put(COLUMN_CAT_NOMBRE, "Sin Categoría");
                cvCat.put(COLUMN_CAT_TIPO, "AMBOS");
                db.insert(TABLE_CATEGORIAS, null, cvCat);
            }
            cursor.close();

            // Migrar tabla movimientos agregando columnas
            try {
                db.execSQL("ALTER TABLE " + TABLE_MOVIMIENTOS + " ADD COLUMN " + COLUMN_CUENTA + " TEXT DEFAULT 'EFECTIVO'");
                db.execSQL("ALTER TABLE " + TABLE_MOVIMIENTOS + " ADD COLUMN " + COLUMN_CATEGORIA_ID + " INTEGER DEFAULT 1");
                db.execSQL("ALTER TABLE " + TABLE_MOVIMIENTOS + " ADD COLUMN " + COLUMN_TIPO_MOVIMIENTO + " TEXT DEFAULT 'INGRESO'");
            } catch (Exception ignored) {}

            // Inicializar saldos
            ContentValues cvEf = new ContentValues();
            cvEf.put(COLUMN_SALDO_CUENTA, "EFECTIVO");
            cvEf.put(COLUMN_SALDO_MONTO, 0.0);
            db.insertWithOnConflict(TABLE_SALDOS_INICIALES, null, cvEf, SQLiteDatabase.CONFLICT_IGNORE);

            ContentValues cvBa = new ContentValues();
            cvBa.put(COLUMN_SALDO_CUENTA, "BANCO");
            cvBa.put(COLUMN_SALDO_MONTO, 0.0);
            db.insertWithOnConflict(TABLE_SALDOS_INICIALES, null, cvBa, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    // ==================== SALDOS INICIALES ====================
    public boolean guardarSaldoInicial(String cuenta, double monto) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SALDO_CUENTA, cuenta);
        cv.put(COLUMN_SALDO_MONTO, monto);
        long result = db.insertWithOnConflict(TABLE_SALDOS_INICIALES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    public double obtenerSaldoInicial(String cuenta) {
        SQLiteDatabase db = this.getReadableDatabase();
        double monto = 0.0;
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_SALDO_MONTO + " FROM " + TABLE_SALDOS_INICIALES +
                " WHERE " + COLUMN_SALDO_CUENTA + " = ?", new String[]{cuenta});
        if (cursor.moveToFirst()) {
            monto = cursor.getDouble(0);
        }
        cursor.close();
        return monto;
    }

    public Map<String, Double> obtenerSaldosPorCuenta() {
        Map<String, Double> saldos = new HashMap<>();
        double saldoEfectivo = obtenerSaldoInicial("EFECTIVO");
        double saldoBanco = obtenerSaldoInicial("BANCO");

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_CUENTA + ", SUM(" + COLUMN_DEBE + "), SUM(" + COLUMN_HABER + ") FROM " +
                TABLE_MOVIMIENTOS + " GROUP BY " + COLUMN_CUENTA, null);

        if (cursor.moveToFirst()) {
            do {
                String cuenta = cursor.getString(0);
                double totalDebe = cursor.getDouble(1);
                double totalHaber = cursor.getDouble(2);
                double neto = totalDebe - totalHaber;

                if ("EFECTIVO".equalsIgnoreCase(cuenta)) {
                    saldoEfectivo += neto;
                } else if ("BANCO".equalsIgnoreCase(cuenta)) {
                    saldoBanco += neto;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        saldos.put("EFECTIVO", saldoEfectivo);
        saldos.put("BANCO", saldoBanco);
        saldos.put("TOTAL", saldoEfectivo + saldoBanco);
        return saldos;
    }

    // ==================== CATEGORÍAS ====================
    public long insertarCategoria(String nombre, String tipo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CAT_NOMBRE, nombre);
        cv.put(COLUMN_CAT_TIPO, tipo);
        return db.insert(TABLE_CATEGORIAS, null, cv);
    }

    public boolean actualizarCategoria(int id, String nombre, String tipo) {
        if (id == 1) return false; // Protección para "Sin Categoría"
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CAT_NOMBRE, nombre);
        cv.put(COLUMN_CAT_TIPO, tipo);
        return db.update(TABLE_CATEGORIAS, cv, COLUMN_CAT_ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean eliminarCategoria(int id) {
        if (id == 1) return false; // No se puede eliminar "Sin Categoría"
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Reasignar movimientos a "Sin Categoría" (ID 1)
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_CATEGORIA_ID, 1);
            db.update(TABLE_MOVIMIENTOS, cv, COLUMN_CATEGORIA_ID + " = ?", new String[]{String.valueOf(id)});

            // Eliminar categoría
            db.delete(TABLE_CATEGORIAS, COLUMN_CAT_ID + " = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public List<Categoria> obtenerCategorias() {
        List<Categoria> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIAS + " ORDER BY " + COLUMN_CAT_ID + " ASC", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAT_ID));
                String nombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_NOMBRE));
                String tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_TIPO));
                lista.add(new Categoria(id, nombre, tipo));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

    public String obtenerNombreCategoria(int categoriaId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String nombre = "Sin Categoría";
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_CAT_NOMBRE + " FROM " + TABLE_CATEGORIAS +
                " WHERE " + COLUMN_CAT_ID + " = ?", new String[]{String.valueOf(categoriaId)});
        if (cursor.moveToFirst()) {
            nombre = cursor.getString(0);
        }
        cursor.close();
        return nombre;
    }

    // ==================== MOVIMIENTOS ====================
    public boolean insertarMovimiento(String fecha, String concepto, double debe, double haber,
                                      String cuenta, int categoriaId, String tipoMovimiento) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FECHA, fecha);
        values.put(COLUMN_CONCEPTO, concepto);
        values.put(COLUMN_DEBE, debe);
        values.put(COLUMN_HABER, haber);
        values.put(COLUMN_CUENTA, cuenta);
        values.put(COLUMN_CATEGORIA_ID, categoriaId);
        values.put(COLUMN_TIPO_MOVIMIENTO, tipoMovimiento);
        long result = db.insert(TABLE_MOVIMIENTOS, null, values);
        return result != -1;
    }

    public boolean registrarTransferencia(String fecha, double monto, String origen, String destino, String concepto) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            String descOrigen = "TRANSFERENCIA A " + destino + ": " + concepto;
            String descDestino = "TRANSFERENCIA DESDE " + origen + ": " + concepto;

            // Salida de la cuenta Origen (Haber)
            ContentValues cvOrigen = new ContentValues();
            cvOrigen.put(COLUMN_FECHA, fecha);
            cvOrigen.put(COLUMN_CONCEPTO, descOrigen);
            cvOrigen.put(COLUMN_DEBE, 0.0);
            cvOrigen.put(COLUMN_HABER, monto);
            cvOrigen.put(COLUMN_CUENTA, origen);
            cvOrigen.put(COLUMN_CATEGORIA_ID, 1);
            cvOrigen.put(COLUMN_TIPO_MOVIMIENTO, "TRANSFERENCIA");
            db.insert(TABLE_MOVIMIENTOS, null, cvOrigen);

            // Entrada a la cuenta Destino (Debe)
            ContentValues cvDestino = new ContentValues();
            cvDestino.put(COLUMN_FECHA, fecha);
            cvDestino.put(COLUMN_CONCEPTO, descDestino);
            cvDestino.put(COLUMN_DEBE, monto);
            cvDestino.put(COLUMN_HABER, 0.0);
            cvDestino.put(COLUMN_CUENTA, destino);
            cvDestino.put(COLUMN_CATEGORIA_ID, 1);
            cvDestino.put(COLUMN_TIPO_MOVIMIENTO, "TRANSFERENCIA");
            db.insert(TABLE_MOVIMIENTOS, null, cvDestino);

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean actualizarMovimiento(int id, String fecha, String concepto, double debe, double haber,
                                        String cuenta, int categoriaId, String tipoMovimiento) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FECHA, fecha);
        values.put(COLUMN_CONCEPTO, concepto);
        values.put(COLUMN_DEBE, debe);
        values.put(COLUMN_HABER, haber);
        values.put(COLUMN_CUENTA, cuenta);
        values.put(COLUMN_CATEGORIA_ID, categoriaId);
        values.put(COLUMN_TIPO_MOVIMIENTO, tipoMovimiento);
        int rows = db.update(TABLE_MOVIMIENTOS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public void eliminarMovimiento(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MOVIMIENTOS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<Movimiento> obtenerMovimientosPorMes(int mes, int anio) {
        List<Movimiento> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String strMes = String.format("%02d", mes);
        String filtroFecha = anio + "-" + strMes + "-%";

        String query = "SELECT m.*, c." + COLUMN_CAT_NOMBRE + " FROM " + TABLE_MOVIMIENTOS + " m " +
                "LEFT JOIN " + TABLE_CATEGORIAS + " c ON m." + COLUMN_CATEGORIA_ID + " = c." + COLUMN_CAT_ID + " " +
                "WHERE m." + COLUMN_FECHA + " LIKE ? ORDER BY m." + COLUMN_FECHA + " ASC, m." + COLUMN_ID + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{filtroFecha});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA));
                String concepto = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONCEPTO));
                double debe = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DEBE));
                double haber = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HABER));
                String cuenta = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUENTA));
                int catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORIA_ID));
                String tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIPO_MOVIMIENTO));
                String catNombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_NOMBRE));

                Movimiento mov = new Movimiento(id, fecha, concepto, debe, haber, cuenta, catId, tipo);
                mov.setNombreCategoria(catNombre != null ? catNombre : "Sin Categoría");
                lista.add(mov);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

    public List<Movimiento> obtenerTodosMovimientos() {
        List<Movimiento> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT m.*, c." + COLUMN_CAT_NOMBRE + " FROM " + TABLE_MOVIMIENTOS + " m " +
                "LEFT JOIN " + TABLE_CATEGORIAS + " c ON m." + COLUMN_CATEGORIA_ID + " = c." + COLUMN_CAT_ID + " " +
                "ORDER BY m." + COLUMN_FECHA + " ASC, m." + COLUMN_ID + " ASC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA));
                String concepto = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONCEPTO));
                double debe = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DEBE));
                double haber = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HABER));
                String cuenta = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUENTA));
                int catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORIA_ID));
                String tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIPO_MOVIMIENTO));
                String catNombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_NOMBRE));

                Movimiento mov = new Movimiento(id, fecha, concepto, debe, haber, cuenta, catId, tipo);
                mov.setNombreCategoria(catNombre != null ? catNombre : "Sin Categoría");
                lista.add(mov);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

    public Map<String, Double> obtenerResumenGastosPorCategoria(int mes, int anio) {
        Map<String, Double> resumen = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String strMes = String.format("%02d", mes);
        String filtroFecha = anio + "-" + strMes + "-%";

        String query = "SELECT c." + COLUMN_CAT_NOMBRE + ", SUM(m." + COLUMN_HABER + ") FROM " + TABLE_MOVIMIENTOS + " m " +
                "JOIN " + TABLE_CATEGORIAS + " c ON m." + COLUMN_CATEGORIA_ID + " = c." + COLUMN_CAT_ID + " " +
                "WHERE m." + COLUMN_FECHA + " LIKE ? AND m." + COLUMN_HABER + " > 0 AND m." + COLUMN_TIPO_MOVIMIENTO + " != 'TRANSFERENCIA' " +
                "GROUP BY c." + COLUMN_CAT_ID;

        Cursor cursor = db.rawQuery(query, new String[]{filtroFecha});
        if (cursor.moveToFirst()) {
            do {
                String catNombre = cursor.getString(0);
                double total = cursor.getDouble(1);
                resumen.put(catNombre, total);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return resumen;
    }
}