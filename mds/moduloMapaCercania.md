# SPEC — Vista de Mapa: Actividades Cercanas
**Módulo**: `actividades-cercanas`
**Controlador destino**: `AllAcessController`
**Acceso**: Público (sin autenticación requerida)
**Estado**: Por implementar
**Fecha**: Abril 2026

---

## 1. Resumen

Vista pública accesible en `/actividades/cercanas` que permite al usuario ver en un mapa interactivo las actividades turísticas más próximas a su ubicación actual. El radio de búsqueda es ajustable entre 1 y 5 km mediante un slider. Al hacer clic sobre cualquier actividad (marcador o tarjeta), el usuario navega al detalle de esa actividad usando la ruta existente `/actividad/{slug}-{id}`.

La página es **completamente pública**: funciona para usuarios anónimos, clientes y colaboradores autenticados.

---

## 2. Archivos a crear

### Backend

| Archivo | Paquete | Descripción |
|---------|---------|-------------|
| `ActividadCercanaDTO.java` | `maineta.eta.dto` | DTO de respuesta con campos necesarios para el mapa y la tarjeta |
| Método `buscarCercanas()` en `ActividadService.java` | `maineta.eta.service` | Lógica Haversine, bounding box, ordenamiento por distancia |
| Método `findByLatitudBetweenAndLongitudBetween()` en `ActividadRepository.java` | `maineta.eta.repository` | Query JPA para bounding box inicial |
| Endpoint `GET /actividades/cercanas` en `AllAcessController.java` | `maineta.eta.controller` | Sirve la vista HTML |
| Endpoint `GET /actividades/cercanas/json` en `AllAcessController.java` | `maineta.eta.controller` | AJAX: devuelve `List<ActividadCercanaDTO>` como JSON |

### Frontend

| Archivo | Ruta | Descripción |
|---------|------|-------------|
| `actividades-cercanas.html` | `src/main/resources/templates/` | Template Thymeleaf de la vista |
| `mapa-cercanas.js` | `src/main/resources/static/js/` | Toda la lógica del mapa, geolocalización, AJAX y renderizado de tarjetas |
| `mapa-cercanas.css` | `src/main/resources/static/css/` | Estilos específicos de esta vista (mapa, tarjetas, panel lateral) |

### Seguridad

No se requieren cambios en `SecurityConfig.java`. Las dos rutas nuevas son públicas y deben añadirse al bloque `permitAll()` existente:

```java
// En SecurityConfig.java — bloque permitAll ya existente
.requestMatchers("/actividades/cercanas", "/actividades/cercanas/json").permitAll()
```

---

## 3. DTO: `ActividadCercanaDTO`

```java
package maineta.eta.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ActividadCercanaDTO {
    private Long idActividad;
    private String titulo;
    private String slug;              // para construir la URL /actividad/{slug}-{id}
    private String imagen;            // imagen principal
    private BigDecimal precioConsumidor; // precio ya con comisión aplicada (via UsuarioHelper)
    private Integer calificacion;
    private String categoriaNombre;
    private String idiomaNombre;
    private Double latitud;
    private Double longitud;
    private Double distanciaKm;       // calculada en el servicio, no en BD
    private Integer totalComentarios;
}
```

**Reglas**:
- `precioConsumidor` siempre calculado con `UsuarioHelper.CalcularPrecioConsumidor()`. Nunca exponer el precio base del colaborador.
- `slug` es necesario para construir la URL de detalle en el frontend sin lógica adicional.
- `distanciaKm` se redondea a 2 decimales antes de asignar.
- `totalComentarios` se carga con la misma estrategia batch usada en el resto del proyecto (`ComentarioService.contarComentariosPorActividades()`), nunca en loop.

---

## 4. Servicio: `ActividadService` — método `buscarCercanas`

### Firma

```java
public List<ActividadCercanaDTO> buscarCercanas(
    double latUser,
    double lonUser,
    int radioKm,
    int limite
)
```

### Algoritmo

```
1. Calcular bounding box (delta = radioKm / 111.0)
   → latMin = latUser - delta  |  latMax = latUser + delta
   → lonMin = lonUser - delta  |  lonMax = lonUser + delta

2. actividadRepository.findByLatitudBetweenAndLongitudBetween(latMin, latMax, lonMin, lonMax)
   → Filtra filas en SQL antes de traer datos a Java.
   → Excluye automáticamente actividades con latitud o longitud null.

3. Para cada candidata: calcular distancia real con Haversine(latUser, lonUser, lat, lon)

4. Filtrar: distanciaKm <= radioKm  (el bounding box puede incluir esquinas fuera del radio)

5. Ordenar ascendente por distanciaKm

6. Tomar los primeros `limite` resultados (máximo 10)

7. Extraer ids → ComentarioService.contarComentariosPorActividades(ids) → Map<Long, Integer>

8. Mapear a ActividadCercanaDTO:
   - precioConsumidor = usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio())
   - totalComentarios = mapa.getOrDefault(id, 0)
   - distanciaKm = Math.round(dist * 100.0) / 100.0

9. Retornar lista
```

