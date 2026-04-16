# Servicio de Predicción de Ocupación

## 📋 Descripción

Este módulo integra un modelo de Machine Learning (Weka) para predecir el nivel de ocupación de actividades turísticas en el sistema ETA.

## 🎯 Predicciones

El modelo clasifica el nivel de ocupación en 4 categorías:
- **baja**: Poca demanda esperada
- **media**: Demanda moderada
- **alta**: Alta demanda
- **agotado**: Se espera que se agoten los cupos

## 📊 Variables de Entrada

El modelo utiliza 5 características transformadas:

### 1. Categoría
Categorías soportadas: aventura, cultura, gastronomia, deportes, naturaleza, bienestar, entretenimiento

### 2. Rango de Precio
- **bajo**: < $50,000 COP
- **medio**: $50,000 - $150,000 COP  
- **alto**: > $150,000 COP

### 3. Rango de Hora
- **mañana**: 06:00 - 11:59
- **tarde**: 12:00 - 17:59
- **noche**: 18:00 - 23:59

### 4. Tipo de Día
- **entre_semana**: Lunes a Viernes
- **fin_semana**: Sábado y Domingo

### 5. Rango de Cupos
- **bajo**: 1 - 10 personas
- **medio**: 11 - 30 personas
- **alto**: > 30 personas

## 🔌 Endpoints REST

### Predicción por Disponibilidad
```http
GET /api/prediccion/disponibilidad/{id}
```

Obtiene una predicción basada en una disponibilidad existente. El sistema automáticamente:
- Extrae los datos de la actividad y disponibilidad
- Realiza las transformaciones necesarias
- Ejecuta la predicción

**Ejemplo de respuesta:**
```json
{
  "nivelOcupacion": "alta",
  "confianza": 0.85,
  "categoria": "aventura",
  "rangoPrecio": "medio",
  "rangoHora": "tarde",
  "tipoDia": "fin_semana",
  "rangoCupos": "medio"
}
```

### Predicción Manual
```http
GET /api/prediccion/manual?categoria=aventura&rangoPrecio=medio&rangoHora=tarde&tipoDia=fin_semana&rangoCupos=alto
```

Permite realizar predicciones con parámetros personalizados sin necesidad de una disponibilidad existente.

**Parámetros:**
- `categoria`: aventura, cultura, gastronomia, etc.
- `rangoPrecio`: bajo, medio, alto
- `rangoHora`: mañana, tarde, noche
- `tipoDia`: entre_semana, fin_semana
- `rangoCupos`: bajo, medio, alto

### Health Check
```http
GET /api/prediccion/health
```

Verifica que el servicio de predicción esté operativo.

## 🏗️ Arquitectura

### Componentes

```
dto/
  └── PrediccionOcupacionDTO.java    # DTO para respuestas

service/
  ├── PrediccionService.java         # Interface del servicio
  └── PrediccionServiceImpl.java     # Implementación con Weka

controller/
  └── PrediccionController.java      # REST Controller

resources/
  └── modeloPredictivo.model         # Modelo entrenado de Weka
```

### Flujo de Predicción

1. **Request** → Controller recibe petición HTTP
2. **Service** → Obtiene datos de BD (si aplica) y transforma features
3. **Weka** → Carga instancia y ejecuta clasificación
4. **Response** → Devuelve DTO con nivel predicho y confianza

## 🔧 Uso Programático

### Desde un Servicio
```java
@Autowired
private PrediccionService prediccionService;

public void ejemplo() {
    // Predicción por disponibilidad
    PrediccionOcupacionDTO prediccion = prediccionService.predecirOcupacion(123L);
    System.out.println("Nivel predicho: " + prediccion.getNivelOcupacion());
    System.out.println("Confianza: " + (prediccion.getConfianza() * 100) + "%");
    
    // Predicción manual
    PrediccionOcupacionDTO manual = prediccionService.predecirOcupacionManual(
        "aventura", "medio", "tarde", "fin_semana", "alto"
    );
}
```

### Desde el Frontend (JavaScript)
```javascript
// Predicción por disponibilidad
fetch('/api/prediccion/disponibilidad/123')
  .then(res => res.json())
  .then(data => {
    console.log('Nivel de ocupación:', data.nivelOcupacion);
    console.log('Confianza:', (data.confianza * 100).toFixed(1) + '%');
  });

// Predicción manual
const params = new URLSearchParams({
  categoria: 'aventura',
  rangoPrecio: 'medio',
  rangoHora: 'tarde',
  tipoDia: 'fin_semana',
  rangoCupos: 'alto'
});

fetch(`/api/prediccion/manual?${params}`)
  .then(res => res.json())
  .then(data => {
    // Mostrar predicción en UI
    mostrarPrediccion(data);
  });
```

## 🎓 Modelo de Machine Learning

### Entrenamiento
El modelo fue entrenado con **Weka 3.8.6** usando datos históricos de reservas.

### Algoritmo
Se utilizó un clasificador supervisado optimizado para la clasificación multiclase.

### Rendimiento
- El modelo incluye distribución de probabilidades para cada clase
- La confianza indica qué tan segura es la predicción (0.0 a 1.0)

## 📝 Notas Técnicas

### Carga del Modelo
El modelo se carga **una sola vez** al inicializar el servicio usando `@PostConstruct`, evitando lecturas repetidas del archivo.

### Transformaciones
Todas las transformaciones (precio, hora, día, cupos) se realizan automáticamente en el servicio siguiendo las especificaciones del modelo.

### Categorías Flexibles
El servicio incluye normalización inteligente de categorías, permitiendo variaciones en los nombres (ej: "gastronomía" → "gastronomia").

### Manejo de Errores
- Si no se encuentra una disponibilidad: **404 Not Found**
- Si falla la predicción: **500 Internal Server Error**
- Los errores se registran en consola para debugging

## 🚀 Casos de Uso

1. **Dashboard de Colaborador**: Mostrar predicción de demanda al crear disponibilidades
2. **Calendario de Actividades**: Indicar visualmente qué fechas tendrán más demanda
3. **Recomendaciones a Clientes**: Sugerir fechas con menor ocupación
4. **Analytics**: Generar reportes de tendencias de demanda

## 🔮 Posibles Mejoras Futuras

- Integrar datos climáticos
- Considerar eventos especiales en Cartagena
- Re-entrenar modelo con nuevos datos de reservas
- A/B testing de predicciones vs. ocupación real
- Cache de predicciones frecuentes
