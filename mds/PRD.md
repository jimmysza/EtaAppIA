# Product Requirements Document (PRD)
## Plataforma ETA — Marketplace de Actividades Turísticas y Recreativas

**Versión:** 2.0  
**Fecha:** 26 de mayo de 2026  
**Estado:** En desarrollo con integraciones de IA, pagos y predicción  

---

## 1. Resumen Ejecutivo

ETA es una plataforma web de tipo marketplace que conecta a proveedores de actividades turísticas y recreativas (denominados **Colaboradores**) con usuarios finales (**Clientes**). Los colaboradores pueden publicar, gestionar y monetizar sus actividades; los clientes pueden descubrirlas, reservarlas y calificarlas. Un **Administrador** supervisa la operación global, gestiona los datos maestros del sistema y controla la comisión de la plataforma.

**Versión 2.0** incorpora capacidades avanzadas de **inteligencia artificial**, **pagos electrónicos reales**, **predicción de demanda** mediante machine learning, **comunicación automatizada** por email, y **planificación de rutas turísticas**. La plataforma integra tecnologías de punta como Anthropic Claude/OpenAI para chatbot conversacional, ePayco para pagos seguros, y algoritmos predictivos para optimizar la ocupación de actividades.

---

## 2. Problema que Resuelve

Los turistas y personas que buscan experiencias recreativas no tienen un punto centralizado donde descubrir actividades locales verificadas, comparar precios, revisar reseñas y realizar reservas en tiempo real con disponibilidad actualizada. Por otro lado, los pequeños proveedores de experiencias carecen de una vitrina digital asequible con herramientas de gestión de reservas integradas.

ETA resuelve ambos lados: ofrece al cliente un catálogo organizado con búsqueda, filtros, reseñas y checkout en línea; y al colaborador un panel de gestión completo de actividades, disponibilidad y reservas recibidas.

---

## 3. Objetivos del Producto

| # | Objetivo | Métrica de Éxito |
|---|----------|-----------------|
| 1 | Permitir que los colaboradores publiquen actividades y gestionen su disponibilidad de forma autónoma | ≥1 actividad creada y con disponibilidad configurada por colaborador registrado |
| 2 | Que los clientes completen reservas sin fricción | Tasa de abandono en checkout < 20% |
| 3 | Generar ingreso a la plataforma por comisiones en cada reserva | Comisión configurable por el administrador (default 18%) |
| 4 | Brindar al administrador visibilidad completa de la operación | Dashboard con métricas en tiempo real |
| 5 | Garantizar seguridad y roles bien diferenciados | 0 accesos no autorizados entre roles |

---

## 4. Usuarios y Roles

### 4.1 Cliente
Usuario final que consume las actividades.

**Capacidades:**
- Registrarse con correo/contraseña o con cuenta de Google (OAuth2).
- Verificar su cuenta mediante un token enviado por email.
- Buscar actividades por texto libre o por categoría.
- Ver el detalle de una actividad (descripción, normas, qué incluye, condiciones, ubicación en mapa, calificación, comentarios, galería de imágenes).
- Seleccionar una disponibilidad (fecha, hora, cupos) y hacer una reserva.
- **Pagar reservas mediante ePayco** (tarjeta de crédito, débito, PSE, efectivo).
- Recibir **email de confirmación** tras crear reserva.
- Recibir **email recordatorio** 24 horas antes de la actividad.
- Gestionar sus reservas desde su dashboard personal.
- Escriturar comentarios y calificaciones (1-5 estrellas) solo si tiene una reserva completada en esa actividad.
- Guardar actividades como favoritas y acceder a la lista desde su perfil.
- Editar su información personal (nombre, cedula, dirección, teléfono, preferencias).
- **Crear planes del día**: Agrupar múltiples actividades en una ruta temática con orden, horarios sugeridos y notas personalizadas.
- **Ver sus planes creados** y compartirlos públicamente.
- **Explorar planes públicos** de otros clientes y colaboradores en vista de mapa.
- **Buscar actividades cercanas** a su ubicación actual con radio de distancia.
- **Interactuar con chatbot de IA** para obtener recomendaciones personalizadas.

### 4.2 Colaborador
Proveedor/empresa que ofrece actividades.

**Capacidades:**
- Registrarse con correo/contraseña, con NIT e ID de seguridad.
- Crear actividades con: título, descripción, categoría, idioma, precio, ubicación (lat/lon), normas, qué incluye, condiciones, imagen principal y galería.
- **Agregar preguntas frecuentes** específicas para cada actividad.
- Configurar disponibilidades puntuales (fecha específica, hora inicio/fin, cupos) o mediante **patrones recurrentes** (días de la semana, rango de fechas, hora y cupos).
- Editar o actualizar actividades existentes.
- Ver listado paginado de sus actividades.
- Consultar las reservas recibidas en cada actividad/disponibilidad.
- **Ver predicciones de ocupación** basadas en modelo de machine learning.
- **Visualizar tendencias de demanda** para planificar mejor la disponibilidad.
- Tener un perfil público visible para los clientes.
- **Crear planes del día**: Agrupar múltiples actividades (propias o de otros) en una ruta temática con orden, horarios sugeridos y notas personalizadas.
- **Ver sus planes creados** y compartirlos públicamente.
- **Explorar planes públicos** en vista de mapa.
- Recibir notificaciones de nuevas reservas (email).
- **Interactuar con chatbot de IA** para análisis de desempeño de actividades.

### 4.3 Administrador
Operador de la plataforma.

**Capacidades:**
- Acceder a un dashboard con métricas globales: ingresos por comisiones, número de usuarios, actividades y reservas.
- Gestionar categorías de actividades (crear, eliminar).
- Gestionar idiomas disponibles en la plataforma (crear, eliminar).
- Configurar el porcentaje de comisión de la plataforma (default 18%).
- Ver listado de clientes registrados.

### 4.4 Usuario Público (no autenticado)
Sin registro.

**Capacidades:**
- Ver la página de inicio (Landing).
- Buscar y explorar actividades.
- Ver el detalle de cualquier actividad.
- Ver perfiles públicos de colaboradores.
- Lista de colaboradores.
- **Explorar planes públicos** en /planes con mapa interactivo.
- **Ver detalle de planes** sin necesidad de autenticación.
- **Buscar actividades cercanas** por ubicación.
- **Interactuar con chatbot de IA** para obtener información general.
- Registrarse o iniciar sesión.

---

## 5. Características y Requerimientos Funcionales

