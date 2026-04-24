Implementa el sistema completo de cancelaciones y modelo de negocio en ETA App.
Lee primero db.md, PRD.md y el archivo Contexto antes de tocar cualquier archivo.

---

## MODELO DE NEGOCIO (resumen base)

- Colaborador publica actividad con precio base (ej: $100.000)
- Cliente ve y paga: precioBase × (1 + comision/100) → $118.000
- ETA recibe el dinero completo vía ePayco
- ETA transfiere manualmente al colaborador su parte
- Admin gestiona los pagos desde su dashboard

---

## POLÍTICAS DE CANCELACIÓN — NÚCLEO DEL SISTEMA

### Quién puede cancelar
- El CLIENTE puede cancelar su reserva
- El COLABORADOR puede cancelar una disponibilidad completa (afecta todas sus reservas)
- El ADMIN puede cancelar cualquier reserva en cualquier estado

### Ventana de cancelación (configurable por el admin)
- Es una variable global en la entidad Admin llamada: horasCancelacion
- Valores posibles: 24, 48, 72, 168 (7 días), 336 (14 días)
- Significa: el cliente puede cancelar SI faltan más de horasCancelacion horas 
  para la fecha/hora de inicio de la actividad
- Si faltan MENOS horas que horasCancelacion → el cliente NO puede cancelar 
  desde la UI (botón deshabilitado con mensaje explicativo)
- El admin y el colaborador SÍ pueden cancelar en cualquier momento sin restricción de tiempo

### Política de reembolso por actividad (configura el colaborador al crear/editar)
El colaborador elige UNA de estas 4 políticas para cada actividad:

  POLITICA_1 — SIN_REEMBOLSO
    El cliente cancela → no recibe dinero de vuelta
    El colaborador se queda con su parte (82% del precioConsumidor)
    ETA se queda con su comisión (18%)
    Nota: aplica solo si el cliente cancela dentro de la ventana permitida;
    si cancela fuera de la ventana, el sistema no lo permite

  POLITICA_2 — REEMBOLSO_TOTAL_SI_A_TIEMPO
    Si cancela dentro de la ventana permitida → reembolso total al cliente
    Si el sistema no permite la cancelación (fuera de ventana) → no aplica
    (esta política es la más común)

  POLITICA_3 — REEMBOLSO_PARCIAL
    Cliente cancela → recibe 50% del precioConsumidor que pagó
    El colaborador recibe: 50% × 82% del precioConsumidor
    ETA retiene: su comisión del 50% restante
    El % de reembolso parcial es fijo en 50% (no configurable por ahora)

  POLITICA_4 — SIEMPRE_GRATUITA
    El cliente puede cancelar en CUALQUIER momento (ignora horasCancelacion)
    Siempre recibe reembolso total
    Esta política sobrescribe la ventana de cancelación global

### Cancelación por el COLABORADOR
  Cuando el colaborador cancela una Disponibilidad:
  - TODAS las reservas asociadas a esa disponibilidad pasan a estado CANCELADA_POR_COLABORADOR
  - TODOS los clientes afectados reciben reembolso total (100%) sin excepción
  - El colaborador recibe una PENALIZACIÓN: se registra en su perfil
    (campo penalizaciones: Integer en entidad Colaborador, se incrementa en 1 por cada cancelación)
  - El admin ve la penalización en el panel del colaborador
  - El admin debe procesar el reembolso manualmente (mismo flujo que pagos)

### Cancelación por el ADMIN
  - Puede cancelar cualquier reserva en cualquier estado
  - Debe elegir manualmente en la UI: ¿reembolso total, parcial o sin reembolso?
  - Se guarda quién canceló y la razón (campo canceladoPor y motivoCancelacion en Reserva)

---

## CAMBIOS EN ENTIDADES

### Entidad Actividad — campos nuevos:
  politicaCancelacion  Enum  NOT NULL  DEFAULT REEMBOLSO_TOTAL_SI_A_TIEMPO
  
  Enum PoliticaCancelacion:
    SIN_REEMBOLSO
    REEMBOLSO_TOTAL_SI_A_TIEMPO
    REEMBOLSO_PARCIAL
    SIEMPRE_GRATUITA

