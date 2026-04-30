package maineta.eta.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import maineta.eta.config.UsuarioHelper;
import maineta.eta.dto.ChatMensajeRequestDTO;
import maineta.eta.dto.ChatMensajeResponseDTO;
import maineta.eta.dto.ChatRecomendacionDTO;
import maineta.eta.dto.MensajeDTO;
import maineta.eta.entity.Actividad;

/**
 * Servicio principal del ChatBot ETA Assistant.
 *
 * Responsabilidades:
 * 1. Construir contexto de actividades relevantes consultando ActividadService.
 * 2. Armar el system prompt con las reglas del bot y el contexto de actividades.
 * 3. Construir la cadena [system + historial + mensaje actual] y llamar al LLM.
 * 4. Devolver la respuesta al controller.
 * 5. Generar recomendaciones inteligentes con filtros estructurados.
 */
@Service
public class ChatBotService {

    private final ChatClient chatClient;
    private final ActividadService actividadService;
    private final UsuarioHelper usuarioHelper;
    private final ChatBotRecomendacionService recomendacionService;

    @Value("${eta.chat.max-historial:10}")
    private int maxHistorial;

    @Value("${eta.chat.max-actividades-contexto:5}")
    private int maxActividadesContexto;

    private static final String SYSTEM_PROMPT_TEMPLATE =
            "Eres \"ETA Assistant\", el asistente virtual de ETA App, una plataforma de " +
            "actividades turísticas en Cartagena de Indias, Colombia.\n\n" +
            "TU PROPÓSITO:\n" +
            "- Ayudar a los usuarios a descubrir y elegir actividades turísticas disponibles " +
            "en la plataforma ETA.\n" +
            "- Responder preguntas sobre Cartagena de Indias: zonas, clima, transporte, " +
            "gastronomía, cultura, historia, playas, seguridad y tips de viaje.\n" +
            "- Explicar cómo funciona la plataforma ETA (registro, reservas, favoritos, planes).\n\n" +
            "REGLAS ABSOLUTAS:\n" +
            "1. SOLO hablas de Cartagena de Indias y de las actividades de ETA App.\n" +
            "2. Si el usuario pregunta algo fuera de ese alcance (política, recetas de otro país, " +
            "matemáticas, tecnología, etc.), responde amablemente: " +
            "\"Solo puedo ayudarte con actividades en Cartagena y temas de la ciudad 😊 " +
            "¿En qué te puedo orientar?\"\n" +
            "3. Cuando recomiendes actividades, usa EXCLUSIVAMENTE las que aparecen en el " +
            "CONTEXTO DE ACTIVIDADES que se te proporciona más abajo. Nunca inventes " +
            "actividades, precios ni calificaciones.\n" +
            "4. Sé conciso: respuestas de máximo 3-4 oraciones o una lista corta de 3 ítems.\n" +
            "5. Tono: amigable, cálido, como un guía local experto. Usa emojis con moderación.\n" +
            "6. Idioma: responde siempre en español, aunque el usuario escriba en otro idioma.\n" +
            "7. Si recomiendas buscar actividades, termina con una frase motivadora como: " +
            "\"¿Te gustaría ver las opciones disponibles?\" o \"Puedo mostrarte lo que tenemos\".\n\n" +
            "CONTEXTO DE ACTIVIDADES DISPONIBLES HOY:\n" +
            "%s\n\n" +
            "Si no hay actividades relevantes en el contexto, dile al usuario que explore el " +
            "catálogo completo en la sección de búsqueda.";

    public ChatBotService(ChatClient chatClient, ActividadService actividadService,
            UsuarioHelper usuarioHelper, ChatBotRecomendacionService recomendacionService) {
        this.chatClient = chatClient;
        this.actividadService = actividadService;
        this.usuarioHelper = usuarioHelper;
        this.recomendacionService = recomendacionService;
    }

    /**
     * Procesa un mensaje del usuario y devuelve la respuesta del asistente.
     */
    public ChatMensajeResponseDTO procesarMensaje(ChatMensajeRequestDTO request) {
        String respuesta = generarRespuesta(request);
        return ChatMensajeResponseDTO.ok(respuesta);
    }

    /**
     * Procesa un mensaje y genera recomendación con filtros estructurados.
     */
    public ChatRecomendacionDTO procesarConRecomendacion(ChatMensajeRequestDTO request) {
        String respuesta = generarRespuesta(request);
        
        // Analizar el mensaje para detectar intención de búsqueda
        return recomendacionService.analizarYGenerarRecomendacion(request.getMensaje(), respuesta);
    }

