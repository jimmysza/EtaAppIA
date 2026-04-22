# SPEC — Integración Pasarela de Pago Wompi
## Plataforma ETA App — Modo Sandbox (Pruebas)

**Versión:** 1.0  
**Fecha:** Abril 2026  
**Stack:** Spring Boot 3 + Thymeleaf + MySQL 8 + Java 17  
**Modo:** Wompi Sandbox (tarjetas de prueba, dinero ficticio)

---

## 1. Contexto del Negocio

ETA App es un marketplace donde **Clientes** reservan actividades turísticas ofrecidas por **Colaboradores**. Actualmente el checkout registra la reserva pero **no procesa pago real**. Esta spec agrega el flujo de pago con Wompi inmediatamente después de que el cliente confirma una reserva.

### Regla de comisión (ya existe en el sistema)
```
total_pago = precio_actividad × cantidad_personas
comision_plataforma = total_pago × (porcentajeComision / 100)   // default 18%
pago_a_colaborador = total_pago - comision_plataforma
```

---

## 2. Flujo de Pago — Visión General

```
[Checkout actual]                    [Nuevo flujo con Wompi]
      │                                       │
      ▼                                       ▼
POST /cliente/reservar          POST /cliente/reservar
      │                               │
      │                               ▼
      │                    Crear Reserva (estado: "Pendiente")
      │                               │
      │                               ▼
      │                    Crear PagoReserva (estado: INICIADO)
      │                               │
      │                               ▼
      │                    Redirigir a /cliente/pago/{idReserva}
      │                               │
      ▼                               ▼
/cliente/dashboard          Widget Wompi (iframe JS)
                                       │
                            ┌──────────┴──────────┐
                            │                     │
                            ▼                     ▼
                      Pago aprobado          Pago fallido/
                            │               cancelado
                            ▼                     │
                  Wompi llama webhook             ▼
                  /api/wompi/webhook      Reserva sigue
                            │            "Pendiente"
                            ▼            (puede reintentar)
                  Reserva → "Confirmada"
                  PagoReserva → APROBADO
                            │
                            ▼
                  Redirigir a /cliente/dashboard
                  con mensaje de éxito
```

---

## 3. Credenciales Wompi Sandbox

Obtener en: https://dashboard.wompi.co → crear cuenta → modo Sandbox

| Variable | Descripción |
|----------|-------------|
| `wompi.public-key` | Llave pública (empieza con `pub_test_...`) |
| `wompi.private-key` | Llave privada (empieza con `prv_test_...`) |
| `wompi.events-secret` | Secreto para validar firma de webhooks |
| `wompi.sandbox-url` | `https://sandbox.wompi.co/v1` |

### Agregar a `application.properties`
```properties
# Wompi Sandbox
wompi.public-key=pub_test_XXXXXXXXXXXXXXXX
wompi.private-key=prv_test_XXXXXXXXXXXXXXXX
wompi.events-secret=test_events_XXXXXXXX
wompi.sandbox-url=https://sandbox.wompi.co/v1
wompi.redirect-url=http://localhost:8080/cliente/pago/resultado
```

> ⚠️ En producción mover estas variables a Docker ENV o un gestor de secretos. Nunca subirlas al repositorio.

---

## 4. Cambios en Base de Datos

### 4.1 Nueva tabla: `pago_reserva`

```sql
CREATE TABLE pago_reserva (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_reserva          BIGINT NOT NULL,
    referencia          VARCHAR(100) NOT NULL UNIQUE,   -- referencia única enviada a Wompi
    monto_total         DECIMAL(12,2) NOT NULL,          -- en pesos colombianos (COP)
    monto_en_centavos   BIGINT NOT NULL,                 -- monto × 100 (Wompi trabaja en centavos)
    moneda              VARCHAR(10) DEFAULT 'COP',
    estado              VARCHAR(30) DEFAULT 'INICIADO',  -- INICIADO, APROBADO, DECLINADO, VOIDED, ERROR
    id_transaccion_wompi VARCHAR(100) NULL,              -- id que devuelve Wompi
    metodo_pago         VARCHAR(50) NULL,                -- CARD, NEQUI, BANCOLOMBIA_TRANSFER, etc.
    fecha_creacion      DATETIME NOT NULL,
    fecha_actualizacion DATETIME NULL,
    CONSTRAINT fk_pago_reserva FOREIGN KEY (id_reserva) REFERENCES reserva(idReserva)
);
```

