### Descripción general

Al registrarse como cliente (o en su primer login si llegó por Google), el usuario pasa por un flujo de onboarding de 4 preguntas de selección. Las respuestas se guardan en su perfil y alimentan el home con listas personalizadas y de tendencias. El colaborador gana visibilidad del rendimiento de sus actividades en su dashboard.

---

### Cambios en el modelo de datos

**Tabla `cliente` — campos nuevos:**

| Campo | Tipo | Descripción |
|---|---|---|
| `paisOrigen` | VARCHAR(80) | Viene de Google OAuth o se pregunta manualmente |
| `grupoViaje` | ENUM('SOLO','PAREJA','FAMILIA','AMIGOS','VARIOS') | Respuesta pregunta 2 |
| `rangoPrecio` | ENUM('ECONOMICO','MODERADO','PREMIUM') | Respuesta pregunta 3 |
| `disponibilidadSemana` | ENUM('FINDE','ENTRE_SEMANA','AMBOS') | Respuesta pregunta 4 |
| `onboardingCompletado` | BOOLEAN default false | Flag para saber si ya hizo el flujo |

**Tabla `actividad` — campos nuevos:**

| Campo | Tipo | Descripción |
|---|---|---|
| `totalVistas` | INT default 0 | Suma cada vez que alguien entra al detalle |
| `totalTendencia` | INT default 0 | Suma cada visita en las últimas 72h (o usar ventana configurable) |

> La diferencia entre `totalVistas` y `totalTendencia` es que tendencia puede resetearse periódicamente o calcularse con una ventana de tiempo, mientras que vistas es acumulado histórico.

---

### Nuevas reglas de negocio

| ID | Regla |
|---|---|
| RN-11 | Al hacer GET `/actividad/{slug}-{id}`, se incrementa `totalVistas + 1` y `totalTendencia + 1` de esa actividad |
| RN-12 | Si `onboardingCompletado = false` al primer login/registro, el sistema redirige a `/cliente/onboarding` antes del dashboard |
| RN-13 | Las preferencias de categorías se derivan de las categorías que el usuario seleccione en pregunta 1 y se guardan como relación `cliente_categorias_preferidas` |
| RN-14 | El home del cliente muestra las 4 listas usando las preferencias guardadas |
| RN-15 | El colaborador ve `totalVistas` y `totalTendencia` de cada actividad en su dashboard |

---

### Preguntas del onboarding (vista `/cliente/onboarding`)

**Pregunta 1 — Categorías favoritas** (selección múltiple, muestra las categorías activas del sistema)
> ¿Qué tipo de actividades te gustan?

**Pregunta 2 — Grupo de viaje** (selección individual)
> ¿Con quién sueles salir?
> Opciones: Solo / En pareja / Con familia / Con amigos / Varía

**Pregunta 3 — Rango de precio** (selección individual)
> ¿Qué presupuesto manejas por actividad?
> Opciones: Económico (< $50k) / Moderado ($50k–$150k) / Premium (> $150k) — *ajustar montos a moneda local*

**Pregunta 4 — Disponibilidad** (selección individual)
> ¿Cuándo haces más actividades?
> Opciones: Fines de semana / Entre semana / Ambos

---

### Dato que aporta Google OAuth (AUTH-04)

Cuando el usuario inicia sesión con Google, extraer del perfil OAuth:
- `locale` → guardar como `paisOrigen` (o mostrar como pregunta opcional si no viene)
- `name` y `picture` → ya contemplados en el flujo existente

---

### Nueva tabla de relación (para pregunta 1)

```
cliente_categorias_preferidas
  - id_cliente (FK → cliente)
  - id_categoria (FK → categoria)
  - PRIMARY KEY (id_cliente, id_categoria)
```

---

### Nuevas vistas / rutas

| Ruta | Método | Descripción |
|---|---|---|
| `GET /cliente/onboarding` | GET | Formulario multi-paso con las 4 preguntas |
| `POST /cliente/onboarding` | POST | Guarda preferencias y marca `onboardingCompletado = true` |

