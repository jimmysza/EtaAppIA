# SPEC.MD — Plataforma ETA
## Marketplace de Actividades Turísticas y Recreativas

**Versión:** 1.0  
**Fecha:** 18 de marzo de 2026  
**Estado:** En desarrollo  
**Stack:** Java 17 · Spring Boot 3 · Thymeleaf · MySQL 8 · Docker  

---

## 1. Descripción General

ETA es una aplicación web monolítica tipo **marketplace** que conecta a proveedores de actividades turísticas y recreativas (**Colaboradores**) con usuarios finales (**Clientes**). Los colaboradores publican, gestionan y monetizan sus actividades; los clientes las descubren, reservan y califican. Un **Administrador** supervisa la operación global, gestiona datos maestros y controla la comisión de la plataforma.

### Problema que Resuelve

Los turistas y personas que buscan experiencias recreativas no tienen un punto centralizado donde descubrir actividades locales verificadas, comparar precios, revisar reseñas y realizar reservas en tiempo real. Los pequeños proveedores carecen de una vitrina digital asequible con herramientas de gestión de reservas integradas. ETA resuelve ambos lados del mercado.

### Objetivos

| # | Objetivo | Métrica de Éxito |
|---|----------|-----------------|
| 1 | Colaboradores publican actividades y gestionan disponibilidad | ≥1 actividad creada con disponibilidad configurada por colaborador registrado |
| 2 | Clientes completan reservas sin fricción | Tasa de abandono en checkout < 20% |
| 3 | Generar ingreso por comisiones en cada reserva | Comisión configurable (default 18%) |
| 4 | Administrador con visibilidad completa de la operación | Dashboard con métricas en tiempo real |
| 5 | Seguridad y roles bien diferenciados | 0 accesos no autorizados entre roles |

---

## 2. Glosario

| Término | Definición |
|---------|-----------|
| **Cliente** | Usuario final que busca y reserva actividades. Rol: `ROLE_CLIENTE` |
| **Colaborador** | Proveedor o empresa que publica actividades. Rol: `ROLE_COLABORADOR` |
| **Administrador** | Operador de la plataforma. Rol: `ROLE_ADMIN` |
| **Actividad** | Servicio turístico o recreativo publicado por un colaborador |
| **Disponibilidad** | Instancia de una actividad en una fecha/hora específica con cupos |
| **PatronDisponibilidad** | Regla recurrente que genera instancias de disponibilidad automáticamente |
| **Reserva** | Registro de un cliente que ha seleccionado una disponibilidad concreta |
| **Cupos** | Número de plazas disponibles en una instancia de disponibilidad |
| **Comisión** | Porcentaje del precio que retiene la plataforma por cada reserva (default 18%) |
| **Slug** | Versión amigable del título de una actividad usada en la URL |
| **emailVerificado** | Flag booleano que indica si el usuario completó la verificación de su cuenta |
| **BCrypt** | Algoritmo de hashing para contraseñas almacenadas en la base de datos |

---

## 3. Arquitectura del Sistema

### 3.1 Tipo de Arquitectura

Aplicación web **monolítica** con patrón **MVC** (Model-View-Controller). Renderizado del lado del servidor via **Thymeleaf**. Sin separación frontend/backend: el mismo servidor sirve HTML, lógica y datos.

### 3.2 Stack Tecnológico

| Capa | Tecnología | Versión |
|------|-----------|---------|
| Lenguaje | Java | 17 |
| Framework backend | Spring Boot | 3.5.7 |
| Persistencia | Spring Data JPA / Hibernate | — |
| Seguridad | Spring Security + OAuth2 Client | — |
| Motor de vistas | Thymeleaf | 3 |
| Estilos | Tailwind CSS | — |
| Interactividad | Vanilla JavaScript | — |
| Base de datos | MySQL | 8 |
| Build tool | Apache Maven | — |
| Contenerización | Docker + Docker Compose | — |
| Email | Spring Mail (SMTP) | — |
| Validación | Spring Validation (Bean Validation) | — |

### 3.3 Diagrama de Componentes

```
┌──────────────────────────────────────────────────────┐
│                  CLIENTE (Navegador)                  │
│           HTML + Thymeleaf + Tailwind + JS            │
└───────────────────────┬──────────────────────────────┘
                        │ HTTP
┌───────────────────────▼──────────────────────────────┐
│              SPRING BOOT APPLICATION                   │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │ Controllers │→ │   Services   │→ │Repositories │  │
│  │  (6 ctrlrs) │  │  (14+ svcs)  │  │  (JPA/SQL)  │  │
│  └─────────────┘  └──────────────┘  └──────┬──────┘  │
│  ┌──────────────────────────────────────────┐         │
│  │  Spring Security + OAuth2 + BCrypt       │         │
│  └──────────────────────────────────────────┘         │
└────────────────────────────────────────────┬──────────┘
                                             │
┌────────────────────────────────────────────▼──────────┐
│                     MySQL 8                            │
│                   (15 tablas)                          │
└───────────────────────────────────────────────────────┘
```

