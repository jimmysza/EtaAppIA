# Product Requirements Document (PRD)
## Plataforma ETA — Marketplace de Actividades Turísticas y Recreativas

**Versión:** 1.0  
**Fecha:** 18 de marzo de 2026  
**Estado:** En desarrollo  

---

## 1. Resumen Ejecutivo

ETA es una plataforma web de tipo marketplace que conecta a proveedores de actividades turísticas y recreativas (denominados **Colaboradores**) con usuarios finales (**Clientes**). Los colaboradores pueden publicar, gestionar y monetizar sus actividades; los clientes pueden descubrirlas, reservarlas y calificarlas. Un **Administrador** supervisa la operación global, gestiona los datos maestros del sistema y controla la comisión de la plataforma.

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
- Gestionar sus reservas desde su dashboard personal.
- Escriturar comentarios y calificaciones (1-5 estrellas) solo si tiene una reserva completada en esa actividad.
- Guardar actividades como favoritas y acceder a la lista desde su perfil.
- Editar su información personal (nombre, cedula, dirección, teléfono, preferencias).
- **Crear planes del día**: Agrupar múltiples actividades en una ruta temática con orden, horarios sugeridos y notas personalizadas.
- **Ver sus planes creados** y compartirlos públicamente.
- **Explorar planes públicos** de otros clientes y colaboradores en vista de mapa.

### 4.2 Colaborador
Proveedor/empresa que ofrece actividades.

**Capacidades:**
- Registrarse con correo/contraseña, con NIT e ID de seguridad.
- Crear actividades con: título, descripción, categoría, idioma, precio, ubicación (lat/lon), normas, qué incluye, condiciones, imagen principal y galería.
- Configurar disponibilidades puntuales (fecha específica, hora inicio/fin, cupos) o mediante **patrones recurrentes** (días de la semana, rango de fechas, hora y cupos).
- Editar o actualizar actividades existentes.
- Ver listado paginado de sus actividades.
- Consultar las reservas recibidas en cada actividad/disponibilidad.
- Tener un perfil público visible para los clientes.
- **Crear planes del día**: Agrupar múltiples actividades (propias o de otros) en una ruta temática con orden, horarios sugeridos y notas personalizadas.
- **Ver sus planes creados** y compartirlos públicamente.
- **Explorar planes públicos** en vista de mapa.

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
| `reserva` | id, estado (Pendiente/Confirmada/Cancelada/Hecho), cantidad, fechaReserva → FK disponibilidad, cliente, actividad |
| `comentario` | id, texto, calificacion (1-5), fechaComentario → FK cliente, actividad |
| `favorito` | id, createdAt → FK cliente, actividad (UNIQUE juntos) |
| `categoria` | id, nombre (UNIQUE), imagen |
| `idioma` | id, codigo (ej: 'es'), nombre (ej: 'Español') |
| `imagen_actividad` | id, nombre → FK actividad |
| `planes` | id, titulo, descripcion, imagenPortada, duracionEstimada, tipo, idClienteCreador, idColaboradorCreador, fechaCreacion, publico, vistas |
| `plan_actividades` | id, idPlan, idActividad, orden, horaSugerida, notaPersonalizada |

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
| Email | Spring Mail |
| Validación | Spring Validation (Bean Validation) |

### 8.3 Componentes del Sistema

```
┌─────────────────────────────────────────────────────┐
│                  CLIENTE (Navegador)                 │
│           HTML + Thymeleaf + Tailwind + JS           │
└───────────────────────┬─────────────────────────────┘
                        │ HTTP
┌───────────────────────▼─────────────────────────────┐
│              SPRING BOOT APPLICATION                  │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │ Controllers │→ │   Services   │→ │Repositories │ │
│  │ (6 ctrlrs) │  │  (14+ svcs)  │  │  (JPA/SQL)  │ │
│  └─────────────┘  └──────────────┘  └──────┬──────┘ │
│  ┌─────────────────────────────────────────┐│        │
│  │ Spring Security + OAuth2 + BCrypt       ││        │
│  └─────────────────────────────────────────┘│        │
└────────────────────────────────────────────┬┘        │
                                             │         │
┌────────────────────────────────────────────▼─────────┐
│                    MySQL 8                            │
│                  (15 tablas)                          │
└──────────────────────────────────────────────────────┘
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
```

**Rutas de Cliente (ROLE_CLIENTE):**
```
GET  /cliente/dashboard         → Mis reservas
GET  /cliente/checkout/{idDispo}→ Formulario de checkout
POST /cliente/reservar          → Confirmar reserva
GET  /cliente/informacion       → Mi perfil
POST /cliente/actualizar/{id}   → Actualizar mi perfil
POST /comentarios/agregar/{id}  → Agregar comentario
```

**Rutas de Colaborador (ROLE_COLABORADOR):**
```
GET  /colaborador/dashboard     → Dashboard del colaborador
GET  /colaborador/actividades   → Mis actividades (paginado)
GET  /colaborador/actividades/nueva → Formulario crear actividad
POST /colaborador/actividades/addAct → Guardar actividad
POST /colaborador/actividades/{id}/actualizar → Editar actividad
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

---

## 14. Fuera del Alcance (Out of Scope) — v1.0

Los siguientes elementos no están contemplados en la versión actual:

- Pasarela de pago real (pagos con tarjeta, PSE, etc.) — actualmente el checkout registra la reserva pero no procesa pago.
- Sistema de notificaciones push o en tiempo real (el websocket existe pero su alcance completo está pendiente).
- App móvil nativa (iOS/Android).
- Sistema de reembolsos automatizados.
- Multi-idioma de la interfaz (el sistema maneja idiomas de actividades, no de la UI).
- Panel de analítica avanzada para colaboradores (estadísticas de sus actividades).
- Sistema de mensajería interna entre clientes y colaboradores.
- Edición de planes ya creados (solo creación y visualización).
- Compartir planes en redes sociales (funcionalidad social futura).
- Sistema de valoración/comentarios para planes (solo para actividades).
- Edición de planes ya creados (solo creación y visualización).
- Compartir planes en redes sociales (funcionalidad social futura).
- Sistema de valoración/comentarios para planes (solo para actividades).

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
