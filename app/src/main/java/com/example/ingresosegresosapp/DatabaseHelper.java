package com.example.ingresosegresosapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ingresos_egresos.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "movimientos";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_FECHA = "fecha";
    public static final String COLUMN_CONCEPTO = "concepto";
    public static final String COLUMN_DEBE = "debe";
    public static final String COLUMN_HABER = "haber";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_FECHA + " TEXT, " +
                COLUMN_CONCEPTO + " TEXT, " +
                COLUMN_DEBE + " REAL, " +
                COLUMN_HABER + " REAL)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insertar un nuevo movimiento
    public boolean insertarMovimiento(String fecha, String concepto, double debe, double haber) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FECHA, fecha);
        values.put(COLUMN_CONCEPTO, concepto);
        values.put(COLUMN_DEBE, debe);
        values.put(COLUMN_HABER, haber);
        long result = db.insert(TABLE_NAME, null, values);
        return result != -1;
    }

    // Obtener todos los movimientos guardados
    public List<Movimiento> obtenerMovimientos() {
        List<Movimiento> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA));
                String concepto = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONCEPTO));
                double debe = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DEBE));
                double haber = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HABER));

                Movimiento mov = new Movimiento(id, fecha, concepto, debe, haber);
                lista.add(mov);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

    // Actualizar un registro existente (Fecha, Concepto, Debe, Haber)
    public boolean actualizarMovimiento(int id, String fecha, String concepto, double debe, double haber) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FECHA, fecha);
        values.put(COLUMN_CONCEPTO, concepto);
        values.put(COLUMN_DEBE, debe);
        values.put(COLUMN_HABER, haber);
        int rows = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // Borrar un registro por su ID
    public void eliminarMovimiento(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }
}