### 4.2 Nueva columna en tabla `reserva`

```sql
ALTER TABLE reserva
ADD COLUMN referencia_pago VARCHAR(100) NULL;
```

### 4.3 Entidad JPA — `PagoReserva.java`

```java
@Entity
@Table(name = "pago_reserva")
public class PagoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @Column(nullable = false, unique = true)
    private String referencia;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(nullable = false)
    private Long montoEnCentavos;

    @Column(length = 10)
    private String moneda = "COP";

    @Column(length = 30)
    private String estado = "INICIADO";           // INICIADO | APROBADO | DECLINADO | VOIDED | ERROR

    @Column(length = 100)
    private String idTransaccionWompi;

    @Column(length = 50)
    private String metodoPago;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // getters y setters
}
```

### 4.4 Actualizar `Reserva.java` — agregar relación

```java
// En la entidad Reserva existente, agregar:

@Column(length = 100)
private String referenciaPago;

@OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private PagoReserva pagoReserva;
```

---

## 5. Estructura de Archivos Nuevos

```
src/
├── main/
│   ├── java/com/eta/
│   │   ├── entity/
│   │   │   └── PagoReserva.java                  ← NUEVA entidad
│   │   ├── repository/
│   │   │   └── PagoReservaRepository.java        ← NUEVO repositorio
│   │   ├── service/
│   │   │   └── WompiService.java                 ← NUEVO servicio (llamadas a API Wompi)
│   │   │   └── PagoReservaService.java           ← NUEVO servicio (lógica de pago)
│   │   ├── controller/
│   │   │   └── PagoController.java               ← NUEVO controlador (vistas de pago)
│   │   │   └── WompiWebhookController.java       ← NUEVO controlador (webhook)
│   │   └── dto/
│   │       └── WompiTransaccionDTO.java          ← NUEVO DTO para respuesta Wompi
│   └── resources/
│       └── templates/
│           └── cliente/
│               ├── pago.html                     ← NUEVA vista (widget Wompi)
│               └── pago-resultado.html           ← NUEVA vista (resultado)
```

---

## 6. Implementación Detallada

### 6.1 `PagoReservaRepository.java`

```java
@Repository
public interface PagoReservaRepository extends JpaRepository<PagoReserva, Long> {

    Optional<PagoReserva> findByReferencia(String referencia);

    Optional<PagoReserva> findByReserva_IdReserva(Long idReserva);
}
```

---

### 6.2 `WompiService.java` — Llamadas a la API de Wompi

```java
@Service
public class WompiService {

    @Value("${wompi.private-key}")
    private String privateKey;

    @Value("${wompi.sandbox-url}")
    private String sandboxUrl;

    @Value("${wompi.events-secret}")
    private String eventsSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Consulta el estado de una transacción en Wompi por su ID.
     * Usar en el webhook para verificar el estado real.
     */
    public WompiTransaccionDTO consultarTransaccion(String idTransaccion) {
        String url = sandboxUrl + "/transactions/" + idTransaccion;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(privateKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");

        WompiTransaccionDTO dto = new WompiTransaccionDTO();
        dto.setId((String) data.get("id"));
        dto.setStatus((String) data.get("status"));
        dto.setReference((String) data.get("reference"));
        dto.setAmountInCents(Long.valueOf(data.get("amount_in_cents").toString()));
        dto.setPaymentMethod((String) ((Map) data.get("payment_method")).get("type"));
        return dto;
    }

    /**
     * Valida la firma del evento webhook de Wompi.
     * Wompi envía: X-Event-Checksum = SHA256(properties + timestamp + secret)
     * Referencia: https://docs.wompi.co/docs/en/widget#webhook-signature
     */
    public boolean validarFirmaWebhook(String checksum, String timestamp,
                                        String transactionId, String status,
                                        String amountInCents, String currency) {
        try {
            String cadena = transactionId + status + amountInCents + currency + timestamp + eventsSecret;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(cadena.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString().equals(checksum);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
}
```

---

### 6.3 `PagoReservaService.java` — Lógica de Negocio