    /**
     * Genera la respuesta del asistente llamando al LLM.
     */
    private String generarRespuesta(ChatMensajeRequestDTO request) {
        // 1. Construir contexto de actividades relevantes
        String actividadesContexto = construirContextoActividades(
                request.getMensaje(), request.getContextoActividad());

        // 2. System prompt con contexto de actividades
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, actividadesContexto);

        // 3. Armar lista de mensajes: [system] + historial (truncado) + [mensaje actual]
        List<Message> mensajes = new ArrayList<>();
        mensajes.add(new SystemMessage(systemPrompt));

        List<MensajeDTO> historial = request.getHistorial();
        if (historial != null) {
            // Truncar al máximo permitido (cada turno = 2 mensajes: user + assistant)
            int inicio = Math.max(0, historial.size() - (maxHistorial * 2));
            for (int i = inicio; i < historial.size(); i++) {
                MensajeDTO m = historial.get(i);
                if ("user".equals(m.getRol())) {
                    mensajes.add(new UserMessage(m.getContenido()));
                } else if ("assistant".equals(m.getRol())) {
                    mensajes.add(new AssistantMessage(m.getContenido()));
                }
            }
        }

        // Mensaje actual del usuario
        mensajes.add(new UserMessage(request.getMensaje()));

        // 4. Llamar al LLM (bloqueante en v1)
        return chatClient.prompt(new Prompt(mensajes))
                .call()
                .content();
    }

    /**
     * Construye el bloque de texto de actividades para el system prompt.
     */
    private String construirContextoActividades(String mensajeUsuario, Long contextoActividadId) {
        StringBuilder sb = new StringBuilder();

        // Si hay actividad de contexto explícita, mostrarla primero
        if (contextoActividadId != null) {
            try {
                Actividad actividadContexto = actividadService.obtenerPorId(contextoActividadId);
                if (actividadContexto != null) {
                    sb.append(formatearActividad(actividadContexto)).append("\n");
                }
            } catch (Exception ignored) {
                // Si no se encuentra la actividad, continuar sin ella
            }
        }

        // Buscar actividades relacionadas al mensaje del usuario
        try {
            String keywords = extraerKeywords(mensajeUsuario);
            Page<Actividad> resultados = actividadService.buscarConFiltros(
                    keywords, null, null, null, null, 0, maxActividadesContexto);

            if (resultados.isEmpty()) {
                // Fallback: actividades más recientes/destacadas
                resultados = actividadService.buscarConFiltros(
                        null, null, null, null, null, 0, maxActividadesContexto);
            }

            for (Actividad actividad : resultados.getContent()) {
                // Evitar duplicado si ya se agregó la actividad de contexto
                if (contextoActividadId != null
                        && actividad.getIdActividad().equals(contextoActividadId)) {
                    continue;
                }
                sb.append(formatearActividad(actividad)).append("\n");
            }
        } catch (Exception ignored) {
            // Continuar con lo que haya si falla la consulta
        }

        if (sb.isEmpty()) {
            return "(Sin actividades disponibles en este momento)";
        }
        return sb.toString().trim();
    }

    /**
     * Formatea una Actividad como línea de texto para el system prompt.
     * Siempre usa precio consumidor (BOT-07).
     */
    private String formatearActividad(Actividad actividad) {
        BigDecimal precioConsumidor = usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio());
        String categoria = actividad.getCategoria() != null
                ? actividad.getCategoria().getNombre() : "General";
        String idioma = actividad.getIdioma() != null
                ? actividad.getIdioma().getNombre() : "Español";
        String calificacion = String.valueOf(actividad.getCalificacion());

        return String.format("- \"%s\" | Categoría: %s | Precio: $%,.0f COP | Calificación: %s | Idioma: %s",
                actividad.getTitulo(), categoria, precioConsumidor, calificacion, idioma);
    }

    /**
     * Extrae palabras clave del mensaje para usar en la búsqueda de actividades.
     */
    private String extraerKeywords(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) return null;

        String[] stopWords = {
            "que", "qué", "hay", "para", "con", "los", "las", "una", "uno",
            "como", "cuál", "cual", "tienen", "me", "mi", "se", "de", "el",
            "la", "en", "es", "son", "un", "su", "del", "por", "más"
        };

        String[] palabras = mensaje.toLowerCase()
                .replaceAll("[^a-záéíóúñü\\s]", "")
                .split("\\s+");

        StringBuilder keywords = new StringBuilder();
        for (String palabra : palabras) {
            if (palabra.length() > 3 && !esPalabraDeParada(palabra, stopWords)) {
                if (!keywords.isEmpty()) keywords.append(" ");
                keywords.append(palabra);
            }
        }
        return keywords.isEmpty() ? null : keywords.toString();
    }

    private boolean esPalabraDeParada(String palabra, String[] stopWords) {
        for (String stop : stopWords) {
            if (stop.equals(palabra)) return true;
        }
        return false;
    }
}

