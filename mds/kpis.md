# Spec — Dashboard de KPIs del Colaborador
**Plataforma:** ETA — Marketplace de Actividades Turísticas  
**Versión:** 1.0  
**Fecha:** Abril 2026  
**Estado:** Listo para implementar

---

## 1. Descripción General

El Dashboard de KPIs del Colaborador es una vista privada dentro del panel `/colaborador/dashboard` que presenta métricas accionables sobre el rendimiento de sus actividades, reservas, ingresos y visibilidad. Toda la información se calcula en tiempo real desde la base de datos relacional existente de ETA, sin necesidad de nuevas tablas.

El objetivo es que el colaborador pueda responder en segundos: ¿cuánto gané este mes?, ¿qué actividad está fallando?, ¿cuántos cupos se están llenando?

---

## 2. Glosario

| Término | Definición |
|---------|-----------|
| `Ingreso bruto` | `reserva.cantidad × actividad.precio` para reservas en estado `Hecho` |
| `Ingreso neto` | Ingreso bruto menos la comisión: `ingreso_bruto × (1 - admin.porcentajeComision / 100)` |
| `Tasa de ocupación` | `(disponibilidad.cuposTotales - disponibilidad.cuposDisponibles) / disponibilidad.cuposTotales × 100` |
| `Conversión visita → reserva` | `total_reservas_actividad / actividad.totalVistas × 100` |
| `Conversión Pendiente → Confirmada` | `reservas_confirmadas / reservas_pendientes_iniciales × 100` |
| `Clientes únicos` | Conteo de `reserva.id_cliente` distintos en el período |
| `Clientes recurrentes` | Clientes únicos con más de una reserva `Hecho` en cualquier actividad del colaborador |
| `Período activo` | Por defecto: mes calendario en curso. Filtrable por el colaborador |

---

## 3. Arquitectura

### 3.1 Ubicación en el sistema
- **Ruta:** `GET /colaborador/dashboard`
- **Rol requerido:** `ROLE_COLABORADOR`
- **Controlador existente:** `ColaboradorController`
- **Renderizado:** Thymeleaf (SSR), sin cambios de stack

### 3.2 Fuentes de datos (tablas existentes)

| KPI | Tablas involucradas |
|-----|-------------------|
| Ingresos | `reserva`, `actividad`, `admin` |
| Reservas por estado | `reserva` |
| Tasa de ocupación | `disponibilidad` |
| Calificación | `comentario`, `actividad` |
| Favoritos | `favorito` |
| Vistas y conversión | `actividad.totalVistas`, `reserva` |
| Clientes únicos/recurrentes | `reserva`, `cliente` |

### 3.3 Nuevos servicios requeridos

Se debe crear `KpiColaboradorService` con los siguientes métodos:

```java
public KpiResumenDTO obtenerResumen(Long idColaborador, YearMonth periodo);
public List<KpiActividadDTO> obtenerKpiPorActividad(Long idColaborador, YearMonth periodo);
public List<PuntoTendenciaDTO> obtenerTendenciaReservas(Long idColaborador, int semanas);
public EstadosReservaDTO obtenerEstadosReserva(Long idColaborador, YearMonth periodo);
public OcupacionDTO obtenerOcupacion(Long idColaborador, YearMonth periodo);
```

### 3.4 DTOs necesarios

```java
// Resumen general
KpiResumenDTO {
    BigDecimal ingresoBruto;
    BigDecimal ingresoNeto;
    int totalReservas;
    double tasaCancelacion;       // porcentaje
    double calificacionPromedio;
    int actividadesPublicadas;
    int clientesUnicos;
    int clientesRecurrentes;
}

// Por actividad
KpiActividadDTO {
    Long idActividad;
    String titulo;
    int reservas;
    double calificacion;
    double tasaOcupacion;         // porcentaje
    int veces_en_favoritos;
    int totalVistas;
    double conversionVisitaReserva; // porcentaje
}

// Tendencia semanal
PuntoTendenciaDTO {
    String etiqueta;              // "Sem 1", "Sem 2"...
    int reservas;
}

// Estados de reserva
EstadosReservaDTO {
    int pendiente;
    int confirmada;
    int hecho;
    int cancelada;
    double tasaConversionPendienteConfirmada;
}

// Ocupación global
OcupacionDTO {
    int disponible;               // cantidad de slots
    int completado;
    int cancelado;
}
```

