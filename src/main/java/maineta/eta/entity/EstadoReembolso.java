package maineta.eta.entity;

public enum EstadoReembolso {
    PENDIENTE_REEMBOLSO("Pendiente de reembolso al cliente"),
    REEMBOLSADO("Reembolsado al cliente"),
    SIN_REEMBOLSO("Sin reembolso (política sin reembolso)");

    private final String descripcion;

    EstadoReembolso(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
