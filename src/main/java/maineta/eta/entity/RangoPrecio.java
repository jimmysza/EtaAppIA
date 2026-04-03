package maineta.eta.entity;

/**
 * Enum que representa el presupuesto preferido del cliente por actividad
 */
public enum RangoPrecio {
    ECONOMICO("Económico (< $50k)"),
    MODERADO("Moderado ($50k–$150k)"),
    PREMIUM("Premium (> $150k)");

    private final String displayName;

    RangoPrecio(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
