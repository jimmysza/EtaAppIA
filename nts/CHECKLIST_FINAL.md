# ✅ CHECKLIST FINAL: VALIDACIÓN DE MIGRACIÓN

## FASE 1: INFRAESTRUCTURA (COMPLETADA ✅)

### Dependencias
- [x] JWT (jjwt-api, jjwt-impl, jjwt-jackson) agregadas a pom.xml
- [x] Versión correcta: 0.12.6

### Configuración Spring Security
- [x] SecurityConfig completamente reescrito
- [x] sessionCreationPolicy = STATELESS
- [x] Filtro JWT agregado
- [x] CORS habilitado (localhost:4200, 3000, 8080)
- [x] Rutas públicas definidas
- [x] Rutas protegidas definidas por rol
- [x] exceptionHandling configurado

### Servicios de JWT
- [x] JwtService.java creado
  - [x] generarToken(Authentication)
  - [x] generarToken(String, String)
  - [x] esValido(String)
  - [x] extraerClaims(String)
  - [x] extraerUsername(String)
  - [x] extraerRoles(String)
  - [x] getAuthentication(String)
- [x] JwtAuthFilter.java creado
  - [x] Extrae Bearer token del header
  - [x] Valida token
  - [x] Establece Authentication en SecurityContext

### Autenticación
- [x] AuthController creado con endpoints:
  - [x] POST /api/auth/login
  - [x] POST /api/auth/registro
  - [x] POST /api/auth/logout
  - [x] GET /api/auth/validate
- [x] DTOs de autenticación:
  - [x] LoginRequestDTO
  - [x] AuthResponseDTO
  - [x] RegistroRequestDTO

### Configuración de propiedades
- [x] jwt.secret agregada a application.properties
- [x] jwt.expiration agregada (24 horas)

---

## FASE 2: ENDPOINTS PÚBLICOS (COMPLETADA ✅)

### AllAccessController (REST)
- [x] Archivo AllAcessController_REST.java creado
- [x] Anotación @RestController
- [x] @RequestMapping("/api")
- [x] Método getDashboard() eliminado de parámetros Model
- [x] Todos los endpoints devuelven ResponseEntity

### Endpoints públicos creados ✅
- [x] GET /api/actividades (Landing page)
- [x] GET /api/actividades/{id} (Detalle)
- [x] GET /api/actividades/buscar (Búsqueda con filtros)
- [x] GET /api/colaboradores/{id} (Perfil colaborador)
- [x] GET /api/categorias (Categorías)
- [x] GET /api/idiomas (Idiomas)
- [x] GET /api/actividades/tendencias (Tendencias)

### DTOs para endpoints públicos
- [x] ActividadDTO (existente, reutilizado)
- [x] ActividadDetalleDTO (nuevo)
- [x] LandingPageDTO (nuevo)
- [x] DisponibilidadDTO (nuevo)
- [x] ComentarioDTO (nuevo)
- [x] CategoriaDTO (existente)
- [x] ColaboradorPublicoDTO (existente)

### DTOs genéricos
- [x] ResponseDTO (mensaje + datos + éxito)
- [x] PaginatedResponseDTO<T> (para listas con paginación)

---

## FASE 3: PENDIENTE - ENDPOINTS AUTENTICADOS

### Que hacer ANTES de compilar

**⚠️ CRÍTICO: Reemplazar AllAccessController.java**

```bash
# En terminal, dentro del proyecto:
cd src/main/java/maineta/eta/controller

# 1. Hacer backup del antiguo
mv AllAcessController.java AllAcessController_BACKUP.java

# 2. Renombrar el nuevo
mv AllAcessController_REST.java AllAcessController.java
```

### Cliente Endpoints (SIN JWT aún)

**IMPORTANTE:** Los siguientes controllers siguen siendo `@Controller` (Thymeleaf):
- [ ] ClienteController.java
- [ ] ColaboradorController.java
- [ ] AdminController.java
- [ ] RegistroController.java
- [ ] ComentarioController.java
- [ ] OnboardingController.java
- [ ] PagoController.java

**Necesitan migrar a `@RestController` + DTOs**

