package maineta.eta.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Spring AI.
 * Selecciona el ChatClient activo según la propiedad eta.chat.provider.
 * Solo uno de los dos beans estará activo en runtime.
 */
@Configuration
public class ChatAiConfig {

    @Bean
    @ConditionalOnProperty(name = "eta.chat.provider", havingValue = "openai")
    public ChatClient openAiChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model).build();
    }

    @Bean
    @ConditionalOnProperty(name = "eta.chat.provider", havingValue = "anthropic")
    public ChatClient anthropicChatClient(AnthropicChatModel model) {
        return ChatClient.builder(model).build();
    }
}
