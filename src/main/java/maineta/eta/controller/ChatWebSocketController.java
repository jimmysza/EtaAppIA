package maineta.eta.controller;

import java.security.Principal;
import java.util.Objects;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import maineta.eta.dto.ChatInboundMessage;
import maineta.eta.dto.ChatMessagePayload;
import maineta.eta.entity.MensajeChat;
import maineta.eta.service.ChatService;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(ChatInboundMessage inboundMessage, Principal principal) {
        if (principal == null || inboundMessage == null || inboundMessage.getConversacionId() == null) {
            return;
        }

        Long conversacionId = Objects.requireNonNull(inboundMessage.getConversacionId());

        MensajeChat mensaje = chatService.enviarMensaje(
                conversacionId,
                inboundMessage.getContenido(),
                principal.getName());

        ChatMessagePayload payload = ChatMessagePayload.from(mensaje);
        messagingTemplate.convertAndSend(
            "/topic/chat." + mensaje.getConversacion().getIdConversacion(),
            Objects.requireNonNull(payload));
    }
}