### 3.4 Infraestructura Docker

| Servicio | Imagen | Puerto | Descripción |
|---------|--------|--------|-------------|
| `app` | Dockerfile (Java 17) | 8080 | Aplicación Spring Boot |
| `db` | mysql:8 | 3306 | Base de datos MySQL |

**Volúmenes:**
- `mysql_data/` → Persistencia de datos MySQL
- `uploads/` → Imágenes subidas por los usuarios, accesibles desde la app

**Inicio:** `docker-compose up`

---

## 4. Modelo de Datos

### 4.1 Diagrama de Relaciones

```
Usuario ────────────── Rol
   │
   ├─── Cliente ───── Reserva ──── Disponibilidad ──── Actividad
   │       │                                               │
   │       ├─── Comentario ──────────────────────────── Actividad
   │       └─── Favorito  ──────────────────────────── Actividad
   │
   ├─── Colaborador ── Actividad ── Categoria
   │                       │
   │                       ├──── Idioma
   │                       ├──── Disponibilidad
   │                       ├──── PatronDisponibilidad
   │                       └──── ImagenActividad
   │
   └─── Admin
```

### 4.2 Entidades y Campos

| Entidad | Campos Principales |
|---------|-------------------|
| `usuarios` | id, nombre, email (UNIQUE), password (BCrypt), telefono, emailVerificado, tokenVerificacion, createdAt |
| `roles` | id, nombre (`ROLE_CLIENTE` / `ROLE_COLABORADOR` / `ROLE_ADMIN`) |
| `cliente` | id, cedula (UNIQUE), direccion, preferencias → FK usuario |
| `colaborador` | id, nit, correoSeguridad → FK usuario |
| `admin` | id, porcentajeComision (default 18.00) → FK usuario |
| `actividad` | id, titulo, descripcion, precio, ubicacion, latitud, longitud, normas, incluye, condiciones, imagen, calificacion, createdAt → FK colaborador, categoria, idioma |
| `disponibilidad` | id, fecha, horaInicio, horaFin, cuposTotales, cuposDisponibles, estado → FK actividad |
| `patron_disponibilidad` | id, diasSemana, horaInicio, horaFin, cuposTotales, fechaInicio, fechaFin, estado → FK actividad |
| `reserva` | id, estado, cantidad, fechaReserva → FK disponibilidad, cliente, actividad |
| `comentario` | id, texto, calificacion (1-5), fechaComentario → FK cliente, actividad |
| `favorito` | id, createdAt → FK cliente, actividad (UNIQUE juntos) |
| `categoria` | id, nombre (UNIQUE), imagen |
| `idioma` | id, codigo (ej: `es`), nombre (ej: `Español`) |
| `imagen_actividad` | id, nombre → FK actividad |

### 4.3 Estados de Entidades

**Reserva:**
```
Pendiente → Confirmada → Hecho
         → Cancelada
```

**Disponibilidad:**
```
DISPONIBLE → COMPLETADO
           → CANCELADO
```

---

## 5. Roles y Capacidades

### 5.1 Usuario Público (no autenticado)
- Ver Landing page y explorar actividades
- Buscar y filtrar actividades por texto o categoría
- Ver detalle completo de una actividad
- Ver perfiles públicos de colaboradores
- Registrarse o iniciar sesión

### 5.2 Cliente (`ROLE_CLIENTE`)
- Todo lo que puede el usuario público
- Registrarse con email/contraseña y verificar cuenta
- Iniciar sesión con Google (OAuth2)
- Seleccionar disponibilidades y confirmar reservas
- Ver y cancelar su historial de reservas (dashboard)
- Dejar comentario + calificación (solo con reserva en estado `Hecho`)
- Marcar/desmarcar actividades como favoritas
- Editar su información personal

### 5.3 Colaborador (`ROLE_COLABORADOR`)
- Registrarse con email/contraseña, NIT e ID de seguridad
- Crear actividades con todos sus campos e imágenes
- Configurar disponibilidades puntuales o mediante patrones recurrentes
- Editar sus propias actividades
- Ver listado paginado de sus actividades
- Consultar reservas recibidas por actividad/disponibilidad
- Tener perfil público visible

### 5.4 Administrador (`ROLE_ADMIN`)
- Ver dashboard con métricas globales (ingresos, usuarios, actividades, reservas)
- CRUD de categorías (nombre, imagen)
- CRUD de idiomas (código, nombre)
- Configurar porcentaje de comisión de la plataforma
- Ver listado de clientes registrados

---

## 6. Requerimientos Funcionales

### AUTH — Autenticación y Gestión de Cuentas

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

