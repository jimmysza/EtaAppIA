# ETA App - Documentación Arquitectónica

**Marketplace de Actividades Turísticas en Cartagena**

---

## 1. Stack Tecnológico y Versiones

### Backend
- **Java**: 17 (Runtime LTS)
- **Spring Boot**: 3.5.7
- **Spring Security**: 6.x (con OAuth2 Google Login)
- **Spring Data JPA**: incluido en Spring Boot 3.5.7
- **Hibernate**: dialecto MySQL
- **Thymeleaf**: motor de plantillas del lado del servidor
- **Lombok**: generación automática de getters/setters
- **Apache Tika**: 2.9.0 (validación MIME de archivos)

### Frontend
- **Tailwind CSS**: 3.4.16 (compilado con PostCSS)
- **PostCSS**: 8.5.6 + autoprefixer + cssnano
- **JavaScript Vanilla**: sin frameworks frontend
- **Lenis**: 1.3.17 (smooth scroll)

### Base de Datos
- **MySQL**: 8.x (jdbc:mysql://localhost:3306/eta_db)
- **JPA DDL Auto**: `update` (no usar en producción)

### Build & Deploy
- **Maven** (wrapper incluido: mvnw)
- **npm**: scripts para compilar Tailwind CSS
- **Puerto**: 8080 (configurable en application.properties)

---

## 2. Arquitectura del Proyecto

### Organización de Paquetes (maineta.eta.*)

```
src/main/java/maineta/eta/
│
├── config/              # Configuración de Spring (Security, MVC, Helpers)
├── controller/          # Controladores REST/MVC (por rol: Admin, Cliente, Colaborador)
├── dto/                 # Data Transfer Objects (formularios y respuestas optimizadas)
├── entity/              # Entidades JPA (modelo de dominio)
├── repository/          # Interfaces JPA Repository (persistencia)
├── service/             # Lógica de negocio (implementaciones de servicios)
└── specification/       # JPA Specifications (filtros dinámicos)
```

### Patrón Arquitectónico

**Clásico MVC + Repository Pattern**:
1. **Controller**: Recibe HTTP request → valida → delega a Service
2. **Service**: Aplica lógica de negocio → llama Repository
3. **Repository**: Interactúa con BD vía JPA
4. **DTO**: Optimiza transferencia cliente-servidor (evita N+1, expone solo lo necesario)

### Capas de Seguridad

- **Spring Security + Custom Success/Failure Handlers**: 
  - Roles: `ROLE_ADMIN`, `ROLE_CLIENTE`, `ROLE_COLABORADOR`
  - Login tradicional (username/password) + OAuth2 Google
  - Control basado en URL patterns (`.hasRole("CLIENTE")`)
  - Sesiones: cookie `CookieEta` (30 min timeout, httpOnly=true)

---

## 3. Mapa de Archivos Clave

### Configuración

| Archivo | Responsabilidad |
|---------|----------------|
| `application.properties` | DB, OAuth2, Thymeleaf, uploads (max 10MB) |
| `SecurityConfig.java` | Rutas protegidas, AuthProvider, login success/failure |
| `MvcConfig.java` | Recursos estáticos, interceptor de clientes (onboarding check) |
| `UsuarioHelper.java` | Cálculo de precio consumidor (precio + comisión admin) |
| `CustomAuthenticationSuccessHandler.java` | Redirige según rol tras login |
| `ClienteInterceptor.java` | Fuerza onboarding para clientes sin completar |

### Entidades Principales

| Entidad | Relación Clave | Descripción |
|---------|---------------|-------------|
| `Usuario` | @OneToOne con Cliente/Colaborador/Admin | Cuenta de acceso (email, password, roles) |
| `Cliente` | @OneToMany Reservas, Favoritos | Usuario final que reserva actividades |
| `Colaborador` | @OneToMany Actividades | Proveedor de experiencias (NIT, correo seguridad) |
| `Actividad` | @ManyToOne Colaborador, Categoria, Idioma | Experiencia turística publicada |
| `Reserva` | @ManyToOne Cliente, Actividad, Disponibilidad | Transacción entre cliente y actividad |
| `Disponibilidad` | @ManyToOne Actividad | Fecha específica con cupos disponibles |
| `PatronDisponibilidad` | @ManyToOne Actividad | Regla recurrente (ej: todos los lunes) |
| `Comentario` | @ManyToOne Cliente, Actividad | Reseña con calificación 1-5 estrellas |
| `Favorito` | @ManyToOne Cliente, Actividad | Lista de deseos del cliente |
| `Categoria` | @OneToMany Actividades | Clasificación (ej: Gastronomía, Aventura) |
| `Idioma` | @OneToMany Actividades | Idioma en que se ofrece la actividad |

### Controllers y Rutas Principales

**AllAcessController** (público o mixto):
- `GET /` → Landing page con actividades destacadas
- `GET /actividad/{slug}-{id}` → Detalle de actividad (incrementa vistas)
- `GET /actividades/buscar?nombre=...&categoriaId=...&idiomaId=...` → Búsqueda con filtros
- `GET /actividades/tendencias` → Top actividades por contador de tendencias
- `GET /actividades/para-ti` → Personalizadas según onboarding del cliente
- `GET /colaboradores/{id}` → Perfil público de colaborador
- `GET /top-colaboradores` → Ranking top 10 por reservas

**ClienteController** (`/cliente/*`):
- `GET /dashboard` → Panel de reservas del cliente
- `POST /reserva` → Crear nueva reserva
- `POST /favorito/toggle/{idActividad}` → Agregar/quitar favorito (AJAX)
- `GET /chats` → Lista de conversaciones
- `GET /settings` → Perfil del cliente

**ColaboradorController** (`/colaborador/*`):
- `GET /dashboard` → KPIs y resumen de negocio
- `GET /actividades` → Listar actividades propias
- `POST /actividades/addAct` → Crear nueva actividad
- `GET /disponibilidades/{idActividad}` → Gestionar calendario
- `POST /disponibilidades/patron/crear` → Crear patrón recurrente
- `GET /reservas/{idActividad}` → Ver reservas por actividad
- `POST /reserva/actualizar/{idReserva}` → Cambiar estado reserva

**AdminController** (`/admin/*`):
- `GET /dashboard` → Estadísticas globales
- `POST /comision` → Actualizar % comisión
- `GET /categorias` → CRUD categorías
- `GET /clientes` → Ver todos los clientes

### Servicios Principales

| Servicio | Método Crítico | Descripción |
|----------|---------------|-------------|
| `ActividadService` | `buscarConFiltros()` | Búsqueda con Specifications (evita N+1) |
| `ReservaService` | `crearReserva()` | Valida cupos, crea reserva, reduce disponibilidad |
| `ColaboradorService` | `obtenerDestacadosPorReservas()` | Ranking por total de reservas/personas |
| `ClienteService` | `actualizarPreferencias()` | Guarda categorías favoritas (onboarding) |
| `UsuarioService` | `loadUserByUsername()` | UserDetailsService para Spring Security |
| `FavoritoService` | `toggleFavorito()` | Crea o elimina favorito (idempotente) |
| `DisponibilidadService` | `generarDesdePatron()` | Crea disponibilidades batch desde patron |
| `KpiColaboradorService` | `obtenerResumen()` | Dashboard KPIs del colaborador |
| `UsuarioManagerService` | Registro unificado | Crea Usuario + Cliente/Colaborador + Roles |

### Templates Thymeleaf (src/main/resources/templates/)

**Públicos**:
- `main.html` → Landing page (hero + categorías + actividades)
- `detalle-actividad.html` → Detalle con calendario, reseñas, formulario reserva
- `resultados-busqueda.html` → Listado con filtros (categoría, idioma, precio)
- `perfil-colaborador.html` → Perfil público con actividades del colaborador
- `top-colaboradores.html` → Ranking top 10

**Cliente** (`cliente/`):
- `dashboard.html` → Mis reservas (próximas, pasadas, canceladas)
- `settings.html` → Editar perfil y preferencias
- `chats.html` → Conversaciones con colaboradores

**Colaborador** (`colaborador/`):
- `dashboard.html` → KPIs (ingresos, reservas, calificación promedio)
- `actividades.html` → CRUD de actividades propias
- `disponibilidades.html` → Calendario con patrones y fechas específicas
- `reservas.html` → Gestión de reservas por actividad
- `settings.html` → Editar perfil (nombre, teléfono, foto)

**Admin** (`admin/`):
- `dashboard.html` → Estadísticas globales
- `categorias.html` → CRUD categorías
- `clientes.html` → Listado de clientes

**Componentes** (`componentes/`):
- `navbar.html` → Header con dropdown de usuario (reutilizable con `th:replace`)
- `footer.html` → Footer global
- `terminos-condiciones.html` → Página de términos

### Estilos y Scripts (src/main/resources/static/)

**CSS**:
- `global.css` → Variables CSS, reset, tipografía
- `tailwind.css` → Output compilado de Tailwind (NO editar, se regenera)
- `tailwind-input.css` → @tailwind directives (fuente para compilar)
- Carpetas por sección: `actividad/`, `admin/`, `cliente/`, `colaborador/`, `componentes/`

**JavaScript**:
- `js/alert.js` → Sistema de notificaciones (success, error, info)
- `js/chat.js` → WebSocket para mensajería en tiempo real
- `js/componentes/navbar.js` → Burger menu y dropdowns
- `js/componentes/Activitycards.js` → Carousel horizontal de actividades
- `js/scrollSlow.js` → Smooth scroll con Lenis

---

## 4. Convenciones del Proyecto

### Nomenclatura

**Backend (Inglés técnico + Español dominio)**:
- Paquetes: `snake_case` implícito en Java (`maineta.eta.controller`)
- Clases: `PascalCase` (ej: `ActividadService`, `ClienteController`)
- Métodos: `camelCase` (ej: `obtenerPorId()`, `crearReserva()`)
- Variables: `camelCase` (ej: `idActividad`, `fechaReserva`)
- Entidades: nombres en **español** (Cliente, Colaborador, Actividad)
- DTOs: sufijo `DTO` (ej: `ActividadDTO`, `ColaboradorPublicoDTO`)

**Frontend (mix)**:
- Clases CSS: `kebab-case` o Tailwind utilities
- IDs HTML: `camelCase` (ej: `heroSearchInput`)
- Funciones JS: `camelCase` (ej: `toggleFavorito()`)

### Endpoints

**Patrón**: `/{rol}/{recurso}/{accion}`

Ejemplos:
- `GET /cliente/dashboard` → Dashboard del cliente
- `POST /colaborador/actividades/addAct` → Crear actividad
- `GET /actividades/buscar?nombre=surf` → Búsqueda pública
- `POST /admin/comision` → Actualizar comisión global

**Rutas públicas no siguen prefijo de rol** (ej: `/`, `/actividad/{slug}-{id}`)

### Idioma en el Código

- **Backend**: Mezcla pragmática
  - Términos técnicos: inglés (service, controller, repository)
  - Dominio del negocio: español (Actividad, Colaborador, precioConsumidor)
- **Frontend**: Español en textos de usuario, inglés en clases CSS
- **Base de datos**: Columnas en español (`nombre`, `descripcion`, `fecha_reserva`)

---

## 5. Flujos Principales

### 1. Registro y Login

**Flujo Cliente**:
1. `GET /registro/cliente` → Muestra formulario
2. `POST /registro/cliente` → `UsuarioManagerService.registrarCliente()` → crea Usuario + Cliente + ROLE_CLIENTE
3. Email de verificación enviado (opcional, depende de feature flag)
4. `GET /login` → Usuario ingresa credenciales
5. `CustomAuthenticationSuccessHandler` → redirige a `/cliente/onboarding` (si no completó) o `/cliente/dashboard`
6. `ClienteInterceptor` valida onboarding en cada request de `/cliente/*`

**Flujo Colaborador**:
Similar, pero requiere NIT y correo de seguridad. Redirige a `/colaborador/dashboard`.

**OAuth2 Google**:
- Spring Security maneja `/oauth2/authorization/google`
- Si usuario no existe, crea cuenta automáticamente
- Si existe, vincula OAuth2 al Usuario existente

### 2. Búsqueda y Descubrimiento

**Flujo**:
1. Usuario ingresa término en `GET /` → formulario busca en `/actividades/buscar?nombre=surf`
2. `AllAcessController.buscarActividadesGet()` llama `ActividadService.buscarConFiltros()`
3. Backend usa **JPA Specifications** para construir query dinámica (nombre, categoría, idioma, rango precio)
4. Evita N+1 con **contarComentariosPorActividades()** (query batch)
5. Mapea a `ActividadDTO` con precio consumidor calculado (`precio * (1 + comisión%)`)
6. Renderiza `resultados-busqueda.html` con filtros activos y badge de filtros aplicados

**Optimización clave**: Un solo query para comentarios de todas las actividades en la página → `Map<Long, Integer>` → lookup O(1)

### 3. Reserva de Actividad

**Flujo completo**:
1. Cliente ve detalle en `GET /actividad/{slug}-{id}`
   - Controller incrementa `vistas` y `tendencia` de la actividad
   - Carga disponibilidades con estado `DISPONIBLE` y `cuposDisponibles > 0`
   - Genera calendario por mes con fechas clickeables
2. Cliente selecciona fecha y cantidad de personas → `POST /cliente/reserva`
3. `ClienteController.crearReserva()` valida:
   - Disponibilidad existe y tiene cupos suficientes
   - Cliente autenticado existe
4. Llama `ReservaService.crearReserva()`:
   - Crea entidad `Reserva` con estado `PENDIENTE`
   - Reduce `cuposDisponibles` de `Disponibilidad`
   - Calcula `precioTotal = cantidad * precioConsumidor`
5. Redirige a `/cliente/dashboard` con mensaje de éxito
6. Colaborador ve reserva en `/colaborador/reservas/{idActividad}` → puede actualizar estado

**Estados de Reserva**: `PENDIENTE` → `CONFIRMADA` / `CANCELADA` / `COMPLETADA`

### 4. Gestión de Disponibilidades (Colaborador)

**Método 1: Fechas específicas**:
1. `POST /colaborador/disponibilidades/agregar` → crea `Disponibilidad` con fecha, cupos, estado

**Método 2: Patrón recurrente (ej: todos los sábados)**:
1. `POST /colaborador/disponibilidades/patron/crear` → crea `PatronDisponibilidad`
2. Backend genera automáticamente `Disponibilidad` para cada ocurrencia del patrón
3. Futuras modificaciones al patrón NO afectan fechas ya generadas (desacoplado)

**Ventaja**: Colaborador puede crear disponibilidades masivas (ej: todos los fines de semana durante 3 meses)

### 5. Personalización "Para Ti" (Clientes)

**Flujo**:
1. Nuevo cliente completa `/cliente/onboarding` → selecciona 3-5 categorías favoritas
2. `ClienteService.actualizarPreferencias()` guarda relación `Cliente → Set<Categoria>`
3. En `GET /`, si cliente autenticado con onboarding completo:
   - `ActividadService.obtenerParaTi(idCliente)` filtra actividades por categorías preferidas
   - Ordena por calificación y recientes
4. Muestra sección "Actividades Para Ti" en landing page

---

## 6. Decisiones de Arquitectura Importantes

### Cálculo de Precio Consumidor

**NUNCA calcular en Controller ni en Vista**. Siempre usar `UsuarioHelper.CalcularPrecioConsumidor()`:
```java
BigDecimal precioColaborador = actividad.getPrecio(); // ej: 100,000 COP
BigDecimal precioFinal = usuarioHelper.CalcularPrecioConsumidor(precioColaborador); // 110,000 COP (si comisión = 10%)
```

**Razón**: La comisión es configurable por Admin (`Admin.porcentajeComision`). Centralizar cálculo evita inconsistencias.

### DTOs para Optimizar N+1

**Contexto**: Mostrar 20 actividades con su cantidad de comentarios genera 21 queries (1 actividades + 20 comentarios por actividad).

**Solución**: `ComentarioService.contarComentariosPorActividades(List<Long> ids)` usa `GROUP BY` → devuelve `Map<Long, Integer>` → lookup O(1) por cada actividad.

Patrón repetido para:
- Comentarios por actividad
- Actividades por categoría
- Clientes únicos por colaborador

### Incremento de Contadores

`ActividadService.incrementarContadores(idActividad)` incrementa `vistas` y `tendencia` SIN leer la actividad completa:
```java
actividadRepository.incrementarVistas(id); // UPDATE actividades SET vistas = vistas + 1 WHERE id = ?
```

**NO hacer**: leer actividad → incrementar → guardar (genera SELECT + UPDATE innecesario)

### Gestión de Favoritos Idempotente

`FavoritoService.toggleFavorito(idCliente, idActividad)`:
- Si existe → borra
- Si no existe → crea
- Devuelve `true` si ahora es favorito, `false` si se quitó

Usado en AJAX (`POST /cliente/favorito/toggle/{id}`) para ícono de corazón.

### Seguridad en Archivos Subidos

`UploadFileServiceImpl` valida MIME type con **Apache Tika** (no confía en extensión del archivo):
```java
String mimeType = tika.detect(file.getInputStream());
if (!mimeType.startsWith("image/")) throw new ValidationException("Solo imágenes");
```

Carpeta de uploads: `/uploads/` (servida como recurso estático en `MvcConfig`)

### Thymeleaf Fragments para Reutilización

Navbar y footer son **fragmentos reutilizables**:
```html
<div th:replace="~{componentes/navbar :: navbar}"></div>
```

**Ventaja**: Un solo lugar para actualizar header global.

### WebSocket para Chat

- `@EnableWebSocketMessageBroker` en config
- Cliente envía mensaje → `/app/chat.sendMessage`
- Servidor broadcast → `/topic/chat/{idReserva}`
- Frontend escribe a DOM sin recargar página

---

## 7. Lo que NO Hacer

### ❌ NO Usar `findById()` en Loop

**Mal**:
```java
for (Long id : actividadIds) {
    Actividad a = actividadRepository.findById(id).orElse(null);
    // ...
}
```

**Bien**:
```java
List<Actividad> actividades = actividadRepository.findAllById(actividadIds); // 1 query
```

### ❌ NO Exponer Entidades Directamente en Controllers

**Mal**:
```java
@GetMapping("/actividades")
public String listar(Model model) {
    model.addAttribute("actividades", actividadRepository.findAll());
    return "lista";
}
```

**Problema**: Lazy loading de relaciones + Serialización circular + Expones toda la entidad (incluye precio del colaborador, no el consumidor).

**Bien**: Usar DTOs (`ActividadDTO`).

### ❌ NO Calcular Precio en Vista

**Mal** (Thymeleaf):
```html
<span th:text="${actividad.precio * 1.10}">Precio</span>
```

**Bien**:
```java
dto.setPrecioConsumidor(usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));
```

### ❌ NO Hardcodear Roles en Strings

**Mal**:
```java
if (usuario.getRol().equals("CLIENTE")) { ... }
```

**Bien**:
```java
if (usuario.getRol().getNombre().equals("ROLE_CLIENTE")) { ... }
// O usar Spring Security: @PreAuthorize("hasRole('CLIENTE')")
```

### ❌ NO Usar Paginación Sin Límite

Siempre especificar `PageRequest.of(page, size)`. Nunca `repository.findAll()` sin PageRequest en producción.

### ❌ NO Recargar Página para Actualizar Favoritos

Usar AJAX + JSON response:
```javascript
fetch('/cliente/favorito/toggle/' + id, { method: 'POST' })
  .then(res => res.json())
  .then(data => {
    if (data.favorito) {
      // Llenar corazón
    } else {
      // Vaciar corazón
    }
  });
```

### ❌ NO Eliminar Reservas/Actividades Sin Validación

- Para actividades: verificar que no tenga reservas `CONFIRMADA` o `PENDIENTE`
- Para disponibilidades: verificar que no tenga reservas asociadas
- Usar soft delete (campo `deleted_at`) en lugar de `repository.delete()` si se requiere auditoría

### ❌ NO Mezclar Lógica de Negocio en Controllers

Controllers deben ser **thin**: validar request, delegar a Service, manejar response.

**Mal**:
```java
@PostMapping("/reserva")
public String reservar(...) {
    Disponibilidad d = disponibilidadRepository.findById(idDisp).orElse(null);
    if (d.getCuposDisponibles() < cantidad) { ... }
    d.setCuposDisponibles(d.getCupos() - cantidad);
    // ...
}
```

**Bien**: mover toda la lógica a `ReservaService.crearReserva()`.

---

## Instrucción para Claude Code

Antes de explorar archivos del proyecto, lee este documento completo. Úsalo como mapa principal para ubicar código relevante. Solo navega a archivos específicos cuando necesites ver implementación concreta. No propongas cambios estructurales que contradigan la arquitectura descrita aquí sin señalarlo explícitamente.

**Principios al trabajar con este proyecto**:
1. Respeta el patrón Controller → Service → Repository
2. Usa DTOs para optimizar queries y evitar N+1
3. Calcula precios siempre con `UsuarioHelper`
4. Valida roles con Spring Security, no con `if/else` manual
5. Prefiere batch queries sobre loops con `findById()`
6. Mantén la separación: lógica de negocio en Services, NO en Controllers
7. Usa Thymeleaf fragments para componentes reutilizables
8. Documenta decisiones no obvias con comentarios en español

Si necesitas cambiar algo estructural (ej: migrar de Sessions a JWT, cambiar patrón de disponibilidades, etc.), primero discute el impacto en esta arquitectura.