### 5.1 Autenticación y Gestión de Cuentas

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| AUTH-01 | Registro de cliente con email, contraseña, nombre, teléfono y cédula | Alta |
| AUTH-02 | Registro de colaborador con email, contraseña, nombre, teléfono y NIT | Alta |
| AUTH-03 | Login estándar con email y contraseña | Alta |
| AUTH-04 | Login con Google (OAuth 2.0) | Alta |
| AUTH-05 | Verificación de email mediante token único enviado al correo | Alta |
| AUTH-06 | Redirección automática post-login según el rol del usuario | Alta |
| AUTH-07 | Manejo de sesiones con máximo 1 sesión simultánea por usuario | Media |
| AUTH-08 | Cierre de sesión con redirección a `/logout?success` | Alta |
| AUTH-09 | Página de error 403 para accesos no autorizados | Alta |

### 5.2 Catálogo y Búsqueda de Actividades (Público)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| CAT-01 | Página de inicio (Landing) con actividades destacadas y categorías | Alta |
| CAT-02 | Búsqueda de actividades por texto libre | Alta |
| CAT-03 | Filtrado de actividades por categoría | Alta |
| CAT-04 | Página de resultados de búsqueda con lista paginada de actividades | Alta |
| CAT-05 | Página de detalle de actividad con: título, descripción, precio, ubicación en mapa, galería de imágenes, normas, qué incluye, condiciones, idioma, categoría, calificación promedio y comentarios | Alta |
| CAT-06 | URL amigable (slug) para actividades: `/actividad/{slug}-{id}` | Media |
| CAT-07 | Lista pública de colaboradores con perfil básico y ranking | Media |

### 5.3 Gestión de Disponibilidad (Colaborador)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| DISP-01 | Crear disponibilidad puntual: fecha, hora inicio, hora fin, cupos totales | Alta |
| DISP-02 | Crear patrón de disponibilidad recurrente: días de semana, rango de fechas, hora, cupos | Alta |
| DISP-03 | Generar instancias de disponibilidad automáticamente a partir de un patrón | Alta |
| DISP-04 | Ver calendario de días con disponibilidades de una actividad | Media |
| DISP-05 | Estado de disponibilidad: `DISPONIBLE`, `CANCELADO`, `COMPLETADO` | Alta |
| DISP-06 | Actualización automática de `cuposDisponibles` al realizarse o cancelarse una reserva | Alta |

### 5.4 Reservas (Cliente)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RES-01 | Seleccionar una disponibilidad desde la página de detalle de actividad | Alta |
| RES-02 | Ver pantalla de checkout con resumen de la reserva (actividad, fecha, hora, cupos, precio total) | Alta |
| RES-03 | Confirmar reserva: crear registro de reserva y decrementar `cuposDisponibles` | Alta |
| RES-04 | Estado de reserva: `Pendiente`, `Confirmada`, `Cancelada`, `Hecho` | Alta |
| RES-05 | Ver historial de reservas en el dashboard del cliente | Alta |
| RES-06 | Cancelar una reserva (devuelve cupos a la disponibilidad) | Media |

### 5.5 Comentarios y Calificaciones

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| COM-01 | Solo clientes con reserva en estado `Hecho` pueden comentar en esa actividad | Alta |
| COM-02 | El comentario incluye texto y calificación de 1 a 5 estrellas | Alta |
| COM-03 | La calificación promedio de la actividad se recalcula tras cada nuevo comentario | Alta |
| COM-04 | Los comentarios se muestran en el detalle de la actividad | Alta |

### 5.6 Favoritos

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| FAV-01 | El cliente puede marcar/desmarcar una actividad como favorita | Media |
| FAV-02 | La combinación cliente-actividad es única (no duplicados) | Alta |
| FAV-03 | El cliente puede ver su lista de favoritos en su panel personal | Media |

### 5.7 Gestión de Actividades (Colaborador)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| ACT-01 | Crear actividad con todos sus campos y imagen principal | Alta |
| ACT-02 | Subir galería de imágenes adicionales para una actividad | Media |
| ACT-03 | Editar actividad existente (campos e imagen) | Alta |
| ACT-04 | Ver listado paginado de propias actividades | Alta |
| ACT-05 | Asociar actividad a categoría e idioma | Alta |
| ACT-06 | Ingresar coordenadas (latitud/longitud) para mostrar en mapa | Media |

### 5.8 Panel de Administración

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| ADM-01 | Dashboard con: ingresos totales por comisiones, número de usuarios, colaboradores, actividades y reservas | Alta |
| ADM-02 | CRUD de categorías (nombre, imagen) | Alta |
| ADM-03 | CRUD de idiomas (código, nombre) | Alta |
| ADM-04 | Ver listado de clientes registrados | Media |
| ADM-05 | Configurar porcentaje de comisión de la plataforma | Alta |

### 5.9 Subida de Archivos

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| FILE-01 | Subida de imágenes para actividades almacenadas en el servidor (`/uploads`) | Alta |
| FILE-02 | Subida de imágenes para categorías | Media |
| FILE-03 | Las imágenes son accesibles públicamente vía URL | Alta || FILE-04 | Subida de imágenes para portadas de planes | Media |

---

### 5.10 Planes del Día (Rutas Turísticas)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------||
| PLAN-01 | Clientes y colaboradores pueden crear planes temáticos agrupando múltiples actividades | Alta |
| PLAN-02 | Cada plan tiene: título, descripción, tipo, duración estimada e imagen de portada | Alta |
| PLAN-03 | Las actividades dentro de un plan tienen orden secuencial (1, 2, 3...) | Alta |
| PLAN-04 | Cada actividad en el plan puede tener hora sugerida y nota personalizada del creador | Media |
| PLAN-05 | Los planes pueden ser públicos (visibles en /planes) o privados | Alta |
| PLAN-06 | Vista pública de planes con mapa interactivo mostrando todas las actividades del plan | Alta |
| PLAN-07 | Detalle de plan muestra itinerario completo con actividades ordenadas | Alta |
| PLAN-08 | Contador de vistas para medir popularidad de planes | Media |
| PLAN-09 | Los creadores pueden ver sus planes en /cliente/planes/mis-planes o /colaborador/planes/mis-planes | Alta |
| PLAN-10 | Top 5 planes más recientes en la vista principal de /planes | Media |

---

### 5.11 Sistema de Pagos (ePayco)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| PAY-01 | Integración completa con pasarela de pagos ePayco | Alta |
| PAY-02 | Checkout seguro con iframe de ePayco en proceso de reserva | Alta |
| PAY-03 | Confirmación de transacción mediante webhook de ePayco | Alta |
| PAY-04 | Actualización automática del estado de reserva tras pago exitoso | Alta |
| PAY-05 | Almacenamiento de referencia de pago (ref_payco) en cada reserva | Alta |
| PAY-06 | Soporte para pruebas en sandbox con credenciales de testing | Media |
| PAY-07 | Configuración de ngrok para desarrollo local con webhooks | Media |
| PAY-08 | Validación de firmas digitales para seguridad en confirmaciones | Alta |

