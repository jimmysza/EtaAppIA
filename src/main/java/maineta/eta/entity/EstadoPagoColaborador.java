package maineta.eta.entity;

public enum EstadoPagoColaborador {
    PENDIENTE_PAGO("Pendiente de pago"),
    PAGADO("Pagado al colaborador"),
    NO_APLICA("No aplica (cancelación con reembolso total)");

    private final String descripcion;

    EstadoPagoColaborador(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