### CAT — Catálogo y Búsqueda (Público)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| CAT-01 | Landing con actividades destacadas y categorías | Alta |
| CAT-02 | Búsqueda de actividades por texto libre | Alta |
| CAT-03 | Filtrado de actividades por categoría | Alta |
| CAT-04 | Página de resultados con lista paginada | Alta |
| CAT-05 | Detalle de actividad: título, descripción, precio, mapa, galería, normas, incluye, condiciones, idioma, categoría, calificación y comentarios | Alta |
| CAT-06 | URL amigable: `/actividad/{slug}-{id}` | Media |
| CAT-07 | Lista pública de colaboradores con perfil básico y ranking | Media |

### DISP — Gestión de Disponibilidad (Colaborador)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| DISP-01 | Crear disponibilidad puntual: fecha, hora inicio, hora fin, cupos | Alta |
| DISP-02 | Crear patrón recurrente: días de semana, rango de fechas, hora, cupos | Alta |
| DISP-03 | Generar instancias automáticas a partir de un patrón | Alta |
| DISP-04 | Ver calendario de disponibilidades de una actividad | Media |
| DISP-05 | Estado de disponibilidad: `DISPONIBLE`, `CANCELADO`, `COMPLETADO` | Alta |
| DISP-06 | Actualización automática de `cuposDisponibles` al reservar o cancelar | Alta |

### RES — Reservas (Cliente)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RES-01 | Seleccionar disponibilidad desde el detalle de la actividad | Alta |
| RES-02 | Pantalla de checkout con resumen (actividad, fecha, hora, cupos, precio total) | Alta |
| RES-03 | Confirmar reserva: crear registro y decrementar `cuposDisponibles` | Alta |
| RES-04 | Estados de reserva: `Pendiente`, `Confirmada`, `Cancelada`, `Hecho` | Alta |
| RES-05 | Historial de reservas en el dashboard del cliente | Alta |
| RES-06 | Cancelar reserva (devuelve cupos a la disponibilidad) | Media |

### COM — Comentarios y Calificaciones

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| COM-01 | Solo clientes con reserva en estado `Hecho` pueden comentar esa actividad | Alta |
| COM-02 | Comentario incluye texto + calificación de 1 a 5 estrellas | Alta |
| COM-03 | Calificación promedio de la actividad se recalcula tras cada comentario | Alta |
| COM-04 | Comentarios visibles en el detalle de la actividad | Alta |

### FAV — Favoritos

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| FAV-01 | Cliente puede marcar/desmarcar actividad como favorita | Media |
| FAV-02 | Combinación cliente-actividad es única (constraint UNIQUE) | Alta |
| FAV-03 | Cliente puede ver su lista de favoritos en su panel | Media |

### ACT — Gestión de Actividades (Colaborador)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| ACT-01 | Crear actividad con todos sus campos e imagen principal | Alta |
| ACT-02 | Subir galería de imágenes adicionales | Media |
| ACT-03 | Editar actividad existente (campos e imagen) | Alta |
| ACT-04 | Ver listado paginado de propias actividades | Alta |
| ACT-05 | Asociar actividad a categoría e idioma | Alta |
| ACT-06 | Ingresar coordenadas (lat/lon) para mostrar en mapa | Media |

### ADM — Panel de Administración

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| ADM-01 | Dashboard: ingresos totales, usuarios, colaboradores, actividades y reservas | Alta |
| ADM-02 | CRUD de categorías (nombre, imagen) | Alta |
| ADM-03 | CRUD de idiomas (código, nombre) | Alta |
| ADM-04 | Ver listado de clientes registrados | Media |
| ADM-05 | Configurar porcentaje de comisión | Alta |

### FILE — Subida de Archivos

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| FILE-01 | Subida de imágenes para actividades almacenadas en `/uploads` | Alta |
| FILE-02 | Subida de imágenes para categorías | Media |
| FILE-03 | Las imágenes son accesibles públicamente vía URL | Alta |

---

## 7. Requerimientos No Funcionales

| ID | Categoría | Requerimiento |
|----|-----------|--------------|
| NF-01 | Seguridad | Contraseñas almacenadas con BCrypt (nunca en texto plano) |
| NF-02 | Seguridad | Protección de rutas por rol mediante Spring Security |
| NF-03 | Seguridad | Verificación de email obligatoria antes de activar la cuenta |
| NF-04 | Seguridad | Sesión única por usuario (sin sesiones simultáneas) |
| NF-05 | Rendimiento | Paginación en listados para evitar carga masiva de datos |
| NF-06 | Usabilidad | Interfaz responsiva (móviles, tablets, escritorio) con Tailwind CSS |
| NF-07 | Usabilidad | Mensajes de error y éxito claros en todas las operaciones |
| NF-08 | Mantenibilidad | Arquitectura MVC estricta: separación controllers / services / repositories |
| NF-09 | Portabilidad | Sistema completo levantable con `docker-compose up` |
| NF-10 | Compatibilidad | Compatible con Java 17 y MySQL 8 |
| NF-11 | Disponibilidad | Plataforma operativa 24/7 cuando está desplegada |

---

## 8. Reglas de Negocio

