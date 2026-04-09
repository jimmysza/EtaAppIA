Aquí está el prompt listo para pasarle a Claude Code:

---

## Prompt para Claude Code — Funcionalidad "Planes del Día"

```
Lee primero el archivo de contexto arquitectónico del proyecto (Contexto_del_proyecto) antes de tocar cualquier archivo.

---

## NUEVA FUNCIONALIDAD: Planes del Día

### Concepto
Un "Plan" es una colección ordenada de Actividades agrupadas en una ruta temática para un día completo en Cartagena (ej: "Día Cultural en el Centro Histórico", "Aventura en las Islas"). Es análogo a lo que Google Maps llama un "day trip plan". Los planes los puede crear tanto un Cliente como un Colaborador, y eso debe quedar reflejado visualmente en la vista.

---

### 1. MODELO DE DATOS — Nuevas Entidades

**Entidad `Plan`** (`maineta/eta/entity/Plan.java`):
```java
@Entity
@Table(name = "planes")
public class Plan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;           // Ej: "Día Cultural en Getsemaní"
    private String descripcion;      // Descripción breve del plan
    private String imagenPortada;    // path a /uploads/
    private String duracionEstimada; // Ej: "8 horas"
    private String tipo;             // Ej: "Cultural", "Aventura", "Gastronómico"

    // Creador polimórfico — solo uno de los dos será no-null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente_creador")
    private Cliente clienteCreador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_colaborador_creador")
    private Colaborador colaboradorCreador;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<PlanActividad> actividades = new ArrayList<>();

    private LocalDateTime fechaCreacion;
    private boolean publico = true; // Si es visible en la vista pública
    private int vistas = 0;
}
```

**Entidad `PlanActividad`** (`maineta/eta/entity/PlanActividad.java`) — tabla intermedia con orden y hora sugerida:
```java
@Entity
@Table(name = "plan_actividades")
public class PlanActividad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan")
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_actividad")
    private Actividad actividad;

    private int orden;               // 1, 2, 3... para ordenar la ruta
    private String horaSugerida;     // Ej: "9:00 AM"
    private String notaPersonalizada; // Tip del creador para esa parada
}
```

---

### 2. DTOs

**`PlanDTO`** (`maineta/eta/dto/PlanDTO.java`):
```java
public class PlanDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private String imagenPortada;
    private String duracionEstimada;
    private String tipo;
    private List<PlanActividadDTO> actividades;
    private String nombreCreador;
    private String rolCreador;   // "CLIENTE" o "COLABORADOR"
    private Long idCreador;
    private LocalDateTime fechaCreacion;
    private int vistas;
}
```

**`PlanActividadDTO`** (`maineta/eta/dto/PlanActividadDTO.java`):
```java
public class PlanActividadDTO {
    private Long idActividad;
    private String nombreActividad;
    private String slugActividad;     // para construir /actividad/{slug}-{id}
    private String imagenActividad;
    private BigDecimal precioConsumidor; // calculado con UsuarioHelper
    private String categoriaActividad;
    private Double latitud;           // para el mapa
    private Double longitud;          // para el mapa
    private int orden;
    private String horaSugerida;
    private String notaPersonalizada;
}
```

**`CrearPlanFormDTO`** (`maineta/eta/dto/CrearPlanFormDTO.java`) — formulario de creación:
```java
public class CrearPlanFormDTO {
    @NotBlank
    private String titulo;
    private String descripcion;
    private String duracionEstimada;
    private String tipo;
    private MultipartFile imagenPortada;
    // IDs de actividades en orden, con hora y nota — enviadas como JSON string
    private String actividadesJson; // se parsea en el servicio
}
```

---

### 3. REPOSITORIOS

**`PlanRepository`** (`maineta/eta/repository/PlanRepository.java`):
```java
public interface PlanRepository extends JpaRepository<Plan, Long> {
    // Top 5 planes públicos más recientes para la vista pública
    List<Plan> findTop5ByPublicoTrueOrderByFechaCreacionDesc();

    // Planes creados por un cliente específico
    List<Plan> findByClienteCreadorIdAndPublicoTrue(Long idCliente);