---

### 5.12 Modelo Predictivo de Ocupación

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| PRED-01 | Modelo predictivo para estimar ocupación futura de disponibilidades | Alta |
| PRED-02 | Predicciones basadas en histórico de reservas y patrones temporales | Alta |
| PRED-03 | Visualización de predicciones en dashboard del colaborador | Media |
| PRED-04 | Indicadores de ocupación esperada (baja, media, alta) por fecha | Media |
| PRED-05 | Integración con calendario de disponibilidades para planificación | Media |
| PRED-06 | Exportación de modelo entrenado (.model) para reutilización | Baja |

---

### 5.13 Chatbot Inteligente con IA

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| CHAT-01 | Chatbot interactivo con IA generativa (Anthropic Claude o OpenAI GPT) | Alta |
| CHAT-02 | Respuestas contextuales sobre actividades, reservas y servicios | Alta |
| CHAT-03 | Widget de chat flotante disponible en todas las páginas principales | Alta |
| CHAT-04 | Integración con base de conocimiento de la plataforma | Media |
| CHAT-05 | Historial de conversación por sesión de usuario | Media |
| CHAT-06 | Renderizado de respuestas en formato Markdown con HTML | Alta |
| CHAT-07 | Configuración intercambiable entre proveedores de IA (Anthropic/OpenAI) | Media |

---

### 5.14 Emails Automatizados

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| EMAIL-01 | Email de confirmación tras crear una reserva | Alta |
| EMAIL-02 | Email recordatorio 24 horas antes de la actividad | Alta |
| EMAIL-03 | Templates HTML profesionales con branding de la plataforma | Media |
| EMAIL-04 | Incluir detalles completos de la reserva en emails (fecha, hora, actividad, precio) | Alta |
| EMAIL-05 | Scheduler automático que ejecuta recordatorios diarios | Alta |
| EMAIL-06 | Configuración SMTP para envío de emails (Gmail, SendGrid, etc.) | Alta |

---

### 5.15 Mapa de Actividades Cercanas

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| MAP-01 | Vista de mapa interactivo mostrando actividades cercanas a ubicación dada | Alta |
| MAP-02 | Búsqueda de actividades por radio de distancia (km) desde un punto | Media |
| MAP-03 | Filtros de actividades cercanas por categoría en vista de mapa | Media |
| MAP-04 | Marcadores en mapa con vista previa de actividad al hacer clic | Alta |
| MAP-05 | Integración con API de mapas (Google Maps, Leaflet, Mapbox) | Media |
| MAP-06 | Cálculo de distancias usando coordenadas geográficas (lat/lon) | Alta |

---

### 5.16 Preguntas Frecuentes por Actividad

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| FAQ-01 | Colaboradores pueden agregar preguntas frecuentes específicas por actividad | Media |
| FAQ-02 | Visualización de FAQs en página de detalle de actividad | Media |
| FAQ-03 | Acordeón/collapse interactivo para mostrar respuestas | Baja |
| FAQ-04 | Edición y eliminación de FAQs desde panel del colaborador | Media |
---

## 6. Requerimientos No Funcionales

| ID | Categoría | Requerimiento |
|----|-----------|--------------|
| NF-01 | Seguridad | Contraseñas almacenadas con BCrypt (nunca en texto plano) |
| NF-02 | Seguridad | Protección de rutas por rol mediante Spring Security |
| NF-03 | Seguridad | Verificación de email obligatoria antes de activar la cuenta |
| NF-04 | Seguridad | Sesión única por usuario para evitar uso compartido de cuentas |
| NF-05 | Rendimiento | Paginación en listados (actividades, reservas) para evitar carga masiva de datos |
| NF-06 | Usabilidad | Interfaz responsiva adaptada a móviles, tablets y escritorio (Tailwind CSS) |
| NF-07 | Usabilidad | Mensajes de error y éxito claros en todas las operaciones de formulario |
| NF-08 | Mantenibilidad | Arquitectura MVC estricta: separación de controladores, servicios y repositorios |
| NF-09 | Portabilidad | El sistema completo (app + base de datos) puede levantarse con `docker-compose up` |
| NF-10 | Compatibilidad | Compatible con Java 17 y MySQL 8 |
| NF-11 | Disponibilidad | La plataforma debe estar operativa en horario continuo (24/7) cuando está desplegada |

---

## 7. Modelo de Datos

### Entidades Principales

```
Usuario ────────────── Rol
   │
   ├─── Cliente ───── Reserva ──── Disponibilidad ──── Actividad
   │       │                                               │
   │       ├─── Comentario ──────────────────────────── Actividad
   │       ├─── Favorito  ────────────────────────── Actividad
   │       └─── Plan
   │               │
   ├─── Colaborador ── Actividad ── Categoria
   │       │           │
   │       └── Plan    ├──── Idioma
   │               │   ├──── Disponibilidad
   │               │   ├──── PatronDisponibilidad
   │               │   ├──── ImagenActividad
   │               │   └──── PlanActividad
   │               │           │
   │               └───────────┘
   │
   └─── Admin
```

### Entidades y Campos Clave

| Entidad | Campos Principales |
|---------|-------------------|
| `usuarios` | id, nombre, email (UNIQUE), password (BCrypt), telefono, emailVerificado, tokenVerificacion, createdAt |
| `roles` | id, nombre (ROLE_CLIENTE / ROLE_COLABORADOR / ROLE_ADMIN) |
| `cliente` | id, cedula (UNIQUE), direccion, preferencias → FK usuario |
| `colaborador` | id, nit, correoSeguridad → FK usuario |
| `admin` | id, porcentajeComision (default 18.00) → FK usuario |
| `actividad` | id, titulo, descripcion, precio, ubicacion, latitud, longitud, normas, incluye, condiciones, imagen, calificacion, createdAt → FK colaborador, categoria, idioma |
| `disponibilidad` | id, fecha, horaInicio, horaFin, cuposTotales, cuposDisponibles, estado → FK actividad |
| `patron_disponibilidad` | id, diasSemana, horaInicio, horaFin, cuposTotales, fechaInicio, fechaFin, estado → FK actividad |
| `reserva` | id, estado (Pendiente/Confirmada/Cancelada/Hecho), cantidad, fechaReserva, ref_payco, precioTotal → FK disponibilidad, cliente, actividad |
| `comentario` | id, texto, calificacion (1-5), fechaComentario → FK cliente, actividad |
| `favorito` | id, createdAt → FK cliente, actividad (UNIQUE juntos) |
| `categoria` | id, nombre (UNIQUE), imagen |
| `idioma` | id, codigo (ej: 'es'), nombre (ej: 'Español') |
| `imagen_actividad` | id, nombre → FK actividad |
| `planes` | id, titulo, descripcion, imagenPortada, duracionEstimada, tipo, idClienteCreador, idColaboradorCreador, fechaCreacion, publico, vistas |
| `plan_actividades` | id, idPlan, idActividad, orden, horaSugerida, notaPersonalizada |
| `pregunta_frecuente_actividad` | id, pregunta, respuesta → FK actividad |

