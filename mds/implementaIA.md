# SPEC — ChatBot IA "ETA Assistant"
## Asistente de Actividades y Guía de Cartagena de Indias

**Versión:** 1.0  
**Fecha:** 22 de abril de 2026  
**Estado:** Listo para implementar  
**Aplica sobre:** PRD v1.0 + db.md v1.0

---

## 1. Resumen

Se añade a la plataforma ETA un chatbot con IA conversacional cuyo único propósito es:

1. **Recomendar actividades** disponibles en la plataforma según las preferencias del usuario.
2. **Responder preguntas exclusivamente sobre Cartagena de Indias**: zonas turísticas, clima, transporte, gastronomía, cultura, seguridad, alojamiento, tips de viaje, etc.

El asistente rechaza educadamente cualquier tema fuera de ese alcance (recetas de otro país, tecnología, política, matemáticas, etc.). El modelo de IA es configurable por variable de entorno; el sistema soporta OpenAI y Anthropic vía Spring AI.

---

## 2. Alcance Funcional

### 2.1 Lo que hace el bot

| Capacidad | Ejemplo de interacción |
|-----------|----------------------|
| Recomendar actividades de la BD | "¿Qué actividades acuáticas tienen disponibles?" → lista con precio y calificación |
| Filtrar por preferencias del usuario | "Busco algo para hacer con niños menos de $100k" |
| Informar sobre Cartagena | "¿Cuál es la mejor época para visitar Cartagena?" |
| Orientar sobre zonas de la ciudad | "¿Cómo llego al Castillo San Felipe?" |
| Resolver dudas sobre la plataforma ETA | "¿Cómo hago una reserva?" |
| Rechazar temas fuera de alcance | "Solo puedo ayudarte con actividades y temas de Cartagena 😊" |

### 2.2 Lo que NO hace el bot (fuera de alcance v1)

- Crear o modificar reservas directamente.
- Acceder a datos privados del usuario (historial de reservas, favoritos personales).
- Responder sobre otras ciudades o países más allá de contexto de origen del viajero.
- Procesar pagos ni dar información de precios de vuelos/hoteles externos.
- Memorizar conversaciones entre sesiones (la memoria es solo por sesión HTTP).

---

## 3. Páginas donde aparece el chatbot

El widget se inyecta como fragmento Thymeleaf en las siguientes plantillas:

| Plantilla | Ruta | Contexto especial disponible |
|-----------|------|------------------------------|
| `main.html` | `/` | Actividades destacadas |
| `resultados-busqueda.html` | `/actividades/buscar` | Filtros activos de la búsqueda |
| `detalle-actividad.html` | `/actividad/{slug}-{id}` | Nombre e id de la actividad actual |
| `para-ti.html` | `/actividades/para-ti` | Categorías preferidas del cliente |
| Vistas de planes | `/planes`, `/planes/{id}` | — |
| `dashboard.html` (cliente) | `/cliente/dashboard` | — |

El widget **NO aparece** en:
- Pantallas de login / registro / onboarding.
- Panel de colaborador.
- Panel de administrador.
- Checkout.

---

## 4. Arquitectura Técnica

### 4.1 Dependencia — Spring AI

Agregar al `pom.xml`. Solo una de las dos implementaciones debe estar activa en runtime según la propiedad `eta.chat.provider`.

```xml
<!-- BOM de Spring AI -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<!-- OpenAI -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Anthropic -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>
```

### 4.2 Configuración en `application.properties`

```properties
# ── ChatBot IA ─────────────────────────────────────────────
# Proveedor activo: "openai" | "anthropic"
eta.chat.provider=openai

# OpenAI
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=500

# Anthropic
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.options.model=claude-haiku-4-5-20251001
spring.ai.anthropic.chat.options.temperature=0.7
spring.ai.anthropic.chat.options.max-tokens=500

# Límites de seguridad del chat
eta.chat.max-historial=10
eta.chat.max-actividades-contexto=5
```

Variables de entorno requeridas (nunca en el repo):
- `OPENAI_API_KEY` si `eta.chat.provider=openai`
- `ANTHROPIC_API_KEY` si `eta.chat.provider=anthropic`

### 4.3 Nuevos archivos a crear