### Entidad Admin — campo nuevo:
  horasCancelacion  Integer  NOT NULL  DEFAULT 24
  (valores permitidos: 24, 48, 72, 168, 336)

### Entidad Colaborador — campos nuevos:
  banco           String(100)  NULL
  numeroCuenta    String(50)   NULL
  tipoCuenta      String(20)   NULL   (enum: AHORROS, CORRIENTE)
  penalizaciones  Integer      DEFAULT 0

### Entidad Reserva — campos nuevos (todos se congelan al crear):
  refPayco                  String(100)   UNIQUE  NULL
  precioColaborador         BigDecimal    NOT NULL
  precioConsumidor          BigDecimal    NOT NULL
  comisionPorcentaje        BigDecimal    NOT NULL
  comisionEta               BigDecimal    NOT NULL
  estadoPagoColaborador     String(30)    DEFAULT "PENDIENTE_PAGO"
  fechaPagoColaborador      LocalDateTime NULL
  politicaAplicada          String(30)    NOT NULL  (snapshot de la política al momento de reservar)
  montoReembolso            BigDecimal    NULL       (se calcula al cancelar)
  estadoReembolso           String(30)    NULL       (PENDIENTE_REEMBOLSO, REEMBOLSADO, SIN_REEMBOLSO)
  fechaReembolso            LocalDateTime NULL
  canceladoPor              String(20)    NULL       (CLIENTE, COLABORADOR, ADMIN)
  motivoCancelacion         String(255)   NULL

  Estados posibles de Reserva:
    CONFIRMADA
    COMPLETADA
    CANCELADA_POR_CLIENTE
    CANCELADA_POR_COLABORADOR
    CANCELADA_POR_ADMIN

---

## LÓGICA DE CANCELACIÓN — ReservaService

### cancelarPorCliente(idReserva, idCliente):

  1. Obtener la reserva, validar que pertenece al cliente autenticado
  2. Validar que el estado es CONFIRMADA (no se puede cancelar COMPLETADA)
  3. Obtener la actividad y su politicaAplicada (usar el snapshot guardado en reserva)
  4. Si politicaAplicada == SIEMPRE_GRATUITA:
       → reembolso total, ignorar ventana de tiempo
  5. Si politicaAplicada != SIEMPRE_GRATUITA:
       → Calcular horas restantes: 
           horasRestantes = horas entre LocalDateTime.now() y disponibilidad.fecha + disponibilidad.horaInicio
       → Obtener horasCancelacion del Admin
       → Si horasRestantes < horasCancelacion:
           lanzar excepción CancelacionFueraDeTiempoException 
           con mensaje "No puedes cancelar con menos de X horas de anticipación"
           (el frontend muestra esto como alert error, no como excepción 500)
  6. Calcular montoReembolso según política:
       SIN_REEMBOLSO              → montoReembolso = 0
       REEMBOLSO_TOTAL_SI_A_TIEMPO → montoReembolso = reserva.precioConsumidor × reserva.cantidad
       REEMBOLSO_PARCIAL          → montoReembolso = (reserva.precioConsumidor × reserva.cantidad) × 0.50
       SIEMPRE_GRATUITA           → montoReembolso = reserva.precioConsumidor × reserva.cantidad
  7. Actualizar reserva:
       estado = CANCELADA_POR_CLIENTE
       canceladoPor = "CLIENTE"
       montoReembolso = calculado
       estadoReembolso = (montoReembolso > 0) ? "PENDIENTE_REEMBOLSO" : "SIN_REEMBOLSO"
  8. Devolver cupos: disponibilidad.cuposDisponibles += reserva.cantidad
  9. Guardar reserva y disponibilidad

### cancelarPorColaborador(idDisponibilidad, idColaborador):

  1. Validar que la disponibilidad pertenece a una actividad del colaborador
  2. Obtener todas las reservas CONFIRMADAS de esa disponibilidad
  3. Para cada reserva:
       estado = CANCELADA_POR_COLABORADOR
       canceladoPor = "COLABORADOR"
       montoReembolso = precioConsumidor × cantidad  (siempre total)
       estadoReembolso = "PENDIENTE_REEMBOLSO"
  4. Cambiar estado de la disponibilidad a CANCELADO
  5. Incrementar colaborador.penalizaciones += 1
  6. Guardar todo