### Fórmula Haversine (método privado)

```java
private double haversine(double lat1, double lon1, double lat2, double lon2) {
    final double R = 6371; // radio Tierra en km
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
             + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
             * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
```

### Casos borde

| Situación | Comportamiento |
|-----------|----------------|
| Actividad con `latitud` o `longitud` null | Excluida por el query JPA (columnas nullable no entran en `BETWEEN`) |
| Sin actividades en el radio | Retorna lista vacía `[]` — no lanza excepción |
| `radioKm` fuera del rango 1-5 | El controller lo clampea: `Math.max(1, Math.min(5, radioKm))` |
| `limite` <= 0 | Se usa el valor por defecto de 10 |

---

## 5. Repository: `ActividadRepository` — método nuevo

```java
List<Actividad> findByLatitudBetweenAndLongitudBetween(
    Double latMin, Double latMax,
    Double lonMin, Double lonMax
);
```

Spring Data JPA genera la query automáticamente. No requiere `@Query` manual.

**Importante**: `latitud` y `longitud` son `Double` (objeto, nullable). JPA `BETWEEN` sobre nullable excluye nulls nativamente en MySQL, que es el comportamiento deseado.

---

## 6. Controller: `AllAcessController` — endpoints nuevos

### 6.1 Vista principal

```java
@GetMapping("/actividades/cercanas")
public String vistaMapa(Model model) {
    // No recibe lat/lon aquí. El navegador obtiene la geolocalización
    // en el cliente y luego llama al endpoint JSON vía AJAX.
    // Solo sirve la página HTML vacía con el mapa inicializado.
    return "actividades-cercanas";
}
```

### 6.2 Endpoint AJAX (JSON)

```java
@GetMapping("/actividades/cercanas/json")
@ResponseBody
public List<ActividadCercanaDTO> cercanasJson(
    @RequestParam double lat,
    @RequestParam double lon,
    @RequestParam(defaultValue = "3") int radio
) {
    int radioClamped = Math.max(1, Math.min(5, radio));
    return actividadService.buscarCercanas(lat, lon, radioClamped, 10);
}
```

**Respuesta exitosa** (`200 OK`):
```json
[
  {
    "idActividad": 42,
    "titulo": "Tour Histórico Getsemaní",
    "slug": "tour-historico-getsamani",
    "imagen": "tour-getsamani.jpg",
    "precioConsumidor": 53100,
    "calificacion": 5,
    "categoriaNombre": "Cultural",
    "idiomaNombre": "Español",
    "latitud": 10.4241,
    "longitud": -75.5508,
    "distanciaKm": 0.34,
    "totalComentarios": 12
  }
]
```

**Respuesta vacía** (`200 OK`): `[]`

---

## 7. Template: `actividades-cercanas.html`

### Estructura de la página

```
navbar (th:replace componentes/navbar)
│
├── SECCIÓN HERO
│   └── Título + descripción corta de la vista
│
├── SECCIÓN CONTROLES
│   ├── Slider radio (1 km – 5 km)
│   ├── Label "Radio: X km"
│   └── Botón "Usar mi ubicación" (dispara geolocalización)
│
├── SECCIÓN PRINCIPAL (layout de dos columnas en desktop, apiladas en móvil)
│   ├── COLUMNA IZQUIERDA: Mapa Leaflet
│   │   ├── Mapa interactivo con marcadores numerados
│   │   ├── Marcador especial para "tu ubicación" (icono de punto azul)
│   │   └── Círculo semitransparente mostrando el radio activo
│   │
│   └── COLUMNA DERECHA: Panel de resultados
│       ├── Contador: "X actividades encontradas"
│       ├── Lista scrolleable de tarjetas (máx 10)
│       │   └── Tarjeta: imagen · título · categoría · distancia · precio · calificación
│       └── Estado vacío: "No hay actividades en este radio"
│
└── footer (th:replace componentes/footer)
```

### Datos embebidos para Leaflet

Thymeleaf no pasa actividades al modelo inicial. La vista se sirve vacía y el JS las carga vía AJAX. El template solo necesita los scripts y estilos necesarios.

```html
<!-- En el <head> -->
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />

<!-- Antes del </body> -->
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script th:src="@{/js/mapa-cercanas.js}"></script>
```

### Estado inicial de la vista

