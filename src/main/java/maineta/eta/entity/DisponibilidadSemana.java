package maineta.eta.entity;

/**
 * Enum que representa cuándo hace actividades habitualmente el cliente
 */
public enum DisponibilidadSemana {
    FINDE("Fines de semana"),
    ENTRE_SEMANA("Entre semana"),
    AMBOS("Ambos");

    private final String displayName;

    DisponibilidadSemana(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
