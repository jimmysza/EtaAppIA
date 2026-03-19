package maineta.eta.dto;

import lombok.Data;

@Data
public class ChatInboundMessage {
    private Long conversacionId;
    private String contenido;
}