Cuando el usuario abre la página sin haber pulsado "Usar mi ubicación":
- El mapa se muestra centrado en Cartagena (`[10.3910, -75.4794]`, zoom 14) como fallback.
- El panel lateral muestra el mensaje: `"Activa tu ubicación para descubrir actividades cerca de ti"`.
- El slider está visible pero deshabilitado hasta obtener coordenadas.

---

## 8. Script: `mapa-cercanas.js`

### Responsabilidades

1. Inicializar el mapa Leaflet centrado en Cartagena como fallback.
2. Solicitar geolocalización al usuario (`navigator.geolocation.getCurrentPosition`).
3. Al obtener coordenadas: centrar el mapa, pintar el marcador del usuario, habilitar el slider y hacer la primera llamada AJAX.
4. Escuchar cambios en el slider → hacer nueva llamada AJAX con el radio actualizado.
5. Procesar la respuesta JSON: pintar marcadores numerados en el mapa + renderizar tarjetas en el panel.
6. Al hacer clic en un marcador del mapa: resaltar la tarjeta correspondiente en el panel y hacer scroll hacia ella.
7. Al hacer clic en una tarjeta: resaltar el marcador en el mapa y navegar a `/actividad/{slug}-{id}` en una nueva pestaña (o la misma, a definir con el equipo).

### Funciones principales

```javascript
inicializarMapa()
  // Crea instancia Leaflet, tile layer OpenStreetMap, centra en Cartagena fallback

solicitarUbicacion()
  // Llama navigator.geolocation.getCurrentPosition
  // onSuccess → guardar lat/lon, centrar mapa, pintar userMarker, cargarActividades()
  // onError   → mostrar alerta usando js/alert.js con mensaje amigable

cargarActividades(lat, lon, radio)
  // fetch('/actividades/cercanas/json?lat=X&lon=Y&radio=R')
  // onSuccess → limpiarMarcadores(), pintarMarcadores(data), renderizarTarjetas(data)
  // onError   → mostrar alerta de error

pintarMarcadores(actividades)
  // Para cada actividad: L.marker([lat, lon]) con icono numerado (orden = distancia)
  // Al click sobre marcador: highlightTarjeta(id) + scroll en panel

renderizarTarjetas(actividades)
  // Genera HTML de tarjetas en el panel lateral
  // Tarjeta incluye: imagen, título, categoría, distanciaKm, precioConsumidor, calificación
  // Click en tarjeta → window.location.href = '/actividad/{slug}-{id}'

highlightTarjeta(idActividad)
  // Añade clase CSS de resaltado a la tarjeta del id dado
  // Remueve resaltado de las demás

limpiarMarcadores()
  // Remueve todos los L.marker del mapa excepto el userMarker

construirUrlDetalle(slug, id)
  // Retorna '/actividad/' + slug + '-' + id
  // Esta función centraliza la construcción de la URL para un solo lugar de cambio
```

### Flujo de datos completo

```
DOMContentLoaded
  → inicializarMapa()         (Leaflet, tile layer, zoom 14, centro Cartagena)
  
click "Usar mi ubicación"
  → solicitarUbicacion()
      → navigator.geolocation.getCurrentPosition()
          onSuccess:
            → centrarMapa(lat, lon)
            → pintarUserMarker(lat, lon)
            → habilitarSlider()
            → cargarActividades(lat, lon, radioActual)
                → fetch JSON
                    → pintarMarcadores(data)
                    → renderizarTarjetas(data)
          onError:
            → alert.js: "No pudimos obtener tu ubicación. Verifica los permisos del navegador."

change slider
  → cargarActividades(latGuardada, lonGuardada, nuevoRadio)
      → fetch JSON (mismo flujo anterior)

click marcador en mapa
  → highlightTarjeta(idActividad)
  → scrollToTarjeta(idActividad)

click tarjeta en panel
  → window.location.href = construirUrlDetalle(slug, id)
```

---

## 9. Estilos: `mapa-cercanas.css`

Clases específicas necesarias que no cubre Tailwind por defecto:

```css
/* Contenedor del mapa — altura fija para que Leaflet lo tome */
#mapaLeaflet {
    height: 520px;
    border-radius: 12px;
    z-index: 1;
}

/* Panel lateral de tarjetas — scroll interno */
#panelResultados {
    max-height: 520px;
    overflow-y: auto;
    scrollbar-width: thin;
}

/* Tarjeta de actividad */
.tarjeta-cercana { /* ... */ }
.tarjeta-cercana.activa { /* borde destacado al resaltar */ }

/* Icono de marcador numerado (div icon de Leaflet) */
.marker-numero {
    background: #1D9E75;
    color: white;
    border-radius: 50%;
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 500;
    border: 2px solid white;
    box-shadow: 0 1px 4px rgba(0,0,0,0.2);
}

/* Icono del usuario */
.marker-usuario {
    background: #178CF2;
    /* mismo patrón */
}
```