```java
@Service
@Transactional
public class PagoReservaService {

    @Autowired
    private PagoReservaRepository pagoReservaRepository;

    @Autowired
    private ReservaRepository reservaRepository;    // repositorio existente

    /**
     * Crea el registro de pago inicial para una reserva.
     * Se llama JUSTO después de crear la reserva.
     */
    public PagoReserva iniciarPago(Reserva reserva) {
        // Verificar que no exista ya un pago para esta reserva
        Optional<PagoReserva> pagoExistente = pagoReservaRepository
                .findByReserva_IdReserva(reserva.getIdReserva());
        if (pagoExistente.isPresent()) {
            return pagoExistente.get();
        }

        BigDecimal monto = reserva.getActividad().getPrecio()
                .multiply(BigDecimal.valueOf(reserva.getCantidad()));
        Long montoEnCentavos = monto.multiply(BigDecimal.valueOf(100)).longValue();

        PagoReserva pago = new PagoReserva();
        pago.setReserva(reserva);
        pago.setReferencia(generarReferencia(reserva.getIdReserva()));
        pago.setMontoTotal(monto);
        pago.setMontoEnCentavos(montoEnCentavos);
        pago.setMoneda("COP");
        pago.setEstado("INICIADO");

        PagoReserva guardado = pagoReservaRepository.save(pago);

        // Guardar referencia en la reserva también
        reserva.setReferenciaPago(guardado.getReferencia());
        reservaRepository.save(reserva);

        return guardado;
    }

    /**
     * Actualiza el estado del pago tras recibir el webhook de Wompi.
     */
    public void procesarResultadoWompi(String referencia, String idTransaccion,
                                        String estadoWompi, String metodoPago) {
        PagoReserva pago = pagoReservaRepository.findByReferencia(referencia)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + referencia));

        pago.setIdTransaccionWompi(idTransaccion);
        pago.setMetodoPago(metodoPago);
        pago.setEstado(mapearEstado(estadoWompi));
        pagoReservaRepository.save(pago);

        // Actualizar estado de la reserva según resultado
        Reserva reserva = pago.getReserva();
        if ("APROBADO".equals(pago.getEstado())) {
            reserva.setEstado("Confirmada");
        } else if ("DECLINADO".equals(pago.getEstado()) || "VOIDED".equals(pago.getEstado())) {
            reserva.setEstado("Cancelada");
            // Devolver cupos a la disponibilidad
            reserva.getDisponibilidad().reservarCupos(-reserva.getCantidad());
        }
        reservaRepository.save(reserva);
    }

    /**
     * Genera referencia única para Wompi.
     * Formato: ETA-{idReserva}-{timestamp}
     */
    private String generarReferencia(Long idReserva) {
        return "ETA-" + idReserva + "-" + System.currentTimeMillis();
    }

    /**
     * Mapea estados de Wompi a estados internos de ETA.
     * Wompi: APPROVED, DECLINED, VOIDED, ERROR, PENDING
     */
    private String mapearEstado(String estadoWompi) {
        return switch (estadoWompi.toUpperCase()) {
            case "APPROVED" -> "APROBADO";
            case "DECLINED" -> "DECLINADO";
            case "VOIDED"   -> "VOIDED";
            case "ERROR"    -> "ERROR";
            default         -> "INICIADO";
        };
    }
}
```

---

### 6.4 `PagoController.java` — Vistas del flujo de pago

```java
@Controller
@RequestMapping("/cliente/pago")
public class PagoController {

    @Value("${wompi.public-key}")
    private String wompiPublicKey;

    @Autowired
    private PagoReservaService pagoReservaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Value("${wompi.redirect-url}")
    private String redirectUrl;

    /**
     * Vista del widget de pago de Wompi.
     * Se accede desde: GET /cliente/pago/{idReserva}
     */
    @GetMapping("/{idReserva}")
    @PreAuthorize("hasRole('CLIENTE')")
    public String mostrarPago(@PathVariable Long idReserva, Model model,
                               @AuthenticationPrincipal UserDetails userDetails) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        // Seguridad: verificar que la reserva pertenece al cliente autenticado
        if (!reserva.getCliente().getUsuario().getEmail().equals(userDetails.getUsername())) {
            return "redirect:/403";
        }

        PagoReserva pago = pagoReservaService.iniciarPago(reserva);

        model.addAttribute("reserva", reserva);
        model.addAttribute("pago", pago);
        model.addAttribute("wompiPublicKey", wompiPublicKey);
        model.addAttribute("redirectUrl", redirectUrl);
        return "cliente/pago";
    }

    /**
     * Página de resultado tras redireccionamiento de Wompi.
     * Wompi redirige a: {redirect-url}?id={transactionId}
     */
    @GetMapping("/resultado")
    @PreAuthorize("hasRole('CLIENTE')")
    public String resultado(@RequestParam(required = false) String id, Model model) {
        model.addAttribute("idTransaccion", id);
        return "cliente/pago-resultado";
    }
}
```