| ID | Regla |
|----|-------|
| RN-01 | Solo clientes con al menos una reserva en estado `Hecho` pueden dejar comentario en esa actividad |
| RN-02 | Un cliente no puede tener dos favoritos para la misma actividad (UNIQUE constraint) |
| RN-03 | Comisión = `precio_actividad × cantidad_personas × porcentaje_comision` |
| RN-04 | Una reserva solo es posible si `cuposDisponibles > 0` en la disponibilidad seleccionada |
| RN-05 | Al cancelar una reserva, los cupos se devuelven a la disponibilidad correspondiente |
| RN-06 | Un usuario no puede autenticarse si `emailVerificado = false` |
| RN-07 | Cada usuario puede tener una sola sesión activa simultánea |
| RN-08 | Un colaborador solo puede editar sus propias actividades |
| RN-09 | El porcentaje de comisión es global y configurable únicamente por el administrador |
| RN-10 | Los patrones de disponibilidad generan instancias para cada día del rango que coincida con los días configurados |

---

## 9. Buenas Prácticas y Abstracciones

### 9.1 Arquitectura MVC

- **Controllers:** Solo reciben requests HTTP, llaman al servicio y devuelven la vista o redirección. Sin lógica de negocio.
- **Services:** Contienen toda la lógica de negocio. Son los únicos que acceden a los repositorios.
- **Repositories:** Interfaces JPA. Solo realizan operaciones de persistencia.
- **Entidades/Models:** Clases Java anotadas con `@Entity`. Representan las tablas de la BD.

### 9.2 Seguridad

- Nunca almacenar contraseñas en texto plano: siempre BCrypt.
- Las rutas protegidas deben estar configuradas en `SecurityConfig`.
- Las credenciales OAuth2 nunca deben ir en repositorios públicos: usar variables de entorno.
- CSRF debe habilitarse en versiones futuras.

### 9.3 Imágenes y Archivos

- Las imágenes se guardan en el servidor en `/uploads/`.
- El directorio `uploads/` debe estar mapeado como volumen Docker para garantizar persistencia.
- Las URLs de las imágenes deben ser accesibles públicamente sin autenticación.

### 9.4 Paginación

- Todos los listados de tamaño indeterminado deben estar paginados (actividades, reservas, clientes).
- Usar `Pageable` de Spring Data JPA.

### 9.5 Validación

- Usar Bean Validation (`@NotBlank`, `@Email`, `@Size`, etc.) en los DTOs/formularios.
- Mostrar mensajes de error amigables en la UI, nunca stack traces.

### 9.6 Disponibilidades

- La generación de instancias desde patrones debe ser un proceso automatizado y atómico.
- El campo `cuposDisponibles` debe actualizarse de forma transaccional al reservar/cancelar para evitar condiciones de carrera.

---

## 10. Interfaces / Vistas del Sistema

| Sección | Vista | Ruta | Acceso |
|---------|-------|------|--------|
| Pública | Landing / Home | `GET /` | Todos |
| Pública | Detalle de Actividad | `GET /actividad/{slug}-{id}` | Todos |
| Pública | Resultados de Búsqueda | `GET /resultados-busqueda` | Todos |
| Pública | Perfil Público Colaborador | `GET /colaboradores/{id}` | Todos |
| Pública | Lista de Colaboradores | `GET /colaboradores` | Todos |
| Auth | Login | `GET /login` | No autenticados |
| Auth | Registro Cliente | `GET /registro/cliente` | No autenticados |
| Auth | Registro Colaborador | `GET /registro/colaborador` | No autenticados |
| Cliente | Dashboard (mis reservas) | `GET /cliente/dashboard` | ROLE_CLIENTE |
| Cliente | Mi Información | `GET /cliente/informacion` | ROLE_CLIENTE |
| Cliente | Mis Favoritos | — | ROLE_CLIENTE |
| Cliente | Checkout | `GET /cliente/checkout/{idDispo}` | ROLE_CLIENTE |
| Colaborador | Dashboard | `GET /colaborador/dashboard` | ROLE_COLABORADOR |
| Colaborador | Mis Actividades | `GET /colaborador/actividades` | ROLE_COLABORADOR |
| Colaborador | Crear Actividad | `GET /colaborador/actividades/nueva` | ROLE_COLABORADOR |
| Colaborador | Editar Actividad | — | ROLE_COLABORADOR |
| Admin | Dashboard (métricas) | `GET /admin/dashboard` | ROLE_ADMIN |
| Admin | Gestión de Categorías | `GET /admin/categorias` | ROLE_ADMIN |
| Admin | Gestión de Idiomas | `GET /admin/idiomas` | ROLE_ADMIN |
| Admin | Lista de Clientes | — | ROLE_ADMIN |
| Error | 403 Acceso Denegado | `/403` | Todos |
| Error | 404 No Encontrado | `/404` | Todos |

---

## 11. Rutas de la API / Endpoints

