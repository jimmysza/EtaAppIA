# 🎯 Modo Sin Pago - ETA App

## ✅ Sistema Configurado para Funcionar SIN Pasarela de Pago

Por defecto, la aplicación está configurada para crear reservas **directamente sin redirigir a Wompi**.

---

## 🔧 Configuración Actual

### application.properties
```properties
# ⚠️ MODO PAGO: false = Sin pago (reserva directa)
pago.enabled=false
```

---

## 🎮 Cómo Funciona AHORA (Sin Pago)

```
Cliente selecciona actividad + fecha + cantidad
    ↓
Submit form GET → /cliente/pago/iniciar
    ↓
PagoController detecta: pago.enabled=false
    ↓
✅ Crea reserva directamente con ReservaService
    ↓
Redirect a /cliente/dashboard con mensaje: "¡Reserva creada exitosamente! (Modo sin pago)"
```

**NO HAY** redirección a Wompi  
**NO SE REQUIERE** pago real  
**LA RESERVA SE CREA** inmediatamente

---

## 🚀 Cómo Activar el Pago en el Futuro

### Opción 1: Cambiar en application.properties
```properties
pago.enabled=true
```

### Opción 2: Variable de entorno (recomendado)
```bash
# Windows (CMD)
set PAGO_ENABLED=true

# Windows (PowerShell)
$env:PAGO_ENABLED="true"

# Linux/Mac
export PAGO_ENABLED=true
```

### Opción 3: Al iniciar la aplicación
```bash
mvnw spring-boot:run -Dpago.enabled=true
```

---

## ⚙️ Requisitos SOLO para Modo con Pago

Cuando cambies a `pago.enabled=true`, necesitarás:

1. **Credenciales reales de Wompi**:
   - Registrarte en https://comercios.wompi.co/
   - Obtener keys de TEST
   - Configurar en application.properties:
     ```properties
     wompi.public.key=pub_test_TU_KEY_REAL
     wompi.private.key=prv_test_TU_KEY_REAL
     wompi.events.key=test_events_TU_KEY_REAL
     wompi.integrity.key=test_integrity_TU_KEY_REAL
     ```

2. **Configurar dominio en Wompi**:
   - Panel Wompi → Configuración → Dominios permitidos
   - Agregar: `http://localhost:8080` (desarrollo)
   - Agregar: `https://tudominio.com` (producción)

3. **Webhook (producción)**:
   - Desarrollo: usar ngrok
   - Producción: configurar URL pública en panel Wompi

---

## 📊 Comparación de Modos

| Aspecto | `pago.enabled=false` | `pago.enabled=true` |
|---------|---------------------|-------------------|
| **Flujo** | Reserva directa | Redirect a Wompi |
| **Pago real** | ❌ No | ✅ Sí |
| **Requiere credenciales** | ❌ No | ✅ Sí |
| **Requiere configuración Wompi** | ❌ No | ✅ Sí |
| **Funciona en localhost** | ✅ Sí | ⚠️ Requiere config |
| **Estado Reserva** | `Confirmada` | `Confirmada` (tras pago) |
| **Ideal para** | Desarrollo, demos | Producción |

---

## 🧪 Probar el Sistema (Modo Sin Pago)

1. **Iniciar aplicación**:
   ```bash
   mvnw spring-boot:run
   ```

2. **Navegar a una actividad**:
   - http://localhost:8080/
   - Seleccionar actividad
   - Elegir fecha y cantidad

3. **Hacer reserva**:
   - Click "Pagar con Wompi" (el botón no cambia de texto)
   - Verás redirect instantáneo a `/cliente/dashboard`
   - Mensaje: "¡Reserva creada exitosamente! (Modo sin pago)"

4. **Verificar en dashboard**:
   - Ver reserva en "Mis Reservas"
   - Estado: `Confirmada`
   - **NO HAY** referencia de pago de Wompi (refWompi será null)

---

## 🔍 Diferencias en la Base de Datos

### Modo Sin Pago (`pago.enabled=false`)
```sql
SELECT * FROM reservas WHERE id_cliente = X;

-- Campos relacionados con Wompi estarán NULL:
ref_wompi: NULL
wompi_transaction_id: NULL
```

### Modo Con Pago (`pago.enabled=true`)
```sql
SELECT * FROM reservas WHERE id_cliente = X;

-- Campos de Wompi tendrán valores:
ref_wompi: "ETA-1-5-1777512797969"
wompi_transaction_id: "wompi_tx_abc123..."
```

---

## ⚡ Cambio Rápido entre Modos

### Durante Desarrollo (sin reiniciar app)
No es posible cambiar en caliente. Debes:
1. Cambiar `pago.enabled` en application.properties
2. Reiniciar aplicación

### Con Spring Boot DevTools (auto-reload)
Si tienes DevTools activo, solo:
1. Cambiar `pago.enabled` en application.properties
2. Guardar archivo
3. Esperar auto-reload (5-10 segundos)

---

## 📝 Código Relevante

### PagoController.java (línea ~90)
```java
// ⚠️ MODO SIN PAGO: Crear reserva directamente (pago.enabled=false)
if (!pagoEnabled) {
    logger.info("Modo sin pago activado - Creando reserva directamente");
    reservaService.crearReserva(idDisponibilidad, cliente.getId(), idActividad, cantidad);
    redirectAttributes.addFlashAttribute("success", "¡Reserva creada exitosamente! (Modo sin pago)");
    return "redirect:/cliente/dashboard";
}

// ✅ MODO CON PAGO: Redirigir a Wompi (pago.enabled=true)
logger.info("Modo con pago activado - Generando URL de Wompi");
String urlWompi = wompiService.generarUrlPago(...);
return "redirect:" + urlWompi;
```

---

## 🎓 Recomendaciones

### Para Desarrollo/Demo/Presentaciones
```properties
pago.enabled=false  ✅ USAR ESTE
```
- Funciona instantáneamente
- No requiere configuración externa
- Ideal para mostrar funcionalidad sin complicaciones

### Para Producción Real
```properties
pago.enabled=true
```
- Requiere cuenta Wompi verificada
- Configurar credenciales reales
- Configurar webhook en producción
- Probar flujo completo de pago

---

## 🚨 Importante

- **NO** cambies ningún otro código para activar/desactivar el pago
- **SOLO** modifica la variable `pago.enabled`
- Todo el código de Wompi sigue intacto (solo se omite condicionalmente)
- Puedes volver a activar el pago en cualquier momento

---

## ✅ Estado Actual

```properties
pago.enabled=false  ← Modo activo AHORA
```

**Resultado**: Sistema funcional, reservas se crean sin pasarela de pago.

Para probar el pago real en el futuro, solo cambia a `pago.enabled=true` y configura credenciales de Wompi.
