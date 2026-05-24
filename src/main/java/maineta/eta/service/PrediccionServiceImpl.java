package maineta.eta.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import maineta.eta.dto.PrediccionOcupacionDTO;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.repository.DisponibilidadRepository;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 * Implementación del servicio de predicción de ocupación.
 * 
 * Carga el modelo entrenado de Weka y realiza predicciones basadas en:
 * - Categoría de la actividad
 * - Precio de la actividad (transformado a rango)
 * - Hora de inicio (transformada a rango)
 * - Día de la semana (transformado a tipo)
 * - Cupos totales (transformado a rango)
 */
@Service
public class PrediccionServiceImpl implements PrediccionService {

    @Autowired
    private DisponibilidadRepository disponibilidadRepository;

    private Classifier modelo;

    /**
     * Carga el modelo entrenado al inicializar el servicio.
     * El modelo debe estar en src/main/resources/ETA_modelo_predictivo.model
     */
    @PostConstruct
    public void cargarModelo() {
        try {
            // Cargar modelo desde resources usando ClassPathResource
            ClassPathResource resource = new ClassPathResource("ETA_modelo_predictivo.model");
            if (!resource.exists()) {
                System.err.println(
                        "❌ El archivo del modelo predictivo no existe en src/main/resources/ETA_modelo_predictivo.model");
                return;
            }
            InputStream inputStream = resource.getInputStream();
            modelo = (Classifier) SerializationHelper.read(inputStream);
            System.out.println("✅ Modelo predictivo cargado exitosamente");
            System.out.println("   Tipo de modelo: " + modelo.getClass().getSimpleName());
        } catch (Exception e) {
            System.err.println("❌ Error al cargar el modelo predictivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public PrediccionOcupacionDTO predecirOcupacion(Long idDisponibilidad) {
        try {
            if (modelo == null) {
                System.err.println("⚠️ Modelo no cargado. No se puede predecir ocupación para disponibilidad: "
                        + idDisponibilidad);
                return null;
            }

            // Obtener la disponibilidad
            Disponibilidad disponibilidad = disponibilidadRepository.findById(idDisponibilidad)
                    .orElseThrow(
                            () -> new RuntimeException("Disponibilidad no encontrada con ID: " + idDisponibilidad));

            Actividad actividad = disponibilidad.getActividad();

            // Transformar los datos
            String categoria = transformarCategoria(actividad.getCategoria().getNombre());
            String rangoPrecio = transformarPrecio(actividad.getPrecio());
            String rangoHora = transformarHora(disponibilidad.getHoraInicio());
            String tipoDia = transformarDia(disponibilidad.getFecha());
            String rangoCupos = transformarCupos(disponibilidad.getCuposTotales());

            System.out.println("🔮 Prediciendo ocupación para disponibilidad " + idDisponibilidad +
                    ":\n   Categoría: " + categoria + "\n   Precio: " + rangoPrecio +
                    "\n   Hora: " + rangoHora + "\n   Día: " + tipoDia + "\n   Cupos: " + rangoCupos);

            // Realizar predicción
            return predecirOcupacionManual(categoria, rangoPrecio, rangoHora, tipoDia, rangoCupos);
        } catch (Exception e) {
            System.err.println(
                    "❌ Error al predecir ocupación para disponibilidad " + idDisponibilidad + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public PrediccionOcupacionDTO predecirOcupacionManual(
            String categoria,
            String rangoPrecio,
            String rangoHora,
            String tipoDia,
            String rangoCupos) {

        try {
            if (modelo == null) {
                throw new RuntimeException(
                        "Modelo predictivo no está cargado. Verifique que el archivo ETA_modelo_predictivo.model existe en resources");
            }

            // Validar que los valores sean válidos
            if (categoria == null || categoria.isEmpty()) {
                throw new IllegalArgumentException("Categoría no puede ser nula o vacía");
            }
            if (rangoPrecio == null || rangoPrecio.isEmpty()) {
                throw new IllegalArgumentException("Rango de precio no puede ser nulo o vacío");
            }
            if (rangoHora == null || rangoHora.isEmpty()) {
                throw new IllegalArgumentException("Rango de hora no puede ser nulo o vacío");
            }
            if (tipoDia == null || tipoDia.isEmpty()) {
                throw new IllegalArgumentException("Tipo de día no puede ser nulo o vacío");
            }
            if (rangoCupos == null || rangoCupos.isEmpty()) {
                throw new IllegalArgumentException("Rango de cupos no puede ser nulo o vacío");
            }

            // Crear instancia para Weka
            Instance instancia = crearInstancia(categoria, rangoPrecio, rangoHora, tipoDia, rangoCupos);

            // Realizar predicción
            double resultado = modelo.classifyInstance(instancia);
            String nivelPredicho = instancia.classAttribute().value((int) resultado);

            // Obtener distribución de probabilidades
            double[] distribucion = modelo.distributionForInstance(instancia);
            double confianza = distribucion[(int) resultado];

            // Crear DTO de respuesta
            PrediccionOcupacionDTO dto = new PrediccionOcupacionDTO();
            dto.setNivelOcupacion(nivelPredicho);
            dto.setConfianza(confianza);
            dto.setCategoria(categoria);
            dto.setRangoPrecio(rangoPrecio);
            dto.setRangoHora(rangoHora);
            dto.setTipoDia(tipoDia);
            dto.setRangoCupos(rangoCupos);

            System.out.println("✅ Predicción realizada: " + nivelPredicho + " (confianza: " +
                    String.format("%.2f", confianza * 100) + "%)");

            return dto;

        } catch (Exception e) {
            System.err.println("❌ Error al realizar predicción manual: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al predecir ocupación", e);
        }
    }

    /**
     * Crea una instancia de Weka con los atributos necesarios para el modelo.
     * Valida que los valores existan en las opciones disponibles.
     */
    private Instance crearInstancia(String categoria, String rangoPrecio, String rangoHora,
            String tipoDia, String rangoCupos) {

        ArrayList<Attribute> atributos = new ArrayList<>();

        // 1. Atributo: categoria
        ArrayList<String> categorias = new ArrayList<>();
        categorias.add("playa");
        categorias.add("gastronomia");
        categorias.add("historia");
        categorias.add("entretenimiento");
        categorias.add("cultura");
        categorias.add("vida_nocturna");

        if (!categorias.contains(categoria)) {
            System.err.println("⚠️ Categoría '" + categoria + "' no válida. Opciones: " + categorias);
            categoria = "entretenimiento"; // Default
        }
        atributos.add(new Attribute("categoria_actividad", categorias));

        // 2. Atributo: rango_precio
        ArrayList<String> precios = new ArrayList<>();
        precios.add("bajo");
        precios.add("medio");
        precios.add("alto");

        if (!precios.contains(rangoPrecio)) {
            System.err.println("⚠️ Rango precio '" + rangoPrecio + "' no válido. Opciones: " + precios);
            rangoPrecio = "medio"; // Default
        }
        atributos.add(new Attribute("rango_precio", precios));

        // 3. Atributo: rango_hora
        ArrayList<String> horas = new ArrayList<>();
        horas.add("manana");
        horas.add("tarde");
        horas.add("noche");

        if (!horas.contains(rangoHora)) {
            System.err.println("⚠️ Rango hora '" + rangoHora + "' no válido. Opciones: " + horas);
            rangoHora = "tarde"; // Default
        }
        atributos.add(new Attribute("rango_horario", horas));

        // 4. Atributo: tipo_dia
        ArrayList<String> dias = new ArrayList<>();
        dias.add("fin_de_semana");
        dias.add("entre_semana");

        if (!dias.contains(tipoDia)) {
            System.err.println("⚠️ Tipo día '" + tipoDia + "' no válido. Opciones: " + dias);
            tipoDia = "entre_semana"; // Default
        }
        atributos.add(new Attribute("tipo_dia", dias));

        // 5. Atributo: rango_cupos
        ArrayList<String> cupos = new ArrayList<>();
        cupos.add("baja");
        cupos.add("media");
        cupos.add("alta");

        if (!cupos.contains(rangoCupos)) {
            System.err.println("⚠️ Rango cupos '" + rangoCupos + "' no válido. Opciones: " + cupos);
            rangoCupos = "media"; // Default
        }
        atributos.add(new Attribute("rango_cupos_totales", cupos));

        // 6. Atributo clase: nivel_ocupacion
        ArrayList<String> clases = new ArrayList<>();
        clases.add("Baja");
        clases.add("Media");
        clases.add("Alta");
        clases.add("Agotado");
        atributos.add(new Attribute("nivel_ocupacion", clases));

        // Crear dataset
        Instances dataset = new Instances("nivel_ocupacion_actividades", atributos, 0);
        dataset.setClassIndex(dataset.numAttributes() - 1);

        // Crear instancia
        DenseInstance instancia = new DenseInstance(dataset.numAttributes());
        instancia.setDataset(dataset);

        // Asignar valores
        instancia.setValue(atributos.get(0), categoria);
        instancia.setValue(atributos.get(1), rangoPrecio);
        instancia.setValue(atributos.get(2), rangoHora);
        instancia.setValue(atributos.get(3), tipoDia);
        instancia.setValue(atributos.get(4), rangoCupos);

        System.out.println("   Instancia Weka creada: [" + categoria + ", " + rangoPrecio + ", " +
                rangoHora + ", " + tipoDia + ", " + rangoCupos + "]");

        return instancia;
    }

    // ==================== TRANSFORMACIONES ====================

    /**
     * Transforma el nombre de la categoría a minúsculas y normaliza
     */
    private String transformarCategoria(String nombreCategoria) {
        if (nombreCategoria == null) {
            System.err.println("⚠️ Nombre de categoría es nulo, usando 'entretenimiento' como default");
            return "entretenimiento";
        }

        String categoria = nombreCategoria.toLowerCase().trim();

        // Normalizar categorías conocidas para que coincidan con el modelo Weka
        if (categoria.contains("playa") || categoria.contains("mar") || categoria.contains("isla")) {
            return "playa";
        } else if (categoria.contains("gastronom") || categoria.contains("comida") || categoria.contains("food")) {
            return "gastronomia";
        } else if (categoria.contains("historia") || categoria.contains("historico") || categoria.contains("museo")) {
            return "historia";
        } else if (categoria.contains("cultura") || categoria.contains("culture") || categoria.contains("arte")) {
            return "cultura";
        } else if (categoria.contains("nocturna") || categoria.contains("fiesta") || categoria.contains("bar")
                || categoria.contains("club")) {
            return "vida_nocturna";
        } else if (categoria.contains("entretenimiento") || categoria.contains("show") || categoria.contains("tour")
                || categoria.contains("aventura") || categoria.contains("naturaleza") || categoria.contains("deporte")
                || categoria.contains("bienestar")) {
            return "entretenimiento";
        }

        // Si no coincide con ninguna, buscar la más cercana
        System.out.println("⚠️ Categoría '" + nombreCategoria + "' no mapea directamente, asignando 'entretenimiento'");
        return "entretenimiento";
    }

    /**
     * Transforma el precio a rango: bajo, medio, alto
     * 
     * - bajo: < 50,000
     * - medio: 50,000 - 150,000
     * - alto: > 150,000
     */
    private String transformarPrecio(BigDecimal precio) {
        BigDecimal UMBRAL_BAJO = new BigDecimal("50000");
        BigDecimal UMBRAL_MEDIO = new BigDecimal("150000");

        if (precio.compareTo(UMBRAL_BAJO) < 0) {
            return "bajo";
        } else if (precio.compareTo(UMBRAL_MEDIO) <= 0) {
            return "medio";
        } else {
            return "alto";
        }
    }

    /**
     * Transforma la hora de inicio a rango: manana, tarde, noche
     * 
     * - manana: 06:00 - 11:59
     * - tarde: 12:00 - 17:59
     * - noche: 18:00 - 23:59
     */
    private String transformarHora(LocalTime horaInicio) {
        int hora = horaInicio.getHour();

        if (hora >= 6 && hora < 12) {
            return "manana";
        } else if (hora >= 12 && hora < 18) {
            return "tarde";
        } else {
            return "noche";
        }
    }

    /**
     * Transforma la fecha a tipo de día: entre_semana, fin_de_semana
     * 
     * - fin_de_semana: sábado, domingo
     * - entre_semana: lunes a viernes
     */
    private String transformarDia(LocalDate fecha) {
        DayOfWeek diaSemana = fecha.getDayOfWeek();

        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            return "fin_de_semana";
        } else {
            return "entre_semana";
        }
    }

    /**
     * Transforma los cupos totales a rango: baja, media, alta
     * 
     * - baja: 1 - 10 personas
     * - media: 11 - 30 personas
     * - alta: > 30 personas
     */
    private String transformarCupos(int cuposTotales) {
        if (cuposTotales <= 10) {
            return "baja";
        } else if (cuposTotales <= 30) {
            return "media";
        } else {
            return "alta";
        }
    }
}
