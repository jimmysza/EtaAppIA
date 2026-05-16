# Guía de Migración a Google Gemini (Futuro)

**Estado**: Spring AI 1.0.0 NO incluye soporte para Gemini  
**Versión Estimada**: Spring AI 1.1.0+ o 1.2.0+

---

## POR QUÉ NO FUNCIONA AHORA

### Error Actual
```
Could not find artifact org.springframework.ai:spring-ai-vertex-ai-gemini-spring-boot-starter:jar:1.0.0
```

### Explicación
Spring AI 1.0.0 (BOM version actual) solo incluye:
- ✅ `spring-ai-openai-spring-boot-starter`
- ✅ `spring-ai-anthropic-spring-boot-starter`
- ❌ `spring-ai-vertex-ai-gemini-spring-boot-starter` ← NO EXISTE

**Estado oficial**: Gemini support está en desarrollo  
**Fuentes**: 
- Spring AI GitHub Issues
- Spring AI Documentation (1.0.0 release notes)

---

## CUÁNDO ESTARÁ DISPONIBLE

### Opciones Actuales (No Recomendadas para Producción)

1. **Usar versión snapshot** (inestable):
   ```xml
   <dependency>
       <groupId>org.springframework.ai</groupId>
       <artifactId>spring-ai-vertex-ai-gemini</artifactId>
       <version>1.1.0-SNAPSHOT</version>
   </dependency>
   ```
   **Riesgos**: 
   - Cambios de API frecuentes
   - Bugs no resueltos
   - Requiere repo snapshot de Spring

2. **Integración manual con Google AI SDK**:
   ```xml
   <dependency>
       <groupId>com.google.ai.client.generativeai</groupId>
       <artifactId>generativeai</artifactId>
       <version>0.7.2</version>
   </dependency>
   ```
   **Problema**: No usa abstracción de Spring AI (ChatClient)

### Recomendación
**Esperar a Spring AI 1.1.0 Release estable** (estimado Q2 2026)

---

## PASOS PARA MIGRAR CUANDO ESTÉ DISPONIBLE

### Paso 1: Actualizar Spring AI BOM

**pom.xml** (cambiar versión):
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.1.0</version> <!-- O la versión que soporte Gemini -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Paso 2: Agregar Dependencia de Gemini

**pom.xml** (en la sección `<dependencies>`):
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-vertex-ai-gemini-spring-boot-starter</artifactId>
    <!-- No especificar version, la hereda del BOM -->
</dependency>
```

### Paso 3: Configurar Google Cloud Project

#### 3.1 Crear Proyecto en Google Cloud
```bash
# Instalar gcloud CLI
# https://cloud.google.com/sdk/docs/install

# Crear proyecto
gcloud projects create eta-app-ia --name="ETA App IA"

# Configurar proyecto activo
gcloud config set project eta-app-ia
```

#### 3.2 Habilitar Vertex AI API
```bash
gcloud services enable aiplatform.googleapis.com
```

#### 3.3 Crear Service Account
```bash
# Crear cuenta de servicio
gcloud iam service-accounts create eta-gemini-sa \
    --display-name="ETA Gemini Service Account"

# Asignar permisos
gcloud projects add-iam-policy-binding eta-app-ia \
    --member="serviceAccount:eta-gemini-sa@eta-app-ia.iam.gserviceaccount.com" \
    --role="roles/aiplatform.user"

# Generar JSON key
gcloud iam service-accounts keys create gemini-key.json \
    --iam-account=eta-gemini-sa@eta-app-ia.iam.gserviceaccount.com
```

#### 3.4 Configurar Autenticación Local
```bash
# Windows PowerShell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\gemini-key.json"

# Linux/Mac
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/gemini-key.json"
```

### Paso 4: Agregar Configuración en application.properties

```properties
# === GEMINI CONFIGURATION ===
spring.ai.vertex.ai.gemini.project-id=${GEMINI_PROJECT_ID:eta-app-ia}
spring.ai.vertex.ai.gemini.location=us-central1

