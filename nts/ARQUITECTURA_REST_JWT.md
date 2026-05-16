# 🏗️ ARQUITECTURA FINAL: REST + JWT

## Diagrama de flujo de autenticación

```
┌─────────────────────────────────────────────────────────────────────┐
│                        USUARIO FINAL (Angular)                      │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│  1. POST /api/auth/login                                            │
│     Email + Password (HTTP)                                         │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│ BACKEND (Spring Boot)                                               │
│                                                                     │
│  AuthController                                                     │
│  ├─ POST /api/auth/login                                           │
│  │  ├─ Recibe LoginRequestDTO                                      │
│  │  ├─ AuthenticationManager.authenticate()                        │
│  │  ├─ JwtService.generarToken()                                   │
│  │  └─ Devuelve AuthResponseDTO (token + rol)                      │
│  │                                                                  │
│  └─ POST /api/auth/registro                                        │
│     ├─ Crea Usuario + Cliente/Colaborador                          │
│     ├─ JwtService.generarToken()                                   │
│     └─ Devuelve AuthResponseDTO                                    │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ↓ (respuesta: JWT token)
┌─────────────────────────────────────────────────────────────────────┐
│ FRONTEND (Angular)                                                  │
│                                                                     │
│  AuthService                                                        │
│  ├─ Guarda token en localStorage                                   │
│  └─ Emite token$ subject                                           │
│                                                                     │
│  AuthInterceptor                                                    │
│  └─ En CADA request: Inyecta Bearer token en header Authorization  │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│  2. GET /api/cliente/dashboard                                      │
│     Headers: { Authorization: "Bearer eyJhbGc..." }                │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│ BACKEND (Spring Boot)                                               │
│                                                                     │
│  JwtAuthFilter (ejecuta antes de cada request)                      │
│  ├─ Extrae token del header "Authorization: Bearer ..."            │
│  ├─ JwtService.esValido(token)                                     │
│  ├─ JwtService.getAuthentication(token)                            │
│  └─ SecurityContextHolder.setAuthentication(auth)                  │
│                                                                     │
│  ClienteRestController                                              │
│  ├─ GET /api/cliente/dashboard                                     │
│  │  ├─ @Secured("ROLE_CLIENTE") ✅                                 │
│  │  ├─ Obtiene usuario del Authentication                          │
│  │  ├─ Carga datos: reservas, favoritos, etc.                      │
│  │  ├─ Construye DashboardClienteDTO                               │
│  │  └─ Devuelve ResponseEntity<DashboardClienteDTO>                │
│  │                                                                  │
│  └─ POST /api/cliente/reserva                                      │
│     ├─ @RequestBody ReservaDTO                                     │
│     ├─ ReservaService.crearReserva()                               │
│     └─ Devuelve ResponseEntity (CREATED)                           │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ↓ (respuesta: JSON + 200/201)
┌─────────────────────────────────────────────────────────────────────┐
│ FRONTEND (Angular)                                                  │
│                                                                     │
│  ApiService                                                         │
│  ├─ Recibe respuesta JSON                                          │
│  ├─ Maps a modelos TypeScript                                      │
│  └─ Emite a componentes vía Observable/Subject                     │
│                                                                     │
│  Componente                                                         │
│  ├─ Recibe datos                                                   │
│  ├─ Renderiza en template                                          │
│  └─ Usuario ve resultados en la pantalla                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Flujo de LOGOUT

```
┌─────────────────────────────────────────────────────────────────────┐
│ FRONTEND (Angular)                                                  │
│                                                                     │
│  Usuario hace clic en "Cerrar Sesión"                              │
│  ├─ AuthService.logout()                                           │
│  │  ├─ localStorage.removeItem('token')                            │
│  │  ├─ localStorage.removeItem('rol')                              │
│  │  └─ tokenSubject.next(null)                                     │
│  ├─ Router.navigate(['/login'])                                    │
│  └─ ✅ Usuario deslogueado (no hay token en localStorage)           │
└─────────────────────────────────────────────────────────────────────┘

En el backend:
├─ JWT es STATELESS
├─ NO hay sesión que cerrar
├─ NO hay SessionRegistry
└─ ✅ Solo borra el token en el cliente
```

---

## Comparativa: ANTES vs DESPUÉS

### ANTES (Thymeleaf + Sesiones)

```
FRONTEND (Forms HTML)
    ↓ (POST + formulario)