### Públicas
```
GET  /                              → Landing page
GET  /login                         → Formulario de login
GET  /registro/cliente              → Registro de cliente
GET  /registro/colaborador          → Registro de colaborador
GET  /registro/verificar?token=XYZ  → Verificar email
GET  /actividad/{slug}-{id}         → Detalle de actividad
GET  /resultados-busqueda           → Búsqueda de actividades
GET  /colaboradores                 → Lista de colaboradores
```

### Cliente (ROLE_CLIENTE)
```
GET  /cliente/dashboard             → Mis reservas
GET  /cliente/checkout/{idDispo}    → Formulario de checkout
POST /cliente/reservar              → Confirmar reserva
GET  /cliente/informacion           → Mi perfil
POST /cliente/actualizar/{id}       → Actualizar perfil
POST /comentarios/agregar/{id}      → Agregar comentario
```

### Colaborador (ROLE_COLABORADOR)
```
GET  /colaborador/dashboard                         → Dashboard
GET  /colaborador/actividades                       → Mis actividades (paginado)
GET  /colaborador/actividades/nueva                 → Formulario crear actividad
POST /colaborador/actividades/addAct                → Guardar actividad
POST /colaborador/actividades/{id}/actualizar       → Editar actividad
```

### Administrador (ROLE_ADMIN)
```
GET  /admin/dashboard               → Métricas globales
GET  /admin/categorias              → Gestión de categorías
POST /admin/categorias/nueva        → Crear categoría
POST /admin/categorias/eliminar     → Eliminar categoría
GET  /admin/idiomas                 → Gestión de idiomas
POST /admin/idiomas/nueva           → Crear idioma
POST /admin/idiomas/eliminar        → Eliminar idioma
```

---

## 12. Flujos de Usuario Principales

### 12.1 Registro y Verificación
```
Usuario
  → /registro/cliente  (POST datos)
  → Crear Usuario + Rol en BD
  → Enviar email con token de verificación
  → Redirigir a /login?pendingVerification
  → Usuario hace clic en enlace del email
  → GET /registro/verificar?token=XYZ
  → emailVerificado = true
  → Redirigir a /login (cuenta activa)
```

### 12.2 Flujo de Reserva
```
Cliente autenticado
  → Busca actividad
  → Abre detalle (/actividad/{slug}-{id})
  → Ve disponibilidades (fechas, horas, cupos)
  → Selecciona disponibilidad
  → GET /cliente/checkout/{idDispo}
  → Revisa resumen y confirma
  → POST /cliente/reservar
  → Se crea Reserva (estado: "Pendiente")
  → cuposDisponibles–
  → Redirige a /cliente/dashboard
```

### 12.3 Publicación de Actividad (Colaborador)
```
Colaborador autenticado
  → GET /colaborador/actividades/nueva
  → Completa formulario (título, precio, categoría, idioma, imagen, etc.)
  → POST /colaborador/actividades/addAct
  → Se guarda Actividad + ImagenActividad
  → Redirige a /colaborador/actividades
  → Configura disponibilidades o patrones recurrentes
```

### 12.4 Flujo de Comentario
```
Cliente con reserva en estado "Hecho"
  → Accede al detalle de la actividad
  → Escribe comentario + selecciona calificación (1-5)
  → POST /comentarios/agregar/{idActividad}
  → Sistema valida que tiene reserva completada
  → Guarda Comentario
  → Recalcula calificación promedio de la actividad
```

---

## 13. Tareas y Funcionalidades — Backlog

### MÓDULO: Autenticación

- [x] Implementar registro de cliente (formulario + validación + servicio)
- [x] Implementar registro de colaborador (formulario + validación + servicio)
- [x] Implementar login estándar (email + contraseña) con Spring Security
- [x] Configurar OAuth2 login con Google
- [x] Enviar email de verificación al registrarse (token único)
- [x] Endpoint de verificación de token (`/registro/verificar`)
- [x] Redirigir post-login según rol del usuario
- [x] Limitar a 1 sesión activa por usuario
- [x] Implementar logout con redirección a `/logout?success`
- [x] Configurar página de error 403

### MÓDULO: Catálogo Público

- [ ] Diseñar y construir Landing page con actividades destacadas las mas reservadas 
- [x] Implementar búsqueda por texto libre en actividades
- [x] Implementar filtro por categoría
- [x] Construir página de resultados con paginación
- [x] Construir página de detalle de actividad (todos los campos + mapa + galería + comentarios)
- [x] Generar slugs amigables para las actividades (`/actividad/{slug}-{id}`)
- [x] Lista pública de colaboradores

### MÓDULO: Disponibilidades

- [x] Formulario para crear disponibilidad puntual
- [x] Formulario para crear patrón de disponibilidad recurrente
- [x] Servicio que genera instancias de disponibilidad desde un patrón
- [x] Calendario de disponibilidades por actividad
- [x] Gestionar estados de disponibilidad (`DISPONIBLE`, `CANCELADO`, `COMPLETADO`)
- [x] Actualización transaccional de `cuposDisponibles`