```
src/main/java/maineta/eta/
│
├── controller/
│   └── ChatController.java          ← POST /chat/mensaje
│
├── service/
│   └── ChatService.java             ← Lógica principal del bot
│
├── config/
│   └── ChatAiConfig.java            ← Selección de ChatClient según provider
│
└── dto/
    ├── ChatMensajeRequestDTO.java   ← { mensaje: String, historial: List<MensajeDTO> }
    ├── ChatMensajeResponseDTO.java  ← { respuesta: String }
    └── MensajeDTO.java              ← { rol: "user"|"assistant", contenido: String }

src/main/resources/templates/
└── componentes/
    └── chat-widget.html             ← Fragmento Thymeleaf del widget

src/main/resources/static/
└── js/
    └── chat-widget.js               ← Lógica JS del widget (vanilla JS)

src/main/resources/static/
└── css/
    └── chat-widget.css              ← Estilos del widget (o en global.css)
```

### 4.4 Flujo de datos completo

```
[Usuario escribe mensaje en el widget]
        │
        ▼
POST /chat/mensaje
Body: {
  mensaje: "¿Qué tour de noche recomiendan?",
  historial: [ {rol:"user", ...}, {rol:"assistant", ...} ],
  contextoActividad: 42   ← opcional, si está en detalle-actividad
}
        │
        ▼
ChatController.java
  → Valida tamaño historial (máx eta.chat.max-historial turnos)
  → Delega a ChatService
        │
        ▼
ChatService.java
  1. Extrae keywords del mensaje del usuario
  2. Consulta ActividadService.buscarConFiltros() (máx 5 resultados)
  3. Construye el SYSTEM PROMPT (ver sección 4.5)
  4. Construye la lista de mensajes: [system] + historial + [user actual]
  5. Llama a ChatClient (Spring AI) — bloqueante en v1
  6. Devuelve ChatMensajeResponseDTO
        │
        ▼
[Frontend recibe JSON y renderiza burbuja de respuesta]
```

### 4.5 System Prompt

El system prompt es la pieza más crítica. Define todo el comportamiento del bot.

```
Eres "ETA Assistant", el asistente virtual de ETA App, una plataforma de 
actividades turísticas en Cartagena de Indias, Colombia.

TU PROPÓSITO:
- Ayudar a los usuarios a descubrir y elegir actividades turísticas disponibles 
  en la plataforma ETA.
- Responder preguntas sobre Cartagena de Indias: zonas, clima, transporte, 
  gastronomía, cultura, historia, playas, seguridad y tips de viaje.
- Explicar cómo funciona la plataforma ETA (registro, reservas, favoritos, planes).

REGLAS ABSOLUTAS:
1. SOLO hablas de Cartagena de Indias y de las actividades de ETA App.
2. Si el usuario pregunta algo fuera de ese alcance (política, recetas de otro país, 
   matemáticas, tecnología, etc.), responde amablemente: 
   "Solo puedo ayudarte con actividades en Cartagena y temas de la ciudad 😊 
   ¿En qué te puedo orientar?"
3. Cuando recomiendes actividades, usa EXCLUSIVAMENTE las que aparecen en el 
   CONTEXTO DE ACTIVIDADES que se te proporciona más abajo. Nunca inventes 
   actividades, precios ni calificaciones.
4. Sé conciso: respuestas de máximo 3-4 oraciones o una lista corta de 3 ítems.
5. Tono: amigable, cálido, como un guía local experto. Usa emojis con moderación.
6. Idioma: responde siempre en español, aunque el usuario escriba en otro idioma.

CONTEXTO DE ACTIVIDADES DISPONIBLES HOY:
{actividades_contexto}

Si no hay actividades relevantes en el contexto, dile al usuario que explore el 
catálogo completo en la sección de búsqueda.
```

El placeholder `{actividades_contexto}` se reemplaza en `ChatService.java` con un bloque de texto como:

```
- "Tour Nocturno Ciudad Amurallada" | Categoría: Cultural | Precio: $85.000 COP | 
  Calificación: 4.8 | Idioma: Español
- "Snorkel en Islas del Rosario" | Categoría: Aventura acuática | Precio: $120.000 COP | 
  Calificación: 4.6 | Idioma: Español/Inglés
```

---

## 5. Especificación del Endpoint

### `POST /chat/mensaje`

