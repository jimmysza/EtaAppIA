package maineta.eta.service;

public class CancelacionFueraDeTiempoException extends RuntimeException {
    public CancelacionFueraDeTiempoException(String mensaje) {
        super(mensaje);
    }
}