### MÓDULO: Reservas

- [] meojorar selector de disponibilidad en la página de detalle
- [] mejorar Pantalla de checkout con resumen de reserva y mejorar yu
- [x] Mejorar Dashboard del cliente con historial de reservas
- [] meojrar ux de cancelar reserva (y devolver cupos)

### MÓDULO: Comentarios y Calificaciones

- [x] Validar que el cliente tiene reserva `Hecho` antes de permitir comentario
- [x] Formulario de comentario + calificación estrellas (1-5) y distrubicion de estrellas
- [x] Servicio que recalcula calificación promedio de la actividad
- [x] Mostrar comentarios en la página de detalle
- [x] realizar paginacion de  comentarios en la página de detalle

### MÓDULO: Favoritos

- [x] Botón marcar/desmarcar favorito en la página de detalle
- [x] Servicio de favoritos con constraint UNIQUE en BD
- [x] Vista de lista de favoritos en el panel del cliente

### MÓDULO: Gestión de Actividades (Colaborador)

- [x] Formulario de creación de actividad (todos los campos)
- [x] Subida de imagen principal
- [ ] Arreglar Galería de imágenes adicionales
- [x] Listado paginado de actividades del colaborador
- [x] Formulario de edición de actividad
- [ ] Vista de reservas recibidas por actividad se organize se vea que dia es hoy se muestren las reservas para hoy,se puedan mostrar las del dia que decidas se pueda tener filtro de por estado

### MÓDULO: Panel de Administración

- [x] Dashboard con métricas globales (ingresos, conteos)
- [ ] CRUD de categorías con imagen
- [x] CRUD de idiomas
- [x] Listado de clientes registrados con paginacion
- [x] Formulario para actualizar porcentaje de comisión 

### MÓDULO: Infraestructura

- [x] Configurar Docker + Docker Compose (app + db)
- [x] Mapear volúmenes de persistencia (`mysql_data/`, `uploads/`)
- [x] Configurar variables de entorno en `application.properties`
- [x] Configurar Spring Mail para envío de emails
- [x] Servicio de almacenamiento y servicio de archivos en `/uploads`

---

## 17. Criterios de Aceptación

### Autenticación
- WHEN un usuario se registra con email y contraseña, THEN el sistema crea la cuenta y envía un email de verificación
- WHEN un usuario intenta iniciar sesión sin haber verificado el email, THEN el acceso es denegado con mensaje claro
- WHEN un usuario autenticado intenta acceder a una ruta de otro rol, THEN recibe error 403
- WHEN un usuario inicia sesión con Google, THEN es redirigido correctamente según su rol
- WHEN un usuario cierra sesión, THEN es redirigido a `/logout?success` y su sesión queda invalidada

### Actividades
- WHEN un colaborador crea una actividad con imagen, THEN la actividad aparece en su catálogo
- WHEN un usuario público accede al detalle de una actividad, THEN puede verla sin iniciar sesión
- WHEN un usuario busca por texto o categoría, THEN obtiene resultados relevantes paginados

### Disponibilidades
- WHEN un colaborador crea un patrón recurrente, THEN el sistema genera automáticamente las instancias de disponibilidad para el rango de fechas configurado
- WHEN un cliente realiza una reserva, THEN `cuposDisponibles` se decrementa en 1 de forma inmediata

### Reservas
- WHEN un cliente intenta reservar una disponibilidad con `cuposDisponibles = 0`, THEN el sistema impide la reserva
- WHEN un cliente confirma la reserva, THEN aparece en su dashboard con estado `Pendiente`
- WHEN un cliente cancela una reserva, THEN los cupos son devueltos a la disponibilidad correspondiente

### Comentarios
- WHEN un cliente con reserva `Hecho` envía un comentario, THEN este aparece en el detalle de la actividad y la calificación promedio se actualiza
- WHEN un cliente sin reserva `Hecho` intenta comentar, THEN el sistema lo impide con mensaje de error

### Administración
- WHEN el administrador accede al dashboard, THEN ve el total de ingresos por comisiones, número de usuarios, actividades y reservas
- WHEN el administrador actualiza el porcentaje de comisión, THEN el nuevo porcentaje se aplica a las reservas futuras

---

## 15. Tareas Pendientes — Ideas del Proyecto (Notion)

> Estas tareas fueron extraídas directamente de la tabla **"Tareas De Eta"** en Notion. Representan funcionalidades, mejoras e ideas que se quieren implementar más allá del MVP base.

### 🎨 UI / UX
- [ ] **Mejora general en la UI/UX** — Interfaz más clara, atractiva y fácil de usar para que el cliente encuentre actividades rápidamente
- [x] **Mejorar formulario de registro del colaborador** — Formulario más intuitivo, sin pasos innecesarios ni confusiones
- [x] **Editar actividades por secciones individuales** — Permitir edición parcial de cada campo de una actividad de forma independiente
- [x] **Más imágenes por actividad** — Soporte de galería múltiple para que el cliente vea más fotos antes de reservar