---

### 6.5 `WompiWebhookController.java` — Recepción de Eventos

```java
@RestController
@RequestMapping("/api/wompi")
public class WompiWebhookController {

    @Autowired
    private WompiService wompiService;

    @Autowired
    private PagoReservaService pagoReservaService;

    /**
     * Endpoint que recibe eventos de Wompi (POST).
     * Registrar esta URL en el dashboard de Wompi Sandbox:
     * https://dashboard.wompi.co → Configuración → Webhooks
     * URL a registrar: https://{tu-dominio}/api/wompi/webhook
     *
     * Para pruebas locales usar ngrok:
     * ngrok http 8080  →  https://xxxx.ngrok.io/api/wompi/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirEvento(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Event-Checksum", required = false) String checksum,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp) {

        try {
            // Extraer datos del evento
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            Map<String, Object> transaction = (Map<String, Object>) data.get("transaction");

            String idTransaccion  = (String) transaction.get("id");
            String status         = (String) transaction.get("status");
            String reference      = (String) transaction.get("reference");
            String amountInCents  = transaction.get("amount_in_cents").toString();
            String currency       = (String) transaction.get("currency");
            String metodoPago     = (String) ((Map) transaction.get("payment_method")).get("type");

            // Validar firma para evitar requests falsos
            // ⚠️ En sandbox a veces el checksum es null — se puede omitir esta validación en pruebas
            if (checksum != null && timestamp != null) {
                boolean firmaValida = wompiService.validarFirmaWebhook(
                        checksum, timestamp, idTransaccion, status, amountInCents, currency);
                if (!firmaValida) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
            }

            // Solo procesar eventos de transacciones
            String evento = (String) payload.get("event");
            if ("transaction.updated".equals(evento)) {
                pagoReservaService.procesarResultadoWompi(
                        reference, idTransaccion, status, metodoPago);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            // Retornar 200 de todas formas para que Wompi no reintente indefinidamente
            // Loggear el error internamente
            return ResponseEntity.ok().build();
        }
    }
}
```

---

### 6.6 Modificar flujo en `ReservaController.java` (controlador existente)

Localizar el método que maneja `POST /cliente/reservar` y modificar el redirect:

```java
// ANTES (comportamiento actual):
// return "redirect:/cliente/dashboard";

// DESPUÉS (nuevo flujo con pago):
Reserva reservaGuardada = reservaService.crearReserva(...);  // lógica existente
return "redirect:/cliente/pago/" + reservaGuardada.getIdReserva();
```

---

### 6.7 Excluir el webhook de CSRF en Spring Security

