package maineta.eta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import maineta.eta.dto.ColaboradorEstadisticasAdminDTO;
import maineta.eta.entity.Colaborador;
import maineta.eta.repository.ColaboradorRepository;
import maineta.eta.entity.Admin;

import maineta.eta.dto.EstadosReservaDTO;
import maineta.eta.dto.IngresoMensualDTO;
import maineta.eta.dto.KpiActividadDTO;
import maineta.eta.dto.KpiResumenDTO;
import maineta.eta.dto.OcupacionDTO;
import maineta.eta.dto.PuntoTendenciaDTO;
import maineta.eta.repository.ActividadRepository;
import maineta.eta.repository.AdminRepository;
import maineta.eta.repository.ComentarioRepository;
import maineta.eta.repository.DisponibilidadRepository;
import maineta.eta.repository.FavoritoRepository;
import maineta.eta.repository.ReservaRepository;

@Service
public class KpiColaboradorServiceImpl implements KpiColaboradorService {

    private final ReservaRepository reservaRepository;
    private final ActividadRepository actividadRepository;
    private final ComentarioRepository comentarioRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final FavoritoRepository favoritoRepository;
    private final AdminRepository adminRepository;
    private final ColaboradorRepository colaboradorRepository;

    public KpiColaboradorServiceImpl(
            ReservaRepository reservaRepository,
            ActividadRepository actividadRepository,
            ComentarioRepository comentarioRepository,
            DisponibilidadRepository disponibilidadRepository,
            FavoritoRepository favoritoRepository,
            AdminRepository adminRepository,
            ColaboradorRepository colaboradorRepository) {
        this.reservaRepository = reservaRepository;
        this.actividadRepository = actividadRepository;
        this.comentarioRepository = comentarioRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.favoritoRepository = favoritoRepository;
        this.adminRepository = adminRepository;
        this.colaboradorRepository = colaboradorRepository;
    }