---

## 8. Arquitectura del Sistema

### 8.1 Tipo de Arquitectura
Aplicación web **monolítica** con patrón **MVC**, renderizado del lado del servidor (Server-Side Rendering via Thymeleaf).

### 8.2 Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 17 |
| Framework backend | Spring Boot 3.5.7 |
| Persistencia | Spring Data JPA / Hibernate |
| Seguridad | Spring Security + OAuth2 Client |
| Motor de vistas | Thymeleaf 3 |
| Estilos | Tailwind CSS |
| Interactividad frontend | Vanilla JavaScript |
| Base de datos | MySQL 8 |
| Build tool | Apache Maven |
| Contenerización | Docker + Docker Compose |
| Email | Spring Mail + SMTP |
| Validación | Spring Validation (Bean Validation) |
| Pasarela de pagos | ePayco (Colombia) |
| IA Generativa | Anthropic Claude API / OpenAI GPT API |
| Machine Learning | Apache Commons Math (regresión lineal) |
| Scheduling | Spring @Scheduled |
| Mapas | Google Maps API / Leaflet |

### 8.3 Componentes del Sistema

```
┌─────────────────────────────────────────────────────┐
│                  CLIENTE (Navegador)                 │
│     HTML + Thymeleaf + Tailwind + JS + Chat Widget  │
└───────────────────────┬─────────────────────────────┘
                        │ HTTP/HTTPS
┌───────────────────────▼─────────────────────────────┐
│              SPRING BOOT APPLICATION                  │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │ Controllers │→ │   Services   │→ │Repositories │ │
│  │ (10+ ctrlrs)│  │  (20+ svcs)  │  │  (JPA/SQL)  │ │
│  └─────────────┘  └──────────────┘  └──────┬──────┘ │
│  ┌─────────────────────────────────────────┐│        │
│  │ Spring Security + OAuth2 + BCrypt       ││        │
│  └─────────────────────────────────────────┘│        │
│  ┌─────────────────────────────────────────┐│        │
│  │ ML Prediction Service (Commons Math)    ││        │
│  └─────────────────────────────────────────┘│        │
│  ┌─────────────────────────────────────────┐│        │
│  │ Scheduler (Email Reminders)             ││        │
│  └─────────────────────────────────────────┘│        │
└────────────────────────────────────────────┬┘        │
                                             │         │
┌────────────────────────────────────────────▼─────────┐
│                    MySQL 8                            │
│                  (17+ tablas)                         │
└──────────────────────────────────────────────────────┘

         Integraciones Externas:
┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐
│  ePayco API     │  │ Anthropic/      │  │ Google Maps  │
│  (Webhooks)     │  │ OpenAI APIs     │  │ API          │
└─────────────────┘  └─────────────────┘  └──────────────┘
         ▲                    ▲                    ▲
         └────────────────────┴────────────────────┘
                    Spring Boot App
```

### 8.4 Rutas Principales

**Rutas Públicas (sin autenticación):**
```
GET  /                          → Landing page
GET  /login                     → Formulario de login
GET  /registro/cliente          → Registro de cliente
GET  /registro/colaborador      → Registro de colaborador
GET  /actividad/{slug}-{id}     → Detalle de actividad
GET  /resultados-busqueda       → Búsqueda
GET  /colaboradores             → Lista de colaboradores
GET  /planes                    → Listado público de planes
GET  /planes/{id}               → Detalle de plan
GET  /actividades-cercanas      → Mapa de actividades cercanas
```

**Rutas de Cliente (ROLE_CLIENTE):**
```
GET  /cliente/dashboard         → Mis reservas
GET  /cliente/checkout/{idDispo}→ Formulario de checkout
POST /cliente/reservar          → Confirmar reserva
GET  /cliente/informacion       → Mi perfil
POST /cliente/actualizar/{id}   → Actualizar mi perfil
POST /comentarios/agregar/{id}  → Agregar comentario
GET  /cliente/planes/crear      → Formulario crear plan
POST /cliente/planes/crear      → Guardar plan
GET  /cliente/planes/mis-planes → Mis planes creados
```

**Rutas de Colaborador (ROLE_COLABORADOR):**
```
GET  /colaborador/dashboard     → Dashboard del colaborador
GET  /colaborador/actividades   → Mis actividades (paginado)
GET  /colaborador/actividades/nueva → Formulario crear actividad
POST /colaborador/actividades/addAct → Guardar actividad
POST /colaborador/actividades/{id}/actualizar → Editar actividad
GET  /colaborador/planes/crear      → Formulario crear plan
POST /colaborador/planes/crear      → Guardar plan
GET  /colaborador/planes/mis-planes → Mis planes creados
GET  /colaborador/disponibilidades/{id}/predicciones → Ver predicciones
```

**Rutas de Administrador (ROLE_ADMIN):**
```
GET  /admin/dashboard           → Métricas globales
GET  /admin/categorias          → Gestión de categorías
POST /admin/categorias/nueva    → Crear categoría
POST /admin/categorias/eliminar → Eliminar categoría
GET  /admin/idiomas             → Gestión de idiomas
POST /admin/idiomas/nueva       → Crear idioma
POST /admin/idiomas/eliminar    → Eliminar idioma
```

**Rutas API/AJAX:**
```
POST /api/chatbot/mensaje       → Enviar mensaje al chatbot
GET  /api/chatbot/historial     → Obtener historial de chat
POST /pagos/confirmacion        → Webhook ePayco (confirmación pago)
GET  /api/actividades/cercanas  → Buscar actividades por coordenadas
```

---

## 9. Flujos de Usuario Principales

### 9.1 Flujo de Registro y Verificación
```
Usuario ──→ /registro/cliente ──→ POST datos
        ──→ Crear Usuario + Rol en BD
        ──→ Enviar email con token de verificación
        ──→ Redirigir a /login?pendingVerification
        ──→ Usuario hace clic en enlace del email
        ──→ GET /registro/verificar?token=XYZ
        ──→ emailVerificado = true
        ──→ Redirigir a /login (cuenta activa)
```