---

## 4. KPIs — Definición y Queries

### 4.1 Resumen General (6 tarjetas métricas)

#### KPI-01 — Ingresos del mes
- **Qué muestra:** Total de ingresos brutos y netos del período
- **Fórmula:** `SUM(r.cantidad * a.precio)` para reservas con `estado = 'Hecho'`
- **Query base:**
```sql
SELECT SUM(r.cantidad * a.precio) AS ingreso_bruto
FROM reserva r
JOIN actividad a ON r.id_actividad = a.idActividad
JOIN colaborador c ON a.id_colaborador = c.idColaborador
WHERE c.id_usuario = :idUsuario
  AND r.estado = 'Hecho'
  AND YEAR(r.fechaReserva) = :anio
  AND MONTH(r.fechaReserva) = :mes;
```
- **Ingreso neto:** `ingreso_bruto * (1 - porcentajeComision/100)` — leer `porcentajeComision` de la tabla `admin`
- **Variación:** comparar con mismo período del mes anterior (badge verde/rojo)

#### KPI-02 — Reservas totales del mes
- **Fórmula:** `COUNT(*)` de reservas en el período (todos los estados excepto `Cancelada`)
- **Badge:** variación de reservas vs semana anterior

#### KPI-03 — Tasa de cancelación
- **Fórmula:** `COUNT(estado='Cancelada') / COUNT(*) * 100`
- **Umbral:** badge naranja si > 10%, badge rojo si > 20%

#### KPI-04 — Calificación promedio
- **Fórmula:** `AVG(comentario.calificacion)` de todas las actividades del colaborador
- **Mostrar:** número con 1 decimal + estrellas visuales

#### KPI-05 — Actividades publicadas
- **Fórmula:** `COUNT(actividad)` del colaborador
- **Badge secundario:** cuántas tienen disponibilidad `DISPONIBLE` hoy

#### KPI-06 — Clientes únicos
- **Fórmula:** `COUNT(DISTINCT r.id_cliente)` en el período
- **Sub-dato:** clientes recurrentes (más de 1 reserva `Hecho`)

---

### 4.2 KPIs por Actividad

#### KPI-07 — Reservas por actividad (ranking)
- **Datos:** lista ordenada de actividades del colaborador con su `COUNT(reservas)` en el período
- **Visualización:** barras horizontales, la más reservada al tope

#### KPI-08 — Calificación por actividad
- **Datos:** `AVG(comentario.calificacion)` agrupado por actividad
- **Umbral de alerta:** resaltar actividades con calificación < 3.5 con color ámbar/rojo

#### KPI-09 — Tasa de ocupación por actividad
- **Fórmula:** para cada actividad, calcular promedio de ocupación sobre sus disponibilidades del período
```sql
SELECT a.titulo,
       AVG((d.cuposTotales - d.cuposDisponibles) * 100.0 / d.cuposTotales) AS ocupacion_pct
FROM disponibilidad d
JOIN actividad a ON d.id_actividad = a.idActividad
JOIN colaborador c ON a.id_colaborador = c.idColaborador
WHERE c.id_usuario = :idUsuario
  AND d.fecha BETWEEN :fechaInicio AND :fechaFin
GROUP BY a.idActividad;
```
- **Semáforo:** verde ≥ 70%, ámbar 40-69%, rojo < 40%

---

### 4.3 Disponibilidades y Ocupación

#### KPI-10 — Estado de disponibilidades (donut)
- **Datos:** `COUNT` agrupado por `disponibilidad.estado` (`DISPONIBLE`, `COMPLETADO`, `CANCELADO`) del período
- **Visualización:** gráfica donut con porcentajes

#### KPI-11 — Ingresos brutos vs netos (barras, 6 meses)
- **Datos:** dos series mensuales de los últimos 6 meses
- **Visualización:** barras agrupadas por mes, azul = bruto, verde = neto

---

### 4.4 Estados de Reservas

#### KPI-12 — Desglose de reservas por estado
- **Datos:** `GROUP BY reserva.estado` del período
- **Incluir:** tasa de conversión Pendiente → Confirmada

