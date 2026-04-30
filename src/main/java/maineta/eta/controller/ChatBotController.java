package maineta.eta.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.validation.Valid;
import maineta.eta.dto.ChatMensajeRequestDTO;
import maineta.eta.dto.ChatMensajeResponseDTO;
import maineta.eta.dto.ChatRecomendacionDTO;
import maineta.eta.service.ChatBotService;

/**
 * Controlador del ChatBot ETA Assistant.
 * Devuelve fragmentos Thymeleaf (HTML parcial) para ser insertados directamente
 * en el DOM del widget de chat.
 *
 * Rutas:
 * - POST /chat/mensaje → Fragmento HTML con la respuesta del bot
 * - POST /chat/recomendar → JSON con recomendación estructurada (filtros + respuesta)
 *
 * Acceso: público (ver SecurityConfig - /chat/**)
 */
@Controller
@RequestMapping("/chat")
public class ChatBotController {

    private final ChatBotService chatBotService;

    public ChatBotController(ChatBotService chatBotService) {
        this.chatBotService = chatBotService;
    }

    /**
     * Procesa un mensaje y retorna un fragmento Thymeleaf con la burbuja de respuesta.
     * El JS del widget inserta el HTML devuelto directamente en el área de mensajes.
     */
    @PostMapping("/mensaje")
    public String procesarMensaje(
            @Valid @RequestBody ChatMensajeRequestDTO request,
            BindingResult binding,
            Model model) {

        if (binding.hasErrors()) {
            model.addAttribute("error", "Mensaje inválido.");
            return "componentes/chat-respuesta :: mensajeError";
        }

        try {
            ChatMensajeResponseDTO response = chatBotService.procesarMensaje(request);
            model.addAttribute("respuesta", response.getRespuesta());
            return "componentes/chat-respuesta :: mensajeBot";
        } catch (Exception e) {
            // Nunca exponer el error técnico al usuario (BOT-06)
            model.addAttribute("error",
                    "El asistente no está disponible en este momento. Intenta más tarde.");
            return "componentes/chat-respuesta :: mensajeError";
        }
    }

    /**
     * Endpoint JSON para obtener recomendaciones inteligentes con filtros estructurados.
     * Retorna la respuesta del bot + filtros para redirigir a /actividades/buscar.
     * 
     * Usado por el widget de chat para detectar recomendaciones y mostrar botones de acción.
     */
    @PostMapping("/recomendar")
    @ResponseBody
    public ResponseEntity<ChatRecomendacionDTO> procesarConRecomendacion(
            @Valid @RequestBody ChatMensajeRequestDTO request,
            BindingResult binding) {

        if (binding.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ChatRecomendacionDTO.sinRecomendacion("Mensaje inválido"));
        }

        try {
            ChatRecomendacionDTO recomendacion = chatBotService.procesarConRecomendacion(request);
            return ResponseEntity.ok(recomendacion);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ChatRecomendacionDTO.sinRecomendacion(
                            "El asistente no está disponible en este momento."));
        }
    }
}


