package maineta.eta.dto;

import maineta.eta.entity.TipoCuenta;

public class ColaboradorPerfilForm {
    private String nombre;
    private String email;
    private String telefono;
    private String nit;
    private String correoSeguridad;
    private String banco;
    private String numeroCuenta;
    private TipoCuenta tipoCuenta;

    public ColaboradorPerfilForm() {
    }

    public ColaboradorPerfilForm(String nombre, String email, String telefono, String nit, String correoSeguridad, String banco, String numeroCuenta, TipoCuenta tipoCuenta) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.nit = nit;
        this.correoSeguridad = correoSeguridad;
        this.banco = banco;
        this.numeroCuenta = numeroCuenta;
        this.tipoCuenta = tipoCuenta;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getCorreoSeguridad() {
        return correoSeguridad;
    }

    public void setCorreoSeguridad(String correoSeguridad) {
        this.correoSeguridad = correoSeguridad;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public TipoCuenta getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(TipoCuenta tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }
}