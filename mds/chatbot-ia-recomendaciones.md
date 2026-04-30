# Sistema de Chatbot IA con Recomendaciones Inteligentes

**ETA Assistant** - Asistente virtual para descubrimiento de actividades turísticas en Cartagena

---

## 1. ARQUITECTURA DEL SISTEMA

### Componentes Principales

```
┌─────────────────┐
│   chat-widget   │ (Frontend: Vanilla JS)
│    .js + .css   │
└────────┬────────┘
         │ POST /chat/recomendar
         ▼
┌─────────────────────────┐
│  ChatBotController      │
│  - procesarMensaje()    │
│  - procesarConRecomen-  │
│    dacion()             │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐       ┌──────────────────────────┐
│  ChatBotService         │◄──────┤ ChatBotRecomendacion     │
│  - generarRespuesta()   │       │ Service                  │
│  - procesarConRecomen-  │       │ - analizarYGenerar-      │
│    dacion()             │       │   Recomendacion()        │
└────────┬────────────────┘       │ - detectarCategoria()    │
         │                        │ - detectarIdioma()       │
         ▼                        │ - detectarRangoPrecio()  │
┌─────────────────────────┐       └──────────────────────────┘
│  Spring AI              │
│  - ChatClient           │
│  - OpenAI / Anthropic   │
└─────────────────────────┘
```

---

## 2. PROVEEDORES DE IA SOPORTADOS

### Configuración Actual (application.properties)

```properties
# Proveedor activo: "openai" | "anthropic"
eta.chat.provider=anthropic
```

### OpenAI (GPT-4o-mini)
- **Modelo**: gpt-4o-mini
- **Ventajas**: Rápido, económico, buena comprensión de español
- **API Key**: Configurar en variable de entorno `OPENAI_API_KEY`
- **Uso**:
  ```properties
  eta.chat.provider=openai
  spring.ai.openai.api-key=${OPENAI_API_KEY}
  spring.ai.openai.chat.options.model=gpt-4o-mini
  spring.ai.openai.chat.options.temperature=0.7
  spring.ai.openai.chat.options.max-tokens=500
  ```

### Anthropic (Claude Haiku) **← ACTIVO**
- **Modelo**: claude-haiku-4-5-20251001
- **Ventajas**: Excelente en español, muy rápido, bajo costo
- **API Key**: Configurar en variable de entorno `ANTHROPIC_API_KEY`
- **Uso**:
  ```properties
  eta.chat.provider=anthropic
  spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
  spring.ai.anthropic.chat.options.model=claude-haiku-4-5-20251001
  spring.ai.anthropic.chat.options.temperature=0.7
  spring.ai.anthropic.chat.options.max-tokens=500
  ```

### Google Gemini (Futuro)
- **Estado**: No disponible en Spring AI 1.0.0
- **Integración Futura**: Cuando Spring AI 1.1.0+ esté disponible:
  1. Agregar dependencia `spring-ai-vertex-ai-gemini`
  2. Configurar proyecto de Google Cloud
  3. Habilitar Vertex AI API
  4. Actualizar `ChatAiConfig.java` con bean de Gemini
  5. Configurar:
     ```properties
     eta.chat.provider=gemini
     spring.ai.vertex.ai.gemini.project-id=${GEMINI_PROJECT_ID}
     spring.ai.vertex.ai.gemini.location=us-central1
     spring.ai.vertex.ai.gemini.chat.options.model=gemini-1.5-flash
     ```

---

## 3. SISTEMA DE RECOMENDACIONES INTELIGENTES

### Flujo de Recomendaciones

```
Usuario: "Busco actividades de buceo baratas en inglés"
    ↓
ChatBotRecomendacionService detecta:
    - Categoría: Aventura (o Acuático)
    - Precio: precioMax = 100,000 COP
    - Idioma: Inglés
    - Keyword: "buceo"
    ↓
Genera FiltrosRecomendadosDTO:
    {
      "nombre": "buceo",
      "categoriaId": 2,
      "categoriaNombre": "Aventura",
      "idiomaId": 1,
      "idiomaNombre": "Inglés",
      "precioMax": 100000,
      "textoBoton": "Ver actividades de Aventura"
    }
    ↓
Frontend muestra:
    [Respuesta del bot]
    [Botón: "Ver actividades de Aventura"]
    ↓
Click → Redirige a:
    /actividades/buscar?nombre=buceo&categoriaId=2&idiomaId=1&precioMax=100000
```