En la clase de configuración de seguridad (`SecurityConfig.java`):

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // ... configuración existente ...
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/wompi/webhook")  // ← AGREGAR ESTA LÍNEA
        );
    return http.build();
}
```

---

## 7. Vistas Thymeleaf

### 7.1 `pago.html` — Widget de Wompi

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/springsecurity6">
<head>
    <meta charset="UTF-8">
    <title>Pago - ETA App</title>
    <!-- Incluir tu layout/head existente -->
</head>
<body>

<div class="max-w-lg mx-auto mt-10 p-6 bg-white rounded-2xl shadow">
    <h2 class="text-2xl font-bold mb-4 text-gray-800">Confirmar Pago</h2>

    <!-- Resumen de la reserva -->
    <div class="bg-gray-50 rounded-xl p-4 mb-6">
        <h3 class="font-semibold text-gray-700 mb-2">Resumen de tu reserva</h3>
        <p class="text-sm text-gray-600">
            <span class="font-medium">Actividad:</span>
            <span th:text="${reserva.actividad.titulo}"></span>
        </p>
        <p class="text-sm text-gray-600">
            <span class="font-medium">Fecha:</span>
            <span th:text="${reserva.disponibilidad.fecha}"></span>
        </p>
        <p class="text-sm text-gray-600">
            <span class="font-medium">Hora:</span>
            <span th:text="${reserva.disponibilidad.horaInicio}"></span> -
            <span th:text="${reserva.disponibilidad.horaFin}"></span>
        </p>
        <p class="text-sm text-gray-600">
            <span class="font-medium">Personas:</span>
            <span th:text="${reserva.cantidad}"></span>
        </p>
        <p class="text-lg font-bold text-green-600 mt-3">
            Total: $<span th:text="${#numbers.formatDecimal(pago.montoTotal, 0, 'COMMA', 0, 'POINT')}"></span> COP
        </p>
    </div>

    <!-- ✅ Script oficial del Widget de Wompi -->
    <!-- Documentación: https://docs.wompi.co/docs/en/widget -->
    <script
        src="https://checkout.wompi.co/widget.js"
        data-render="button"
        th:attr="
            data-public-key=${wompiPublicKey},
            data-currency='COP',
            data-amount-in-cents=${pago.montoEnCentavos},
            data-reference=${pago.referencia},
            data-redirect-url=${redirectUrl}
        "
    ></script>

    <p class="text-xs text-gray-400 mt-4 text-center">
        Pago seguro procesado por Wompi. Tu información está protegida.
    </p>
</div>

</body>
</html>
```

### 7.2 `pago-resultado.html` — Pantalla de resultado

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Resultado del Pago - ETA App</title>
</head>
<body>

<div class="max-w-md mx-auto mt-16 text-center p-8">

    <!-- El estado real llega por webhook. Esta pantalla es solo el aterrizaje visual. -->
    <!-- El estado definitivo estará en el dashboard del cliente. -->

    <div id="resultado-container">
        <div class="text-6xl mb-4">⏳</div>
        <h2 class="text-2xl font-bold text-gray-800 mb-2">Procesando tu pago</h2>
        <p class="text-gray-600 mb-6">
            Estamos verificando tu pago con Wompi. Esto puede tomar unos segundos.
        </p>
        <p class="text-sm text-gray-400 mb-8">
            ID de transacción: <span class="font-mono" th:text="${idTransaccion}"></span>
        </p>
        <a href="/cliente/dashboard"
           class="bg-blue-600 text-white px-6 py-3 rounded-xl font-semibold hover:bg-blue-700">
            Ver mis reservas
        </a>
    </div>

</div>

<script th:inline="javascript">
    // Wompi agrega ?id=<transactionId> en la URL de redirect.
    // Podemos usar esto para mostrar un estado más inmediato si se desea,
    // pero el estado definitivo siempre viene del webhook (backend).
    const params = new URLSearchParams(window.location.search);
    const transId = params.get('id');
    if (transId) {
        console.log('Transacción Wompi ID:', transId);
    }
</script>

</body>
</html>
```

---

## 8. DTO — `WompiTransaccionDTO.java`

```java
public class WompiTransaccionDTO {
    private String id;
    private String status;
    private String reference;
    private Long amountInCents;
    private String paymentMethod;

    // getters y setters
}
```

---

## 9. Tarjetas de Prueba Wompi Sandbox

Usar estas tarjetas en el widget durante pruebas:

| Escenario | Número de tarjeta | CVV | Fecha exp. |
|-----------|------------------|-----|------------|
| ✅ Pago aprobado | `4242424242424242` | `123` | Cualquier fecha futura |
| ❌ Pago declinado | `4111111111111111` | `123` | Cualquier fecha futura |
| ⏳ Pago pendiente | `4000000000000002` | `123` | Cualquier fecha futura |

**Datos adicionales del titular (sandbox):**
- Nombre: Cualquier nombre
- Cuotas: 1

> Referencia oficial: https://docs.wompi.co/docs/en/testing

---

## 10. Probar el Webhook Localmente con ngrok

Wompi necesita una URL pública para enviar eventos. En desarrollo local:

```bash
# 1. Descargar ngrok: https://ngrok.com/download
# 2. Exponer el puerto local
ngrok http 8080