### cancelarPorAdmin(idReserva, tipoReembolso, motivo):

  tipoReembolso: "TOTAL", "PARCIAL", "SIN_REEMBOLSO"
  
  1. Calcular montoReembolso según tipoReembolso elegido por el admin
  2. Actualizar reserva:
       estado = CANCELADA_POR_ADMIN
       canceladoPor = "ADMIN"
       motivoCancelacion = motivo
       montoReembolso = calculado
       estadoReembolso = según corresponda
  3. Devolver cupos a disponibilidad
  4. Guardar

---

## DISTRIBUCIÓN DEL DINERO EN CANCELACIONES

### Si política es SIN_REEMBOLSO (cliente cancela):
  El colaborador recibe su 82% igual (estadoPagoColaborador sigue PENDIENTE_PAGO)
  ETA conserva su 18%
  montoReembolso = 0

### Si política es REEMBOLSO_TOTAL o SIEMPRE_GRATUITA:
  ETA devuelve el 100% al cliente manualmente
  El colaborador NO recibe nada (estadoPagoColaborador = NO_APLICA)
  montoReembolso = precioConsumidor × cantidad

### Si política es REEMBOLSO_PARCIAL:
  ETA devuelve 50% al cliente manualmente
  El colaborador recibe: 50% × 82% del precioConsumidor × cantidad
  ETA conserva: 50% de su comisión + diferencia
  montoReembolso = precioConsumidor × cantidad × 0.50
  estadoPagoColaborador = PENDIENTE_PAGO (por el 41% que le corresponde)

### Si cancela el COLABORADOR:
  ETA devuelve 100% al cliente manualmente
  El colaborador NO recibe nada + recibe penalización
  estadoPagoColaborador = NO_APLICA

---

## MÓDULOS UI A IMPLEMENTAR

### 1. Crear/Editar Actividad (colaborador)
  Agregar campo politicaCancelacion como selector con las 4 opciones
  Mostrar descripción de cada política al seleccionar (JS vanilla)
  Ejemplo: "REEMBOLSO_PARCIAL → El cliente recibe el 50% si cancela a tiempo"

### 2. Settings del Colaborador
  Sección "Método de cobro": banco, numeroCuenta, tipoCuenta
  Sección "Mis penalizaciones": mostrar contador con alerta si > 3

### 3. Dashboard del Cliente
  Por cada reserva CONFIRMADA mostrar:
  - Política de cancelación aplicada (en lenguaje amigable)
  - Botón "Cancelar reserva" si está dentro de la ventana permitida
  - Si está fuera de ventana: mostrar texto gris "Cancelación no disponible 
    (menos de Xh antes de la actividad)" en lugar del botón
  - Si política es SIEMPRE_GRATUITA: botón siempre visible
  Al hacer clic en cancelar:
  - Mostrar modal de confirmación con: política aplicada, monto a reembolsar,
    y texto "¿Estás seguro? Esta acción no se puede deshacer"
  - POST /cliente/reserva/cancelar/{idReserva}
  - Si CancelacionFueraDeTiempoException → mostrar alert error con el mensaje
  - Si éxito → mostrar alert success con monto de reembolso

### 4. Dashboard del Colaborador — Cancelar disponibilidad
  En la vista de disponibilidades, botón "Cancelar fecha" por cada disponibilidad
  Modal de confirmación: "Esta acción cancelará X reservas y recibirás una penalización"
  POST /colaborador/disponibilidades/cancelar/{idDisponibilidad}
  Mostrar contador de penalizaciones en el header del dashboard con badge rojo si > 3

