package maineta.eta.service;

import java.time.YearMonth;
import java.util.List;

import maineta.eta.dto.EstadosReservaDTO;
import maineta.eta.dto.IngresoMensualDTO;
import maineta.eta.dto.KpiActividadDTO;
import maineta.eta.dto.KpiResumenDTO;
import maineta.eta.dto.OcupacionDTO;
import maineta.eta.dto.PuntoTendenciaDTO;

public interface KpiColaboradorService {
    
    /**
     * Obtiene el resumen general de KPIs para un colaborador en un período
     */
    KpiResumenDTO obtenerResumen(Long idColaborador, YearMonth periodo);
    
    /**
     * Obtiene los KPIs individuales por cada actividad del colaborador
     */
    List<KpiActividadDTO> obtenerKpiPorActividad(Long idColaborador, YearMonth periodo);
    
    /**
     * Obtiene la tendencia de reservas por semana
     */
    List<PuntoTendenciaDTO> obtenerTendenciaReservas(Long idColaborador, int semanas);
    
    /**
     * Obtiene el desglose de reservas por estado
     */
    EstadosReservaDTO obtenerEstadosReserva(Long idColaborador, YearMonth periodo);
    
    /**
     * Obtiene el estado de las disponibilidades
     */
    OcupacionDTO obtenerOcupacion(Long idColaborador, YearMonth periodo);
    
    /**
     * Obtiene los ingresos brutos y netos de los últimos N meses
     */
    List<IngresoMensualDTO> obtenerIngresosMensuales(Long idColaborador, int meses);

    /**
     * Obtiene estadísticas consolidadas del colaborador para el administrador
     */
    maineta.eta.dto.ColaboradorEstadisticasAdminDTO obtenerEstadisticasAdmin(Long idColaborador);
}