**Acceso:** Público (no requiere autenticación). Spring Security debe incluir esta ruta en las excepciones.

**Request Body:**
```json
{
  "mensaje": "¿Qué actividades tienen para familia con niños?",
  "historial": [
    { "rol": "user", "contenido": "Hola" },
    { "rol": "assistant", "contenido": "¡Hola! ¿En qué te puedo ayudar?" }
  ],
  "contextoActividad": null
}
```

| Campo | Tipo | Reglas |
|-------|------|--------|
| `mensaje` | String | NOT NULL, max 500 caracteres |
| `historial` | List | Max `eta.chat.max-historial` turnos (se trunca el más antiguo si supera) |
| `contextoActividad` | Long | Opcional. ID de la actividad si el usuario está en detalle-actividad |

**Response 200:**
```json
{
  "respuesta": "Para familias con niños les recomiendo el Tour en Chiva 🎉, ideal para todas las edades, con salidas cada fin de semana. También tienen el Paseo en Bici por el Centro Histórico..."
}
```

**Response 400** (mensaje vacío o demasiado largo):
```json
{ "error": "Mensaje inválido" }
```

**Response 500** (fallo de la API externa):
```json
{ "error": "El asistente no está disponible en este momento. Intenta más tarde." }
```

---

## 6. Lógica de Selección de Actividades para Contexto

`ChatService` debe construir el contexto de actividades así:

```java
// 1. Si viene contextoActividad (ID), incluir esa actividad primero
// 2. Extraer palabras clave del mensaje del usuario
// 3. Llamar ActividadService.buscarConFiltros(nombre=keywords, page=0, size=5)
// 4. Si no hay resultados con keywords, traer las 5 mejor calificadas (activas)
// 5. Formatear como texto plano para el system prompt
```

El contexto de actividades se construye solo con campos públicos:
- `titulo`, `categoria.nombre`, `precio` (precio consumidor vía `UsuarioHelper`), `calificacion`, `idioma.nombre`

**No incluir nunca:** id del colaborador, coordenadas exactas, campos internos.

---

## 7. Configuración de Spring Security

Agregar a `SecurityConfig.java` en el bloque de rutas públicas:

```java
.requestMatchers("/chat/**").permitAll()
```

---

## 8. Implementación del Widget Frontend

### 8.1 Fragmento Thymeleaf — `componentes/chat-widget.html`

```html
<!-- th:fragment="chatWidget" -->
<div id="eta-chat-widget" th:attr="
    data-actividad-id=${actividadId != null ? actividadId : ''}">

  <!-- Botón de apertura -->
  <button id="chat-toggle-btn" aria-label="Abrir asistente ETA">
    <span id="chat-icon-open">💬</span>
    <span id="chat-icon-close" style="display:none">✕</span>
  </button>

  <!-- Panel del chat -->
  <div id="chat-panel" aria-live="polite" style="display:none">
    <div id="chat-header">
      <span>ETA Assistant 🌴</span>
      <small>Tu guía en Cartagena</small>
    </div>
    <div id="chat-messages">
      <!-- Mensaje inicial del bot -->
      <div class="chat-msg bot">
        ¡Hola! Soy ETA Assistant 🌴 Tu guía en Cartagena. 
        ¿Buscas una actividad o tienes preguntas sobre la ciudad?
      </div>
    </div>
    <div id="chat-input-area">
      <input type="text" id="chat-input"
             placeholder="Pregúntame sobre actividades..."
             maxlength="500"
             autocomplete="off"/>
      <button id="chat-send-btn" aria-label="Enviar">
        <svg><!-- ícono enviar --></svg>
      </button>
    </div>
    <p id="chat-disclaimer">Solo respondo sobre Cartagena y actividades ETA.</p>
  </div>
</div>
```

### 8.2 Inclusión en cada plantilla

En las plantillas indicadas, al final del `<body>` antes del `</body>`, incluir:

```html
<!-- Para detalle-actividad.html -->
<div th:replace="~{componentes/chat-widget :: chatWidget}"
     th:with="actividadId=${actividad.idActividad}">
</div>

<!-- Para el resto de páginas -->
<div th:replace="~{componentes/chat-widget :: chatWidget}"></div>
```