# Opciones del modelo Gemini
spring.ai.vertex.ai.gemini.chat.options.model=gemini-1.5-flash
spring.ai.vertex.ai.gemini.chat.options.temperature=0.7
spring.ai.vertex.ai.gemini.chat.options.max-output-tokens=500
spring.ai.vertex.ai.gemini.chat.options.top-p=0.95
spring.ai.vertex.ai.gemini.chat.options.top-k=40

# Activar Gemini como proveedor
eta.chat.provider=gemini
```

**Variables de entorno requeridas:**
```bash
GEMINI_PROJECT_ID=eta-app-ia
GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\gemini-key.json
```

### Paso 5: Actualizar ChatAiConfig.java

**Agregar import:**
```java
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
```

**Agregar bean de Gemini:**
```java
@Bean
@ConditionalOnProperty(name = "eta.chat.provider", havingValue = "gemini")
public ChatClient geminiChatClient(VertexAiGeminiChatModel model) {
    return ChatClient.builder(model)
        .defaultSystem("""
            Eres "ETA Assistant", el asistente virtual de ETA App.
            Especialízate en actividades turísticas de Cartagena, Colombia.
            """)
        .build();
}
```

**Archivo completo**:
```java
package maineta.eta.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatAiConfig {

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
    @ConditionalOnProperty(name = "eta.chat.provider", havingValue = "gemini")
    public ChatClient geminiChatClient(VertexAiGeminiChatModel model) {
        return ChatClient.builder(model).build();
    }
}
```

### Paso 6: Compilar y Probar

```bash
# Limpiar y compilar
mvnw.cmd clean compile

# Verificar que no haya errores
mvnw.cmd test -Dtest=ChatBotServiceTest