### 9.2 Flujo de Reserva
```
Cliente autenticado
  ──→ Busca actividad
  ──→ Abre detalle (/actividad/{slug}-{id})
  ──→ Ve disponibilidades (fechas, horas, cupos)
  ──→ Selecciona disponibilidad
  ──→ GET /cliente/checkout/{idDispo}
  ──→ Revisa resumen y confirma
  ──→ POST /cliente/reservar
  ──→ Se crea Reserva (estado: "Pendiente")
  ──→ cuposDisponibles–
  ──→ Redirige a /cliente/dashboard
```

### 9.3 Flujo de Publicación de Actividad (Colaborador)
```
Colaborador autenticado
  ──→ GET /colaborador/actividades/nueva
  ──→ Completa formulario (título, precio, categoría, idioma, imagen, etc.)
  ──→ POST /colaborador/actividades/addAct
  ──→ Se guarda Actividad + ImagenActividad
  ──→ Redirige a /colaborador/actividades
  ──→ Configura disponibilidades o patrones
```

### 9.4 Flujo de Comentario
```
Cliente con reserva en estado "Hecho"
  ──→ Accede al detalle de la actividad
  ──→ Escribe comentario + selecciona calificación (1-5)
  ──→ POST /comentarios/agregar/{idActividad}
  ──→ Sistema valida que tiene reserva completada
  ──→ Guarda Comentario
  ──→ Recalcula calificación promedio de la actividad
```

### 9.5 Flujo de Creación de Plan del Día
```
Cliente o Colaborador autenticado
  ──→ GET /{rol}/planes/crear
  ──→ Completa formulario (título, descripción, tipo, duración, imagen)
  ──→ Selecciona múltiples actividades en orden
  ──→ Para cada actividad: define hora sugerida y nota personalizada
  ──→ POST /{rol}/planes/crear
  ──→ Sistema crea Plan + PlanActividad (con campo orden)
  ──→ Redirige a /planes/{id} (detalle del plan creado)
```

### 9.6 Flujo de Exploración de Planes
```
Usuario (autenticado o anónimo)
  ──→ GET /planes
  ──→ Ve mapa interactivo con marcadores de todos los planes públicos
  ──→ Ve top 5 planes más recientes
  ──→ Selecciona un plan
  ──→ GET /planes/{id}
  ──→ Ve itinerario completo con actividades ordenadas
  ──→ Incrementa contador de vistas del plan
  ──→ Puede hacer clic en cada actividad para ver detalle (/actividad/{slug}-{id})
```

### 9.7 Flujo de Pago con ePayco
```
Cliente autenticado
  ──→ Selecciona disponibilidad
  ──→ GET /cliente/checkout/{idDispo}
  ──→ Revisa resumen de reserva (actividad, fecha, cupos, precio total)
  ──→ POST /cliente/reservar → Se abre iframe de ePayco
  ──→ Cliente ingresa datos de pago (tarjeta/PSE/efectivo)
  ──→ ePayco procesa transacción
  ──→ ePayco envía webhook a /pagos/confirmacion
  ──→ Sistema valida firma digital
  ──→ Sistema actualiza estado de Reserva a "Confirmada"
  ──→ Sistema guarda ref_payco en Reserva
  ──→ Sistema envía email de confirmación al cliente
  ──→ Redirige a /cliente/reserva-exitosa
```

### 9.8 Flujo de Chatbot con IA
```
Usuario (en cualquier página)
  ──→ Hace clic en widget de chat flotante
  ──→ Escribe pregunta (ej: "¿Qué actividades de aventura tienen?")
  ──→ POST /api/chatbot/mensaje
  ──→ Sistema envía contexto a API de IA (Anthropic Claude o OpenAI)
  ──→ IA genera respuesta contextual basada en actividades reales
  ──→ Sistema renderiza respuesta en Markdown
  ──→ Usuario ve respuesta con links a actividades sugeridas
  ──→ Conversación se guarda en sesión
```

### 9.9 Flujo de Emails Automatizados
```
Sistema (Scheduler ejecuta diariamente a las 9 AM)
  ──→ RecordatorioReservaScheduler busca reservas con actividad mañana
  ──→ Para cada reserva encontrada:
      ──→ Carga template emails/recordatorio-reserva.html
      ──→ Reemplaza variables (nombre cliente, actividad, fecha, hora)
      ──→ Envía email vía SMTP
      ──→ Registra en log

// Email de confirmación (inmediato tras crear reserva):
Cliente crea reserva
  ──→ ReservaService.crearReserva()
  ──→ EmailReservaService.enviarConfirmacion(reserva)
  ──→ Carga template emails/confirmacion-reserva.html
  ──→ Envía email con detalles completos de la reserva
```

### 9.10 Flujo de Mapa de Actividades Cercanas
```
Usuario
  ──→ GET /actividades-cercanas
  ──→ Permite al navegador obtener geolocalización
  ──→ JavaScript obtiene latitud/longitud actual
  ──→ GET /api/actividades/cercanas?lat=X&lon=Y&radio=10
  ──→ Sistema calcula distancias usando fórmula de Haversine
  ──→ Devuelve actividades dentro del radio ordenadas por distancia
  ──→ Frontend renderiza mapa con marcadores
  ──→ Usuario hace clic en marcador → ver detalle de actividad
```

### 9.11 Flujo de Predicción de Ocupación
```
Colaborador autenticado
  ──→ GET /colaborador/disponibilidades/{idActividad}
  ──→ Ve calendario con disponibilidades
  ──→ Hace clic en botón "Ver Predicciones"
  ──→ GET /colaborador/disponibilidades/{id}/predicciones
  ──→ PrediccionService analiza histórico de reservas
  ──→ Modelo ML calcula ocupación esperada por fecha
  ──→ Sistema devuelve predicciones (baja/media/alta)
  ──→ Frontend muestra gráfico de tendencias
  ──→ Colaborador ajusta cupos basándose en predicción
```

---

## 10. Reglas de Negocio