También incluir los scripts al final:
```html
<script th:src="@{/js/chat-widget.js}"></script>
```

### 8.3 JavaScript — `chat-widget.js`

Responsabilidades:

1. **Toggle** de apertura/cierre del panel.
2. **Historial en memoria**: array `conversationHistory = []` que se mantiene mientras la página esté abierta. Se limpia al navegar a otra página (comportamiento esperado en v1).
3. **Enviar mensaje** al hacer clic en botón o presionar Enter.
4. **Renderizar burbujas** de usuario y bot con clases CSS diferenciadas.
5. **Indicador de "escribiendo..."** mientras espera la respuesta.
6. **Gestionar errores** mostrando mensaje amigable en el chat.
7. **Leer contexto**: leer `data-actividad-id` del widget para incluirlo en el payload si existe.

```javascript
// Estructura principal (pseudocódigo)
const widget = document.getElementById('eta-chat-widget');
const actividadId = widget?.dataset?.actividadId || null;
let conversationHistory = [];

async function sendMessage(texto) {
  if (!texto.trim()) return;

  appendMessage('user', texto);
  showTypingIndicator();

  const payload = {
    mensaje: texto,
    historial: conversationHistory.slice(-10),  // máx 10 turnos
    contextoActividad: actividadId ? parseInt(actividadId) : null
  };

  try {
    const res = await fetch('/chat/mensaje', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    removeTypingIndicator();

    if (data.respuesta) {
      appendMessage('assistant', data.respuesta);
      conversationHistory.push(
        { rol: 'user', contenido: texto },
        { rol: 'assistant', contenido: data.respuesta }
      );
    } else {
      appendMessage('bot-error', data.error || 'Error inesperado.');
    }
  } catch (e) {
    removeTypingIndicator();
    appendMessage('bot-error', 'Sin conexión con el asistente. Intenta de nuevo.');
  }
}
```

---

## 9. Diseño Visual del Widget

El widget sigue el sistema de diseño de ETA App (Tailwind CSS). Directrices:

- **Posición**: Fija, esquina inferior derecha. `position: fixed; bottom: 24px; right: 24px; z-index: 1000`.
- **Botón de toggle**: Circular (56×56px), con fondo del color primario de ETA, sombra suave. Muestra emoji 💬 cuando cerrado, ✕ cuando abierto.
- **Panel abierto**: 360px ancho × 480px alto (móvil: 100vw × 60vh). Bordes redondeados. Fondo blanco. Sombra pronunciada.
- **Header**: Fondo del color primario de ETA. Nombre del bot + subtítulo "Tu guía en Cartagena".
- **Burbujas de usuario**: Alineadas a la derecha, fondo color primario, texto blanco.
- **Burbujas del bot**: Alineadas a la izquierda, fondo gris claro, texto oscuro.
- **Input**: Bordes redondeados, botón de envío integrado.
- **Disclaimer**: Texto gris pequeño al pie: "Solo respondo sobre Cartagena y actividades ETA."
- **Indicador de typing**: Tres puntos animados con CSS mientras el bot responde.
- **Responsivo**: En pantallas < 640px el panel ocupa el ancho completo con un offset mínimo.

---

## 10. Implementación de `ChatAiConfig.java`

La configuración selecciona el `ChatClient` activo según la propiedad:

```java
@Configuration
public class ChatAiConfig {

    @Value("${eta.chat.provider}")
    private String provider;

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
```

Ambas implementaciones de `ChatModel` exponen la misma interfaz de Spring AI (`ChatClient`), por lo que `ChatService` no necesita saber cuál está activo.

---

## 11. Reglas de Negocio

| ID | Regla |
|----|-------|
| BOT-01 | El system prompt debe siempre incluir la restricción de solo hablar de Cartagena y actividades ETA. No puede ser modificado desde el frontend. |
| BOT-02 | El historial enviado al backend se trunca a los últimos `eta.chat.max-historial` turnos (default 10). El frontend no debe enviar más. |
| BOT-03 | El contexto de actividades se construye en el backend con máximo `eta.chat.max-actividades-contexto` actividades (default 5). El frontend nunca manda actividades directamente. |
| BOT-04 | El mensaje del usuario tiene máximo 500 caracteres. El backend rechaza con 400 si se supera. |
| BOT-05 | Ningún campo de autenticación, token de sesión ni dato sensible del usuario se incluye en el payload al LLM externo. |
| BOT-06 | En caso de error de la API externa (timeout, rate limit, etc.), el sistema responde con un mensaje genérico amigable, nunca expone el error técnico al usuario. |
| BOT-07 | Los precios mostrados al usuario por el bot son siempre el precio consumidor (con comisión incluida), calculado con `UsuarioHelper`. |
| BOT-08 | La conversación no persiste en base de datos. Es exclusivamente en memoria de la sesión del navegador (JavaScript array). |

