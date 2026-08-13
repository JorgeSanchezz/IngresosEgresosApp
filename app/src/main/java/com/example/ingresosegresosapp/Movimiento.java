package com.example.ingresosegresosapp;

public class Movimiento {
    private int id;
    private String fecha;
    private String concepto;
    private double debe;
    private double haber;
    private double saldo;

    // Constructor con ID (para registros de BD)
    public Movimiento(int id, String fecha, String concepto, double debe, double haber) {
        this.id = id;
        this.fecha = fecha;
        this.concepto = concepto;
        this.debe = debe;
        this.haber = haber;
        this.saldo = 0.0;
    }

    // Constructor sin ID (para nuevos registros antes de guardar)
    public Movimiento(String fecha, String concepto, double debe, double haber) {
        this.id = -1;
        this.fecha = fecha;
        this.concepto = concepto;
        this.debe = debe;
        this.haber = haber;
        this.saldo = 0.0;
    }

    public int getId() { return id; }
    public String getFecha() { return fecha; }
    public String getConcepto() { return concepto; }
    public double getDebe() { return debe; }
    public double getHaber() { return haber; }
    public double getSaldo() { return saldo; }

    public void setFecha(String fecha) { this.fecha = fecha; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public void setDebe(double debe) { this.debe = debe; }
    public void setHaber(double haber) { this.haber = haber; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
}