Necesito integrar ePayco como pasarela de pago en ETA App.
Lee primero los archivos de contexto del proyecto (db.md, PRD.md, Contexto)
antes de tocar cualquier archivo.

---

## CONTEXTO DEL FLUJO ACTUAL

La ruta de checkout actual es:
  GET /cliente/checkout/{idDisponibilidad}  → muestra resumen de la reserva
  POST /cliente/reservar                    → crea la Reserva en BD

El nuevo flujo reemplaza el POST directo al backend por un pago con ePayco.
El backend NO crea la reserva hasta que ePayco confirme el pago.

---

## CREDENCIALES DISPONIBLES (leer de application.properties, nunca hardcodear)

En application.properties agregar:
  epayco.public.key=TU_PUBLIC_KEY
  epayco.private.key=TU_PRIVATE_KEY
  epayco.client.id=TU_CLIENT_ID
  epayco.test=true   # cambiar a false en producción

En la clase de configuración EpaycoConfig.java (nueva, en config/) inyectar
con @Value estos cuatro campos y exponerlos como beans o getters para que
los servicios los consuman.

---

## FLUJO COMPLETO

### Paso 1 — GET /cliente/checkout/actividad/{idActividad}

NOTA: La ruta cambia de /checkout/{idDisponibilidad} a
/checkout/actividad/{idActividad} para mostrar primero la actividad
y dejar al cliente elegir disponibilidad + cantidad en la misma pantalla.

El controller carga:
- La Actividad (con precio base del colaborador)
- El precioConsumidor calculado con UsuarioHelper.CalcularPrecioConsumidor()
- Las Disponibilidades con estado DISPONIBLE y cuposDisponibles > 0
- Los datos del Cliente autenticado (nombre, email, teléfono) para pre-llenar ePayco

El modelo expone al template:
  actividad         → ActividadDTO
  disponibilidades  → List<DisponibilidadDTO>
  precioUnitario    → BigDecimal (precio con comisión, para mostrar al cliente)
  clienteNombre     → String
  clienteEmail      → String
  clienteTelefono   → String
  epaycoPublicKey   → String (leído de EpaycoConfig)
  epaycoTest        → boolean

### Paso 2 — Interacción en el frontend (checkout.html)

El cliente:
1. Selecciona una disponibilidad del listado (dropdown o cards con fecha/hora/cupos).
2. Ingresa la cantidad de personas (input number, mínimo 1, máximo cuposDisponibles).
3. Ve el total calculado en tiempo real: precioUnitario × cantidad (JS vanilla).
4. Hace clic en "Pagar con ePayco".

Al hacer clic, el botón lanza el widget JS de ePayco con estos parámetros:

  name:          título de la actividad
  description:   "Reserva para [fecha] [horaInicio] - [horaFin]"
  invoice:       generado en frontend como "ETA-[timestamp]" (provisional, solo referencia visual)
  currency:      "cop"
  amount:        total calculado (precioUnitario × cantidad), como string sin decimales
  tax_base:      "0"
  tax:           "0"
  country:       "co"
  lang:          "es"
  external:      "false"        ← usa el widget embebido, no redirección externa
  confirmation:  URL absoluta → "https://[dominio]/cliente/pago/confirmacion"
  response:      URL absoluta → "https://[dominio]/cliente/pago/respuesta"

  // Datos del cliente (pre-llenados desde Thymeleaf, NO editables por el usuario)
  name_billing:   clienteNombre
  address_billing: "" (vacío si no hay)
  type_doc_billing: "CC"
  mobilephone_billing: clienteTelefono
  email_billing:  clienteEmail

  // Metadatos para recuperar el contexto al confirmar
  extra1:  idDisponibilidad (el que seleccionó el cliente)
  extra2:  cantidad (número de personas)
  extra3:  idActividad

El script de ePayco se carga desde:
  <script src="https://checkout.epayco.co/checkout.js"></script>

Usar el handler de ePayco:
  var handler = ePayco.checkout.configure({ key: epaycoPublicKey, test: epaycoTest });
  handler.open({ ...parámetros... });

### Paso 3 — Endpoint de confirmación (backend, POST)

ePayco llama a este endpoint cuando el pago se procesa (puede ser éxito o falla):

  POST /cliente/pago/confirmacion

Este endpoint DEBE ser público en SecurityConfig (sin autenticación requerida,
ya que la llamada viene del servidor de ePayco, no del navegador del cliente).
Agregar la ruta a la lista de permitAll() en SecurityConfig.java.

El body de ePayco llega como application/x-www-form-urlencoded con los campos:
  x_ref_payco         → ID único del pago en ePayco
  x_transaction_state → estado: "Aceptada", "Rechazada", "Pendiente", "Fallida"
  x_amount            → monto cobrado
  x_extra1            → idDisponibilidad
  x_extra2            → cantidad
  x_extra3            → idActividad
  x_signature         → hash de verificación

IMPORTANTE — Verificar firma antes de procesar:
  La firma se verifica concatenando:
    clientId + "^" + publicKey + "^" + x_ref_payco + "^" + x_transaction_id +
    "^" + x_amount + "^" + x_currency_code
  y calculando SHA-256. Si el hash no coincide con x_signature → rechazar con 400.

