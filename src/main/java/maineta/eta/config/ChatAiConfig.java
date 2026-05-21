package maineta.eta.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Spring AI para ETA Chatbot.
 * Define beans condicionales según la propiedad eta.chat.provider.
 * 
 * Proveedores soportados:
 * - openai (por defecto): Usa GPT-4 o GPT-3.5
 * - anthropic: Usa Claude
 * - groq: Usa Groq (Llama 3, etc.)
 */
@Configuration
public class ChatAiConfig {

    @Value("${eta.chat.groq.api-key:gsk_placeholder}")
    private String groqApiKey;

    @Value("${eta.chat.groq.model:llama-3.1-8b-instant}")
    private String groqModel;

    @Value("${eta.chat.groq.base-url:https://api.groq.com/openai/v1}")
    private String groqBaseUrl;

    @Value("${eta.chat.groq.temperature:0.7}")
    private Double groqTemperature;

    @Value("${eta.chat.groq.max-tokens:500}")
    private Integer groqMaxTokens;

    @Bean
    @ConditionalOnProperty(name = "eta.chat.provider", havingValue = "openai", matchIfMissing = true)
    public ChatClient openAiChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model).build();
    }

    @Bean
    @ConditionalOnProperty(name = "eta.chat.provider", havingValue = "anthropic")
    public ChatClient anthropicChatClient(AnthropicChatModel model) {
        return ChatClient.builder(model).build();
    }

    @Bean
    @ConditionalOnProperty(name = "eta.chat.provider", havingValue = "groq")
    public ChatClient groqChatClient() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(groqBaseUrl)
                .apiKey(groqApiKey)
                .build();
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(groqModel)
                .temperature(groqTemperature)
                .maxTokens(groqMaxTokens)
                .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
                
        return ChatClient.builder(model).build();
    }
}