Spring Security (formLogin)
    ↓ 
SessionRegistry (guarda sesión en servidor)
    ↓ 
Controller (Model + template)
    ↓ (render HTML)
FRONTEND (recibe HTML)
    ↓ (Cookie JSESSIONID guardado)
Siguiente request con cookie
```

**Problemas:**
- No se escalable (sesiones en servidor)
- CSRF complejo de manejar
- SPA (Single Page App) difícil con sesiones
- Logout requiere invalidar sesión servidor

---

### DESPUÉS (REST + JWT)

```
FRONTEND (JSON + localStorage)
    ↓ (POST JSON)
AuthController
    ↓ 
JwtService (generar token)
    ↓ 
FRONTEND (recibe token)
    ↓ (localStorage.setItem('token'))
Siguiente request con header "Authorization: Bearer ..."
    ↓
JwtAuthFilter (valida token)
    ↓
RestController
    ↓ (JSON response)
FRONTEND (procesa JSON)
```

**Ventajas:**
- ✅ Escalable (sin sesiones servidor)
- ✅ Seguro (firma criptográfica)
- ✅ Perfecto para SPA
- ✅ CORS nativo
- ✅ Funciona con cualquier frontend (Angular, React, Vue, mobile)
- ✅ Stateless (fácil de horizontal-scale)

---

## Estructura de archivos (DESPUÉS)

```
src/main/java/maineta/eta/
│
├── config/
│   ├── SecurityConfig.java              ✅ NUEVO (stateless)
│   ├── JwtService.java                  ✅ NUEVO
│   ├── JwtAuthFilter.java               ✅ NUEVO
│   ├── UsuarioHelper.java               ⭐ (sin cambios)
│   └── MvcConfig.java                   ❌ (eliminar después)
│
├── controller/
│   ├── AuthController.java              ✅ NUEVO (login/registro)
│   ├── AllAcessController.java          ✅ RENOMBRADO (REST)
│   ├── ClienteRestController.java       ⏳ PENDIENTE
│   ├── ColaboradorRestController.java   ⏳ PENDIENTE
│   ├── AdminRestController.java         ⏳ PENDIENTE
│   ├── ChatWebSocketController.java     ⭐ (sin cambios)
│   └── ...otros controllers             ⏳ PENDIENTE (convertir)
│
├── dto/
│   ├── LoginRequestDTO.java             ✅ NUEVO
│   ├── AuthResponseDTO.java             ✅ NUEVO
│   ├── RegistroRequestDTO.java          ✅ NUEVO
│   ├── ActividadDetalleDTO.java         ✅ NUEVO
│   ├── LandingPageDTO.java              ✅ NUEVO
│   ├── ResponseDTO.java                 ✅ NUEVO
│   ├── DashboardClienteDTO.java         ✅ NUEVO
│   ├── DashboardColaboradorDTO.java     ✅ NUEVO
│   ├── PaginatedResponseDTO.java        ✅ NUEVO
│   └── ...DTOs existentes               ⭐ (sin cambios)
│
├── entity/                              ⭐ (sin cambios)
├── repository/                          ⭐ (sin cambios)
├── service/                             ⭐ (sin cambios)
└── specification/                       ⭐ (sin cambios)

src/main/resources/
├── application.properties               ✅ (JWT config agregada)
├── templates/                           ❌ (eliminar después - no se usan)
└── static/                              ⭐ (sin cambios)

pom.xml                                  ✅ (JWT deps agregadas)
```

---

## Endpoints principales

### 🔓 PÚBLICOS (sin token)

```
GET    /api/actividades                    → LandingPageDTO
GET    /api/actividades/{id}               → ActividadDetalleDTO
GET    /api/actividades/buscar             → LandingPageDTO
GET    /api/colaboradores/{id}             → ColaboradorPublicoDTO
GET    /api/categorias                     → List<Categoria>
GET    /api/idiomas                        → List<Idioma>