Lógica del endpoint (en EpaycoService.java, nuevo servicio):

  Si x_transaction_state == "Aceptada":
    1. Verificar que no exista ya una Reserva con refPayco == x_ref_payco (idempotencia).
    2. Leer Disponibilidad por x_extra1. Validar que exista y tenga cupos.
    3. Leer Cliente por sesión... PROBLEMA: el confirmation es server-to-server,
       no tiene sesión. Solución: en extra3 pasar también el idCliente.
       Ajustar extra3 = idActividad + "|" + idCliente (separar con pipe al recibirlo).
    4. Llamar ReservaService.crearReservaDesdeEpayco(idDisponibilidad, idCliente,
       idActividad, cantidad, refPayco) que crea la Reserva con estado "Confirmada".
    5. Responder HTTP 200 OK (texto plano "OK" es suficiente para ePayco).

  Si x_transaction_state == "Rechazada" o "Fallida":
    1. No crear reserva.
    2. Responder HTTP 200 OK igualmente (ePayco espera 200 siempre).
    3. Loguear el rechazo con x_ref_payco para auditoría.

  Si x_transaction_state == "Pendiente":
    1. No crear reserva todavía.
    2. Responder 200. (ePayco puede llamar de nuevo cuando se resuelva)

### Paso 4 — Endpoint de respuesta (frontend, GET)

ePayco redirige al navegador del cliente aquí al cerrar el widget:

  GET /cliente/pago/respuesta

Parámetros que llegan en query string:
  ref_payco           → ID del pago
  x_transaction_state → "Aceptada", "Rechazada", "Pendiente", "Fallida"
  x_extra1            → idDisponibilidad
  x_extra3            → idActividad|idCliente

Este endpoint SÍ requiere autenticación (ROLE_CLIENTE).

Lógica:
  Si x_transaction_state == "Aceptada":
    - Buscar la Reserva por refPayco (puede que ya exista si el confirmation llegó antes).
    - Si existe → redirect a /cliente/dashboard?pago=exitoso
    - Si no existe aún (el confirmation aún no llegó) → redirect a /cliente/dashboard?pago=procesando

  Si "Rechazada" o "Fallida":
    - Redirect a /cliente/checkout/actividad/{idActividad}?pago=fallido&idDispo={x_extra1}

  Si "Pendiente":
    - Redirect a /cliente/dashboard?pago=pendiente

### Paso 5 — Alertas en el frontend

En el template checkout.html y dashboard.html leer el parámetro ?pago= de la URL
con JavaScript vanilla al cargar la página, y mostrar alertas usando el sistema
existente js/alert.js:

  pago=exitoso     → alert tipo "success": "¡Reserva confirmada! Tu pago fue procesado."
  pago=procesando  → alert tipo "info":    "Tu pago está siendo procesado. Te notificaremos pronto."
  pago=pendiente   → alert tipo "info":    "Tu pago quedó pendiente de confirmación."
  pago=fallido     → alert tipo "error":   "El pago no fue procesado. Intenta de nuevo."

El idDispo se puede pasar como parámetro adicional para pre-seleccionar
la disponibilidad en el formulario si el cliente regresa al checkout.

---

## CAMPO NUEVO EN ENTIDAD Reserva

Agregar campo:
  refPayco  String(100) NULL UNIQUE

Este campo almacena el x_ref_payco de ePayco para:
- Idempotencia: evitar crear la reserva dos veces si ePayco llama dos veces al confirmation.
- Auditoría: trazabilidad del pago.

---

## ARCHIVOS A CREAR

### Nuevas clases Java:

1. config/EpaycoConfig.java
   - @Configuration con @Value para las 4 credenciales
   - Método utilidad: verificarFirma(params) → boolean

2. service/EpaycoService.java
   - procesarConfirmacion(Map<String, String> params) → void
   - verificarFirma(params) → boolean (delega a EpaycoConfig)

3. controller/PagoController.java  (mapeado en /cliente/pago/*)
   - POST /cliente/pago/confirmacion  → público, llama EpaycoService
   - GET  /cliente/pago/respuesta     → ROLE_CLIENTE, redirige según estado

### Templates nuevos/modificados:

1. cliente/checkout.html  (reemplaza el checkout anterior)
   - Muestra actividad, disponibilidades, selector de cantidad, total en tiempo real
   - Botón "Pagar" que abre el widget de ePayco
   - Script de ePayco cargado al final del body
   - Lógica JS para leer ?pago= y mostrar alertas

2. Sin template nuevo para /respuesta ni /confirmacion
   (son redirects puros, no renderizan template propio)

---

## SEGURIDAD — CAMBIOS EN SecurityConfig.java

Agregar a las rutas públicas (permitAll):
  /cliente/pago/confirmacion   ← server-to-server de ePayco, sin sesión

Mantener protegida:
  /cliente/pago/respuesta      ← el navegador del cliente sí tiene sesión

Asegurarse de que el CSRF está deshabilitado para /cliente/pago/confirmacion
si CSRF está habilitado globalmente (ePayco no envía token CSRF).
Solución recomendada: usar .csrf(csrf -> csrf.ignoringRequestMatchers("/cliente/pago/confirmacion"))

---

## RESTRICCIONES DE ARQUITECTURA

- La lógica de verificación de firma y creación de reserva va en EpaycoService,
  NO en el controller.
- ReservaService.crearReservaDesdeEpayco() reutiliza la misma lógica de
  crearReserva() pero recibe refPayco como parámetro adicional.
- Nunca hardcodear las credenciales de ePayco en código Java ni en templates.
  Siempre leerlas de application.properties vía @Value.
- El cálculo del monto total para ePayco usa UsuarioHelper.CalcularPrecioConsumidor()
  igual que en el resto del sistema.
- No usar la librería Java de ePayco (epayco-java) si requiere versiones de Java
  incompatibles; basta con la verificación manual del SHA-256.
- Loguear con SLF4J (ya disponible con Spring Boot) todos los eventos de pago:
  confirmación recibida, firma verificada/rechazada, reserva creada, errores.
- Seguir el patrón Controller → Service → Repository sin excepciones.