---

## 10. UX — Estados de la vista

| Estado | Qué muestra el mapa | Qué muestra el panel |
|--------|--------------------|-----------------------|
| Inicial (sin ubicación) | Mapa centrado en Cartagena, sin marcadores | Mensaje: "Activa tu ubicación" + botón CTA |
| Cargando ubicación | Spinner sobre el botón | "Detectando tu posición..." |
| Ubicación obtenida, cargando actividades | Marcador de usuario en el mapa, spinner | "Buscando actividades cercanas..." |
| Resultados OK | Marcadores numerados + círculo de radio | Lista de hasta 10 tarjetas |
| Sin resultados en el radio | Solo marcador de usuario + círculo | "No hay actividades en X km. Intenta ampliar el radio." |
| Error de geolocalización | Estado inicial sin cambios | Alerta usando `js/alert.js` con mensaje amigable |
| Error de red (AJAX falla) | Estado anterior sin cambios | Alerta usando `js/alert.js` |

---

## 11. Restricciones y reglas de negocio aplicables

| Regla | Aplicación en este módulo |
|-------|--------------------------|
| Precio siempre con comisión | `ActividadCercanaDTO.precioConsumidor` calculado con `UsuarioHelper` en el servicio |
| No exponer entidades en controller | El endpoint JSON devuelve `List<ActividadCercanaDTO>`, nunca `List<Actividad>` |
| No N+1 | Comentarios cargados con `contarComentariosPorActividades()` batch |
| No lógica en controller | El cálculo Haversine y el filtrado viven en `ActividadService` |
| Actividades con lat/lon null | Excluidas por el bounding box query — nunca llegan al cálculo Haversine |
| Radio fuera de rango | Clampeado en el controller: `Math.max(1, Math.min(5, radio))` |
| Ruta de detalle existente | El link usa la ruta ya existente `/actividad/{slug}-{id}` — no se crea una nueva |

---

## 12. Lo que este módulo NO hace (fuera de alcance)

- No implementa favoritos desde el mapa (eso requiere autenticación, está en `ClienteController`).
- No filtra por categoría ni precio en esta vista (eso ya existe en `/actividades/buscar`).
- No guarda el historial de búsquedas por ubicación.
- No implementa clustering de marcadores (no necesario con límite de 10).
- No usa el campo `totalTendencia` ni `totalVistas` para ordenar — el criterio es exclusivamente distancia.
- No modifica la tabla `actividad` ni incrementa contadores (es una lectura pura).

---

## 13. Dependencias externas

| Librería | Versión | Cómo se carga | Uso |
|----------|---------|---------------|-----|
| Leaflet.js | 1.9.4 | CDN `unpkg.com` desde el template HTML | Renderizado del mapa interactivo |
| OpenStreetMap | — | Tile layer dentro de Leaflet | Tiles del mapa (gratuito, sin API key) |

No se requiere API key de ningún servicio de mapas.

---

## 14. Checklist de implementación

### Backend
- [ ] Crear `ActividadCercanaDTO` con todos los campos del apartado 3
- [ ] Agregar `findByLatitudBetweenAndLongitudBetween()` a `ActividadRepository`
- [ ] Implementar `buscarCercanas()` en `ActividadService` con Haversine y batch de comentarios
- [ ] Agregar `GET /actividades/cercanas` en `AllAcessController` (vista)
- [ ] Agregar `GET /actividades/cercanas/json` en `AllAcessController` (AJAX, `@ResponseBody`)
- [ ] Añadir ambas rutas al bloque `permitAll()` en `SecurityConfig`

### Frontend
- [ ] Crear `actividades-cercanas.html` con estructura del apartado 7
- [ ] Crear `mapa-cercanas.js` con todas las funciones del apartado 8
- [ ] Crear `mapa-cercanas.css` con los estilos del apartado 9
- [ ] Verificar que `navbar` y `footer` se incluyen con `th:replace`
- [ ] Verificar que `js/alert.js` está disponible en la vista para mensajes de error

### QA
- [ ] Probar en Cartagena con actividades que tienen lat/lon cargadas
- [ ] Verificar que actividades con lat/lon null no rompen el endpoint
- [ ] Verificar que el click en tarjeta navega a la URL correcta de detalle
- [ ] Verificar que el click en marcador del mapa resalta la tarjeta correspondiente
- [ ] Verificar que el slider recarga correctamente con 1, 2, 3, 4 y 5 km
- [ ] Probar estado de error de geolocalización (denegar permisos en el navegador)
- [ ] Probar en móvil: layout apilado, mapa usable con touch