El home del cliente (`la landing si está autenticado) ahora carga 4 secciones:
1. **Más tendencias** — ORDER BY `totalTendencia` DESC LIMIT 10
2. **Más vistas** — ORDER BY `totalVistas` DESC LIMIT 10
3. **Más reservadas** — COUNT de reservas por actividad DESC LIMIT 10
4. **Para ti** — actividades cuya `categoria` esté en `cliente_categorias_preferidas` del cliente, ordenadas por calificación DESC LIMIT 10

---

### Cambios en el dashboard del colaborador

Agregar en la vista de "Mis actividades" dos columnas nuevas: **Vistas** (`totalVistas`) y **Tendencia** (`totalTendencia`). Si se desea, una barra o indicador visual relativo al máximo del propio colaborador.

---

### Criterios de aceptación

```
WHEN un usuario visita el detalle de una actividad
THEN totalVistas y totalTendencia se incrementan en 1

WHEN un cliente completa el onboarding
THEN onboardingCompletado = true y sus preferencias quedan guardadas

WHEN el cliente accede al home
THEN ve 4 listas diferenciadas: tendencias, más vistas, más reservadas, para ti

WHEN el colaborador accede a su dashboard
THEN ve totalVistas y totalTendencia de cada una de sus actividades

WHEN un usuario se registra vía Google
THEN el sistema extrae el locale/país del perfil OAuth y lo guarda en paisOrigen
```

---

### Fuera del alcance de esta versión

- Algoritmo de recomendación con ML (la personalización es por filtro de categorías, no por comportamiento)
- Reset automático de `totalTendencia` (se puede implementar después con un scheduled job)
- Segmentación por `rangoPrecio` o `grupoViaje` en las listas (queda guardado para una v2 de personalización)

# Planning: Onboarding de preferencias y Home personalizado
## Plataforma ETA — Spring Boot 3 + Thymeleaf + Vanilla JS

---

## Descripción general

Se agrega un flujo de onboarding al registro de clientes para capturar sus preferencias. Con esas preferencias, el home del cliente muestra cuatro listas personalizadas. El colaborador gana visibilidad de cuántas veces fue vista cada una de sus actividades. Todo se resuelve con SSR (Thymeleaf), sin endpoints REST nuevos ni frameworks de frontend.

---

## Stack tecnológico de referencia

| Capa | Tecnología |
|---|---|
| Backend | Java 17 + Spring Boot 3 |
| Persistencia | Spring Data JPA / Hibernate + MySQL 8 |
| Seguridad | Spring Security + OAuth2 Client (Google) |
| Vistas | Thymeleaf 3 |
| Estilos | Tailwind CSS |
| Interactividad | Vanilla JavaScript |

---

## Glosario

| Término | Definición |
|---|---|
| `Cliente` | Usuario final registrado con `ROLE_CLIENTE` |
| `Colaborador` | Proveedor de actividades con `ROLE_COLABORADOR` |
| `onboardingCompletado` | Flag booleano en `Cliente`; `false` hasta que responda las preguntas |
| `categoriasPreferidas` | Relación `@ManyToMany` entre `Cliente` y `Categoria` |
| `totalVistas` | Contador acumulado de visitas al detalle de una actividad |
| `totalTendencia` | Contador de visitas recientes (ventana de tiempo configurable) |
| `GrupoViaje` | ENUM: `SOLO`, `PAREJA`, `FAMILIA`, `AMIGOS`, `VARIOS` |
| `RangoPrecio` | ENUM: `ECONOMICO`, `MODERADO`, `PREMIUM` |
| `DisponibilidadSemana` | ENUM: `FINDE`, `ENTRE_SEMANA`, `AMBOS` |

---

## Objetivos

1. Capturar preferencias del cliente en el momento del registro mediante 4 preguntas.
2. Mostrar 4 listas diferenciadas en el home: tendencias, más vistas, más reservadas y "para ti".
3. Incrementar los contadores de vistas al acceder al detalle de una actividad.
4. Mostrar `totalVistas` y `totalTendencia` en el dashboard del colaborador.
5. Aprovechar el `locale` de Google OAuth para guardar el país de origen del cliente.

---

## Cambios en el modelo de datos

### Tabla `cliente` — campos nuevos

```sql
ALTER TABLE cliente
  ADD COLUMN pais_origen     VARCHAR(80)  NULL,
  ADD COLUMN grupo_viaje     VARCHAR(20)  NULL,
  ADD COLUMN rango_precio    VARCHAR(20)  NULL,
  ADD COLUMN disponibilidad_semana VARCHAR(20) NULL,
  ADD COLUMN onboarding_completado BOOLEAN NOT NULL DEFAULT FALSE;
```

### Tabla `actividad` — campos nuevos

```sql
ALTER TABLE actividad
  ADD COLUMN total_vistas     INT NOT NULL DEFAULT 0,
  ADD COLUMN total_tendencia  INT NOT NULL DEFAULT 0;
```

### Tabla nueva `cliente_categorias_preferidas`

```sql
CREATE TABLE cliente_categorias_preferidas (
  cliente_id   BIGINT NOT NULL,
  categoria_id BIGINT NOT NULL,
  PRIMARY KEY (cliente_id, categoria_id),
  FOREIGN KEY (cliente_id)   REFERENCES cliente(id),
  FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);
```

### Entidad `Cliente.java` — anotaciones JPA

```java
@Enumerated(EnumType.STRING)
@Column(name = "grupo_viaje")
private GrupoViaje grupoViaje;

@Enumerated(EnumType.STRING)
@Column(name = "rango_precio")
private RangoPrecio rangoPrecio;

@Enumerated(EnumType.STRING)
@Column(name = "disponibilidad_semana")
private DisponibilidadSemana disponibilidadSemana;

@Column(name = "pais_origen")
private String paisOrigen;

@Column(name = "onboarding_completado")
private boolean onboardingCompletado = false;

@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
  name = "cliente_categorias_preferidas",
  joinColumns = @JoinColumn(name = "cliente_id"),
  inverseJoinColumns = @JoinColumn(name = "categoria_id")
)
private Set<Categoria> categoriasPreferidas = new HashSet<>();
```

### Entidad `Actividad.java` — campos nuevos

```java
@Column(name = "total_vistas")
private int totalVistas = 0;