    @Override
    public KpiResumenDTO obtenerResumen(Long idColaborador, YearMonth periodo) {
        LocalDate inicio = periodo.atDay(1);
        LocalDate fin = periodo.atEndOfMonth();
        LocalDateTime inicioDateTime = inicio.atStartOfDay();
        LocalDateTime finDateTime = fin.atTime(23, 59, 59);

        KpiResumenDTO kpi = new KpiResumenDTO();

        // Ingreso bruto
        BigDecimal ingresoBruto = reservaRepository.calcularIngresoBrutoPorColaborador(
                idColaborador, inicioDateTime, finDateTime, "Hecho");
        kpi.setIngresoBruto(ingresoBruto != null ? ingresoBruto : BigDecimal.ZERO);

        // Obtener porcentaje de comisión del admin más reciente
        BigDecimal comision = adminRepository.findTopByOrderByIdAdminDesc()
                .map(admin -> admin.getPorcentajeComision())
                .orElse(BigDecimal.valueOf(18.00));

        // Ingreso neto
        BigDecimal factorNeto = BigDecimal.ONE.subtract(comision.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        kpi.setIngresoNeto(kpi.getIngresoBruto().multiply(factorNeto).setScale(2, RoundingMode.HALF_UP));

        // Total de reservas (excluye canceladas)
        Long totalReservas = reservaRepository.contarReservasPorColaboradorYPeriodo(
                idColaborador, inicioDateTime, finDateTime);
        kpi.setTotalReservas(totalReservas != null ? totalReservas.intValue() : 0);

        // Tasa de cancelación
        Long canceladas = reservaRepository.contarReservasPorColaboradorYEstado(
                idColaborador, inicioDateTime, finDateTime, "Cancelada");
        Long totales = reservaRepository.contarTodasReservasPorColaborador(
                idColaborador, inicioDateTime, finDateTime);
        if (totales != null && totales > 0) {
            kpi.setTasaCancelacion((canceladas != null ? canceladas : 0) * 100.0 / totales);
        } else {
            kpi.setTasaCancelacion(0.0);
        }

        // Calificación promedio
        Double calificacionPromedio = comentarioRepository.calcularCalificacionPromedioColaborador(idColaborador);
        kpi.setCalificacionPromedio(calificacionPromedio != null ? calificacionPromedio : 0.0);

        // Actividades publicadas
        Long actividadesCount = actividadRepository.contarActividadesPorColaborador(idColaborador);
        kpi.setActividadesPublicadas(actividadesCount != null ? actividadesCount.intValue() : 0);

        // Clientes únicos
        Long clientesUnicosCount = reservaRepository.contarClientesUnicosPorColaborador(
                idColaborador, inicioDateTime, finDateTime);
        kpi.setClientesUnicos(clientesUnicosCount != null ? clientesUnicosCount.intValue() : 0);

        // Clientes recurrentes (más de 1 reserva Hecho en cualquier momento)
        Long clientesRecurrentes = reservaRepository.contarClientesRecurrentesPorColaborador(idColaborador);
        kpi.setClientesRecurrentes(clientesRecurrentes != null ? clientesRecurrentes.intValue() : 0);

        // Variaciones vs período anterior
        YearMonth periodoAnterior = periodo.minusMonths(1);
        LocalDate inicioAnterior = periodoAnterior.atDay(1);
        LocalDate finAnterior = periodoAnterior.atEndOfMonth();
        LocalDateTime inicioAnteriorDateTime = inicioAnterior.atStartOfDay();
        LocalDateTime finAnteriorDateTime = finAnterior.atTime(23, 59, 59);

        Long reservasAnterior = reservaRepository.contarReservasPorColaboradorYPeriodo(
                idColaborador, inicioAnteriorDateTime, finAnteriorDateTime);
        if (reservasAnterior != null && reservasAnterior > 0 && totalReservas != null) {
            double variacion = ((totalReservas - reservasAnterior) * 100.0) / reservasAnterior;
            kpi.setVariacionReservas(variacion);
        }

        BigDecimal ingresoAnterior = reservaRepository.calcularIngresoBrutoPorColaborador(
                idColaborador, inicioAnteriorDateTime, finAnteriorDateTime, "Hecho");
        if (ingresoAnterior != null && ingresoAnterior.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = kpi.getIngresoBruto().subtract(ingresoAnterior);
            double variacion = diff.multiply(BigDecimal.valueOf(100))
                    .divide(ingresoAnterior, 2, RoundingMode.HALF_UP).doubleValue();
            kpi.setVariacionIngresos(variacion);
        }

        return kpi;
    }

    @Override
    public List<KpiActividadDTO> obtenerKpiPorActividad(Long idColaborador, YearMonth periodo) {
        LocalDate inicio = periodo.atDay(1);
        LocalDate fin = periodo.atEndOfMonth();
        LocalDateTime inicioDateTime = inicio.atStartOfDay();
        LocalDateTime finDateTime = fin.atTime(23, 59, 59);

        List<Object[]> actividadesData = actividadRepository.obtenerActividadesConKpis(
                idColaborador, inicioDateTime, finDateTime, inicio, fin);

        List<KpiActividadDTO> kpis = new ArrayList<>();

        for (Object[] row : actividadesData) {
            KpiActividadDTO kpi = new KpiActividadDTO();
            kpi.setIdActividad((Long) row[0]);
            kpi.setTitulo((String) row[1]);
            kpi.setReservas(row[2] != null ? ((Number) row[2]).intValue() : 0);
            kpi.setCalificacion(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0);
            kpi.setTasaOcupacion(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0);
            kpi.setVecesEnFavoritos(row[5] != null ? ((Number) row[5]).intValue() : 0);
            kpi.setTotalVistas(row[6] != null ? ((Number) row[6]).intValue() : 0);

            // Conversión visita -> reserva
            if (kpi.getTotalVistas() > 0) {
                kpi.setConversionVisitaReserva((kpi.getReservas() * 100.0) / kpi.getTotalVistas());
            } else {
                kpi.setConversionVisitaReserva(0.0);
            }

            kpis.add(kpi);
        }

        return kpis;
    }

    @Override
    public List<PuntoTendenciaDTO> obtenerTendenciaReservas(Long idColaborador, int semanas) {
        YearMonth actual = YearMonth.now();
        LocalDate inicioMes = actual.atDay(1);
        LocalDate finMes = actual.atEndOfMonth();

        List<PuntoTendenciaDTO> tendencia = new ArrayList<>();

        for (int i = 0; i < semanas; i++) {
            LocalDate inicioSemana = inicioMes.plusDays(i * 7);
            LocalDate finSemana = inicioSemana.plusDays(6);
            if (finSemana.isAfter(finMes)) {
                finSemana = finMes;
            }

            LocalDateTime inicioDateTime = inicioSemana.atStartOfDay();
            LocalDateTime finDateTime = finSemana.atTime(23, 59, 59);

            Long reservas = reservaRepository.contarReservasPorColaboradorYPeriodo(
                    idColaborador, inicioDateTime, finDateTime);

            PuntoTendenciaDTO punto = new PuntoTendenciaDTO();
            punto.setEtiqueta("Sem " + (i + 1));
            punto.setReservas(reservas != null ? reservas.intValue() : 0);
            tendencia.add(punto);

            if (finSemana.equals(finMes)) {
                break;
            }
        }

        return tendencia;
    }

    @Override
    public EstadosReservaDTO obtenerEstadosReserva(Long idColaborador, YearMonth periodo) {
        LocalDate inicio = periodo.atDay(1);
        LocalDate fin = periodo.atEndOfMonth();
        LocalDateTime inicioDateTime = inicio.atStartOfDay();
        LocalDateTime finDateTime = fin.atTime(23, 59, 59);

        EstadosReservaDTO estados = new EstadosReservaDTO();

        Long pendiente = reservaRepository.contarReservasPorColaboradorYEstado(
                idColaborador, inicioDateTime, finDateTime, "Pendiente");
        estados.setPendiente(pendiente != null ? pendiente.intValue() : 0);

        Long confirmada = reservaRepository.contarReservasPorColaboradorYEstado(
                idColaborador, inicioDateTime, finDateTime, "Confirmada");
        estados.setConfirmada(confirmada != null ? confirmada.intValue() : 0);

        Long hecho = reservaRepository.contarReservasPorColaboradorYEstado(
                idColaborador, inicioDateTime, finDateTime, "Hecho");
        estados.setHecho(hecho != null ? hecho.intValue() : 0);

        Long cancelada = reservaRepository.contarReservasPorColaboradorYEstado(
                idColaborador, inicioDateTime, finDateTime, "Cancelada");
        estados.setCancelada(cancelada != null ? cancelada.intValue() : 0);

        Long noShowCliente = reservaRepository.contarReservasPorColaboradorYEstado(
                idColaborador, inicioDateTime, finDateTime, "NO_SHOW_CLIENTE");
        estados.setNoShowCliente(noShowCliente != null ? noShowCliente.intValue() : 0);

        Long noShowColaborador = reservaRepository.contarReservasPorColaboradorYEstado(
                idColaborador, inicioDateTime, finDateTime, "NO_SHOW_COLABORADOR");
        estados.setNoShowColaborador(noShowColaborador != null ? noShowColaborador.intValue() : 0);

        // Tasa de conversión Pendiente -> Confirmada
        if (estados.getPendiente() > 0) {
            estados.setTasaConversionPendienteConfirmada(
                    (estados.getConfirmada() * 100.0) / estados.getPendiente());
        } else {
            estados.setTasaConversionPendienteConfirmada(0.0);
        }

        return estados;
    }

    @Override
    public OcupacionDTO obtenerOcupacion(Long idColaborador, YearMonth periodo) {
        LocalDate inicio = periodo.atDay(1);
        LocalDate fin = periodo.atEndOfMonth();

        OcupacionDTO ocupacion = new OcupacionDTO();

        Long disponible = disponibilidadRepository.contarDisponibilidadesPorColaboradorYEstado(
                idColaborador, inicio, fin, "DISPONIBLE");
        ocupacion.setDisponible(disponible != null ? disponible.intValue() : 0);

        Long completado = disponibilidadRepository.contarDisponibilidadesPorColaboradorYEstado(
                idColaborador, inicio, fin, "COMPLETADO");
        ocupacion.setCompletado(completado != null ? completado.intValue() : 0);

        Long cancelado = disponibilidadRepository.contarDisponibilidadesPorColaboradorYEstado(
                idColaborador, inicio, fin, "CANCELADO");
        ocupacion.setCancelado(cancelado != null ? cancelado.intValue() : 0);

        return ocupacion;
    }

    @Override
    public List<IngresoMensualDTO> obtenerIngresosMensuales(Long idColaborador, int meses) {
        List<IngresoMensualDTO> ingresos = new ArrayList<>();
        YearMonth actual = YearMonth.now();

        // Obtener comisión del admin
        BigDecimal comision = adminRepository.findTopByOrderByIdAdminDesc()
                .map(admin -> admin.getPorcentajeComision())
                .orElse(BigDecimal.valueOf(18.00));
        BigDecimal factorNeto = BigDecimal.ONE.subtract(comision.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        for (int i = meses - 1; i >= 0; i--) {
            YearMonth periodo = actual.minusMonths(i);
            LocalDate inicio = periodo.atDay(1);
            LocalDate fin = periodo.atEndOfMonth();
            LocalDateTime inicioDateTime = inicio.atStartOfDay();
            LocalDateTime finDateTime = fin.atTime(23, 59, 59);

            BigDecimal ingresoBruto = reservaRepository.calcularIngresoBrutoPorColaborador(
                    idColaborador, inicioDateTime, finDateTime, "Hecho");
            ingresoBruto = ingresoBruto != null ? ingresoBruto : BigDecimal.ZERO;

            BigDecimal ingresoNeto = ingresoBruto.multiply(factorNeto).setScale(2, RoundingMode.HALF_UP);

            IngresoMensualDTO dto = new IngresoMensualDTO();
            dto.setMes(periodo.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES")));
            dto.setIngresoBruto(ingresoBruto);
            dto.setIngresoNeto(ingresoNeto);

            ingresos.add(dto);
        }

        return ingresos;
    }

    @Override
    public ColaboradorEstadisticasAdminDTO obtenerEstadisticasAdmin(Long idColaborador) {
        Colaborador colab = colaboradorRepository.findByIdColaborador(idColaborador)
                .orElseThrow(() -> new IllegalArgumentException("Colaborador no encontrado"));

        ColaboradorEstadisticasAdminDTO dto = new ColaboradorEstadisticasAdminDTO();
        dto.setIdColaborador(colab.getIdColaborador());
        dto.setNombre(colab.getUsuario().getNombre());
        dto.setEmail(colab.getUsuario().getEmail());
        dto.setNit(colab.getNit());
        
        long meses = ChronoUnit.MONTHS.between(colab.getUsuario().getCreatedAt().toLocalDate(), LocalDate.now());
        dto.setAntiguedadMeses((int) meses);

        Long actividadesCount = actividadRepository.contarActividadesPorColaborador(idColaborador);
        dto.setTotalActividades(actividadesCount != null ? actividadesCount.intValue() : 0);

        List<maineta.eta.entity.Actividad> actividades = actividadRepository.findByColaborador_IdColaboradorOrderByCreatedAtDesc(idColaborador);
        int popularidadGlobal = 0;
        for (maineta.eta.entity.Actividad act : actividades) {
            popularidadGlobal += act.getTotalVistas() + act.getTotalTendencia();
        }
        dto.setPopularidadGlobal(popularidadGlobal);

        Double calificacionPromedio = comentarioRepository.calcularCalificacionPromedioColaborador(idColaborador);
        dto.setCalificacionPromedio(calificacionPromedio != null ? calificacionPromedio : 0.0);

        LocalDateTime inicioDateTime = colab.getUsuario().getCreatedAt();
        LocalDateTime finDateTime = LocalDateTime.now();

        Long totalReservas = reservaRepository.contarTodasReservasPorColaborador(idColaborador, inicioDateTime, finDateTime);
        dto.setTotalReservas(totalReservas != null ? totalReservas.intValue() : 0);

        Long hecho = reservaRepository.contarReservasPorColaboradorYEstado(idColaborador, inicioDateTime, finDateTime, "Hecho");
        if (totalReservas != null && totalReservas > 0) {
            dto.setTasaCumplimiento((hecho != null ? hecho : 0) * 100.0 / totalReservas);
        } else {
            dto.setTasaCumplimiento(0.0);
        }

        Long canceladas = reservaRepository.contarReservasPorColaboradorYEstado(idColaborador, inicioDateTime, finDateTime, "Cancelada");
        if (totalReservas != null && totalReservas > 0) {
            dto.setTasaCancelacion((canceladas != null ? canceladas : 0) * 100.0 / totalReservas);
        } else {
            dto.setTasaCancelacion(0.0);
        }

        Long noShowColaborador = reservaRepository.contarReservasPorColaboradorYEstado(idColaborador, inicioDateTime, finDateTime, "NO_SHOW_COLABORADOR");
        dto.setPenalizacionesNoShow(colab.getPenalizaciones() + (noShowColaborador != null ? noShowColaborador.intValue() : 0));

        LocalDate inicioDate = colab.getUsuario().getCreatedAt().toLocalDate();
        LocalDate finDate = LocalDate.now();
        Long disponible = disponibilidadRepository.contarDisponibilidadesPorColaboradorYEstado(idColaborador, inicioDate, finDate, "DISPONIBLE");
        Long completado = disponibilidadRepository.contarDisponibilidadesPorColaboradorYEstado(idColaborador, inicioDate, finDate, "COMPLETADO");
        Long totalDisp = (disponible != null ? disponible : 0) + (completado != null ? completado : 0);
        if (totalDisp > 0) {
            dto.setTasaOcupacionPromedio((completado != null ? completado : 0) * 100.0 / totalDisp);
        } else {
            dto.setTasaOcupacionPromedio(0.0);
        }

        BigDecimal ingresoBruto = reservaRepository.calcularIngresoBrutoPorColaborador(idColaborador, inicioDateTime, finDateTime, "Hecho");
        dto.setIngresosGenerados(ingresoBruto != null ? ingresoBruto : BigDecimal.ZERO);

        BigDecimal comisionAdmin = adminRepository.findTopByOrderByIdAdminDesc()
                .map(Admin::getPorcentajeComision)
                .orElse(BigDecimal.valueOf(18.00));
        
        dto.setComisionesEta(dto.getIngresosGenerados().multiply(comisionAdmin).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));

        return dto;
    }
}