---

## 12. Consideraciones de Seguridad

| Riesgo | Mitigación |
|--------|-----------|
| Prompt injection por el usuario | El system prompt se construye exclusivamente en el servidor. El mensaje del usuario va solo en el turno `user`, nunca concatenado al system prompt. |
| Abuso de API (flood de requests) | Implementar rate limiting básico: máximo 20 requests/minuto por IP con Spring's `HandlerInterceptor` o un bucket simple. |
| Exposición de API key | Leer siempre de variable de entorno. Nunca en el repo. Agregar a `.gitignore` cualquier `.env` local. |
| Costos descontrolados | `max-tokens: 500` por respuesta. Monitorear en el dashboard del proveedor. Considerar un presupuesto diario en la cuenta de OpenAI/Anthropic. |
| XSS en burbujas de chat | Usar `textContent` en JS al renderizar respuestas del bot, nunca `innerHTML`. |

---

## 13. Criterios de Aceptación

| ID | Criterio |
|----|---------|
| CA-01 | El widget aparece en las 6 páginas indicadas sin romper el layout existente |
| CA-02 | El bot responde correctamente a "¿Qué actividades de aventura tienen?" con actividades reales de la BD |
| CA-03 | Si se pregunta "¿Cuánto cuesta un vuelo a Cartagena?" el bot rechaza el tema educadamente |
| CA-04 | Si se pregunta "¿Cuál es la mejor playa de Cartagena?" el bot responde con información de la ciudad |
| CA-05 | El historial se mantiene durante la sesión en la misma página |
| CA-06 | Al estar en `/actividad/{slug}-42`, el contexto incluye la actividad id=42 |
| CA-07 | Cambiar `eta.chat.provider=anthropic` y reiniciar funciona sin cambiar código |
| CA-08 | El endpoint es accesible sin login (usuario público puede usarlo) |
| CA-09 | El widget no aparece en login, registro, panel colaborador ni admin |
| CA-10 | En mobile el panel no rompe el layout (usa 100% del ancho disponible) |

---

## 14. Impacto en Archivos Existentes

| Archivo | Tipo de cambio |
|---------|---------------|
| `pom.xml` | Agregar BOM y 2 dependencias de Spring AI |
| `application.properties` | Agregar bloque de configuración de chat |
| `SecurityConfig.java` | Agregar `/chat/**` a rutas públicas |
| `main.html` | Agregar `th:replace` del fragmento chat-widget al final del body |
| `resultados-busqueda.html` | Ídem |
| `detalle-actividad.html` | Ídem, pasando `actividadId` como parámetro |
| Vistas de planes | Ídem |
| `dashboard.html` (cliente) | Ídem |
| `componentes/navbar.html` | Sin cambios |

Archivos solo de lectura (no modificar): entidades, repositorios, servicios existentes. `ChatService` reutiliza `ActividadService` pero no lo modifica.

---

## 15. Fuera del Alcance — v1

Los siguientes puntos quedan documentados para una versión futura:

- **Persistencia de conversaciones** en tabla `chat_bot_historial` (para análisis de uso).
- **Streaming de respuesta** (Server-Sent Events) para efecto de "escritura en tiempo real".
- **RAG con embeddings** sobre la base de datos completa de actividades para recuperación más precisa.
- **Personalización por usuario autenticado** (usar preferencias del cliente para preseleccionar actividades en el contexto).
- **Métricas de uso** del bot en el dashboard del administrador.
- **Rate limiting avanzado** por usuario autenticado además de por IP.
- **Soporte multilenguaje** (responder en inglés si el usuario escribe en inglés).

---

*Spec generado para el proyecto ETA App — Spring Boot 3.5.7 + MySQL + Thymeleaf.*