    // Planes creados por un colaborador específico
    List<Plan> findByColaboradorCreadorIdAndPublicoTrue(Long idColaborador);
}
```

---

### 4. SERVICIO

**`PlanService`** (`maineta/eta/service/PlanService.java` + `PlanServiceImpl.java`):

Métodos requeridos:
- `List<PlanDTO> obtenerTop5Recientes()` — para la vista pública principal
- `PlanDTO obtenerPorId(Long id)` — detalle de un plan (incrementa vistas)
- `Plan crearPlan(CrearPlanFormDTO form, Long idCreador, String rolCreador)` — lógica de creación
- `void incrementarVistas(Long idPlan)` — UPDATE directo sin SELECT, igual que en ActividadService

Reglas en `crearPlan()`:
- Parsear `actividadesJson` para obtener la lista de `{idActividad, orden, horaSugerida, nota}`
- Usar `actividadRepository.findAllById(ids)` para obtener actividades en batch (NO loop con findById)
- Calcular `precioConsumidor` de cada actividad con `UsuarioHelper.CalcularPrecioConsumidor()`
- Asignar `clienteCreador` o `colaboradorCreador` según `rolCreador`
- Guardar imagen con `UploadFileServiceImpl` validando MIME con Apache Tika

---

### 5. CONTROLLERS

**`PlanController`** (`maineta/eta/controller/PlanController.java`) — rutas PÚBLICAS y mixtas:

```
GET  /planes                  → Vista principal con top 5 planes + mapa (planes.html)
GET  /planes/{id}             → Detalle de un plan (detalle-plan.html) — incrementa vistas
```

**`ClientePlanController`** (`maineta/eta/controller/ClientePlanController.java`) — solo ROLE_CLIENTE:
```
GET  /cliente/planes/crear    → Formulario crear plan (crear-plan.html)
POST /cliente/planes/crear    → Procesa creación
GET  /cliente/planes/mis-planes → Lista planes creados por este cliente
```

**`ColaboradorPlanController`** (`maineta/eta/controller/ColaboradorPlanController.java`) — solo ROLE_COLABORADOR:
```
GET  /colaborador/planes/crear    → Formulario crear plan (crear-plan.html, mismo template reutilizado)
POST /colaborador/planes/crear    → Procesa creación
GET  /colaborador/planes/mis-planes → Lista planes del colaborador
```

En ambos controllers de creación, el formulario de creación debe ser el MISMO template `planes/crear-plan.html`, pasando como variable de modelo `rolActual` ("CLIENTE" o "COLABORADOR") para adaptar textos y el endpoint del formulario via Thymeleaf.

---

### 6. SEGURIDAD — SecurityConfig.java

Añadir en la configuración de rutas protegidas:
```java
.requestMatchers("/cliente/planes/**").hasRole("CLIENTE")
.requestMatchers("/colaborador/planes/**").hasRole("COLABORADOR")
.requestMatchers("/planes/**").permitAll()
```

---

### 7. ENTIDAD `Actividad` — Añadir coordenadas

La vista necesita colocar marcadores en el mapa. Añadir a la entidad `Actividad` dos campos nuevos opcionales:
```java
private Double latitud;
private Double longitud;
```
Con getter/setter Lombok. El campo puede ser null (actividades sin geolocalización simplemente no aparecen en el mapa pero sí en la lista del plan).

---

### 8. TEMPLATE PRINCIPAL — `planes.html`

Ruta: `src/main/resources/templates/planes/planes.html`

Layout de la página (de arriba a abajo):

**A) Header de sección:**
- Título "Planes del Día en Cartagena"
- Subtítulo explicativo
- Botón CTA: si el usuario está autenticado → "Crear mi Plan" (redirige según rol: `/cliente/planes/crear` o `/colaborador/planes/crear`). Si no está autenticado → "Crea tu Plan (Inicia sesión)"
- Usar `th:if` con `${#authorization.expression('isAuthenticated()')}` y `sec:authorize` de Thymeleaf Security

**B) Mapa interactivo (lado izquierdo, sticky en desktop):**
- Integrar Google Maps JavaScript API (o Leaflet.js como alternativa sin API key — PREFERIR Leaflet con OpenStreetMap para evitar costos)
- El mapa muestra TODOS los marcadores de TODOS los planes visibles, con colores distintos por plan (usar una paleta de 5 colores)
- Al hacer click en un marcador → resalta el plan correspondiente en la lista lateral
- Usar `th:inline="javascript"` para pasar los datos de coordenadas al JS