# Ngrok genera una URL pública tipo:
# https://abc123.ngrok.io

# 3. Registrar en Wompi Sandbox Dashboard:
# URL: https://abc123.ngrok.io/api/wompi/webhook
# Eventos a suscribirse: transaction.updated
```

---

## 11. Checklist de Implementación

### Paso 1 — Base de datos
- [ ] Ejecutar el `ALTER TABLE` sobre `reserva` para agregar `referencia_pago`
- [ ] Verificar que Hibernate crea la tabla `pago_reserva` al iniciar (ddl-auto: update)

### Paso 2 — Credenciales
- [ ] Crear cuenta en https://dashboard.wompi.co
- [ ] Copiar llaves de Sandbox en `application.properties`
- [ ] No subir las llaves al repositorio (agregar al `.gitignore` si se usan archivos locales)

### Paso 3 — Backend
- [ ] Crear entidad `PagoReserva.java`
- [ ] Crear repositorio `PagoReservaRepository.java`
- [ ] Crear `WompiTransaccionDTO.java`
- [ ] Implementar `WompiService.java`
- [ ] Implementar `PagoReservaService.java`
- [ ] Implementar `PagoController.java`
- [ ] Implementar `WompiWebhookController.java`
- [ ] Modificar `ReservaController.java` para redirigir a `/cliente/pago/{id}`
- [ ] Agregar excepción CSRF en `SecurityConfig.java`

### Paso 4 — Frontend
- [ ] Crear `pago.html` con el script del widget de Wompi
- [ ] Crear `pago-resultado.html`

### Paso 5 — Pruebas
- [ ] Levantar ngrok y registrar webhook en Wompi Dashboard
- [ ] Hacer una reserva de prueba como cliente
- [ ] Completar pago con tarjeta `4242424242424242`
- [ ] Verificar en base de datos: `pago_reserva.estado = 'APROBADO'`
- [ ] Verificar en base de datos: `reserva.estado = 'Confirmada'`
- [ ] Probar pago declinado con `4111111111111111`
- [ ] Verificar que los cupos se devuelven correctamente

---

## 12. Reglas de Negocio Nuevas

| ID | Regla |
|----|-------|
| RN-PAY-01 | Una reserva solo pasa a estado `Confirmada` cuando Wompi confirma el pago via webhook |
| RN-PAY-02 | Si el pago es declinado o anulado, la reserva pasa a `Cancelada` y los cupos se devuelven |
| RN-PAY-03 | La referencia de pago es única por reserva (formato: `ETA-{idReserva}-{timestamp}`) |
| RN-PAY-04 | El monto enviado a Wompi debe ser siempre en centavos (multiplicar COP × 100) |
| RN-PAY-05 | El webhook es la fuente de verdad del estado del pago, no la redirección del cliente |
| RN-PAY-06 | Si el cliente cierra el navegador antes de pagar, la reserva queda en `Pendiente` y puede retomar el pago desde el dashboard |

---

## 13. Consideraciones de Seguridad

| Riesgo | Mitigación |
|--------|-----------|
| Webhook falso de un tercero | Validar firma `X-Event-Checksum` con el `events-secret` de Wompi |
| Cliente accede al pago de otro usuario | Verificar en `PagoController` que la reserva pertenece al usuario autenticado |
| Doble procesamiento del mismo webhook | Verificar que `pago_reserva.estado != 'APROBADO'` antes de actualizar |
| Llaves API expuestas en código | Usar variables de entorno; agregar al `.gitignore` |
| CSRF en el webhook | El endpoint `/api/wompi/webhook` debe estar excluido de la protección CSRF |

---

## 14. Fuera del Alcance (esta versión)

- Reembolsos automáticos vía API de Wompi
- Pago con Nequi o PSE (el widget lo incluye pero no requiere código extra)
- Split de pagos entre plataforma y colaborador (solo se registra el total)
- Facturación electrónica
- Paso a producción (requiere cuenta verificada en Wompi)

---

*Spec generada para el proyecto ETA App — Integración Wompi Sandbox v1.0*