### 🔍 Búsqueda y Descubrimiento
- [ ] **Arquitectura de búsqueda avanzada** — Búsqueda por categoría, precio, ubicación y fecha; no solo texto libre
- [ ] **Filtros por rol** — Filtros específicos segun rol colaborador (ver sus reservas por fecha, mostrar por hora), admin (gestionar usuarios)
- [ ] **Estructura de URL por ciudad y categoría** — Rutas como `/actividades/{ciudad}` y `/actividades/{ciudad}/{categoria}` para SEO y navegación
- [ ] **Mejorar SEO con metatags** — Implementar metatags, Open Graph y datos estructurados en páginas de actividades y categorías
- [ ] **Categorías con descripción SEO** — Que cada categoría tenga una descripción que ayude al posicionamiento y explique qué tipo de actividades contiene

### 🗺️ Ubicación
- [x] **Ubicaciones exactas con latitud y longitud** — Mostrar mapa interactivo con coordenadas precisas en el detalle de cada actividad para que el cliente sepa a dónde ir

### 💳 Pagos
- [ ] **Pasarela de pago WOMPI o Stripe** — Integrar pago real en el checkout (actualmente solo registra la reserva sin procesar pago)

### 💬 Comunicación
- [x] **Websocket Chat cliente ↔ Colaborador** — Canal de mensajería interna en tiempo real entre clientes y colaboradores
- [ ] **Websocket Chat cliente ↔ Admin** — en caso de reclamo directo al admin desde el dashboard cliente realizar o websocket y tambien opcion de enviar a cierto email
- [ ] **Notificaciones por correo o WhatsApp al confirmar una reserva** — Enviar confirmación automática al cliente cuando su reserva es confirmada

### 🤖 Inteligencia Artificial
- [ ] **KIMI PROMPT — Asistente IA para usuarios** — Integrar prompts de IA que asistan a los usuarios con información sobre actividades y precios, dando respuestas rápidas y personalizadas sin intervención humana

### 📊 Datos y Reportes
- [ ] **DB Datos Demográficos** — Registrar y consultar datos demográficos de usuarios (nacionalidad, fecha de reserva) para analizar el perfil del turista
- [ ] **Resumen de personas por actividad y disponibilidad** — Vista tipo "to-do" para el colaborador que muestre quién confirmó, quién falta y sus datos de contacto
- [ ] **Reservas agrupadas por fecha o disponibilidad** — Vista organizada del historial de reservas para el colaborador
- [ ] **Modelo Predictivo** — Mostrar actividades más vistas, más rentables y con mejor margen de no-cancelación

### 🏗️ Arquitectura y Código
- [ ] **Arquitectura de carpetas estandarizada** — Definir y documentar la estructura oficial de paquetes del proyecto
- [x] **Implementación de ControllerAdvice** — Manejo global de excepciones con respuestas de error estandarizadas
- [x] **Implementación de logs con colores** — Configurar logging con niveles y colores diferenciados por entorno (dev/prod)
- [ ] **Testing** — Implementar suite de pruebas unitarias e integración para los servicios y controladores principales

### 🔒 Seguridad y Legal
- [ ] **Verificación de colaborador por el administrador** — Al registrarse, el colaborador debe ser aprobado por el admin (validar NIT/RUT antes de activar la cuenta)
- [ ] **¿Somos Legales? — Verificación legal del colaborador** — Permitir que el colaborador registre su información legal (NIT, constitución de empresa) para operar formalmente
- [ ] **Políticas de manejo de datos** — Mostrar políticas de privacidad antes del registro para que el usuario acepte informado (cumplimiento RGPD/Habeas Data)

### ⭐ Personalización
- [ ] **Preferencias del cliente** — Campo en el perfil del cliente para seleccionar categorías de interés y recibir recomendaciones personalizadas en el home

### 💼 Modelo de Negocio
- [ ] **Modelo de negocio documentado** — Comisión del 18% sobre el precio del colaborador (el cliente paga precio + comisión); debe quedar claro en el sistema cómo se calcula y registra
- [ ] **Propuesta de valor clara en la plataforma** — Lograr que cliente y colaborador prefieran usar la app a hacer tratos por fuera

---

### 📋 Nuevas Tareas — Tutoría (18 Mar 2026)

> Tareas definidas en sesión de tutoría. Cubren mejoras de UX, nuevas funcionalidades por rol y reglas de negocio adicionales.

