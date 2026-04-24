package maineta.eta.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import maineta.eta.entity.Admin;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.EstadoPagoColaborador;
import maineta.eta.entity.EstadoReembolso;
import maineta.eta.entity.PoliticaCancelacion;
import maineta.eta.entity.Reserva;
import maineta.eta.repository.AdminRepository;
import maineta.eta.repository.ColaboradorRepository;
import maineta.eta.repository.DisponibilidadRepository;
import maineta.eta.repository.ReservaRepository;

/**
 * Servicio para gestionar cancelaciones de reservas y disponibilidades
 * según las políticas de cancelación y reglas de negocio.
 * 
 * Implementa RN-01 a RN-14 del módulo de lógica de negocio y cancelaciones.
 */
@Service
@RequiredArgsConstructor
public class CancelacionService {

    private final ReservaRepository reservaRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final AdminRepository adminRepository;
    private final ColaboradorRepository colaboradorRepository;

    /**
     * RN-03: Cancelación por Cliente
     * - Solo si estado = CONFIRMADA
     * - Validar ventana de cancelación según política
     * - Calcular monto de reembolso según política
     * - Actualizar estado de pago al colaborador si aplica
     */
    @Transactional
    public void cancelarPorCliente(Long idReserva, Long idCliente) {
        Reserva reserva = reservaRepository.findById(idReserva)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        // Validar que la reserva pertenece al cliente
        if (!reserva.getCliente().getId().equals(idCliente)) {
            throw new IllegalArgumentException("Esta reserva no pertenece al cliente");
        }

        // RN-06: Solo se pueden cancelar reservas CONFIRMADA
        if (!"CONFIRMADA".equals(reserva.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden cancelar reservas confirmadas");
        }

        // Obtener política aplicada
        PoliticaCancelacion politica = reserva.getPoliticaAplicada();
        if (politica == null) {
            politica = reserva.getActividad().getPoliticaCancelacion();
        }

        // RN-04: Si política es SIEMPRE_GRATUITA, no validar tiempo
        if (politica == PoliticaCancelacion.SIEMPRE_GRATUITA) {
            procesarCancelacionCliente(reserva, politica, reserva.getPrecioConsumidor(), "CLIENTE");
            return;
        }

        // RN-05: Validar ventana de cancelación
        LocalDateTime fechaActividad = reserva.getDisponibilidad().getFecha().atStartOfDay();
        LocalDateTime ahora = LocalDateTime.now();
        
        // Obtener horas de cancelación configuradas por admin
        Admin admin = adminRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No hay administrador configurado"));
        
        int horasCancelacion = admin.getHorasCancelacion();
        long horasRestantes = Duration.between(ahora, fechaActividad).toHours();

        // Si no está dentro de la ventana de cancelación
        if (horasRestantes < horasCancelacion) {
            // RN-07: Si política es SIN_REEMBOLSO, permitir cancelación pero sin dinero
            if (politica == PoliticaCancelacion.SIN_REEMBOLSO) {
                procesarCancelacionCliente(reserva, politica, BigDecimal.ZERO, "CLIENTE");
                return;
            }
            
            // RN-08: Para otras políticas, lanzar excepción
            throw new CancelacionFueraDeTiempoException(
                String.format("Debe cancelar con al menos %d horas de anticipación. Solo quedan %d horas.", 
                    horasCancelacion, horasRestantes)
            );
        }

        // Dentro de la ventana: calcular reembolso según política
        BigDecimal montoReembolso = calcularMontoReembolso(reserva, politica);
        procesarCancelacionCliente(reserva, politica, montoReembolso, "CLIENTE");
    }

    /**
     * RN-09: Cancelación por Colaborador de una disponibilidad completa
     * - Cancela TODAS las reservas asociadas a esa disponibilidad
     * - Reembolso TOTAL a cada cliente (100% del precioConsumidor)
     * - Incrementa penalizaciones del colaborador
     * - Estado pago colaborador → NO_APLICA para esas reservas
     */
    @Transactional
    public void cancelarPorColaborador(Long idDisponibilidad, Long idColaborador, String motivo) {
        Disponibilidad disponibilidad = disponibilidadRepository.findById(idDisponibilidad)
            .orElseThrow(() -> new IllegalArgumentException("Disponibilidad no encontrada"));

        // Validar que la disponibilidad pertenece al colaborador
        if (!disponibilidad.getActividad().getColaborador().getIdColaborador().equals(idColaborador)) {
            throw new IllegalArgumentException("Esta disponibilidad no pertenece al colaborador");
        }

        // RN-10: Obtener todas las reservas CONFIRMADA de esa disponibilidad
        List<Reserva> reservas = reservaRepository.findByDisponibilidadAndEstado(disponibilidad, "CONFIRMADA");

        if (reservas.isEmpty()) {
            throw new IllegalArgumentException("No hay reservas confirmadas para cancelar en esta fecha");
        }

        // RN-11: Cancelar cada reserva con reembolso total
        for (Reserva reserva : reservas) {
            reserva.setEstado("CANCELADA_POR_COLABORADOR");
            reserva.setCanceladoPor("COLABORADOR");
            reserva.setMotivoCancelacion(motivo);
            reserva.setMontoReembolso(reserva.getPrecioConsumidor()); // Reembolso 100%
            reserva.setEstadoReembolso(EstadoReembolso.PENDIENTE_REEMBOLSO);
            reserva.setEstadoPagoColaborador(EstadoPagoColaborador.NO_APLICA);
            reservaRepository.save(reserva);
        }

        // RN-12: Incrementar penalizaciones del colaborador
        Colaborador colaborador = colaboradorRepository.findById(idColaborador.intValue())
            .orElseThrow(() -> new IllegalArgumentException("Colaborador no encontrado"));
        colaborador.setPenalizaciones(colaborador.getPenalizaciones() + 1);
        colaboradorRepository.save(colaborador);

        // Marcar disponibilidad como cancelada
        disponibilidad.setEstado("CANCELADA");
        disponibilidadRepository.save(disponibilidad);
    }

    /**
     * RN-13: Cancelación por Admin
     * - Admin puede cancelar cualquier reserva
     * - Admin decide manualmente si habrá reembolso y cuánto
     * - No afecta penalizaciones del colaborador
     */
    @Transactional
    public void cancelarPorAdmin(Long idReserva, String tipoReembolso, BigDecimal montoReembolso, String motivo) {
        Reserva reserva = reservaRepository.findById(idReserva)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        if (!"CONFIRMADA".equals(reserva.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden cancelar reservas confirmadas");
        }

        reserva.setEstado("CANCELADA_POR_ADMIN");
        reserva.setCanceladoPor("ADMIN");
        reserva.setMotivoCancelacion(motivo);

        // Admin decide el reembolso manualmente
        if ("TOTAL".equals(tipoReembolso)) {
            reserva.setMontoReembolso(reserva.getPrecioConsumidor());
            reserva.setEstadoReembolso(EstadoReembolso.PENDIENTE_REEMBOLSO);
            reserva.setEstadoPagoColaborador(EstadoPagoColaborador.NO_APLICA);
        } else if ("PARCIAL".equals(tipoReembolso)) {
            reserva.setMontoReembolso(montoReembolso);
            reserva.setEstadoReembolso(EstadoReembolso.PENDIENTE_REEMBOLSO);
            // El pago al colaborador depende de si ya se pagó o no
        } else if ("SIN_REEMBOLSO".equals(tipoReembolso)) {
            reserva.setMontoReembolso(BigDecimal.ZERO);
            reserva.setEstadoReembolso(EstadoReembolso.SIN_REEMBOLSO);
            // Estado de pago al colaborador se mantiene
        }

        reservaRepository.save(reserva);

        // Restaurar cupos de disponibilidad
        Disponibilidad disponibilidad = reserva.getDisponibilidad();
        disponibilidad.setCuposDisponibles(disponibilidad.getCuposDisponibles() + reserva.getCantidad());
        disponibilidadRepository.save(disponibilidad);
    }

    /**
     * RN-14: Validar si un cliente puede cancelar una reserva
     * Devuelve información sobre si puede cancelar y cuánto recibiría
     */
    public CancelacionInfo validarCancelacion(Long idReserva, Long idCliente) {
        Reserva reserva = reservaRepository.findById(idReserva)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        if (!reserva.getCliente().getId().equals(idCliente)) {
            throw new IllegalArgumentException("Esta reserva no pertenece al cliente");
        }

        if (!"CONFIRMADA".equals(reserva.getEstado())) {
            return new CancelacionInfo(false, BigDecimal.ZERO, "Solo se pueden cancelar reservas confirmadas", 0);
        }

        PoliticaCancelacion politica = reserva.getPoliticaAplicada();
        if (politica == null) {
            politica = reserva.getActividad().getPoliticaCancelacion();
        }

        // Si es SIEMPRE_GRATUITA, siempre puede
        if (politica == PoliticaCancelacion.SIEMPRE_GRATUITA) {
            return new CancelacionInfo(true, reserva.getPrecioConsumidor(), "Cancelación gratuita siempre", 999999);
        }

        // Calcular horas restantes
        LocalDateTime fechaActividad = reserva.getDisponibilidad().getFecha().atStartOfDay();
        LocalDateTime ahora = LocalDateTime.now();
        long horasRestantes = Duration.between(ahora, fechaActividad).toHours();

        Admin admin = adminRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No hay administrador configurado"));
        int horasCancelacion = admin.getHorasCancelacion();

        // Si está fuera de tiempo
        if (horasRestantes < horasCancelacion) {
            if (politica == PoliticaCancelacion.SIN_REEMBOLSO) {
                return new CancelacionInfo(true, BigDecimal.ZERO, 
                    "Fuera del tiempo de cancelación. Sin reembolso.", horasRestantes);
            }
            return new CancelacionInfo(false, BigDecimal.ZERO, 
                String.format("Debe cancelar con al menos %d horas de anticipación", horasCancelacion), 
                horasRestantes);
        }

        // Dentro del tiempo: calcular reembolso
        BigDecimal montoReembolso = calcularMontoReembolso(reserva, politica);
        return new CancelacionInfo(true, montoReembolso, "Puede cancelar con reembolso", horasRestantes);
    }

    // ===== MÉTODOS PRIVADOS DE APOYO =====

    private void procesarCancelacionCliente(Reserva reserva, PoliticaCancelacion politica, 
                                           BigDecimal montoReembolso, String canceladoPor) {
        reserva.setEstado("CANCELADA_POR_CLIENTE");
        reserva.setCanceladoPor(canceladoPor);
        reserva.setMontoReembolso(montoReembolso);

        if (montoReembolso.compareTo(BigDecimal.ZERO) > 0) {
            reserva.setEstadoReembolso(EstadoReembolso.PENDIENTE_REEMBOLSO);
            
            // RN-02: Si hay reembolso total, no se paga al colaborador
            if (montoReembolso.compareTo(reserva.getPrecioConsumidor()) == 0) {
                reserva.setEstadoPagoColaborador(EstadoPagoColaborador.NO_APLICA);
            }
            // Si es parcial, el colaborador puede recibir la diferencia
        } else {
            reserva.setEstadoReembolso(EstadoReembolso.SIN_REEMBOLSO);
        }

        reservaRepository.save(reserva);

        // Restaurar cupos en disponibilidad
        Disponibilidad disponibilidad = reserva.getDisponibilidad();
        disponibilidad.setCuposDisponibles(disponibilidad.getCuposDisponibles() + reserva.getCantidad());
        disponibilidadRepository.save(disponibilidad);
    }

    private BigDecimal calcularMontoReembolso(Reserva reserva, PoliticaCancelacion politica) {
        BigDecimal precioConsumidor = reserva.getPrecioConsumidor();
        
        switch (politica) {
            case SIN_REEMBOLSO:
                return BigDecimal.ZERO;
            
            case REEMBOLSO_TOTAL_SI_A_TIEMPO:
                return precioConsumidor;
            
            case REEMBOLSO_PARCIAL:
                return precioConsumidor.multiply(new BigDecimal("0.50")); // 50%
            
            case SIEMPRE_GRATUITA:
                return precioConsumidor;
            
            default:
                return BigDecimal.ZERO;
        }
    }

    // ===== CLASE INTERNA PARA RESPUESTA DE VALIDACIÓN =====

    public static class CancelacionInfo {
        private boolean puede;
        private BigDecimal montoReembolso;
        private String mensaje;
        private long horasRestantes;

        public CancelacionInfo(boolean puede, BigDecimal montoReembolso, String mensaje, long horasRestantes) {
            this.puede = puede;
            this.montoReembolso = montoReembolso;
            this.mensaje = mensaje;
            this.horasRestantes = horasRestantes;
        }

        public boolean isPuede() { return puede; }
        public BigDecimal getMontoReembolso() { return montoReembolso; }
        public String getMensaje() { return mensaje; }
        public long getHorasRestantes() { return horasRestantes; }
    }
}