@Column(name = "total_tendencia")
private int totalTendencia = 0;
```

### ENUMs nuevos

```java
public enum GrupoViaje { SOLO, PAREJA, FAMILIA, AMIGOS, VARIOS }
public enum RangoPrecio { ECONOMICO, MODERADO, PREMIUM }
public enum DisponibilidadSemana { FINDE, ENTRE_SEMANA, AMBOS }
```

---

## Reglas de negocio

| ID | Regla |
|---|---|
| RN-11 | Cada `GET /actividad/{slug}-{id}` incrementa `totalVistas + 1` y `totalTendencia + 1` en la actividad correspondiente |
| RN-12 | Si `onboardingCompletado = false`, cualquier acceso a `/cliente/**` redirige a `GET /cliente/onboarding` (excepto la propia ruta de onboarding) |
| RN-13 | El `POST /cliente/onboarding` guarda todas las preferencias y setea `onboardingCompletado = true` |
| RN-14 | Las 4 listas del home se calculan en el servidor y se pasan al modelo de Thymeleaf; no hay fetch ni AJAX |
| RN-15 | El colaborador ve `totalVistas` y `totalTendencia` de cada actividad propia en su dashboard |
| RN-16 | Al registrarse con Google OAuth2, el sistema extrae el `locale` del perfil y lo guarda en `paisOrigen` |
| RN-17 | La lista "Para ti" usa las `categoriasPreferidas` del cliente; si está vacía, muestra las mejor calificadas |

---

## Arquitectura del flujo (SSR)

```
[Registro/Login]
     │
     ▼
ClienteInterceptor (preHandle en /cliente/**)
     │  onboardingCompletado = false?
     ├──YES──► redirect: /cliente/onboarding
     │
     └──NO───► continúa normalmente
                    │
                    ▼
           GET /cliente/dashboard
                    │
           HomeService.poblarModel(model, cliente)
           ├─ tendencias   ← ORDER BY totalTendencia DESC LIMIT 10
           ├─ masVistas    ← ORDER BY totalVistas DESC LIMIT 10
           ├─ masReservadas← COUNT(reservas) GROUP BY actividad LIMIT 10
           └─ paraTi       ← WHERE categoria IN categoriasPreferidas LIMIT 10
                    │
                    ▼
           home.html → th:each para cada lista
```

---

## Nuevas rutas

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/cliente/onboarding` | `ROLE_CLIENTE` | Muestra el formulario de preferencias |
| `POST` | `/cliente/onboarding` | `ROLE_CLIENTE` | Guarda preferencias y redirige al dashboard |

Las demás rutas no cambian.

---

## Nuevo componente: `ClienteInterceptor`

Implementa `HandlerInterceptor` y se registra en `WebMvcConfigurer` solo para `/cliente/**`.

```java
@Component
public class ClienteInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        // 1. Obtener cliente autenticado del SecurityContext
        // 2. Si la URI contiene "/onboarding", dejar pasar (return true)
        // 3. Si !cliente.isOnboardingCompletado() → res.sendRedirect("/cliente/onboarding"); return false
        return true;
    }
}
```

Registro en `WebMvcConfigurer`:
```java
registry.addInterceptor(clienteInterceptor).addPathPatterns("/cliente/**");
```

---

## Nuevo DTO: `OnboardingForm`

```java
public class OnboardingForm {
    @NotNull
    private List<Long> categoriasIds;   // checkboxes de categorías

    @NotNull
    private GrupoViaje grupoViaje;

    @NotNull
    private RangoPrecio rangoPrecio;

    @NotNull
    private DisponibilidadSemana disponibilidadSemana;
}
```

---

## Nuevo controlador: `OnboardingController`

```java
@GetMapping("/cliente/onboarding")
public String mostrarOnboarding(Model model) {
    model.addAttribute("categorias", categoriaService.findAll());
    model.addAttribute("form", new OnboardingForm());
    return "cliente/onboarding";
}

@PostMapping("/cliente/onboarding")
public String guardarOnboarding(@Valid OnboardingForm form, BindingResult result, ...) {
    if (result.hasErrors()) return "cliente/onboarding";
    clienteService.guardarPreferencias(clienteAutenticado, form);
    return "redirect:/cliente/dashboard";
}
```

---

## Nueva vista: `onboarding.html` (Thymeleaf)

Un único `<form th:action="@{/cliente/onboarding}" method="post">` con:

- **Pregunta 1** — checkboxes generados con `th:each`:
  ```html
  <input type="checkbox" name="categoriasIds"
         th:value="${cat.id}"
         th:text="${cat.nombre}"
         th:each="cat : ${categorias}">
  ```

- **Pregunta 2** — radio buttons para `grupoViaje` (SOLO / PAREJA / FAMILIA / AMIGOS / VARIOS)

- **Pregunta 3** — radio buttons para `rangoPrecio` (ECONOMICO / MODERADO / PREMIUM)

- **Pregunta 4** — radio buttons para `disponibilidadSemana` (FINDE / ENTRE_SEMANA / AMBOS)

- Botón de envío estándar: `<button type="submit">Continuar</button>`

**Efecto wizard opcional con Vanilla JS** (sin carga de página):
```javascript
// Mostrar/ocultar secciones de preguntas sin cambiar la URL
const pasos = document.querySelectorAll('.paso');
let actual = 0;
document.getElementById('siguiente').addEventListener('click', () => {
    pasos[actual].classList.add('hidden');
    actual++;
    if (actual < pasos.length) pasos[actual].classList.remove('hidden');
    else document.getElementById('btn-submit').click();
});
```

Si no se usa el wizard, se muestran todas las preguntas en scroll en una sola página.

---

## Cambios en `ActividadService`

En el método que ya maneja `GET /actividad/{slug}-{id}`, agregar al inicio:

```java
actividad.setTotalVistas(actividad.getTotalVistas() + 1);
actividad.setTotalTendencia(actividad.getTotalTendencia() + 1);
actividadRepository.save(actividad);
```

---

## Nuevas queries en `ActividadRepository`

```java
// Más tendencias
List<Actividad> findTop10ByOrderByTotalTendenciaDesc();

// Más vistas
List<Actividad> findTop10ByOrderByTotalVistasDesc();

// Más reservadas (JPQL)
@Query("SELECT a FROM Actividad a JOIN a.reservas r GROUP BY a ORDER BY COUNT(r) DESC")
List<Actividad> findTop10MasReservadas(Pageable pageable);

// Para ti
List<Actividad> findByCategoriaInOrderByCalificacionDesc(Set<Categoria> categorias, Pageable pageable);
```

---

## Cambios en el Home del cliente

En el controlador que maneja el dashboard/home del cliente, agregar al modelo:

```java
model.addAttribute("tendencias",    actividadRepo.findTop10ByOrderByTotalTendenciaDesc());
model.addAttribute("masVistas",     actividadRepo.findTop10ByOrderByTotalVistasDesc());
model.addAttribute("masReservadas", actividadRepo.findTop10MasReservadas(PageRequest.of(0,10)));
model.addAttribute("paraTi",        actividadRepo.findByCategoriaInOrderByCalificacionDesc(
                                        cliente.getCategoriasPreferidas(), PageRequest.of(0,10)));
```

En `home.html`, cuatro secciones con `th:each`:

```html
<section th:each="lista : ${ {tendencias, masVistas, masReservadas, paraTi} }">
  <div th:each="act : ${lista}">
    <a th:href="@{/actividad/{s}-{id}(s=${act.slug},id=${act.id})}"
       th:text="${act.titulo}"></a>
  </div>
</section>
```

---

## Cambios en el dashboard del colaborador

En `actividades.html`, agregar dos columnas a la tabla existente:

```html
<td th:text="${act.totalVistas}">0</td>
<td th:text="${act.totalTendencia}">0</td>
```

No requiere controlador nuevo; los datos ya vienen en la entidad.

---

## Google OAuth2 — captura de país

En el `OAuth2UserService` o `AuthenticationSuccessHandler` existente, al crear o actualizar el `Cliente`:

```java
String locale = oAuth2User.getAttribute("locale"); // ej: "es-CO"
if (locale != null) {
    String pais = new Locale(locale.split("-")[0], locale.contains("-") ? locale.split("-")[1] : "")
                      .getDisplayCountry(new Locale("es"));
    cliente.setPaisOrigen(pais);
}
```

---

## Criterios de aceptación

```
WHEN un cliente recién registrado accede a /cliente/dashboard
THEN el sistema lo redirige a /cliente/onboarding

WHEN el cliente completa el onboarding y hace POST
THEN onboardingCompletado = true, sus preferencias quedan guardadas y es redirigido al dashboard

WHEN cualquier usuario (autenticado o no) abre el detalle de una actividad
THEN totalVistas y totalTendencia de esa actividad se incrementan en 1

WHEN el cliente autenticado accede al home
THEN ve 4 secciones diferenciadas: tendencias, más vistas, más reservadas, para ti

WHEN el colaborador accede a su dashboard de actividades
THEN ve las columnas totalVistas y totalTendencia para cada actividad propia

WHEN un cliente se registra vía Google OAuth
THEN el campo paisOrigen se guarda con el país derivado del locale del perfil
```

---

## Tareas

```
[ ] Crear ENUMs: GrupoViaje, RangoPrecio, DisponibilidadSemana
[ ] Agregar campos nuevos a la entidad Cliente (+ migración SQL ALTER TABLE)
[ ] Agregar totalVistas / totalTendencia a la entidad Actividad (+ migración SQL)
[ ] Crear tabla cliente_categorias_preferidas y la relación @ManyToMany en Cliente
[ ] Crear OnboardingForm DTO con validaciones @NotNull
[ ] Crear OnboardingController (GET + POST /cliente/onboarding)
[ ] Crear vista onboarding.html con form Thymeleaf (preguntas 1-4)
[ ] Agregar efecto wizard opcional con Vanilla JS (mostrar/ocultar .paso)
[ ] Implementar ClienteInterceptor y registrarlo en WebMvcConfigurer para /cliente/**
[ ] Modificar ActividadService.getDetalle() para incrementar los contadores
[ ] Agregar queries al ActividadRepository (tendencias, masVistas, masReservadas, paraTi)
[ ] Modificar el controlador del home para poblar las 4 listas en el Model
[ ] Actualizar home.html con las 4 secciones th:each
[ ] Actualizar actividades.html del colaborador con columnas de vistas y tendencia
[ ] Modificar OAuth2UserService para extraer y guardar paisOrigen del locale
```

---

## Fuera del alcance de esta iteración

- Algoritmo de recomendación con ML (la lista "para ti" filtra solo por categoría preferida).
- Reset automático de `totalTendencia` (queda para un scheduled job futuro).
- Segmentación por `rangoPrecio` o `grupoViaje` en las listas del home (guardado para v2).
- Notificaciones al colaborador cuando su actividad supere un umbral de vistas.