### DTOs para Cliente
- [x] DashboardClienteDTO (creado)
- [ ] ReservaDetalleDTO (usar ReservaDTO existente)
- [ ] ChatDTO (crear)
- [ ] UsuarioPerfilDTO (dentro de DashboardClienteDTO)

### DTOs para Colaborador
- [x] DashboardColaboradorDTO (creado)
- [ ] KPIsDTO (dentro de DashboardColaboradorDTO)

---

## COMPILACIÓN Y TESTING

### Paso 1: Verificación previa

```bash
# Verificar que NO hay templates en uso (Controllers REST)
grep -r "return \"cliente/" src/ 2>/dev/null | wc -l
# Debe retornar: 0 (después de migración)

# Verificar que SÍ hay ResponseEntity
grep -r "ResponseEntity" src/main/java/maineta/eta/controller/ | wc -l
# Debe retornar: > 0

# Verificar que todos son @RestController
grep -r "@RestController" src/main/java/maineta/eta/controller/ | wc -l
# Debe retornar: número de controllers REST
```

### Paso 2: Compilación

```bash
# Clean compile
mvn clean compile

# Si hay errores, revisar:
# 1. ¿Falta @RequestBody en POST/PUT?
# 2. ¿Hay model.addAttribute() todavía?
# 3. ¿Falta ResponseEntity en algún return?
# 4. ¿Hay imports faltantes?
```

### Paso 3: Ejecución

```bash
mvn spring-boot:run

# Espera que veas:
# Tomcat started on port(s): 8080
# Started EtaAppIARest in X.XXX seconds
```

### Paso 4: Testing manual

```bash
# Test 1: Landing page (público)
curl http://localhost:8080/api/actividades | jq '.'

# Test 2: Login
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "cliente@example.com", "password": "sanchez"}' \
  | jq -r '.token')

echo "Token: $TOKEN"

# Test 3: Usar token en endpoint protegido
curl http://localhost:8080/api/cliente/dashboard \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.'

# Test 4: Token inválido → 401
curl http://localhost:8080/api/cliente/dashboard \
  -H "Authorization: Bearer invalid" \
  | jq '.'

# Test 5: Sin token en ruta protegida → 401
curl http://localhost:8080/api/cliente/dashboard \
  | jq '.'
```

### Resultados esperados

```json
// Test 1: OK
{
  "actividades": [...],
  "categorias": [...],
  "totalPages": 5,
  ...
}

// Test 2: OK
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "rol": "ROLE_CLIENTE",
  "nombre": "Juan",
  "email": "cliente@example.com"
}

// Test 3: OK (si endpoint existe)
{
  "usuarioPerfil": {...},
  "reservas": [...],
  ...
}

// Test 4: 401
{ "error": "Unauthorized" }

// Test 5: 401
{ "error": "Unauthorized" }
```

---

## ERRORES COMUNES Y SOLUCIONES

### Error: "Cannot resolve symbol 'JwtService'"
- **Causa:** No importó JwtService
- **Solución:** Agregar import en el controller

### Error: "Model cannot be resolved"
- **Causa:** Todavía hay Model model en algún método
- **Solución:** Eliminar Model de los parámetros

### Error: "return value cannot be resolved"
- **Causa:** Retorna string de template (antes era Thymeleaf)
- **Solución:** Cambiar a return ResponseEntity.ok(dto)

### Error: 400 Bad Request en POST
- **Causa:** Falta @RequestBody en DTO
- **Solución:** Agregar @RequestBody antes del parámetro DTO

### Error: 401 Unauthorized en /api/cliente/**
- **Causa:** Token no incluido en header o es inválido
- **Solución:** Verificar que el token se envía: `Authorization: Bearer <token>`

### Error: CORS error en Angular
- **Causa:** CORS no está bien configurado
- **Solución:** Verificar SecurityConfig tiene corsConfigurationSource()

---

## DESPUÉS DE COMPILAR EXITOSAMENTE

### Paso 5: Eliminar archivos Thymeleaf

**SOLO cuando TODOS los controllers sean REST:**

