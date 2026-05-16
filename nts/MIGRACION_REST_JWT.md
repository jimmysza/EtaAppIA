# 📋 GUÍA DE MIGRACIÓN: RESTO DE CONTROLLERS A REST + JWT

## Resumen del trabajo completado ✅

Ya está listo:
1. ✅ Dependencias JWT en pom.xml
2. ✅ JwtService (generar, validar tokens)
3. ✅ JwtAuthFilter (interceptor de tokens)
4. ✅ SecurityConfig (stateless)
5. ✅ AuthController (login, registro)
6. ✅ AllAccessController migrado a REST

## Controladores a migrar 🔲

### Lista completa de controllers:

1. **AllAcessController.java** → Reemplazar con AllAcessController_REST.java ⚠️
2. **ClienteController.java** → ClienteRestController.java (EN PROGRESO)
3. **ColaboradorController.java** → ColaboradorRestController.java (PENDIENTE)
4. **AdminController.java** → AdminRestController.java (PENDIENTE)
5. **RegistroController.java** → Reemplazar por endpoints en AuthController
6. **ComentarioController.java** → ComentarioRestController (PENDIENTE)
7. **ChatWebSocketController.java** → Mantener (WebSocket funciona igual)
8. **OnboardingController.java** → OnboardingRestController (PENDIENTE)
9. **PagoController.java** → PagoRestController (PENDIENTE)
10. **DocumentoController.java** → DocumentoRestController (PENDIENTE)
11. **PlanController.java** → PlanRestController (PENDIENTE)
12. **ClientePlanController.java** → Merge con ClienteRestController
13. **ColaboradorPlanController.java** → Merge con ColaboradorRestController

---

## PATRÓN DE CONVERSIÓN

Cada controller sigue este patrón:

### ANTES (Thymeleaf @Controller)
```java
@Controller
@RequestMapping("/cliente")
public class ClienteController {
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        // Cargar datos
        model.addAttribute("reservas", reservas);
        model.addAttribute("usuario", usuario);
        return "cliente/dashboard"; // Template HTML
    }
    
    @PostMapping("/reserva")
    public String crearReserva(@ModelAttribute ReservaDTO dto, RedirectAttributes redirectAttributes) {
        // Lógica
        redirectAttributes.addFlashAttribute("mensaje", "Éxito");
        return "redirect:/cliente/dashboard";
    }
}
```

### DESPUÉS (REST @RestController)
```java
@RestController
@RequestMapping("/api/cliente")
public class ClienteRestController {
    
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> dashboard(Authentication auth) {
        // Misma lógica, pero construir DTO
        DashboardDTO dto = new DashboardDTO(reservas, usuario, ...);
        return ResponseEntity.ok(dto);
    }
    
    @PostMapping("/reserva")
    public ResponseEntity<?> crearReserva(@RequestBody ReservaDTO dto, Authentication auth) {
        // Misma lógica
        // Sin redirect (Angular lo maneja)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ResponseDTO("Éxito", nuevaReserva));
    }
}
```

---

## GUÍA POR CONTROLADOR

### 1. ClienteController.java → ClienteRestController.java

**Cambios principales:**
- `@Controller` → `@RestController`
- `@RequestMapping("/cliente")` → `@RequestMapping("/api/cliente")`
- `Model model` ELIMINAR
- `return "cliente/dashboard"` → `return ResponseEntity.ok(dashboardDTO)`
- `@ModelAttribute` → `@RequestBody`
- `RedirectAttributes` ELIMINAR

**Endpoints clave:**
```
GET    /api/cliente/dashboard           → DashboardClienteDTO
POST   /api/cliente/reserva             → ResponseEntity
GET    /api/cliente/reserva/{id}        → ReservaDetalleDTO
GET    /api/cliente/chats               → List<ChatDTO>
GET    /api/cliente/favoritos           → List<ActividadDTO>
PUT    /api/cliente/perfil              → ResponseEntity
POST   /api/cliente/favorito/toggle/{id}→ ResponseEntity
```

---

### 2. ColaboradorController.java → ColaboradorRestController.java

**Cambios principales:**
- Mismos cambios que ClienteController
- `@RequestMapping("/colaborador")` → `@RequestMapping("/api/colaborador")`