# Iniciar aplicación
mvnw.cmd spring-boot:run
```

### Paso 7: Validar Integración

**Test manual en chat widget:**
```
Usuario: "Hola, ¿qué puedes hacer?"
Bot: [Respuesta generada por Gemini]
```

**Verificar logs**:
```
INFO  maineta.eta.service.ChatBotService : Usando proveedor: gemini
INFO  maineta.eta.service.ChatBotService : Modelo: gemini-1.5-flash
```

---

## COMPARACIÓN DE PROVEEDORES

| Característica | OpenAI (GPT-4o-mini) | Anthropic (Claude Haiku) | Gemini (Flash) |
|---------------|---------------------|-------------------------|---------------|
| **Velocidad** | ⚡⚡⚡ Rápido | ⚡⚡⚡⚡ Muy rápido | ⚡⚡⚡⚡ Muy rápido |
| **Costo** | $0.15 / 1M tokens | $0.25 / 1M tokens | **$0.075 / 1M tokens** 🏆 |
| **Español** | ⭐⭐⭐⭐ Excelente | ⭐⭐⭐⭐⭐ Nativo | ⭐⭐⭐⭐ Muy bueno |
| **Context Window** | 128K tokens | 200K tokens | **1M tokens** 🏆 |
| **Multimodal** | ✅ Sí (imágenes) | ✅ Sí (imágenes) | ✅ Sí (imágenes + video) |
| **JSON Mode** | ✅ Nativo | ⚠️ Via prompt | ✅ Nativo |
| **Disponibilidad** | ✅ Global | ✅ Global | ⚠️ Requiere Google Cloud |
| **Setup** | 🟢 Fácil (solo API key) | 🟢 Fácil (solo API key) | 🟡 Complejo (GCP project) |

### Recomendación de Uso

**Gemini es ideal para ETA App si**:
- ✅ Tienes presupuesto limitado (mitad del costo de OpenAI)
- ✅ Necesitas procesar historial largo de chat (1M tokens)
- ✅ Ya usas Google Cloud para otros servicios
- ✅ Quieres integrar análisis de imágenes de actividades (futuro)

**Mantener Claude/OpenAI si**:
- ✅ Quieres setup simple sin infraestructura de Google Cloud
- ✅ Necesitas máxima calidad en español (Claude Haiku es superior)
- ✅ No quieres depender de un solo proveedor de cloud

---

## VENTAJAS DE GEMINI PARA ETA APP

### 1. Procesamiento de Imágenes (Futuro Feature)
```java
// Cuando se implemente:
public String analizarImagenActividad(MultipartFile imagen) {
    ChatResponse response = chatClient.prompt()
        .user(u -> u
            .text("Describe esta actividad turística en Cartagena")
            .media(MimeTypeUtils.IMAGE_JPEG, imagen.getBytes())
        )
        .call()
        .chatResponse();
    
    return response.getResult().getOutput().getContent();
}
```

**Casos de uso**:
- Validar que fotos de actividades coincidan con categoría
- Generar descripciones automáticas a partir de imágenes
- Sugerir categorías basadas en contenido visual

### 2. Context Window de 1M Tokens
- Procesar historial completo de conversación de un cliente
- Analizar todas las actividades de un colaborador de una vez
- Generar reportes de KPIs usando todos los datos históricos

### 3. Multimodalidad (Video)
```java
// Futuro: Analizar videos de actividades
public String generarDescripcionDesdeVideo(String videoUrl) {
    // Gemini puede procesar videos de YouTube directamente
    return chatClient.prompt()
        .user(u -> u
            .text("Resume este video de la actividad turística")
            .media(MimeTypeUtils.parse("video/mp4"), videoUrl)
        )
        .call()
        .content();
}
```

### 4. Integración con Vertex AI Features
- **AutoML**: Entrenar modelo personalizado con actividades de ETA
- **Vector Search**: Búsqueda semántica de actividades similares
- **Embeddings**: Recomendaciones basadas en similitud de texto

---

## COSTOS ESTIMADOS (Producción)

### Escenario: 1000 usuarios/día, 5 mensajes promedio

**Tokens por conversación**:
- System prompt: ~200 tokens
- Historial (5 mensajes): ~500 tokens
- Respuesta generada: ~200 tokens
- **Total**: ~900 tokens por conversación

**Cálculo mensual**:
- 1000 usuarios × 30 días × 900 tokens = 27M tokens/mes

| Proveedor | Costo Mensual |
|-----------|--------------|
| OpenAI GPT-4o-mini | $4.05 |
| Anthropic Claude Haiku | $6.75 |
| **Gemini Flash** | **$2.03** 🏆 |

**Ahorro con Gemini**: ~$2/mes vs OpenAI, ~$4.72/mes vs Claude

---

## TROUBLESHOOTING GEMINI

### Error: "Could not load private key from stream"
**Causa**: Service account JSON inválido o mal configurado

**Solución**:
```bash
# Verificar que la variable apunte al archivo correcto
echo $env:GOOGLE_APPLICATION_CREDENTIALS

# Re-generar key si es necesario
gcloud iam service-accounts keys create new-key.json \
    --iam-account=eta-gemini-sa@eta-app-ia.iam.gserviceaccount.com
```

### Error: "Permission denied on project"
**Causa**: Service account no tiene permisos de Vertex AI

**Solución**:
```bash
gcloud projects add-iam-policy-binding eta-app-ia \
    --member="serviceAccount:eta-gemini-sa@eta-app-ia.iam.gserviceaccount.com" \
    --role="roles/aiplatform.user"
