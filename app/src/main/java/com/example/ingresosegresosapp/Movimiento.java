package com.example.ingresosegresosapp;

public class Movimiento {
    private int id;
    private String fecha; // YYYY-MM-DD
    private String concepto;
    private double debe;
    private double haber;
    private double saldo;
    private String cuenta; // "EFECTIVO" o "BANCO"
    private int categoriaId;
    private String tipoMovimiento; // "INGRESO", "EGRESO", "TRANSFERENCIA"
    private String nombreCategoria;

    public Movimiento(int id, String fecha, String concepto, double debe, double haber,
                      String cuenta, int categoriaId, String tipoMovimiento) {
        this.id = id;
        this.fecha = fecha;
        this.concepto = concepto;
        this.debe = debe;
        this.haber = haber;
        this.saldo = 0.0;
        this.cuenta = cuenta;
        this.categoriaId = categoriaId;
        this.tipoMovimiento = tipoMovimiento;
        this.nombreCategoria = "Sin Categoría";
    }

    public int getId() { return id; }
    public String getFecha() { return fecha; }
    public String getConcepto() { return concepto; }
    public double getDebe() { return debe; }
    public double getHaber() { return haber; }
    public double getSaldo() { return saldo; }
    public String getCuenta() { return cuenta; }
    public int getCategoriaId() { return categoriaId; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public String getNombreCategoria() { return nombreCategoria; }

    public void setFecha(String fecha) { this.fecha = fecha; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public void setDebe(double debe) { this.debe = debe; }
    public void setHaber(double haber) { this.haber = haber; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    public void setCuenta(String cuenta) { this.cuenta = cuenta; }
    public void setCategoriaId(int categoriaId) { this.categoriaId = categoriaId; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }
}