| ID | Regla |
|----|-------|
| RN-01 | Solo los clientes con al menos una reserva en estado `Hecho` pueden dejar comentario en una actividad |
| RN-02 | Un cliente no puede tener dos favoritos para la misma actividad (constraint UNIQUE) |
| RN-03 | La comisión que recibe la plataforma se calcula como: `precio_actividad × cantidad_personas × porcentaje_comision` |
| RN-04 | Una reserva solo es posible si `cuposDisponibles > 0` en la disponibilidad seleccionada |
| RN-05 | Al cancelar una reserva, los cupos se devuelven a la disponibilidad correspondiente |
| RN-06 | Un usuario no puede autenticarse si `emailVerificado = false` |
| RN-07 | Cada usuario puede tener una sola sesión activa simultánea |
| RN-08 | Un colaborador solo puede editar sus propias actividades, nunca las de otro |
| RN-09 | El porcentaje de comisión es global y configurable únicamente por el administrador |
| RN-10 | Los patrones de disponibilidad generan instancias de `Disponibilidad` automáticamente para cada día del rango que coincida con los días configurados |
| RN-11 | Un plan puede incluir actividades de diferentes colaboradores (no solo del creador del plan) |
| RN-12 | Solo el creador de un plan puede editarlo o eliminarlo |
| RN-13 | Los planes privados (`publico = false`) no aparecen en la vista /planes ni son accesibles por otros usuarios |

---

## 11. Vistas / Páginas del Sistema

| Sección | Vista | Acceso |
|---------|-------|--------|
| Pública | Landing / Home | Todos |
| Pública | Detalle de Actividad | Todos |
| Pública | Resultados de Búsqueda | Todos |
| Pública | Perfil Público de Colaborador | Todos |
| Pública | Lista de Colaboradores | Todos |
| Auth | Login | No autenticados |
| Auth | Registro Cliente | No autenticados |
| Auth | Registro Colaborador | No autenticados |
| Cliente | Dashboard (mis reservas) | ROLE_CLIENTE |
| Cliente | Mi Información | ROLE_CLIENTE |
| Cliente | Mis Favoritos | ROLE_CLIENTE |
| Cliente | Checkout | ROLE_CLIENTE |
| Colaborador | Dashboard | ROLE_COLABORADOR |
| Colaborador | Mis Actividades | ROLE_COLABORADOR |
| Colaborador | Crear Actividad | ROLE_COLABORADOR |
| Colaborador | Editar Actividad | ROLE_COLABORADOR |
| Admin | Dashboard (métricas) | ROLE_ADMIN |
| Admin | Gestión de Categorías | ROLE_ADMIN |
| Admin | Gestión de Idiomas | ROLE_ADMIN |
| Admin | Lista de Clientes | ROLE_ADMIN |
| Error | 403 Acceso Denegado | Todos |
| Error | 404 No Encontrado | Todos |
| Planes | Vista Principal de Planes (/planes) | Todos |
| Planes | Detalle de Plan | Todos |
| Cliente | Crear Plan | ROLE_CLIENTE |
| Cliente | Mis Planes | ROLE_CLIENTE |
| Colaborador | Crear Plan | ROLE_COLABORADOR |
| Colaborador | Mis Planes | ROLE_COLABORADOR |

---

## 12. Infraestructura y Despliegue

### 12.1 Contenedores Docker

El sistema se levanta mediante `docker-compose up` con dos servicios:

| Servicio | Imagen | Puerto | Descripción |
|---------|--------|--------|-------------|
| `app` | Dockerfile (Java 17) | 8080 | Aplicación Spring Boot |
| `db` | mysql:8 | 3306 | Base de datos MySQL |

### 12.2 Volúmenes
- `mysql_data/` → Persistencia de datos de MySQL montada localmente.
- `uploads/` → imágenes subidas por los usuarios, accesibles desde la app.

### 12.3 Variables de Entorno / Configuración (`application.properties`)
- Conexión a MySQL (`spring.datasource.*`)
- Credenciales OAuth2 de Google (`spring.security.oauth2.client.*`)
- Configuración de envío de email (`spring.mail.*`)
- Ruta de almacenamiento de archivos subidos

---

## 13. Criterios de Aceptación por Módulo

### Autenticación
- [x] Un usuario puede registrarse con email y contraseña
- [x] Un usuario puede iniciar sesión con Google
- [x] El sistema envía email de verificación; la cuenta queda bloqueada hasta verificar
- [x] Las rutas protegidas redirigen a login si el usuario no está autenticado
- [x] Las rutas de un rol no son accesibles por usuarios de otro rol

### Actividades
- [x] Un colaborador puede crear una actividad con imagen y verla en su catálogo
- [x] Un usuario público puede ver el detalle de la actividad sin iniciar sesión
- [x] La búsqueda devuelve resultados relevantes por texto o categoría

### Reservas
- [x] Un cliente autenticado puede reservar una disponibilidad con cupos > 0
- [x] Tras la reserva, el cupo se decrementa correctamente
- [x] La reserva aparece en el dashboard del cliente

### Comentarios
- [x] Solo clientes con reserva completada pueden comentar
- [x] La calificación promedio de la actividad se actualiza correctamente

### Admin
- [x] El dashboard muestra el total de ingresos por comisiones
- [x] El administrador puede crear y eliminar categorías e idiomas

### Planes del Día
- [x] Clientes y colaboradores pueden crear planes agrupando múltiples actividades
- [x] Los planes tienen título, descripción, tipo, duración estimada e imagen
- [x] Las actividades dentro de un plan tienen orden, hora sugerida y notas personalizadas
- [x] Vista pública /planes muestra mapa interactivo con todos los planes
- [x] Detalle de plan muestra itinerario completo ordenado
- [x] Los creadores pueden ver sus planes en /mis-planes

### Sistema de Pagos
- [x] Integración completa con ePayco para procesar pagos
- [x] Checkout con iframe seguro de ePayco
- [x] Confirmación automática vía webhook
- [x] Almacenamiento de referencia de pago en cada reserva
- [x] Validación de firmas digitales para seguridad
- [x] Soporte para ambiente sandbox y producción

### Modelo Predictivo
- [x] Modelo de regresión lineal para predecir ocupación
- [x] Visualización de predicciones en calendario de colaborador
- [x] Indicadores de ocupación esperada (baja/media/alta)
- [x] Integración con datos históricos de reservas

### Chatbot con IA
- [x] Widget de chat flotante en todas las páginas
- [x] Integración con Anthropic Claude o OpenAI GPT
- [x] Respuestas contextuales sobre actividades y reservas
- [x] Renderizado Markdown de respuestas
- [x] Historial de conversación por sesión

### Emails Automatizados
- [x] Email de confirmación tras crear reserva
- [x] Email recordatorio 24h antes de la actividad
- [x] Templates HTML profesionales con branding
- [x] Scheduler automático para recordatorios diarios
- [x] Configuración SMTP flexible

### Mapa de Actividades Cercanas
- [x] Vista de mapa con actividades cercanas a ubicación
- [x] Búsqueda por radio de distancia
- [x] Filtros por categoría en vista de mapa
- [x] Marcadores con vista previa de actividad
- [x] Cálculo de distancias con coordenadas geográficas