### Detección de Intenciones

#### 1. Categorías (con sinónimos)
- **Gastronomía**: comer, comida, restaurante
- **Aventura**: adrenalina, emoción
- **Cultural**: cultura, historia, museos
- **Naturaleza**: natural, ecológica, playa, mar
- **Deportes**: deporte, ejercicio, fitness
- **Vida Nocturna**: noche, fiesta, bar, disco
- **Familiar**: niños, familia, infantil
- **Romántico**: pareja, romántica, amor

#### 2. Idiomas
- **Español**, **Inglés**, **Francés**, etc.
- Sinónimos: "english", "ingles", "french", "frances"

#### 3. Rangos de Precio (con parser inteligente)

**Patrones reconocidos:**
```
"menos de 100k"          → precioMax: 100,000
"bajo 150000"            → precioMax: 150,000
"entre $50 y $100"       → precioMin: 50,000 / precioMax: 100,000
"de 50000 a 100000"      → precioMin: 50,000 / precioMax: 100,000
"barato", "económico"    → precioMax: 100,000
"premium", "lujo"        → precioMin: 200,000
```

**Regex Patterns (ChatBotRecomendacionService.java:221-240):**
```java
Pattern patronMenosDe = Pattern.compile("(menos|bajo|menor)\\s+(de\\s+)?\\$?([0-9,]+)k?");
Pattern patronRango = Pattern.compile("(entre|de)\\s+\\$?([0-9,]+)k?\\s+(y|a)\\s+\\$?([0-9,]+)k?");
```

#### 4. Keywords de Actividades
```java
String[] keywords = {
    "buceo", "snorkel", "kayak", "surf", "paseo", "tour", "cata",
    "taller", "clase", "show", "concierto", "museo", "castillo",
    "playa", "isla", "barco", "lancha", "crucero", "pesca",
    "fotografía", "yoga", "masaje", "spa", "ciclismo", "senderismo"
};
```

---

## 4. DTOs Y ESTRUCTURA DE DATOS

### ChatRecomendacionDTO
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRecomendacionDTO {
    private String respuesta;              // Texto del bot
    private boolean tieneRecomendacion;    // Si incluye filtros
    private FiltrosRecomendadosDTO filtros; // Filtros estructurados
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FiltrosRecomendadosDTO {
        private String nombre;              // Keyword para búsqueda
        private Long categoriaId;           // ID de categoría
        private String categoriaNombre;     // Nombre para mostrar
        private Long idiomaId;              // ID de idioma
        private String idiomaNombre;        // Nombre del idioma
        private BigDecimal precioMin;       // Precio mínimo
        private BigDecimal precioMax;       // Precio máximo
        private String textoBoton;          // Texto del CTA button
    }
}
```

### Ejemplo de Respuesta JSON
```json
{
  "respuesta": "¡Claro! Tenemos varias opciones de buceo económicas en inglés. Te puedo mostrar las actividades disponibles.",
  "tieneRecomendacion": true,
  "filtros": {
    "nombre": "buceo",
    "categoriaId": 2,
    "categoriaNombre": "Aventura",
    "idiomaId": 1,
    "idiomaNombre": "Inglés",
    "precioMin": null,
    "precioMax": 100000,
    "textoBoton": "Ver actividades de Aventura"
  }
}
```

---

## 5. ENDPOINTS DEL CHATBOT

### POST /chat/recomendar
**Descripción**: Procesa mensaje del usuario y devuelve recomendación con filtros

**Request Body (ChatMensajeRequestDTO):**
```json
{
  "mensaje": "Busco actividades de buceo baratas",
  "historial": [
    { "rol": "user", "contenido": "Hola" },
    { "rol": "assistant", "contenido": "¡Hola! Soy ETA Assistant..." }
  ],
  "contextoActividad": null
}
```

**Response (ChatRecomendacionDTO):**
```json
{
  "respuesta": "Texto de la respuesta del bot",
  "tieneRecomendacion": true,
  "filtros": { ... }
}
```

### POST /chat/mensaje
**Descripción**: Endpoint antiguo que devuelve fragmento Thymeleaf (mantener compatibilidad)

**Response**: HTML Fragment
```html
<div class="chat-msg bot">
    <div class="chat-bubble">Respuesta del bot...</div>