#### Vista y UX General
- [ ] **Mostrar actividades disponibles al usuario** — En el home/catálogo, solo mostrar actividades que tengan disponibilidades activas con cupos > 0
- [ ] **Mejorar las vistas generales del sistema** — Revisión y rediseño de las vistas principales para mejorar claridad y consistencia visual
- [ ] **Mejorar específicamente las vistas de reservas** — Rediseñar la vista de reservas del cliente y del colaborador para que sea más legible y práctica
- [ ] **Simplificar la interfaz para que sea más práctica y fácil de usar** — Reducir fricción en los flujos más usados; priorizar acciones clave por encima del fold
- [ ] **Mostrar los meses en español** — Todos los componentes de fecha (calendarios, listados, etiquetas) deben mostrar los meses en español (ej: "Enero", "Febrero"…)
- [ ] **Agregar separador de miles en los valores de reservas** — Formatear precios y montos con separador de miles (ej: $1.200.000)
- [ ] **Aplicar separador de miles en todos los números del sistema** — Extender el formateo a cualquier campo numérico: comisiones, totales, estadísticas del admin, etc.

#### Reservas
- [ ] **Permitir ver reservaciones organizadas por día** — Vista de reservas agrupadas por fecha tanto para clientes como para colaboradores
- [ ] **Implementar filtrado diario para colaboradores** — El colaborador puede filtrar sus reservas/disponibilidades por día en su dashboard
- [ ] **Crear filtros para cancelaciones de colaboradores** — El admin puede filtrar y ver específicamente las reservas que fueron canceladas por colaboradores
- [ ] **Automatizar el cambio de estado de reservas (Pendiente → Hecho)** — Cuando la fecha y hora de la disponibilidad ya pasó y la reserva sigue en estado `Pendiente` o `Confirmada`, el sistema debe cambiarla automáticamente a `Hecho` (job programado / scheduler)
- [ ] **Validar reservas anteriores para evitar inconsistencias o duplicados** — Implementar validación al crear reservas para detectar posibles duplicados o estados inconsistentes de reservas pasadas

#### Módulo de Quejas, Reclamos y Auxilio (Cliente → Admin)
- [ ] **Habilitar que los clientes puedan comunicarse, enviar quejas y reclamos al admin** — Canal formal de comunicación cliente → administrador para reportar problemas con colaboradores o el servicio
- [ ] **Agregar opción en el sidebar del cliente para reportar (auxilio)** — Botón/enlace visible en el menú del cliente que permita iniciar un reporte de auxilio cuando un colaborador no cumple
- [ ] **Definir mecanismo para verificar que el colaborador cumplió con su actividad/servicio** — Proceso concreto (confirmación del cliente, check del admin, o cambio de estado automático) para validar el cumplimiento post-actividad

#### Módulo de Cumplimiento y Control (Admin)
- [ ] **Permitir al admin ver colaboradores y validar si cumplieron con las reservas** — Vista en el panel admin que muestre el historial de cumplimiento de cada colaborador (reservas atendidas vs. canceladas/incumplidas)
- [ ] **Permitir al admin monitorear y confirmar el cumplimiento de los colaboradores** — Herramienta de seguimiento activo en el dashboard del admin con alertas o indicadores de colaboradores con bajo cumplimiento
- [ ] **Permitir banear o inactivar colaboradores** — El admin puede suspender temporalmente o banear definitivamente a un colaborador que incumpla las reglas de la plataforma
- [ ] **Agregar más funcionalidades para el rol de admin** — Ampliar el panel de administración con más controles operativos (ver sección anterior)
- [ ] **Definir tiempo de respuesta del admin para aprobación de actividades (máx. 10 días hábiles)** — Regla de negocio: el admin tiene máximo 10 días hábiles para revisar y aprobar/rechazar una nueva actividad publicada por un colaborador

#### Notificaciones
- [ ] **Implementar notificaciones para colaboradores sobre nuevas reservas** — Cuando un cliente realiza una reserva, el colaborador recibe notificación (email y/o en la plataforma) informando los datos de la reserva

---

## 16. Fuera del Alcance (v1.0) — Versión Base

- Pasarela de pago real (el checkout registra la reserva pero no procesa pago)
- Sistema de notificaciones push o en tiempo real (WebSocket — alcance completo pendiente)
- App móvil nativa (iOS/Android)
- Sistema de reembolsos automatizados
- Multi-idioma de la interfaz (el sistema maneja idiomas de actividades, no de la UI)
- Panel de analítica avanzada para colaboradores
- Sistema de mensajería interna entre clientes y colaboradores

---

## 18. Riesgos y Consideraciones

| Riesgo | Impacto | Mitigación |
|--------|---------|-----------|
| Imágenes en `/uploads` sin volumen Docker | Alto — pérdida de datos | Mapear directorio como volumen persistente |
| CSRF deshabilitado en Spring Security | Medio — ataques cross-site | Habilitar CSRF en próximas versiones |
| Credenciales OAuth2 en `application.properties` | Alto — exposición si el repo es público | Usar variables de entorno o gestor de secretos |
| Sin paginación en algunos listados del admin | Bajo | Agregar paginación al crecer el volumen de datos |
| Sesiones de Spring en memoria | Medio — no escalable horizontalmente | Migrar a Redis si se necesita escalado horizontal |

---

*SPEC generado a partir del PRD v1.0 de ETA — Plataforma Marketplace de Actividades Turísticas y Recreativas.*