POST   /api/auth/login                     → AuthResponseDTO
POST   /api/auth/registro                  → AuthResponseDTO
GET    /api/auth/validate                  → {válido: boolean}
```

### 🔒 CLIENTE (token + ROLE_CLIENTE)

```
GET    /api/cliente/dashboard              → DashboardClienteDTO
POST   /api/cliente/reserva                → ResponseDTO
GET    /api/cliente/reserva/{id}           → ReservaDetalleDTO
POST   /api/cliente/favorito/toggle/{id}   → {esFavorito: boolean}
GET    /api/cliente/chats                  → List<ChatDTO>
PUT    /api/cliente/perfil                 → ResponseDTO
```

### 🏢 COLABORADOR (token + ROLE_COLABORADOR)

```
GET    /api/colaborador/dashboard          → DashboardColaboradorDTO
GET    /api/colaborador/actividades        → List<ActividadDTO>
POST   /api/colaborador/actividades        → ResponseDTO
PUT    /api/colaborador/actividades/{id}   → ResponseDTO
GET    /api/colaborador/reservas           → List<ReservaDTO>
PUT    /api/colaborador/reservas/{id}      → ResponseDTO
```

### 👑 ADMIN (token + ROLE_ADMIN)

```
GET    /api/admin/dashboard                → AdminDashboardDTO
GET    /api/admin/usuarios                 → PageDTO<UsuarioDTO>
POST   /api/admin/categorias               → ResponseDTO
PUT    /api/admin/comision                 → ResponseDTO
```

---

## Flujo de una transacción completa

### Escenario: Cliente reserva una actividad

**1️⃣ Usuario abre la app (no autenticado)**
```
GET /api/actividades → 200 OK (datos públicos)
```

**2️⃣ Usuario hace login**
```
POST /api/auth/login
  body: { email: "cliente@example.com", password: "123456" }
  ↓
response: {
  token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  rol: "ROLE_CLIENTE",
  nombre: "Juan",
  email: "cliente@example.com"
}
↓
Angular: localStorage.setItem('token', response.token)
```

**3️⃣ Angular ve el detalle de una actividad**
```
GET /api/actividades/5 (SIN token - es público)
  ↓
response: {
  id: 5,
  titulo: "Tour a Cartagena",
  descripcion: "...",
  disponibilidades: [...],
  comentarios: [...],
  ...
}
```

**4️⃣ Usuario selecciona fecha y cantidad, reserva**
```
POST /api/cliente/reserva
  headers: { Authorization: "Bearer eyJ..." }
  body: {
    idActividad: 5,
    fecha: "2026-05-15",
    cantidad: 2
  }
  ↓
Spring Security: Valida token, extrae usuario
ClienteRestController: Procesa reserva
ReservaService: Crea reserva en BD
  ↓
response: {
  mensaje: "Reserva creada exitosamente",
  datos: { idReserva: 123, total: 250000, ... },
  exito: true
}
```

**5️⃣ Usuario ve su dashboard**
```
GET /api/cliente/dashboard
  headers: { Authorization: "Bearer eyJ..." }
  ↓
Spring Security: Token válido + rol CLIENTE
ClienteRestController: Carga datos
  ↓
response: {
  usuarioPerfil: { nombre: "Juan", email: "...", ... },
  reservas: [{ id: 123, titulo: "Tour Cartagena", ... }, ...],
  favoritos: [...],
  ...
}
```

**6️⃣ Usuario hace logout**
```
Angular:
  localStorage.removeItem('token')
  Router.navigate(['/login'])
↓
Token desaparece del localStorage
Siguiente request NO tiene token → rechazado si es protegido
```

---

## Seguridad JWT

### Ventajas
- ✅ **Stateless:** No necesita sesión en servidor
- ✅ **Signature:** Token firmado criptográficamente
- ✅ **Claims:** Rol y datos embebidos en el token
- ✅ **Expiration:** Token expira automáticamente
- ✅ **Mobile-friendly:** Funciona perfectamente con apps mobile

### Consideraciones
- ⚠️ Token se almacena en localStorage (accesible a XSS)
  - **Mitigación:** Content Security Policy headers
- ⚠️ Token se envía en cada request
  - **OK:** HTTPS encripta el transport
- ⚠️ No puedes revocar un token activo
  - **Alternativa:** Usar blacklist en Redis (opcional)

---

## Próximas mejoras (opcionales)

1. **Refresh Tokens:** Token corto + refresh token largo
2. **Blacklist:** Guardar tokens revocados en Redis
3. **Rate Limiting:** Limitar requests por IP/token
4. **RBAC Avanzado:** Permisos granulares, no solo roles
5. **OAuth2 con Google:** Adaptado para devolver JWT
6. **2FA:** Autenticación de dos factores

---

**Diagrama actualizado:** 01-May-2026
**Versión:** 1.0
**Status:** Arquitectura lista, Endpoints públicos listos, Autenticación lista