**Endpoints clave:**
```
GET    /api/colaborador/dashboard       → DashboardColaboradorDTO (KPIs)
GET    /api/colaborador/actividades     → List<ActividadDTO>
POST   /api/colaborador/actividades     → ResponseEntity (crear)
PUT    /api/colaborador/actividades/{id}→ ResponseEntity (editar)
GET    /api/colaborador/reservas        → List<ReservaDTO>
PUT    /api/colaborador/reservas/{id}   → ResponseEntity (cambiar estado)
GET    /api/colaborador/disponibilidades→ List<DisponibilidadDTO>
POST   /api/colaborador/disponibilidades→ ResponseEntity (crear)
```

---

### 3. AdminController.java → AdminRestController.java

**Cambios principales:**
- `@RequestMapping("/admin")` → `@RequestMapping("/api/admin")`
- Solo usuarios con rol ROLE_ADMIN pueden acceder

**Endpoints clave:**
```
GET    /api/admin/dashboard             → AdminDashboardDTO (estadísticas)
GET    /api/admin/usuarios              → PageDTO<UsuarioDTO>
GET    /api/admin/categorias            → List<CategoriaDTO>
POST   /api/admin/categorias            → ResponseEntity
PUT    /api/admin/comision              → ResponseEntity
```

---

## RECOMENDACIÓN DE ORDEN DE MIGRACIÓN

**Prioridad ALTA (hacer primero):**
1. Reemplazar AllAccessController.java por AllAccessController_REST.java
2. ClienteController → ClienteRestController (muchas rutas críticas)
3. ColaboradorController → ColaboradorRestController

**Prioridad MEDIA:**
4. AdminController → AdminRestController
5. ComentarioController → ComentarioRestController
6. OnboardingController → OnboardingRestController

**Prioridad BAJA (opcional):**
7. PagoController → adaptarla a REST (puede quedar igual si recibe webhooks)
8. DocumentoController → DocumentoRestController
9. PlanController → PlanRestController

---

## ⚠️ ERRORES COMUNES A EVITAR

1. **NO eliminar archivos de @Controller antigos.** Crearlos con nuevo nombre (_REST) primero.
2. **NO olvidar cambiar `@RequestMapping`** al nuevo `/api/...` path.
3. **NO pasar `Model model` como parámetro** en endpoints REST.
4. **NO retornar strings de template** (`return "vista"`), SIEMPRE retornar ResponseEntity.
5. **NO olvidar `@RequestBody`** en POST/PUT, NO `@ModelAttribute`.
6. **NO mezclar Thymeleaf con REST** en el mismo controller.

---

## CHECKLIST PARA CADA MIGRATION

- [ ] Crear nuevo archivo: `*RestController.java`
- [ ] Cambiar `@Controller` → `@RestController`
- [ ] Cambiar `@RequestMapping` a `/api/...`
- [ ] Eliminar todos los parámetros `Model model`
- [ ] Cambiar todos `return "template"` → `return ResponseEntity.ok(dto)`
- [ ] Cambiar `@ModelAttribute` → `@RequestBody`
- [ ] Crear DTOs de respuesta para cada endpoint
- [ ] Cambiar `RedirectAttributes` por ResponseEntity
- [ ] Validar que NO hay `model.addAttribute(...)`
- [ ] Probar endpoints con Postman/curl
- [ ] Eliminar archivo antiguo solo después de validar completamente
- [ ] Actualizar rutas en SecurityConfig (si es necesario)

---

## Próximos pasos manuales:

1. Reemplazar archivo AllAccessController.java:
   ```bash
   rm AllAccessController.java
   mv AllAccessController_REST.java AllAccessController.java
   ```

2. Crear ClienteRestController (seguir el patrón arriba)

3. Crear ColaboradorRestController

4. Crear AdminRestController

5. Eliminar archivos antiguos de controllers

6. Recompilación y testing

---

## DTOs necesarios para TODAS las migraciones

Ya existen:
- ActividadDTO ✅
- ReservaDTO ✅
- ActividadDetalleDTO ✅
- ComentarioDTO ✅
- DisponibilidadDTO ✅
- LoginRequestDTO ✅
- AuthResponseDTO ✅

A crear:
- DashboardClienteDTO (reservas, favoritos, usuario)
- DashboardColaboradorDTO (KPIs, estadísticas, actividades)
- AdminDashboardDTO (estadísticas globales)
- ChatDTO (conversaciones)
- ResponseDTO (respuesta genérica con mensaje)
- PaginatedResponseDTO (para listas con paginación)

---

**Esta guía es tu roadmap. Síguelo paso a paso y no habrá problemas.**