#### KPI-13 — Tendencia semanal de nuevas reservas
- **Datos:** `COUNT(*)` agrupado por semana del mes actual
- **Visualización:** línea con área rellena

---

### 4.5 Favoritos y Visibilidad

#### KPI-14 — Total de veces en favoritos
- **Datos:** `COUNT(favorito)` de todas las actividades del colaborador (acumulado histórico)

#### KPI-15 — Actividad más guardada en favoritos
- **Datos:** actividad con mayor `COUNT(favorito.id_actividad)`

#### KPI-16 — Total de vistas
- **Datos:** `SUM(actividad.totalVistas)` de las actividades del colaborador en el período

#### KPI-17 — Conversión visita → reserva
- **Fórmula:** `COUNT(reservas_periodo) / SUM(totalVistas_periodo) * 100`
- **Nota:** `totalVistas` es acumulado; si no hay granularidad mensual en vistas, mostrar conversión histórica

---

## 5. Interfaz de Usuario

### 5.1 Layout general
```
/colaborador/dashboard
├── Header con nombre del colaborador y selector de período (mes/año)
├── Sección "Resumen general"       → 6 tarjetas métricas en grid 3×2
├── Sección "Actividades"           → 2 columnas: barras reservas | barras calificación
├── Sección "Disponibilidad"        → 2 columnas: donut estados | barras ocupación con semáforo
├── Sección "Ingresos"              → gráfica de barras agrupadas 6 meses (ancho completo)
├── Sección "Reservas"              → 2 columnas: tabla estados | gráfica línea tendencia
└── Sección "Favoritos y visibilidad" → 4 tarjetas métricas
```

### 5.2 Componentes Thymeleaf

Crear fragmentos reutilizables en `templates/fragments/`:

```
kpi-metric-card.html      → tarjeta métrica con label, valor, badge de variación
kpi-bar-chart.html        → barras horizontales con etiqueta, barra, valor
kpi-section-header.html   → encabezado de sección con label en mayúsculas pequeñas
```

### 5.3 Gráficas (Chart.js)

Usar Chart.js desde CDN (ya disponible en el proyecto). Tres tipos de gráfica:

| Gráfica | Tipo Chart.js | Canvas ID |
|---------|--------------|-----------|
| Estados de disponibilidades | `doughnut` | `chartDonutDisponibilidad` |
| Ingresos brutos vs netos | `bar` (agrupado) | `chartBarIngresos` |
| Tendencia semanal de reservas | `line` (con fill) | `chartLineReservas` |

Pasar datos desde Thymeleaf a JS usando variables inline:
```html
<script th:inline="javascript">
  const datosIngresos = /*[[${kpi.seriesIngresos}]]*/ [];
</script>
```

### 5.4 Estilos (Tailwind CSS)

Paleta de colores para KPIs:

| Concepto | Color Tailwind |
|---------|---------------|
| Ingreso / positivo | `text-green-700 bg-green-100` |
| Alerta / moderado | `text-amber-700 bg-amber-100` |
| Peligro / bajo | `text-red-700 bg-red-100` |
| Informativo | `text-blue-700 bg-blue-100` |
| Calificación (estrellas) | `text-amber-500` |

---

## 6. Reglas de Negocio

| ID | Regla |
|----|-------|
| RN-KPI-01 | Solo se cuentan como ingresos las reservas con estado `Hecho`; `Pendiente` y `Confirmada` no generan ingreso contabilizado |
| RN-KPI-02 | El porcentaje de comisión se lee en tiempo real de `admin.porcentajeComision`; si hay varios registros de admin, usar el más reciente |
| RN-KPI-03 | Un colaborador solo ve KPIs de sus propias actividades — siempre filtrar por `actividad.id_colaborador` |
| RN-KPI-04 | La tasa de ocupación se calcula sobre disponibilidades con `estado != 'CANCELADO'` para no distorsionar el promedio |
| RN-KPI-05 | Las gráficas de tendencia semanal usan semanas del mes en curso (Sem 1 = días 1-7, Sem 2 = 8-14, etc.) |
| RN-KPI-06 | Los KPIs se calculan sobre el período seleccionado por el colaborador; por defecto: mes calendario actual |
| RN-KPI-07 | Si una actividad no tiene comentarios, mostrar calificación como `—` en lugar de `0` |
| RN-KPI-08 | Clientes recurrentes = clientes con ≥ 2 reservas en estado `Hecho` en CUALQUIER actividad del colaborador, no solo en el período |