### Preguntas Frecuentes
- [x] FAQs específicas por actividad
- [x] Visualización en página de detalle
- [x] Componente interactivo (acordeón)

---

## 14. Fuera del Alcance (Out of Scope) — v2.0

Los siguientes elementos no están contemplados en la versión actual:

- App móvil nativa (iOS/Android).
- Sistema de reembolsos automatizados.
- Multi-idioma de la interfaz (el sistema maneja idiomas de actividades, no de la UI).
- Panel de analítica avanzada para colaboradores (estadísticas de sus actividades).
- Sistema de mensajería interna entre clientes y colaboradores.
- Edición de planes ya creados (solo creación y visualización).
- Compartir planes en redes sociales (funcionalidad social futura).
- Sistema de valoración/comentarios para planes (solo para actividades).
- Integración con múltiples pasarelas de pago (solo ePayco en v2.0).
- Soporte para múltiples monedas (solo COP - Pesos colombianos).
- Sistema de cupones y descuentos.
- Programa de puntos o fidelización.

---

## 15. Riesgos y Consideraciones

| Riesgo | Impacto | Mitigación |
|--------|---------|-----------|
| Imágenes almacenadas localmente en `/uploads` | Alto (pérdida de datos si se elimina el contenedor sin volumen mapeado) | Mapear el directorio como volumen persistente en Docker |
| CSRF deshabilitado en Spring Security | Medio (ataques cross-site) | Habilitar CSRF en futuras versiones o implementar tokens manualmente |
| Credenciales OAuth2 en `application.properties` | Alto (exposición si el repo es público) | Usar variables de entorno o un gestor de secretos |
| Sin paginación en algunos listados admin | Bajo | Agregar paginación cuando el volumen de datos crezca |
| Sesiones de Spring en memoria | Medio (no escalable horizontalmente) | Migrar a Redis si se necesita escalado horizontal |

---

*Documento preparado a partir del código fuente real del proyecto ETA (eta_app) — Spring Boot 3 + MySQL + Thymeleaf.*

---

## 16. Historial de Cambios

### Versión 2.0 — 24 de abril de 2026

Esta versión representa un salto significativo en funcionalidades avanzadas, agregando inteligencia artificial, pagos reales, y predicciones de ocupación.

#### Commit 1: Planes del Día + UI Responsive (8 de abril de 2026)
**Hash:** `c5672a72`

**Nuevas Características:**
- ✅ **Sistema completo de Planes del Día**: Clientes y colaboradores pueden crear rutas turísticas agrupando múltiples actividades
- ✅ **Vistas de planes**: `/planes` (listado público), `/planes/{id}` (detalle), `/mis-planes` (del creador)
- ✅ **Mapa interactivo en planes**: Muestra todas las actividades del plan con marcadores
- ✅ **Preguntas frecuentes por actividad**: Entidad `PreguntaFrecuenteActividad` con accordion UI
- ✅ **CLAUDE.md**: Documentación arquitectónica completa para IA
- ✅ **Mejoras UI/UX**: Responsive design, mejoras en login, detalle de actividad, perfil colaborador

**Archivos Clave:**
- `entity/Plan.java`, `entity/PlanActividad.java`, `entity/PreguntaFrecuenteActividad.java`
- `controller/PlanController.java`, `ClientePlanController.java`, `ColaboradorPlanController.java`
- `service/PlanService.java` + implementación
- `templates/planes/` (crear-plan.html, detalle-plan.html, mis-planes.html, planes.html)
- `static/js/planes/` (crear-plan.js, planes.js)
- `CLAUDE.md` (493 líneas de arquitectura documentada)

---

#### Commit 2: Modelo Predictivo de Ocupación (15 de abril de 2026)
**Hash:** `6f876f1f`

**Nuevas Características:**
- ✅ **Modelo de Machine Learning**: Regresión lineal para predecir ocupación de disponibilidades
- ✅ **Servicio de Predicción**: `PrediccionService` con algoritmo basado en Apache Commons Math
- ✅ **Visualización de predicciones**: Dashboard colaborador muestra ocupación esperada (baja/media/alta)
- ✅ **DTO especializado**: `PrediccionOcupacionDTO` con múltiples métricas
- ✅ **Archivo de modelo entrenado**: `ETA_modelo_predictivo.model` (190KB)
- ✅ **Documentación técnica**: `mds/creaccionDelModelo.md` (100 líneas)

**Archivos Clave:**
- `service/PrediccionService.java` + `PrediccionServiceImpl.java` (292 líneas)
- `dto/PrediccionOcupacionDTO.java`
- `static/js/prediccion.js` (242 líneas de visualización)
- `resources/ETA_modelo_predictivo.model`
- `predict/README.md` (201 líneas de explicación del algoritmo)
- Actualización de `mds/db.md` (120 líneas) y `mds/PRD.md`

**Tecnologías Agregadas:**
- Apache Commons Math 3.x para regresión lineal

---

#### Commit 3: IA Generativa + Emails + Mapa de Cercanía (22 de abril de 2026)
**Hash:** `356bb0b1`

**Nuevas Características:**
- ✅ **Chatbot con IA**: Integración con Anthropic Claude y OpenAI GPT
- ✅ **Widget de chat flotante**: Disponible en todas las páginas principales
- ✅ **Respuestas contextuales**: El bot conoce actividades, reservas y servicios de la plataforma
- ✅ **Emails automatizados**:
  - Email de confirmación tras crear reserva
  - Email recordatorio 24h antes de la actividad
  - Templates HTML profesionales (`templates/emails/`)
- ✅ **Scheduler de recordatorios**: `RecordatorioReservaScheduler` ejecuta diariamente a las 9 AM
- ✅ **Mapa de actividades cercanas**: `/actividades-cercanas` con búsqueda por radio de distancia
- ✅ **Cálculo de distancias geográficas**: Query nativa con fórmula de Haversine

**Archivos Clave:**
- `config/ChatAiConfig.java` (configuración multi-proveedor IA)
- `controller/ChatBotController.java` (62 líneas)
- `service/ChatBotService.java` (214 líneas con integración dual Anthropic/OpenAI)
- `service/EmailReservaService.java` + implementación (119 líneas)
- `service/RecordatorioReservaScheduler.java` (94 líneas con @Scheduled)
- `templates/emails/confirmacion-reserva.html` (226 líneas)
- `templates/emails/recordatorio-reserva.html` (236 líneas)
- `templates/actividades-cercanas.html` (142 líneas con mapa)
- `static/js/chat-widget.js` (185 líneas)
- `static/js/mapa-cercanas.js` (421 líneas con integración de mapas)
- `dto/ChatMensajeRequestDTO.java`, `ChatMensajeResponseDTO.java`, `ActividadCercanaDTO.java`