</div>
```

---

## 6. FRONTEND - WIDGET DE CHAT

### chat-widget.js (Funciones Clave)

#### sendMessage()
```javascript
async function sendMessage(texto) {
    // 1. Enviar a /chat/recomendar
    const res = await fetch('/chat/recomendar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            mensaje: texto,
            historial: conversationHistory.slice(-20),
            contextoActividad: actividadId
        })
    });
    
    const data = await res.json();
    
    // 2. Mostrar respuesta
    if (data.tieneRecomendacion && data.filtros) {
        appendMessageWithRecommendation(data.respuesta, data.filtros);
    } else {
        appendMessage('bot', data.respuesta);
    }
}
```

#### appendMessageWithRecommendation()
```javascript
function appendMessageWithRecommendation(respuesta, filtros) {
    const wrapper = document.createElement('div');
    wrapper.className = 'chat-msg bot';
    
    // Burbuja de texto
    const bubble = document.createElement('div');
    bubble.className = 'chat-bubble';
    bubble.textContent = respuesta;
    
    // Botón de acción
    const button = document.createElement('button');
    button.className = 'chat-recommend-btn';
    button.textContent = filtros.textoBoton || 'Ver actividades';
    button.onclick = function() {
        redirigirConFiltros(filtros);
    };
    
    wrapper.appendChild(bubble);
    wrapper.appendChild(button);
    messagesEl.appendChild(wrapper);
}
```

#### redirigirConFiltros()
```javascript
function redirigirConFiltros(filtros) {
    const params = new URLSearchParams();
    
    if (filtros.nombre) params.append('nombre', filtros.nombre);
    if (filtros.categoriaId) params.append('categoriaId', filtros.categoriaId);
    if (filtros.idiomaId) params.append('idiomaId', filtros.idiomaId);
    if (filtros.precioMin) params.append('precioMin', filtros.precioMin);
    if (filtros.precioMax) params.append('precioMax', filtros.precioMax);
    
    window.location.href = `/actividades/buscar?${params.toString()}`;
}
```

### chat-widget.css (Estilos del Botón)
```css
.chat-recommend-btn {
    margin-top: 8px;
    padding: 8px 16px;
    background: linear-gradient(135deg, var(--color-main, #23aae2), var(--color-medium-blue, #5483b3));
    color: white;
    border: none;
    border-radius: 20px;
    font-size: 0.85rem;
    font-weight: 600;
    cursor: pointer;
    transition: transform 0.2s ease, box-shadow 0.2s ease;
    box-shadow: 0 2px 8px rgba(35, 170, 226, 0.3);
}

.chat-recommend-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(35, 170, 226, 0.4);
}
```

---

## 7. SYSTEM PROMPT DEL CHATBOT

**Objetivo**: Asistente especializado en Cartagena y actividades de ETA App

**Reglas del System Prompt (ChatBotService.java:48-72):**

```
TU PROPÓSITO:
- Ayudar a descubrir actividades turísticas en ETA App
- Responder sobre Cartagena: zonas, clima, transporte, gastronomía, cultura
- Explicar cómo funciona ETA (registro, reservas, favoritos, planes)

REGLAS ABSOLUTAS:
1. SOLO hablas de Cartagena y ETA App
2. Si pregunta fuera de alcance → respuesta amable + redirect
3. Solo recomienda actividades del CONTEXTO proporcionado
4. Sé conciso: máximo 3-4 oraciones o lista de 3 ítems
5. Tono: amigable, cálido, como guía local experto
6. Idioma: siempre español (aunque usuario escriba en otro idioma)
7. Termina recomendaciones con frase motivadora
```

**Inyección de Contexto Dinámico:**
- El sistema inyecta automáticamente actividades relevantes al mensaje del usuario
- Usa `ActividadService.buscarConFiltros()` para encontrar actividades relacionadas
- Formato:
  ```
  - "Buceo en Islas del Rosario" | Categoría: Aventura | Precio: $110,000 COP | Calificación: 4.8 | Idioma: Español
  ```

---

## 8. TESTING Y DEBUGGING

### Casos de Prueba

#### Caso 1: Búsqueda por categoría + precio
```
Usuario: "Quiero algo de aventura barato"

Esperado:
{
  "tieneRecomendacion": true,
  "filtros": {
    "categoriaId": 2,
    "categoriaNombre": "Aventura",
    "precioMax": 100000,
    "textoBoton": "Ver actividades de Aventura"
  }
}
```

#### Caso 2: Búsqueda por idioma + keyword
```
Usuario: "Tours en inglés"

Esperado:
{
  "tieneRecomendacion": true,
  "filtros": {
    "nombre": "tour",
    "idiomaId": 1,
    "idiomaNombre": "Inglés",
    "textoBoton": "Ver actividades en Inglés"
  }
}
```

#### Caso 3: Sin recomendación
```
Usuario: "Hola, ¿cómo estás?"

Esperado:
{
  "tieneRecomendacion": false,
  "respuesta": "¡Hola! Soy ETA Assistant..."
}
```

### Logs Importantes

```java
// En ChatBotRecomendacionService
logger.info("Analizando mensaje para recomendaciones: {}", mensajeUsuario);
logger.info("Detectada categoría: {} (ID: {})", categoria.getNombre(), categoria.getIdCategoria());
logger.info("Rango de precio: {} - {}", precioMin, precioMax);
```

---

## 9. CONFIGURACIÓN DE API KEYS

### Variables de Entorno Requeridas

**Para OpenAI:**
```bash
export OPENAI_API_KEY="sk-proj-..."
```

**Para Anthropic (Claude):**
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

### Obtener API Keys

#### OpenAI
1. Ir a https://platform.openai.com/api-keys
2. Crear nueva API key
3. Copiar y guardar en variable de entorno

#### Anthropic
1. Ir a https://console.anthropic.com/settings/keys
2. Crear nueva API key
3. Copiar y guardar en variable de entorno

### Validar Configuración

```bash
# Windows PowerShell
echo $env:ANTHROPIC_API_KEY

# Linux/Mac
echo $ANTHROPIC_API_KEY
```

---

## 10. LIMITACIONES Y FUTURAS MEJORAS

### Limitaciones Actuales
- ❌ Gemini no disponible en Spring AI 1.0.0
- ⚠️ Detección de intenciones basada en keywords (no usa NLP avanzado)
- ⚠️ Rangos de precio limitados a patrones predefinidos
- ⚠️ No guarda historial de conversación en BD (solo en memoria del navegador)

### Roadmap
- ✅ Sistema de recomendaciones con filtros estructurados
- ✅ Soporte multi-proveedor (OpenAI/Anthropic)
- 🔄 Integración con Gemini (cuando esté disponible)
- 🔄 Fine-tuning con datos de actividades reales
- 📅 Historial de conversaciones persistente por usuario
- 📅 Analytics de intenciones de búsqueda
- 📅 A/B testing de system prompts
- 📅 Recomendaciones basadas en preferencias guardadas del cliente
- 📅 Integración con sistema de onboarding (preferencias → mejores recomendaciones)

---

## 11. TROUBLESHOOTING

### Error: "cannot find symbol: class VertexAiGeminiChatModel"
**Solución**: Gemini no está soportado. Cambiar a `openai` o `anthropic` en application.properties

### Error: "401 Unauthorized" al llamar API
**Solución**: Verificar que la API key esté configurada correctamente en variables de entorno

### Error: Constructor ChatRecomendacionDTO no encontrado
**Solución**: Ejecutar `mvnw.cmd clean compile` para regenerar clases de Lombok

### El botón de recomendación no aparece
**Solución**: 
1. Verificar que `tieneRecomendacion` sea `true` en JSON response
2. Verificar que `filtros` no sea `null`
3. Revisar consola del navegador para errores JS

### Las redirecciones no aplican filtros
**Solución**: Verificar que `AllAcessController.buscarActividadesGet()` esté parseando los query params correctamente

---

## REFERENCIAS

- Spring AI Docs: https://docs.spring.io/spring-ai/reference/
- OpenAI API: https://platform.openai.com/docs
- Anthropic API: https://docs.anthropic.com/
- Lombok: https://projectlombok.org/features/
