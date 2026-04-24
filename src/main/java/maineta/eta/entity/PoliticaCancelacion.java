package maineta.eta.entity;

public enum PoliticaCancelacion {
    SIN_REEMBOLSO("Sin reembolso", "El cliente no recibe dinero de vuelta si cancela"),
    REEMBOLSO_TOTAL_SI_A_TIEMPO("Reembolso total si cancela a tiempo", "El cliente recibe reembolso del 100% si cancela dentro de la ventana permitida"),
    REEMBOLSO_PARCIAL("Reembolso parcial del 50%", "El cliente recibe el 50% del precio si cancela a tiempo"),
    SIEMPRE_GRATUITA("Cancelación gratuita siempre", "El cliente puede cancelar en cualquier momento con reembolso total");

    private final String nombre;
    private final String descripcion;

    PoliticaCancelacion(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
