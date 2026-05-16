# 📋 LISTA DE ARCHIVOS: QUÉ MIGRAR DESPUÉS

## Archivos que YA están listos ✅

```
src/main/java/maineta/eta/
├── config/
│   ├── JwtService.java ......................... ✅ NUEVO
│   ├── JwtAuthFilter.java ..................... ✅ NUEVO
│   └── SecurityConfig.java ................... ✅ REESCRITO
│
├── controller/
│   ├── AuthController.java ................... ✅ NUEVO
│   └── AllAcessController_REST.java ......... ✅ NUEVO (reemplaza)
│
├── dto/
│   ├── LoginRequestDTO.java ................. ✅ NUEVO
│   ├── AuthResponseDTO.java ................. ✅ NUEVO
│   ├── RegistroRequestDTO.java .............. ✅ NUEVO
│   ├── ActividadDetalleDTO.java ............. ✅ NUEVO
│   ├── LandingPageDTO.java .................. ✅ NUEVO
│   ├── DisponibilidadDTO.java ............... ✅ NUEVO
│   ├── ComentarioDTO.java ................... ✅ NUEVO
│   ├── ResponseDTO.java ..................... ✅ NUEVO
│   ├── PaginatedResponseDTO.java ........... ✅ NUEVO
│   ├── DashboardClienteDTO.java ............ ✅ NUEVO
│   └── DashboardColaboradorDTO.java ........ ✅ NUEVO

src/main/resources/
├── application.properties ................... ✅ MODIFICADO (JWT config)

pom.xml ....................................... ✅ MODIFICADO (JWT deps)
```

---

## Archivos que NECESITAN migración 🔲

### CRÍTICO (Hacer primero)

```
src/main/java/maineta/eta/controller/

1. AllAcessController.java ........................ 🔲 REEMPLAZAR
   ├─ Acción: Renombrar _BACKUP
   ├─ Acción: Renombrar _REST.java a .java
   └─ Tiempo: 5 minutos

2. ClienteController.java ........................ 🔲 CREAR ClienteRestController.java
   ├─ @RestController
   ├─ @RequestMapping("/api/cliente")
   ├─ GET /api/cliente/dashboard → DashboardClienteDTO
   ├─ POST /api/cliente/reserva
   ├─ POST /api/cliente/favorito/toggle/{id}
   ├─ PUT /api/cliente/perfil
   ├─ GET /api/cliente/chats
   └─ Tiempo: ~45 minutos

3. ColaboradorController.java ................... 🔲 CREAR ColaboradorRestController.java
   ├─ @RestController
   ├─ @RequestMapping("/api/colaborador")
   ├─ GET /api/colaborador/dashboard
   ├─ GET /api/colaborador/actividades
   ├─ POST /api/colaborador/actividades
   ├─ PUT /api/colaborador/actividades/{id}
   ├─ GET /api/colaborador/reservas
   ├─ PUT /api/colaborador/reservas/{id}
   └─ Tiempo: ~45 minutos

4. AdminController.java .......................... 🔲 CREAR AdminRestController.java
   ├─ @RestController
   ├─ @RequestMapping("/api/admin")
   ├─ GET /api/admin/dashboard
   ├─ GET /api/admin/usuarios
   ├─ POST /api/admin/categorias
   ├─ PUT /api/admin/comision
   └─ Tiempo: ~30 minutos
```

### IMPORTANTE (Hacer después)

```
5. RegistroController.java ....................... 🔲 MERGE con AuthController
   └─ Ya está todo el registro en AuthController.java

6. ComentarioController.java .................... 🔲 CREAR ComentarioRestController.java
   ├─ @RequestMapping("/api/comentarios")
   ├─ POST /api/comentarios (crear)
   ├─ PUT /api/comentarios/{id} (editar)
   ├─ DELETE /api/comentarios/{id} (eliminar)
   └─ Tiempo: ~20 minutos

7. OnboardingController.java .................... 🔲 MERGE con ClienteRestController
   ├─ GET /api/cliente/onboarding
   ├─ POST /api/cliente/onboarding
   └─ Tiempo: ~15 minutos

8. PagoController.java ........................... 🔲 ADAPTAR (WebHooks)
   ├─ POST /api/pago/confirmar (webhook - público)
   ├─ GET /api/cliente/pagos
   └─ Tiempo: ~20 minutos
```

### OPCIONALES (Hacer al final)

```
9. DocumentoController.java ..................... 🔲 CREAR DocumentoRestController.java
   ├─ POST /api/documentos/upload (multipart)
   ├─ GET /api/documentos
   └─ Tiempo: ~15 minutos

10. PlanController.java .......................... 🔲 CREAR PlanRestController.java
    ├─ GET /api/planes
    ├─ GET /api/planes/{id}
    └─ Tiempo: ~10 minutos

11. ClientePlanController.java .................. 🔲 MERGE con ClienteRestController
12. ColaboradorPlanController.java .............. 🔲 MERGE con ColaboradorRestController

13. ChatWebSocketController.java ................ ✅ SIN CAMBIOS
    └─ Funciona igual (WebSocket no cambia)

14. ChatBotController.java ....................... ⭐ Revisar si es REST o Thymeleaf
```

---

## ORDEN RECOMENDADO DE MIGRACIÓN

### Día 1 (2 horas)
1. [ ] Reemplazar AllAccessController.java (5 min)
2. [ ] Compilar y testear (15 min)
3. [ ] Crear ClienteRestController (45 min)
4. [ ] Testear ClienteRestController (15 min)