**Documentación:**
- `mds/implementaIA.md` (548 líneas)
- `mds/moduloMapaCercania.md` (474 líneas)
- `mds/configuracion-emails.md` (218 líneas)
- `mds/wompi.md` (792 líneas - documentación de alternativa de pago)

**Tecnologías Agregadas:**
- Spring AI (Anthropic Claude)
- OpenAI Java SDK
- Spring Mail + SMTP
- Spring @Scheduled para tareas programadas

**Dependencias Maven:**
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>service</artifactId>
</dependency>
```

---

#### Commit 4: Integración ePayco + Mejoras en Reservas (22 de abril de 2026)
**Hash:** `f95a3129`

**Nuevas Características:**
- ✅ **Pasarela de pagos ePayco**: Integración completa para Colombia
- ✅ **Checkout con iframe seguro**: `PagoController` maneja flujo de pago
- ✅ **Confirmación vía webhook**: Endpoint `/pagos/confirmacion` valida firmas digitales
- ✅ **Campo `ref_payco` en Reserva**: Almacena referencia de transacción
- ✅ **Estado de pago en reservas**: Actualización automática tras pago exitoso
- ✅ **Configuración ngrok**: Script PowerShell para desarrollo local con webhooks
- ✅ **Ambiente sandbox/producción**: Credenciales configurables
- ✅ **Mejoras en vista de reservas**: UI mejorada para colaboradores
- ✅ **Sistema de comentarios refinado**: Componente mejorado con mejor UX
- ✅ **Detalles de actividad enriquecidos**: Más información en vista de detalle

**Archivos Clave:**
- `config/EpaycoConfig.java` (78 líneas)
- `controller/PagoController.java` (141 líneas con lógica de webhooks)
- `service/EpaycoService.java` (133 líneas)
- `entity/Reserva.java` (campo `ref_payco` agregado)
- `templates/cliente/checkout.html` (199 líneas con iframe ePayco)
- `templates/colaborador/reservaciones-actividad.html` (525 líneas mejoradas)
- `templates/componentes/comentarios.html` (195 líneas refactorizadas)
- `templates/componentes/preguntasFrecuentes.html` (65 líneas)
- `templates/detalle-actividad.html` (88 líneas de mejoras)
- `static/css/cliente/detalles.css` (237 líneas)
- `static/css/componentes/preguntasFrecuentes.css` (160 líneas)
- `start-epayco-dev.ps1` (144 líneas - script automatización ngrok)

**Documentación ePayco:**
- `EPAYCO_CREDENCIALES.md` (145 líneas)
- `INICIO_RAPIDO_EPAYCO.md` (164 líneas)
- `SETUP_NGROK_EPAYCO.md` (290 líneas)
- `SOLUCION_EPAYCO_CLIENT_TOKEN.md` (346 líneas)
- `mds/epayco.md` (254 líneas)

**Configuración:**
```properties
# application.properties
epayco.public-key=test_xxx
epayco.private-key=test_xxx
epayco.p-cust-id=xxx
epayco.p-key=xxx
epayco.test=true
epayco.callback-url=https://xxx.ngrok.io/pagos/confirmacion
epayco.confirmation-url=https://xxx.ngrok.io/pagos/confirmacion
epayco.response-url=http://localhost:8080/cliente/reserva-exitosa
```

---

#### Commit 4: Correcciones JPA + Optimización de Queries (26 de mayo de 2026)
**Hash:** `pending` (en progreso)

**Cambios Técnicos:**
- ✅ **Corrección de queries JPA**: Reparación de `GROUP BY` en 4 métodos de `ActividadRepository`
  - `findTop10ByOrderByFavoritosDesc()`: `GROUP BY a.idActividad` (era `GROUP BY a`)
  - `findTop10MejorRendimiento()`: Ídem
  - `findTop10MasReservadas()`: Ídem
  - `findActividadAleatoria()`: `FUNCTION('RAND')` (era `ORDER BY RAND()`)
- ✅ **Actualización de documentación**: Sincronización de `db.md` y `PRD.md` a estado 26 de mayo
- ✅ **Nueva entidad documentada**: `PreguntaFrecuenteActividad` en `db.md`
- ✅ **Configuración de pagos documentada**: `ConfiguracionPagos` entidad con credenciales ePayco
- ✅ **Índices de base de datos**: Documentados 9 índices recomendados para optimización
- ✅ **Validaciones de negocio**: Documentadas 9 validaciones de integridad críticas

**Archivos Actualizados:**
- `src/main/java/maineta/eta/repository/ActividadRepository.java` (4 métodos JPA reparados)
- `mds/db.md` (Versión 2.0 - 26 de mayo, +800 líneas con optimizaciones)
- `mds/PRD.md` (Versión 2.0 - 26 de mayo, actualizado con Commit 4)

**Contexto:**
Después de agregar múltiples integraciones avanzadas (IA, pagos, predicción) en abril, se identificaron errores de compilación en el layer de persistencia relacionados con MySQL strict mode. Esta actualización documenta las correcciones y optimizaciones aplicadas.

---

### Resumen de Cambios v1.0 → v2.0

| Categoría | Cambios |
|-----------|---------|
| **Nuevas Entidades** | Plan, PlanActividad, PreguntaFrecuenteActividad |
| **Nuevos Servicios** | PlanService, PrediccionService, ChatBotService, EmailReservaService, EpaycoService |
| **Nuevos Controllers** | PlanController, ClientePlanController, ColaboradorPlanController, ChatBotController, PagoController |
| **Integraciones Externas** | ePayco, Anthropic Claude, OpenAI GPT, Google Maps API |
| **Machine Learning** | Modelo predictivo de ocupación (regresión lineal) |
| **Emails** | Confirmación y recordatorios automatizados con templates HTML |
| **UI/UX** | Responsive design completo, chat widget, mapas interactivos |
| **Documentación** | +2,500 líneas de documentación técnica en `/mds` |
| **Scripts** | `start-epayco-dev.ps1` para automatización ngrok |
| **Líneas de Código** | ~8,000 líneas nuevas en Java/HTML/JS/CSS |

---

### Próximas Versiones Planificadas

**v2.1 (Mayo 2026):**
- Sistema de notificaciones en tiempo real (WebSocket completo)
- Panel de analítica para colaboradores
- Sistema de reembolsos

**v3.0 (Junio 2026):**
- App móvil (React Native/Flutter)
- Multi-idioma en interfaz
- Sistema de cupones y descuentos
- Programa de puntos de fidelización

---

*Última actualización: 26 de mayo de 2026*