```

### Error: "API aiplatform.googleapis.com is not enabled"
**Solución**:
```bash
gcloud services enable aiplatform.googleapis.com
```

### Error: "RESOURCE_EXHAUSTED: Quota exceeded"
**Causa**: Límite de requests por minuto alcanzado (60 RPM en free tier)

**Solución**:
1. Implementar rate limiting en backend:
   ```java
   @Bean
   public RateLimiter chatRateLimiter() {
       return RateLimiter.create(1.0); // 1 request/segundo
   }
   ```

2. O solicitar aumento de cuota en Google Cloud Console

---

## ROADMAP DE IMPLEMENTACIÓN

### Fase 1: Validación (Cuando Gemini esté disponible)
- [ ] Verificar Spring AI 1.1.0+ release
- [ ] Crear proyecto de prueba en Google Cloud
- [ ] Probar integración en entorno local
- [ ] Comparar calidad de respuestas vs Claude/OpenAI

### Fase 2: Setup Producción
- [ ] Crear proyecto de producción en Google Cloud
- [ ] Configurar service account con permisos mínimos
- [ ] Implementar monitoring con Cloud Logging
- [ ] Configurar alertas de cuota

### Fase 3: Migración Gradual
- [ ] Migrar 10% de tráfico a Gemini (A/B test)
- [ ] Comparar métricas:
  - Latencia de respuesta
  - Calidad de recomendaciones
  - Satisfacción de usuario
- [ ] Escalar al 100% si métricas son positivas

### Fase 4: Features Avanzadas
- [ ] Análisis de imágenes de actividades
- [ ] Procesamiento de videos promocionales
- [ ] Generación automática de descripciones
- [ ] Chatbot multimodal (texto + imagen)

---

## MONITOREO Y LOGS

### Métricas Importantes

```java
@Service
public class GeminiMonitoringService {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public void registrarLlamada(long latenciaMs, int tokensUsados) {
        // Latencia
        meterRegistry.timer("gemini.latency").record(latenciaMs, TimeUnit.MILLISECONDS);
        
        // Tokens
        meterRegistry.counter("gemini.tokens", "tipo", "total").increment(tokensUsados);
        
        // Costo estimado
        double costo = (tokensUsados / 1_000_000.0) * 0.075;
        meterRegistry.counter("gemini.costo").increment(costo);
    }
}
```

### Logs Estructurados

```java
@Slf4j
@Service
public class ChatBotService {
    
    public ChatRecomendacionDTO procesarConRecomendacion(ChatMensajeRequestDTO request) {
        long inicio = System.currentTimeMillis();
        
        try {
            String respuesta = generarRespuesta(request);
            ChatRecomendacionDTO resultado = recomendacionService.analizarYGenerarRecomendacion(
                request.getMensaje(), respuesta
            );
            
            long latencia = System.currentTimeMillis() - inicio;
            log.info("Gemini response OK | latencia={}ms | tieneRecomendacion={}", 
                     latencia, resultado.isTieneRecomendacion());
            
            return resultado;
            
        } catch (Exception e) {
            log.error("Gemini error | mensaje={} | error={}", 
                     request.getMensaje(), e.getMessage(), e);
            throw e;
        }
    }
}
```

---

## REFERENCIAS

### Documentación Oficial
- **Spring AI Gemini Docs** (cuando esté disponible): https://docs.spring.io/spring-ai/reference/
- **Google Vertex AI**: https://cloud.google.com/vertex-ai/docs
- **Gemini API Reference**: https://ai.google.dev/gemini-api/docs

### Tutoriales
- **Vertex AI Quickstart**: https://cloud.google.com/vertex-ai/docs/start/introduction-unified-platform
- **Spring AI GitHub**: https://github.com/spring-projects/spring-ai

### Pricing
- **Gemini Pricing**: https://ai.google.dev/pricing
- **Vertex AI Pricing**: https://cloud.google.com/vertex-ai/pricing

---

## CONCLUSIÓN

**Gemini será una excelente opción para ETA App cuando esté disponible en Spring AI**, especialmente por:
- ✅ Bajo costo (mitad que OpenAI)
- ✅ Multimodalidad (imágenes + videos)
- ✅ Context window gigante (1M tokens)

**Sin embargo, la arquitectura actual ya está preparada**:
- ✅ Abstracción con `ChatClient` permite cambiar proveedor sin tocar código
- ✅ Solo necesitas agregar bean y configuración
- ✅ El resto del sistema (recomendaciones, DTOs, frontend) funciona igual

**Acción recomendada**: 
Esperar a **Spring AI 1.1.0+ estable** y seguir los pasos de este documento cuando esté disponible.