### Día 2 (2 horas)
5. [ ] Crear ColaboradorRestController (45 min)
6. [ ] Testear ColaboradorRestController (15 min)
7. [ ] Crear AdminRestController (30 min)
8. [ ] Testear AdminRestController (10 min)

### Día 3 (1.5 horas)
9. [ ] Crear ComentarioRestController (20 min)
10. [ ] Adaptar PagoController (20 min)
11. [ ] Testeo completo (30 min)

### Día 4+ (Opcional)
- DocumentoController
- PlanController
- Demás controllers menores

---

## PLANTILLA PARA CADA CONTROLLER

### Template ClienteRestController.java

```java
@RestController
@RequestMapping("/api/cliente")
public class ClienteRestController {

    private final ActividadService actividadService;
    private final ReservaService reservaService;
    private final UsuarioService usuarioService;
    // ... más servicios

    // Constructor

    /**
     * GET /api/cliente/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardClienteDTO> getDashboard(Authentication auth) {
        // 1. Validar authentication
        String email = auth.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Optional<Cliente> clienteOpt = clienteService.obtenerPorUsuario(usuario);
        
        if (clienteOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Cliente cliente = clienteOpt.get();

        // 2. Cargar datos
        List<Reserva> reservas = reservaService.obtenerPorCliente(cliente.getId());
        Set<Actividad> favoritos = favoritoService.obtenerFavoritos(cliente);

        // 3. Construir DTO
        DashboardClienteDTO dto = new DashboardClienteDTO();
        // ... rellenar dto

        // 4. Retornar ResponseEntity
        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/cliente/reserva
     */
    @PostMapping("/reserva")
    public ResponseEntity<?> crearReserva(
            @RequestBody ReservaDTO dto,
            Authentication auth) {
        // Mismo código de antes, pero:
        // return ResponseEntity.status(HttpStatus.CREATED)
        //     .body(new ResponseDTO("Reserva creada", nuevaReserva));
    }

    /**
     * POST /api/cliente/favorito/toggle/{id}
     * AJAX: Retorna {esFavorito: boolean}
     */
    @PostMapping("/favorito/toggle/{idActividad}")
    public ResponseEntity<?> toggleFavorito(
            @PathVariable Long idActividad,
            Authentication auth) {
        // ...
        return ResponseEntity.ok(Map.of("esFavorito", ahora_es_favorito));
    }
}
```

---

## DTOs NECESARIOS PARA CADA CONTROLLER

### ClienteRestController necesita:
- [x] DashboardClienteDTO ✅
- [ ] ReservaDetalleDTO (usar ReservaDTO existente)
- [ ] ChatDTO (crear)

### ColaboradorRestController necesita:
- [x] DashboardColaboradorDTO ✅
- [ ] ActividadDetalleDTO (ya existe: usar ActividadDTO)
- [ ] ReservaManagementDTO (crear)

### AdminRestController necesita:
- [ ] AdminDashboardDTO (crear)
- [ ] EstadísticasDTO (crear)

### ComentarioRestController necesita:
- [x] ComentarioDTO ✅ (ya existe)

### PagoController necesita:
- [ ] PagoResponseDTO (crear)
- [ ] PaymentWebhookDTO (crear)

---

## CHECKLIST PRE-MIGRACIÓN

Antes de empezar a migrar cada controller:

- [ ] Archivo antiguo @Controller renombrado a _BACKUP
- [ ] Nuevo archivo @RestController creado
- [ ] @RequestMapping cambiado a /api/...
- [ ] Todos los métodos return ResponseEntity<DTO>
- [ ] @RequestBody en POST/PUT
- [ ] Model model eliminado
- [ ] model.addAttribute() eliminado
- [ ] RedirectAttributes eliminado
- [ ] DTOs creados para cada endpoint
- [ ] Compilar sin errores
- [ ] Testear con curl

---

## CURL TESTING TEMPLATES

### Testear endpoint GET sin token

```bash
curl -X GET http://localhost:8080/api/actividades | jq
```

### Testear endpoint GET con token

```bash
TOKEN="eyJ..."
curl -X GET http://localhost:8080/api/cliente/dashboard \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Testear endpoint POST

```bash
curl -X POST http://localhost:8080/api/cliente/reserva \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "idActividad": 1,
    "fecha": "2026-05-15",
    "cantidad": 2
  }' | jq
```

### Testear endpoint PUT

```bash
curl -X PUT http://localhost:8080/api/cliente/perfil \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nombre": "Juan Nuevo",
    "telefono": "1234567890"
  }' | jq
```

---

## ELIMINACIÓN DE ARCHIVOS THYMELEAF

**SOLO después de que TODO sea REST:**

```bash
# 1. Hacer backup
mv src/main/resources/templates src/main/resources/templates_backup

# 2. (Opcional) Eliminar Thymeleaf de pom.xml
# <dependency>
#   <groupId>org.springframework.boot</groupId>
#   <artifactId>spring-boot-starter-thymeleaf</artifactId>
# </dependency>

# 3. Recompilar
mvn clean compile
```

---

## REFERENCIAS RÁPIDAS

| Tarea | Tiempo | Archivos |
|-------|--------|---------|
| Reemplazar AllAccessController | 5 min | 1 archivo |
| ClienteRestController | 45 min | 1 nuevo + DTOs |
| ColaboradorRestController | 45 min | 1 nuevo + DTOs |
| AdminRestController | 30 min | 1 nuevo + DTOs |
| ComentarioRestController | 20 min | 1 nuevo |
| Demás controllers | 30+ min | N/A |

**Total de work:** ~4-5 horas

---

**Creado:** 01-May-2026 | Versión: 1.0