```bash
# Hacer backup de templates (por si acaso)
mv src/main/resources/templates src/main/resources/templates_backup

# Eliminar Thymeleaf de pom.xml (opcional, pero recomendado)
# <dependency>
#   <groupId>org.springframework.boot</groupId>
#   <artifactId>spring-boot-starter-thymeleaf</artifactId>
# </dependency>

# Si eliminaste Thymeleaf de pom.xml:
mvn clean compile
```

### Paso 6: Configurar Frontend (Angular)

Ver archivo: `ANGULAR_SETUP.md`

Checklist Angular:
- [ ] AuthService creado
- [ ] AuthInterceptor creado y registrado
- [ ] AuthGuard creado
- [ ] RoleGuard creado
- [ ] Routes protegidas configuradas
- [ ] CORS verificado en SecurityConfig
- [ ] Componentes de login listos

---

## ROLLBACK (si es necesario)

Si algo falla y necesitas rollback a Thymeleaf:

```bash
# 1. Restaurar archivo antiguo
mv AllAcessController_BACKUP.java AllAcessController.java
rm AllAcessController_REST.java

# 2. Restaurar templates (si los borraste)
mv src/main/resources/templates_backup src/main/resources/templates

# 3. Restaurar SecurityConfig (si hiciste cambios)
# git checkout src/main/java/maineta/eta/config/SecurityConfig.java

# 4. Compilar de nuevo
mvn clean compile
```

---

## PERFORMANCE & MONITORING

### Verificar que JWT está funcionando

```bash
# Ver el token decodificado (en jwt.io o terminal)
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Base64 decodificar (Linux/Mac)
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq '.'

# Resultado esperado:
{
  "sub": "cliente@example.com",
  "roles": "ROLE_CLIENTE",
  "iat": 1704067200,
  "exp": 1704153600
}
```

### Logs útiles

```bash
# Agregar en application.properties (opcional)
logging.level.maineta.eta.config=DEBUG
logging.level.org.springframework.security=DEBUG

# Ver en logs:
# - Token being validated
# - Authentication set in SecurityContext
# - Request processed by JwtAuthFilter
```

---

## DOCUMENTACIÓN CREADA PARA TI

| Archivo | Propósito |
|---------|-----------|
| RESUMEN_MIGRACION.md | Resumen ejecutivo (ESTE ES EL PRINCIPAL) |
| MIGRACION_REST_JWT.md | Guía detallada paso a paso |
| ARQUITECTURA_REST_JWT.md | Diagramas y arquitectura |
| ANGULAR_SETUP.md | Configuración del frontend Angular |
| application.properties | JWT config agregada |

---

## SIGUIENTE: TU TURNO

### Próximas tareas (en orden):

1. ✅ Reemplazar AllAccessController.java (CRITICO - hacer primero)
2. [ ] Compilar y testear endpoints públicos
3. [ ] Crear ClienteRestController
4. [ ] Crear ColaboradorRestController
5. [ ] Crear AdminRestController
6. [ ] Testear endpoints protegidos
7. [ ] Integración con Angular

### Estimado de tiempo:

- Paso 1: 5 minutos
- Paso 2: 15 minutos
- Paso 3: 45 minutos
- Paso 4: 45 minutos
- Paso 5: 45 minutos
- Paso 6: 30 minutos
- Paso 7: 2 horas (Angular)

**Total estimado:** 4-5 horas

---

## FINAL CHECKLIST

- [ ] Reemplazado AllAccessController.java
- [ ] mvn clean compile (sin errores)
- [ ] mvn spring-boot:run (inicia exitosamente)
- [ ] curl /api/actividades (200 OK)
- [ ] curl POST /api/auth/login (200 OK + token)
- [ ] curl con token → /api/cliente/dashboard (200 OK)
- [ ] curl sin token → /api/cliente/dashboard (401 Unauthorized)
- [ ] CORS funcionando (Angular puede conectar)
- [ ] Todos los DTOs mapeados correctamente
- [ ] Documentación leída y entendida

---

**Creado:** 01-May-2026
**Versión:** 1.0
**Estado:** Listo para comenzar fase de implementación
**Tiempo para completar:** ~5 horas

---

**✨ Buena suerte con la migración. Sigue este checklist y no tendrás problemas.**
