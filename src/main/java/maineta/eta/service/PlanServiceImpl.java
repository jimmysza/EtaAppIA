package maineta.eta.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import maineta.eta.config.UsuarioHelper;
import maineta.eta.dto.CrearPlanFormDTO;
import maineta.eta.dto.PlanActividadDTO;
import maineta.eta.dto.PlanDTO;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Plan;
import maineta.eta.entity.PlanActividad;
import maineta.eta.repository.ActividadRepository;
import maineta.eta.repository.ClienteRepository;
import maineta.eta.repository.ColaboradorRepository;
import maineta.eta.repository.PlanRepository;

/**
 * Implementación del servicio de Planes del Día.
 * 
 * Gestiona la creación y consulta de planes turísticos que agrupan
 * actividades en rutas temáticas.
 */
@Service
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final ActividadRepository actividadRepository;
    private final ClienteRepository clienteRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final IUploadFileService uploadFileService;
    private final UsuarioHelper usuarioHelper;
    private final ObjectMapper objectMapper;

    @Autowired
    public PlanServiceImpl(
            PlanRepository planRepository,
            ActividadRepository actividadRepository,
            ClienteRepository clienteRepository,
            ColaboradorRepository colaboradorRepository,
            IUploadFileService uploadFileService,
            UsuarioHelper usuarioHelper) {
        this.planRepository = planRepository;
        this.actividadRepository = actividadRepository;
        this.clienteRepository = clienteRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.uploadFileService = uploadFileService;
        this.usuarioHelper = usuarioHelper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<PlanDTO> obtenerTop5Recientes() {
        List<Plan> planes = planRepository.findTop5ByPublicoTrueOrderByFechaCreacionDesc();
        return planes.stream()
                .map(this::convertirAPlanDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PlanDTO obtenerPorId(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado con ID: " + id));
        
        // Incrementar vistas
        incrementarVistas(id);
        
        return convertirAPlanDTO(plan);
    }

    @Override
    @Transactional
    public Plan crearPlan(CrearPlanFormDTO form, Long idCreador, String rolCreador) {
        Plan plan = new Plan();
        plan.setTitulo(form.getTitulo());
        plan.setDescripcion(form.getDescripcion());
        plan.setDuracionEstimada(form.getDuracionEstimada());
        plan.setTipo(form.getTipo());
        plan.setFechaCreacion(LocalDateTime.now());
        plan.setPublico(true);

        // Asignar creador según rol
        if ("CLIENTE".equals(rolCreador)) {
            Cliente cliente = clienteRepository.findById(idCreador)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            plan.setClienteCreador(cliente);
        } else if ("COLABORADOR".equals(rolCreador)) {
            Colaborador colaborador = colaboradorRepository.findById(idCreador.intValue())
                    .orElseThrow(() -> new RuntimeException("Colaborador no encontrado"));
            plan.setColaboradorCreador(colaborador);
        }

        // Subir imagen de portada si existe
        if (form.getImagenPortada() != null && !form.getImagenPortada().isEmpty()) {
            try {
                String nombreImagen = uploadFileService.copy(form.getImagenPortada());
                plan.setImagenPortada(nombreImagen);
            } catch (Exception e) {
                throw new RuntimeException("Error al subir imagen de portada: " + e.getMessage());
            }
        }

        // Parsear JSON de actividades
        List<ActividadParaPlan> actividadesData = parsearActividadesJson(form.getActividadesJson());
        
        // Obtener IDs de actividades para batch query
        List<Long> actividadIds = actividadesData.stream()
                .map(ActividadParaPlan::getIdActividad)
                .collect(Collectors.toList());

        // Batch query para evitar N+1
        List<Actividad> actividades = actividadRepository.findAllById(actividadIds);
        
        // Crear mapa para lookup O(1)
        var actividadMap = actividades.stream()
                .collect(Collectors.toMap(Actividad::getIdActividad, a -> a));

        // Crear PlanActividad por cada elemento
        List<PlanActividad> planActividades = new ArrayList<>();
        for (ActividadParaPlan actData : actividadesData) {
            Actividad actividad = actividadMap.get(actData.getIdActividad());
            if (actividad != null) {
                PlanActividad planActividad = new PlanActividad();
                planActividad.setPlan(plan);
                planActividad.setActividad(actividad);
                planActividad.setOrden(actData.getOrden());
                planActividad.setHoraSugerida(actData.getHoraSugerida());
                planActividad.setNotaPersonalizada(actData.getNotaPersonalizada());
                planActividades.add(planActividad);
            }
        }

        plan.setActividades(planActividades);
        
        return planRepository.save(plan);
    }

    @Override
    @Transactional
    public void incrementarVistas(Long idPlan) {
        planRepository.incrementarVistas(idPlan);
    }

    @Override
    public List<PlanDTO> obtenerPlanesPorCliente(Long idCliente) {
        List<Plan> planes = planRepository.findByClienteCreadorIdAndPublicoTrue(idCliente);
        return planes.stream()
                .map(this::convertirAPlanDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlanDTO> obtenerPlanesPorColaborador(Long idColaborador) {
        List<Plan> planes = planRepository.findByColaboradorCreadorIdColaboradorAndPublicoTrue(idColaborador);
        return planes.stream()
                .map(this::convertirAPlanDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una entidad Plan a DTO con toda la información necesaria.
     */
    private PlanDTO convertirAPlanDTO(Plan plan) {
        PlanDTO dto = new PlanDTO();
        dto.setId(plan.getId());
        dto.setTitulo(plan.getTitulo());
        dto.setDescripcion(plan.getDescripcion());
        dto.setImagenPortada(plan.getImagenPortada());
        dto.setDuracionEstimada(plan.getDuracionEstimada());
        dto.setTipo(plan.getTipo());
        dto.setFechaCreacion(plan.getFechaCreacion());
        dto.setVistas(plan.getVistas());

        // Determinar creador
        if (plan.getClienteCreador() != null) {
            dto.setRolCreador("CLIENTE");
            dto.setNombreCreador(plan.getClienteCreador().getUsuario().getNombre());
            dto.setIdCreador(plan.getClienteCreador().getId());
        } else if (plan.getColaboradorCreador() != null) {
            dto.setRolCreador("COLABORADOR");
            dto.setNombreCreador(plan.getColaboradorCreador().getUsuario().getNombre());
            dto.setIdCreador(plan.getColaboradorCreador().getIdColaborador());
        }

        // Convertir actividades
        List<PlanActividadDTO> actividadesDTO = plan.getActividades().stream()
                .map(this::convertirAPlanActividadDTO)
                .collect(Collectors.toList());
        dto.setActividades(actividadesDTO);

        return dto;
    }

    /**
     * Convierte PlanActividad a DTO con precio calculado.
     */
    private PlanActividadDTO convertirAPlanActividadDTO(PlanActividad planActividad) {
        Actividad actividad = planActividad.getActividad();
        
        PlanActividadDTO dto = new PlanActividadDTO();
        dto.setIdActividad(actividad.getIdActividad());
        dto.setNombreActividad(actividad.getTitulo());
        dto.setSlugActividad(generarSlug(actividad.getTitulo()));
        dto.setImagenActividad(actividad.getImagen());
        
        // Calcular precio consumidor con UsuarioHelper
        BigDecimal precioConsumidor = usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio());
        dto.setPrecioConsumidor(precioConsumidor);
        
        dto.setCategoriaActividad(actividad.getCategoria().getNombre());
        dto.setLatitud(actividad.getLatitud());
        dto.setLongitud(actividad.getLongitud());
        dto.setOrden(planActividad.getOrden());
        dto.setHoraSugerida(planActividad.getHoraSugerida());
        dto.setNotaPersonalizada(planActividad.getNotaPersonalizada());
        
        return dto;
    }

    /**
     * Genera un slug a partir del título de la actividad.
     */
    private String generarSlug(String titulo) {
        return titulo.toLowerCase()
                .replaceAll("[áàäâã]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöôõ]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("ñ", "n")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    /**
     * Parsea el JSON de actividades del formulario.
     */
    private List<ActividadParaPlan> parsearActividadesJson(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<List<ActividadParaPlan>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear JSON de actividades: " + e.getMessage());
        }
    }

    /**
     * Clase interna para deserializar el JSON de actividades del formulario.
     */
    private static class ActividadParaPlan {
        private Long idActividad;
        private int orden;
        private String horaSugerida;
        private String notaPersonalizada;

        public Long getIdActividad() { return idActividad; }
        public void setIdActividad(Long idActividad) { this.idActividad = idActividad; }
        
        public int getOrden() { return orden; }
        public void setOrden(int orden) { this.orden = orden; }
        
        public String getHoraSugerida() { return horaSugerida; }
        public void setHoraSugerida(String horaSugerida) { this.horaSugerida = horaSugerida; }
        
        public String getNotaPersonalizada() { return notaPersonalizada; }
        public void setNotaPersonalizada(String notaPersonalizada) { this.notaPersonalizada = notaPersonalizada; }
    }
}
