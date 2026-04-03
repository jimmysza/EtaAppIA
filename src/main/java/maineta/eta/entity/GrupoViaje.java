package maineta.eta.entity;

/**
 * Enum que representa con quién viaja habitualmente el cliente
 */
public enum GrupoViaje {
    SOLO("Solo"),
    PAREJA("En pareja"),
    FAMILIA("Con familia"),
    AMIGOS("Con amigos"),
    VARIOS("Varía");

    private final String displayName;

    GrupoViaje(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