**C) Lista de planes (lado derecho, scrolleable):**
- Mostrar los top 5 planes más recientes como cards verticales
- Cada card muestra:
  - Imagen de portada
  - Título y tipo (badge)
  - Duración estimada
  - Número de actividades incluidas
  - Badge del creador: si `rolCreador == "COLABORADOR"` → badge azul "Creado por Colaborador" con nombre; si `rolCreador == "CLIENTE"` → badge verde "Creado por la Comunidad" con nombre
  - Preview de las primeras 3 actividades del plan (nombre + hora sugerida + link `/actividad/{slug}-{id}`)
  - Botón "Ver Plan Completo" → `/planes/{id}`

**D) Sección "Crea tu Propio Plan":**
- Banner CTA al final animado con Tailwind
- Explica brevemente la funcionalidad

---

### 9. TEMPLATE CREACIÓN — `crear-plan.html`

Ruta: `src/main/resources/templates/planes/crear-plan.html`

El formulario tiene dos secciones principales:

**Sección 1 — Info del plan:**
- Título, descripción, tipo (select), duración estimada, imagen de portada (file upload)

**Sección 2 — Selector de actividades (dinámico con JS):**
- Un buscador de actividades que hace `fetch('/actividades/buscar?nombre=X')` y muestra resultados
- Al seleccionar una actividad → se añade a una lista visual de "paradas del plan" con:
  - Número de orden (drag para reordenar, o simplemente ↑↓ buttons)
  - Nombre de la actividad
  - Campo "Hora sugerida" (input time)
  - Campo "Nota para los viajeros" (textarea pequeño)
  - Botón eliminar parada
- Al final, un `<input type="hidden" name="actividadesJson">` que se rellena con JS antes del submit con el JSON de la lista
- El `action` del formulario se adapta via Thymeleaf: `th:action="${rolActual == 'CLIENTE' ? '/cliente/planes/crear' : '/colaborador/planes/crear'}"`

---

### 10. CONVENCIONES A RESPETAR

- Usar `th:replace="~{componentes/navbar :: navbar}"` y `~{componentes/footer :: footer}` en ambos templates
- Compilar los nuevos estilos en `tailwind-input.css` (NO editar `tailwind.css` directamente)
- Crear `src/main/resources/static/css/planes/planes.css` para estilos específicos de esta sección
- Crear `src/main/resources/static/js/planes/planes.js` para la lógica del mapa y la interacción
- Crear `src/main/resources/static/js/planes/crear-plan.js` para el selector dinámico de actividades
- NO calcular precios en el template — `PlanActividadDTO` ya trae `precioConsumidor` calculado
- NO usar `findById()` en loop — batch queries en el servicio
- `Plan` no tiene eliminación directa — usar campo `publico = false` como soft delete
- Documentar decisiones no obvias con comentarios en español en el código

---

### ORDEN DE IMPLEMENTACIÓN SUGERIDO

1. Entidades `Plan` y `PlanActividad` + campo latitud/longitud en `Actividad`
2. Repositorios
3. DTOs
4. `PlanService` + `PlanServiceImpl`
5. `SecurityConfig` — añadir rutas
6. `PlanController`, `ClientePlanController`, `ColaboradorPlanController`
7. Template `planes.html` (vista pública con mapa y lista)
8. Template `crear-plan.html` (formulario compartido)
9. CSS y JS por sección
10. Verificar que `npm run build:css` recompila Tailwind correctamente
```

---

**Notas clave de diseño que tomé de la imagen que compartiste:**
- El layout es mapa a la izquierda + lista de planes scrolleable a la derecha, igual al estilo de Google Maps que se ve en la captura
- Los marcadores numerados corresponden a cada parada del plan
- El badge de rol del creador diferencia visualmente planes de colaboradores (verificados/negocio) vs. planes de la comunidad (clientes), lo que es un diferenciador de valor para la app