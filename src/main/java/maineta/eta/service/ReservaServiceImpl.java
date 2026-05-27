package maineta.eta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import maineta.eta.config.UsuarioHelper;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.EstadoPagoColaborador;
import maineta.eta.entity.EstadoReembolso;
import maineta.eta.entity.Reserva;
import maineta.eta.repository.ReservaRepository;

@Service
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final ActividadService actividadService;
    private final ClienteService clienteService;
    private final DisponibilidadService disponibilidadService;
    private final UsuarioHelper usuarioHelper;
    private final AdminService adminService;

    public ReservaServiceImpl(ReservaRepository reservaRepository, 
                             ActividadService actividadService,
                             ClienteService clienteService,
                             DisponibilidadService disponibilidadService,
                             UsuarioHelper usuarioHelper,
                             AdminService adminService) {
        this.reservaRepository = reservaRepository;
        this.actividadService = actividadService;
        this.clienteService = clienteService;
        this.disponibilidadService = disponibilidadService;
        this.usuarioHelper = usuarioHelper;
        this.adminService = adminService;
    }

    @Override
    public Long ContadorReservas() {
        return reservaRepository.count();
    }

    @Override
    @Transactional
    public Reserva hacerReserva(Cliente cliente, Actividad actividad,
            Disponibilidad disponibilidad, int cantidad) throws Exception {

        // Verificar cupos
        if (disponibilidad.getCuposDisponibles() < cantidad) {
            throw new Exception("No hay suficientes cupos disponibles. Solo quedan: " + disponibilidad.getCuposDisponibles());

        }

        // Verificar disponibilidad
        if (!disponibilidad.getActividad().getIdActividad().equals(actividad.getIdActividad())) {
            throw new Exception("La disponibilidad no pertenece a esta actividad");
        }

        // Crear reserva
        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setActividad(actividad);
        reserva.setDisponibilidad(disponibilidad);
        reserva.setCantidad(cantidad);
        reserva.setFechaReserva(LocalDateTime.now());
        completarDatosEconomicosReserva(reserva, actividad, cantidad);

        // Reducir cupos
        disponibilidad.setCuposDisponibles(disponibilidad.getCuposDisponibles() - cantidad);
        actividadService.agregarActividad(actividad);

        // Guardar reserva
        return reservaRepository.save(reserva);
    }
    
    @Override
    public Reserva guardarReserva(Reserva reserva){
        return reservaRepository.save(reserva);
    }

    @Override
    public Optional<Reserva> ObtenerReservaPorId(Long idReserva){
        return reservaRepository.findByidReserva(idReserva);
    }

    @Override
    public void EliminarReservacion(Reserva reserva) {
        reservaRepository.delete(reserva);
    }

    @Override
    public boolean existeReservaRealizada(Long idCliente, Long idActividad) {
        List<Reserva> reservas = reservaRepository
                .findByCliente_IdAndActividad_IdActividad(idCliente, idActividad);

        // Verificar si ALGUNA reserva tiene estado "Hecho"
        return reservas.stream()
                .anyMatch(r -> "Hecho".equalsIgnoreCase(r.getEstado()));
    }


    @Override
    public List<Reserva> getReservasCliente(Cliente cliente) {
        // ✅ Usar el repositorio en lugar de la lista en memoria
        return reservaRepository.findByCliente(cliente);
    }

    @Override
    public List<Reserva> getReservasColaborador(Long idColaborador) {
        return reservaRepository.findByActividad_Colaborador_IdColaboradorOrderByFechaReservaDesc(idColaborador);
    }

    @Override
    public List<Reserva> getReservasPorIdActividad(Long idActividad){
        return reservaRepository.findByActividad_IdActividad(idActividad);
    }

    @Override
    @Transactional
    public Reserva crearReservaDesdeEpayco(Long idDisponibilidad, Long idCliente, 
                                           Long idActividad, int cantidad, String refPayco) throws Exception {
        
        // Obtener actividad
        Actividad actividad = actividadService.listarById(idActividad);
        if (actividad == null) {
            throw new Exception("Actividad no encontrada");
        }
                
        // Obtener disponibilidad usando el servicio
        Disponibilidad disponibilidad = disponibilidadService.obtenerPorId(idDisponibilidad)
                .orElseThrow(() -> new Exception("Disponibilidad no encontrada"));

        // Verificar cupos
        if (disponibilidad.getCuposDisponibles() < cantidad) {
            throw new Exception("No hay suficientes cupos disponibles. Solo quedan: " + disponibilidad.getCuposDisponibles());
        }

        // Obtener cliente
        Cliente cliente = clienteService.obtenerPorId(idCliente);
        if (cliente == null) {
            throw new Exception("Cliente no encontrado");
        }

        // Crear reserva
        Reserva reserva = new Reserva();
        reserva.setActividad(actividad);
        reserva.setDisponibilidad(disponibilidad);
        reserva.setCliente(cliente);
        reserva.setCantidad(cantidad);
        reserva.setFechaReserva(LocalDateTime.now());
        reserva.setEstado("Confirmada"); // Estado confirmado porque el pago fue exitoso
        reserva.setRefPayco(refPayco);
        completarDatosEconomicosReserva(reserva, actividad, cantidad);

        // Reducir cupos
        disponibilidad.setCuposDisponibles(disponibilidad.getCuposDisponibles() - cantidad);
        actividadService.agregarActividad(actividad);
        
        // Guardar y retornar
        return reservaRepository.save(reserva);
    }
    
    @Override
    @Transactional
    public Reserva crearReservaDesdeWompi(Long idDisponibilidad, Long idCliente, 
                                          Long idActividad, int cantidad, String reference,
                                          String wompiTransactionId) throws Exception {
        
        // Obtener actividad
        Actividad actividad = actividadService.listarById(idActividad);
        if (actividad == null) {
            throw new Exception("Actividad no encontrada");
        }
                
        // Obtener disponibilidad usando el servicio
        Disponibilidad disponibilidad = disponibilidadService.obtenerPorId(idDisponibilidad)
                .orElseThrow(() -> new Exception("Disponibilidad no encontrada"));

        // Verificar cupos
        if (disponibilidad.getCuposDisponibles() < cantidad) {
            throw new Exception("No hay suficientes cupos disponibles. Solo quedan: " + disponibilidad.getCuposDisponibles());
        }

        // Obtener cliente
        Cliente cliente = clienteService.obtenerPorId(idCliente);
        if (cliente == null) {
            throw new Exception("Cliente no encontrado");
        }

        // Crear reserva
        Reserva reserva = new Reserva();
        reserva.setActividad(actividad);
        reserva.setDisponibilidad(disponibilidad);
        reserva.setCliente(cliente);
        reserva.setCantidad(cantidad);
        reserva.setFechaReserva(LocalDateTime.now());
        reserva.setEstado("Confirmada"); // Estado confirmado porque el pago fue exitoso
        reserva.setRefWompi(reference); // Referencia de Wompi
        reserva.setWompiTransactionId(wompiTransactionId); // ID de transacción de Wompi
        completarDatosEconomicosReserva(reserva, actividad, cantidad);

        // Reducir cupos
        disponibilidad.setCuposDisponibles(disponibilidad.getCuposDisponibles() - cantidad);
        actividadService.agregarActividad(actividad);
        
        // Guardar y retornar
        return reservaRepository.save(reserva);
    }
    
    @Override
    public Page<Reserva> obtenerReservasConPagoPendiente(Pageable pageable) {
        return reservaRepository.findByEstadoPagoColaborador(EstadoPagoColaborador.PENDIENTE_PAGO, pageable);
    }
    
    @Override
    public Page<Reserva> obtenerReservasConReembolsoPendiente(Pageable pageable) {
        return reservaRepository.findByEstadoReembolso(EstadoReembolso.PENDIENTE_REEMBOLSO, pageable);
    }
    
    @Override
    public Page<Reserva> obtenerReservasPendientesDePago(Pageable pageable) {
        return reservaRepository.findByEstadoIgnoreCaseAndEstadoPagoColaborador(
                "Pendiente",
                EstadoPagoColaborador.PENDIENTE_PAGO,
                pageable);
    }
    
    @Override
    public Page<Reserva> obtenerTodasReservas(Pageable pageable) {
        return reservaRepository.findAll(pageable);
    }
    
    @Override
    public Page<Reserva> obtenerPorEstado(String estado, Pageable pageable) {
        return reservaRepository.findByEstado(estado, pageable);
    }

    @Override
    public Page<Reserva> obtenerPorEstados(List<String> estados, Pageable pageable) {
        return reservaRepository.findByEstadoIn(estados, pageable);
    }
    
    @Override
    public Optional<Reserva> obtenerPorId(Long idReserva) {
        return reservaRepository.findById(idReserva);
    }

    @Override
    public BigDecimal calcularTotalComisionesGanadas() {
        return reservaRepository.calcularTotalComisionesGanadas();
    }

    private void completarDatosEconomicosReserva(Reserva reserva, Actividad actividad, int cantidad) {
        BigDecimal precioColaborador = actividad.getPrecio() != null ? actividad.getPrecio() : BigDecimal.ZERO;
        BigDecimal precioConsumidor = usuarioHelper.CalcularPrecioConsumidor(precioColaborador);
        BigDecimal porcentajeComision = adminService.obtenerAdminPrincipal().getPorcentajeComision();

        if (porcentajeComision == null) {
            porcentajeComision = new BigDecimal("18.00");
        }

        BigDecimal comisionEta = precioConsumidor.subtract(precioColaborador)
                .multiply(BigDecimal.valueOf(Math.max(1, cantidad)))
                .setScale(2, RoundingMode.HALF_UP);

        reserva.setPrecioColaborador(precioColaborador.setScale(2, RoundingMode.HALF_UP));
        reserva.setPrecioConsumidor(precioConsumidor.setScale(2, RoundingMode.HALF_UP));
        reserva.setComisionPorcentaje(porcentajeComision.setScale(2, RoundingMode.HALF_UP));
        reserva.setComisionEta(comisionEta);
    }
}