### 5. Dashboard Admin — Gestión de pagos y reembolsos
  GET /admin/pagos — dos secciones:
  
  SECCIÓN A: Pagos pendientes al colaborador
    Tabla con reservas CONFIRMADAS/COMPLETADAS con estadoPagoColaborador = PENDIENTE_PAGO
    y que NO estén canceladas
    Columnas: colaborador, banco, cuenta, actividad, fecha actividad,
    precioConsumidor, comisionEta, monto a transferir al colaborador
    Botón "Marcar como pagado" → POST /admin/pagos/marcar-pagado/{idReserva}

  SECCIÓN B: Reembolsos pendientes al cliente
    Tabla con reservas cuyo estadoReembolso = PENDIENTE_REEMBOLSO
    Columnas: cliente, email, actividad, canceladoPor, politicaAplicada, montoReembolso
    Botón "Marcar reembolso enviado" → POST /admin/reembolsos/marcar-enviado/{idReserva}
      → cambia estadoReembolso = REEMBOLSADO, guarda fechaReembolso = now()

  GET /admin/ingresos — métricas:
    Total recaudado (suma precioConsumidor de todas las reservas CONFIRMADAS/COMPLETADAS)
    Total comisión ETA (suma comisionEta)
    Total transferido a colaboradores (suma monto de estadoPagoColaborador = PAGADO)
    Total reembolsado a clientes (suma montoReembolso de estadoReembolso = REEMBOLSADO)
    Ganancia neta ETA = total comisión - total reembolsado proporcional
    Tabla por colaborador: total generado, cobrado, pendiente, penalizaciones
    Tabla por actividad: total reservas, ingresos, cancelaciones

  GET /admin/configuracion (o integrar en dashboard):
    Campo editable horasCancelacion con selector: 24h, 48h, 72h, 7 días, 14 días
    POST /admin/configuracion/cancelacion

---

## REGLAS DE NEGOCIO FINALES

  RN-01: precio cliente = precioColaborador × (1 + comisionPorcentaje/100)
  RN-02: Todos los campos de precio se congelan al crear la Reserva
  RN-03: La Reserva solo se crea si ePayco confirma "Aceptada"
  RN-04: Solo ADMIN puede cambiar estadoPagoColaborador y estadoReembolso
  RN-05: Una reserva COMPLETADA no puede cancelarse por ningún actor
  RN-06: politicaAplicada en Reserva es un snapshot; si el colaborador cambia
          la política después, las reservas anteriores no se ven afectadas
  RN-07: SIEMPRE_GRATUITA ignora completamente horasCancelacion
  RN-08: El colaborador que cancela una disponibilidad siempre genera reembolso 
          total a todos los clientes afectados + penalización en su perfil
  RN-09: El admin puede cancelar sin restricción de tiempo ni política,
          pero debe especificar tipo de reembolso y motivo
  RN-10: Si montoReembolso = 0, estadoReembolso = SIN_REEMBOLSO (no aparece 
          en la cola de reembolsos pendientes del admin)
  RN-11: El colaborador NO puede ver ni modificar estadoPagoColaborador
  RN-12: El cliente NO ve el precioColaborador en ningún momento, 
          solo ve el precioConsumidor (con comisión incluida)
  RN-13: estadoPagoColaborador = NO_APLICA cuando el colaborador cancela 
          o cuando aplica reembolso total (ETA no transfiere nada al colaborador)
  RN-14: Un colaborador con más de 3 penalizaciones muestra badge de advertencia 
          en el panel admin

---

## RESTRICCIONES DE ARQUITECTURA

  - Calcular precios SIEMPRE con UsuarioHelper.CalcularPrecioConsumidor()
  - Lógica de cancelación en CancelacionService (clase nueva), NO en controllers
  - CancelacionFueraDeTiempoException es una excepción de negocio personalizada,
    el controller la captura y devuelve redirect con parámetro ?error=fuera_de_tiempo
  - Usar @Transactional en todos los métodos de cancelación (son multi-entidad)
  - Queries de totales con JPQL GROUP BY, nunca loops en Java
  - Todos los montos son BigDecimal, nunca double ni float
  - Seguir patrón Controller → Service → Repository estrictamente
  - Usar DTOs para vistas del admin
  - Thymeleaf fragments para componentes reutilizables
  - JS vanilla para modales de confirmación y cálculo de totales en tiempo real
  - Loguear con SLF4J todos los eventos de cancelación y pago