---

## 7. Criterios de Aceptación

```
DADO que un colaborador autenticado accede a /colaborador/dashboard
CUANDO carga la página sin seleccionar filtro de período
ENTONCES ve los KPIs del mes calendario en curso

DADO que el colaborador tiene reservas en estado 'Hecho'
CUANDO se calculan los ingresos
ENTONCES el ingreso bruto = suma de (cantidad × precio) y el ingreso neto descuenta el porcentajeComision del admin

DADO que una actividad tiene calificación promedio < 3.5
CUANDO se muestra la barra de calificación por actividad
ENTONCES la barra se muestra en color ámbar o rojo (no verde)

DADO que una disponibilidad tiene estado 'CANCELADO'
CUANDO se calcula la tasa de ocupación de esa actividad
ENTONCES esa disponibilidad NO se incluye en el cálculo

DADO que el colaborador selecciona un período diferente (mes anterior)
CUANDO cambia el selector de período
ENTONCES todos los KPIs se actualizan para reflejar ese período (recarga de página o llamada AJAX)

DADO que una actividad no tiene comentarios en el período
CUANDO se muestra la columna de calificación
ENTONCES muestra '—' en lugar de un número

DADO que el colaborador no tiene reservas en el período seleccionado
CUANDO se muestran las métricas
ENTONCES se muestran en cero con un mensaje "Sin datos para este período"
```

---

## 8. Tareas de Implementación

```
[ ] 1. Crear KpiColaboradorService con los 5 métodos descritos en sección 3.3
[ ] 2. Crear los DTOs: KpiResumenDTO, KpiActividadDTO, PuntoTendenciaDTO, EstadosReservaDTO, OcupacionDTO
[ ] 3. Escribir las queries JPQL/SQL nativas para cada KPI (ver sección 4)
[ ] 4. Actualizar ColaboradorController para pasar el objeto KpiColaboradorDTO al modelo de la vista
[ ] 5. Crear fragmentos Thymeleaf: kpi-metric-card, kpi-bar-chart, kpi-section-header
[ ] 6. Construir la vista dashboard.html con el layout de 6 secciones (ver sección 5.1)
[ ] 7. Integrar Chart.js para las 3 gráficas: donut, barras agrupadas, línea
[ ] 8. Pasar datos de Java a Chart.js via th:inline="javascript"
[ ] 9. Añadir selector de período (mes/año) con GET param ?periodo=2026-04
[ ] 10. Aplicar semáforo de colores Tailwind en la tasa de ocupación (verde/ámbar/rojo)
[ ] 11. Validar que ningún KPI exponga datos de actividades de otros colaboradores (prueba de seguridad)
[ ] 12. Manejar el caso sin datos (período vacío) con mensaje apropiado en cada sección
```

---

## 9. Fuera del Alcance (v1.0)

- Exportar KPIs a PDF o Excel
- Notificaciones automáticas cuando un KPI supera un umbral (ej: cancelación > 20%)
- Comparativa entre actividades de distintos colaboradores
- KPIs en tiempo real vía WebSocket (los datos se calculan al cargar la página)
- Predicciones o proyecciones de ingresos futuros
- Filtro por actividad individual dentro del dashboard (se muestran todas las actividades del colaborador)

---

## 10. Dependencias y Notas Técnicas

- Chart.js ya está disponible en el proyecto vía CDN — no requiere instalación adicional
- La tabla `admin` puede tener múltiples registros si se agregaron varios admins; usar `ORDER BY id DESC LIMIT 1` para obtener la comisión vigente
- `actividad.totalVistas` es un contador acumulado — no tiene granularidad mensual. Si se necesita vistas por período, se debe agregar un campo `updatedAt` al contador o una tabla de eventos de vista (fuera del alcance de v1.0)
- Todas las queries deben filtrar por `c.id_usuario = :idUsuario` (no por `idColaborador` directamente) para seguir el patrón de seguridad existente en el proyecto
- Usar `@Query` de Spring Data JPA para queries complejas; evitar lógica de cálculo en los controladores