package maineta.eta.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import maineta.eta.entity.Reserva;
import maineta.eta.repository.ReservaRepository;

/**
 * Servicio de tareas programadas para envío automático de recordatorios de reservas.
 */
@Slf4j
@Service
public class RecordatorioReservaScheduler {

    private final ReservaRepository reservaRepository;
    private final EmailReservaService emailReservaService;

    public RecordatorioReservaScheduler(ReservaRepository reservaRepository, 
                                        EmailReservaService emailReservaService) {
        this.reservaRepository = reservaRepository;
        this.emailReservaService = emailReservaService;
    }

    /**
     * Se ejecuta diariamente a las 9:00 AM para enviar recordatorios
     * de actividades que ocurrirán mañana (24 horas antes).
     * 
     * Expresión cron: "0 0 9 * * ?" = A las 9:00 AM todos los días
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void enviarRecordatorios24Horas() {
        log.info("Iniciando envío de recordatorios 24 horas antes...");
        
        try {
            // Calcular fecha de mañana
            LocalDate manana = LocalDate.now().plusDays(1);
            
            // Buscar reservas cuya disponibilidad sea mañana
            List<Reserva> reservasManana = reservaRepository.findReservasPorFechaDisponibilidad(manana);
            
            log.info("Encontradas {} reservas para {}", reservasManana.size(), manana);
            
            // Enviar recordatorio por cada reserva
            int enviados = 0;
            int errores = 0;
            
            for (Reserva reserva : reservasManana) {
                try {
                    emailReservaService.enviarEmailRecordatorioReserva(reserva);
                    enviados++;
                    log.debug("Recordatorio enviado para reserva #{}", reserva.getIdReserva());
                } catch (Exception e) {
                    errores++;
                    log.error("Error enviando recordatorio para reserva #{}: {}", 
                             reserva.getIdReserva(), e.getMessage());
                }
            }
            
            log.info("Recordatorios procesados: {} enviados, {} errores", enviados, errores);
            
        } catch (Exception e) {
            log.error("Error en tarea programada de recordatorios: {}", e.getMessage(), e);
        }
    }

    /**
     * Método de prueba que se puede ejecutar manualmente.
     * Descomenta la anotación @Scheduled para activarlo (útil para desarrollo).
     * Se ejecuta cada 5 minutos.
     */
    // @Scheduled(fixedRate = 300000) // Cada 5 minutos
    public void enviarRecordatoriosPrueba() {
        log.info("PRUEBA: Buscando reservas para enviar recordatorios...");
        
        // Buscar reservas de mañana
        LocalDate manana = LocalDate.now().plusDays(1);
        List<Reserva> reservasManana = reservaRepository.findReservasPorFechaDisponibilidad(manana);
        
        log.info("PRUEBA: Encontradas {} reservas para mañana ({})", 
                reservasManana.size(), manana);
        
        if (!reservasManana.isEmpty()) {
            Reserva primeraReserva = reservasManana.get(0);
            log.info("PRUEBA: Enviando recordatorio de prueba para reserva #{}", 
                    primeraReserva.getIdReserva());
            emailReservaService.enviarEmailRecordatorioReserva(primeraReserva);
        }
    }
}
