package maineta.eta.service;

import maineta.eta.entity.Reserva;

/**
 * Servicio para envío de correos electrónicos relacionados con reservas.
 */
public interface EmailReservaService {
    
    /**
     * Envía un email de confirmación cuando se realiza una nueva reserva.
     * 
     * @param reserva La reserva recién creada
     */
    void enviarEmailConfirmacionReserva(Reserva reserva);
    
    /**
     * Envía un email de recordatorio 24 horas antes de la fecha de la actividad.
     * 
     * @param reserva La reserva próxima
     */
    void enviarEmailRecordatorioReserva(Reserva reserva);
}
