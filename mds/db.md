# Base de Datos — ETA App — Marketplace de Actividades Turísticas

**Versión:** 2.0  
**Fecha de Actualización:** 26 de mayo de 2026  
**Estado:** En desarrollo con integraciones de IA, pagos y predicción  

## 1. Introducción

### 1.1 Propósito del Documento
Este documento especifica la arquitectura, estructura y almacenamiento de datos de la plataforma **ETA App**, una aplicación que conecta turistas (clientes) con proveedores de experiencias locales (colaboradores). Define todas las entidades, relaciones, tipos de datos y estrategias de persistencia que soportan las funcionalidades core del negocio, incluyendo pagos, IA, planes turísticos y predicción de ocupación.

### 1.2 Alcance
El documento cubre:
- Modelo de datos relacional completo
- Entidades y sus atributos
- Relaciones entre entidades
- Estrategias de almacenamiento
- Consideraciones de seguridad y privacidad
- Configuración de persistencia

---

## 2. Stack Tecnológico de Base de Datos

### 2.1 Sistema de Gestión de Base de Datos
- **SGBD**: MySQL 8.0+
- **URL de Conexión**: `jdbc:mysql://localhost:3306/eta_db`
- **Configuración**: Creación automática de base de datos si no existe

### 2.2 Framework de Persistencia
- **ORM**: Hibernate + JPA (Jakarta Persistence API)
- **Estrategia DDL**: `update` - Actualización automática del esquema
- **Dialecto**: MySQL Dialect
- **SQL Logging**: Habilitado con formato SQL legible

### 2.3 Configuración de Seguridad
- **Autenticación OAuth2**: Google OAuth2 para login social
- **Gestión de Sesiones**: Cookies HTTP-only con timeout de 30 minutos
- **Subida de Archivos**: Límite de 10MB por archivo/request

---

## 3. Arquitectura del Modelo de Datos

### 3.1 Diagrama de Entidades Principal

```
┌─────────────┐       ┌──────────────┐       ┌─────────────┐
│   Usuario   │◄──────│   Cliente    │◄──────│   Favorito  │
│             │       │              │       │             │
│   (Core)    │       │ (Perfil)     │       │ (Relación)  │
└──────┬──────┘       └──────┬───────┘       └──────┬──────┘
       │                     │                      │
       │                     │                      │
       ├─────────────────────┤                      │
       │                     │                      │
       ▼                     ▼                      ▼
┌─────────────┐       ┌──────────────┐       ┌─────────────┐
│Colaborador  │──────►│  Actividad   │◄──────│  Categoría  │
│             │       │              │       │             │
│ (Proveedor) │       │   (Oferta)   │       │  (Catalog)  │
└──────┬──────┘       └──────┬───────┘       └─────────────┘
       │                     │
       │        ┌────────────┼────────────┬───────────┐
       │        │            │            │           │
       │        ▼            ▼            ▼           ▼
       │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
       │  │Disponib. │ │Comentario│ │ImagenAct.│ │PlanAct.  │
       │  └─────┬────┘ └──────────┘ └──────────┘ └────┬─────┘
       │        │                                      │
       │        ▼                                      ▼
       │  ┌──────────┐       ┌──────────────┐   ┌──────────┐
       │  │  Reserva │──────►│Conversación  │   │   Plan   │
       │  │          │       │    Chat      │   │(Ruta)    │
       └──┼──────────┘       └──────┬───────┘   └────┬─────┘
          │                         │                │
          │                         ▼                │
          │                  ┌──────────────┐        │
          │                  │ MensajeChat  │        │
          │                  └──────────────┘        │
          └──────────────────────────────────────────┘
```

---

## 4. Entidades del Sistema

### 4.1 Entidades de Usuario y Roles

#### 4.1.1 **Usuario** (Tabla: `usuarios`)
**Propósito**: Entidad base que representa cualquier usuario del sistema.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | Long | PK, AUTO_INCREMENT | Identificador único |
| `nombre` | String(100) | NOT NULL | Nombre completo del usuario |
| `email` | String(150) | UNIQUE, NOT NULL | Email único del usuario |
| `password` | String(255) | NOT NULL | Contraseña encriptada |
| `telefono` | String(15) | NOT NULL | Número de teléfono |
| `createdAt` | LocalDateTime | NOT NULL, DEFAULT NOW | Fecha de creación |
| `emailVerificado` | Boolean | DEFAULT FALSE | Estado de verificación de email |
| `tokenVerificacion` | String(120) | NULL | Token para verificación de email |
| `tokenVerificacionExpiraEn` | LocalDateTime | NULL | Fecha de expiración del token |
| `id_rol` | Long | FK → Rol, NOT NULL | Rol del usuario |

**Relaciones**:
- `ManyToOne` → `Rol`: Define el rol del usuario (Cliente, Colaborador, Admin)

---

#### 4.1.2 **Rol** (Tabla: `roles`)
**Propósito**: Catálogo de roles del sistema para control de acceso.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | Long | PK, AUTO_INCREMENT | Identificador único |
| `nombre` | String(50) | UNIQUE, NOT NULL | Nombre del rol |

**Valores Esperados**:
- `ROLE_CLIENTE`
- `ROLE_COLABORADOR`
- `ROLE_ADMIN`

---

#### 4.1.3 **Cliente** (Tabla: `cliente`)
**Propósito**: Extensión de Usuario para clientes/turistas que buscan y reservan actividades.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | Long | PK, AUTO_INCREMENT | Identificador único |
| `id_usuario` | Long | FK → Usuario, UNIQUE, NOT NULL | Referencia al usuario base |
| `cedula` | Long | UNIQUE, NOT NULL | Número de identificación |
| `direccion` | String | NULL | Dirección del cliente |
| `preferencias` | TEXT | NULL | Preferencias generales |
| `paisOrigen` | String(80) | NULL | País de origen del turista |
| `grupoViaje` | Enum | NULL | Con quién viaja (SOLO, PAREJA, FAMILIA, AMIGOS, VARIOS) |
| `rangoPrecio` | Enum | NULL | Rango de precio preferido (ECONOMICO, MODERADO, PREMIUM) |
| `disponibilidadSemana` | Enum | NULL | Cuándo hace actividades (FINDE, ENTRE_SEMANA, AMBOS) |
| `onboardingCompletado` | Boolean | DEFAULT FALSE | Estado del proceso de onboarding |

**Relaciones**:
- `OneToOne` → `Usuario`: Extiende la información del usuario
- `OneToMany` → `Reserva`: Historial de reservas del cliente
- `OneToMany` → `Comentario`: Comentarios realizados
- `OneToMany` → `Favorito`: Actividades marcadas como favoritas
- `ManyToMany` → `Categoria`: Categorías preferidas del cliente

---

#### 4.1.4 **Colaborador** (Tabla: `colaborador`)
**Propósito**: Extensión de Usuario para proveedores que ofrecen actividades.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idColaborador` | Long | PK, AUTO_INCREMENT | Identificador único |
| `id_usuario` | Long | FK → Usuario, UNIQUE, NOT NULL | Referencia al usuario base |
| `nit` | String(50) | NULL | Número de identificación tributaria |
| `correoSeguridad` | String(150) | NULL | Email secundario de seguridad |

**Relaciones**:
- `OneToOne` → `Usuario`: Extiende la información del usuario
- `OneToMany` → `Actividad`: Actividades ofrecidas por el colaborador

---

#### 4.1.5 **Admin** (Tabla: `admin`)
**Propósito**: Extensión de Usuario para administradores de la plataforma.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idAdmin` | Long | PK, AUTO_INCREMENT | Identificador único |
| `id_usuario` | Long | FK → Usuario, UNIQUE, NOT NULL | Referencia al usuario base |
| `porcentajeComision` | Decimal(5,2) | DEFAULT 18.00 | Porcentaje de comisión de la plataforma |

**Relaciones**:
- `OneToOne` → `Usuario`: Extiende la información del usuario

---

### 4.2 Entidades de Catálogos

#### 4.2.1 **Categoria** (Tabla: `categoria`)
**Propósito**: Clasificación de actividades (ej: aventura, cultura, gastronomía).

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idCategoria` | Long | PK, AUTO_INCREMENT | Identificador único |
| `nombre` | String(100) | UNIQUE, NOT NULL | Nombre de la categoría |
| `imagen` | String | NULL | URL o ruta de imagen representativa |

**Relaciones**:
- `OneToMany` → `Actividad`: Actividades de esta categoría

---

#### 4.2.2 **Idioma** (Tabla: `idioma`)
**Propósito**: Idiomas en los que se ofrecen las actividades.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idIdioma` | Long | PK, AUTO_INCREMENT | Identificador único |
| `codigo` | String(10) | NOT NULL | Código ISO del idioma (ej: 'es', 'en') |
| `nombre` | String(50) | NOT NULL | Nombre del idioma (ej: 'Español') |

**Relaciones**:
- `OneToMany` → `Actividad`: Actividades que se ofrecen en este idioma

---

#### 4.2.3 **Beneficio** (Tabla: `beneficio`)
**Propósito**: Catálogo de beneficios que pueden incluir las actividades.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idBeneficio` | Long | PK, AUTO_INCREMENT | Identificador único |
| `nombre` | String(100) | NOT NULL | Nombre del beneficio |

---

### 4.3 Entidades de Actividades

#### 4.3.1 **Actividad** (Tabla: `actividad`)
**Propósito**: Representa una experiencia turística ofrecida por un colaborador.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idActividad` | Long | PK, AUTO_INCREMENT | Identificador único |
| `titulo` | String(200) | NOT NULL | Título de la actividad |
| `descripcion` | TEXT | NULL | Descripción detallada |
| `calificacion` | Integer | DEFAULT 0 | Calificación promedio |
| `ubicacion` | String | NULL | Dirección o lugar |
| `latitud` | Double | NULL | Coordenada geográfica |
| `longitud` | Double | NULL | Coordenada geográfica |
| `normas` | TEXT | NULL | Normas y reglas de la actividad |
| `incluye` | TEXT | NULL | Qué incluye el precio |
| `id_idioma` | Long | FK → Idioma, NOT NULL | Idioma de la actividad |
| `condiciones` | TEXT | NULL | Condiciones de participación |
| `imagen` | String | NULL | Imagen principal |
| `precio` | Decimal | NOT NULL | Precio base de la actividad |
| `createdAt` | LocalDateTime | NOT NULL | Fecha de creación |
| `updatedAt` | LocalDateTime | NULL | Última actualización |
| `id_colaborador` | Long | FK → Colaborador, NOT NULL | Proveedor de la actividad |
| `id_categoria` | Long | FK → Categoria, NOT NULL | Categoría de la actividad |
| `totalVistas` | Integer | DEFAULT 0 | Contador de vistas (personalización) |
| `totalTendencia` | Integer | DEFAULT 0 | Contador de tendencia |

**Relaciones**:
- `ManyToOne` → `Colaborador`: Proveedor de la actividad
- `ManyToOne` → `Categoria`: Categoría de la actividad
- `ManyToOne` → `Idioma`: Idioma en que se ofrece
- `OneToMany` → `Comentario`: Comentarios de clientes
- `OneToMany` → `Reserva`: Reservas realizadas
- `OneToMany` → `ImagenActividad`: Galería de imágenes
- `OneToMany` → `Disponibilidad`: Fechas y horarios disponibles
- `OneToMany` → `Favorito`: Clientes que la tienen en favoritos

---

#### 4.3.2 **ImagenActividad** (Tabla: `imagen_actividad`)
**Propósito**: Galería de imágenes adicionales de una actividad.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idImagen` | Long | PK, AUTO_INCREMENT | Identificador único |
| `nombre` | String | NOT NULL | Nombre/ruta del archivo de imagen |
| `id_actividad` | Long | FK → Actividad, NOT NULL | Actividad a la que pertenece |

**Relaciones**:
- `ManyToOne` → `Actividad`: Actividad a la que pertenece esta imagen

---

### 4.4 Entidades de Disponibilidad y Reservas

#### 4.4.1 **PatronDisponibilidad** (Tabla: `patron_disponibilidad`)
**Propósito**: Define patrones recurrentes de disponibilidad (ej: "todos los lunes y miércoles de 10-12h").

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idPatron` | Long | PK, AUTO_INCREMENT | Identificador único |
| `id_actividad` | Long | FK → Actividad, NOT NULL | Actividad asociada |
| `horaInicio` | LocalTime | NOT NULL | Hora de inicio del horario |
| `horaFin` | LocalTime | NOT NULL | Hora de fin del horario |
| `diasSemana` | String(200) | NOT NULL | Días separados por coma (ej: "MONDAY,WEDNESDAY") |
| `cuposTotales` | Integer | NOT NULL | Cupos disponibles por sesión |
| `fechaInicio` | LocalDate | NOT NULL | Fecha desde la que aplica el patrón |
| `fechaFin` | LocalDate | NOT NULL | Fecha hasta la que aplica el patrón |
| `estado` | String(20) | DEFAULT 'ACTIVO' | Estado del patrón (ACTIVO, INACTIVO) |
| `createdAt` | LocalDateTime | DEFAULT NOW | Fecha de creación del patrón |

**Relaciones**:
- `ManyToOne` → `Actividad`: Actividad con disponibilidad recurrente
- `OneToMany` → `Disponibilidad`: Disponibilidades generadas a partir del patrón

---

#### 4.4.2 **Disponibilidad** (Tabla: `disponibilidad`)
**Propósito**: Instancia específica de disponibilidad en una fecha y hora concreta.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idDisponibilidad` | Long | PK, AUTO_INCREMENT | Identificador único |
| `fecha` | LocalDate | NOT NULL | Fecha específica |
| `horaInicio` | LocalTime | NOT NULL | Hora de inicio |
| `horaFin` | LocalTime | NOT NULL | Hora de finalización |
| `cuposTotales` | Integer | NOT NULL | Cupos totales |
| `cuposDisponibles` | Integer | NOT NULL | Cupos aún disponibles |
| `estado` | String(20) | DEFAULT 'DISPONIBLE' | Estado (DISPONIBLE, CANCELADO, COMPLETADO) |
| `id_actividad` | Long | FK → Actividad, NOT NULL | Actividad disponible |
| `id_patron` | Long | FK → PatronDisponibilidad, NULL | Patrón del que se generó (si aplica) |

**Métodos de Negocio**:
- `reservarCupos(int cantidad)`: Reduce cupos disponibles con validación

**Relaciones**:
- `ManyToOne` → `Actividad`: Actividad disponible
- `ManyToOne` → `PatronDisponibilidad`: Patrón origen (opcional)
- `OneToMany` → `Reserva`: Reservas para esta disponibilidad

---

#### 4.4.3 **Reserva** (Tabla: `reserva`)
**Propósito**: Representa una reserva confirmada de un cliente para una actividad.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idReserva` | Long | PK, AUTO_INCREMENT | Identificador único |
| `id_disponibilidad` | Long | FK → Disponibilidad, NOT NULL | Disponibilidad reservada |
| `id_cliente` | Long | FK → Cliente, NOT NULL | Cliente que realizó la reserva |
| `id_actividad` | Long | FK → Actividad, NOT NULL | Actividad reservada |
| `estado` | String | DEFAULT 'Pendiente' | Estado de la reserva (Pendiente/Confirmada/Cancelada/Hecho) |
| `cantidad` | Integer | NOT NULL | Número de cupos reservados |
| `fechaReserva` | LocalDateTime | NOT NULL | Fecha en que se realizó la reserva |
| `precioTotal` | Decimal(10,2) | NOT NULL | Precio total de la reserva con comisión incluida |
| `ref_payco` | String(100) | NULL | Referencia de transacción de ePayco (si pagó) |
| `estadoPago` | String(50) | DEFAULT 'PENDIENTE' | Estado del pago (PENDIENTE/CONFIRMADO/FALLIDO/REEMBOLSADO) |

**Relaciones**:
- `ManyToOne` → `Cliente`: Cliente que reservó
- `ManyToOne` → `Actividad`: Actividad reservada
- `ManyToOne` → `Disponibilidad`: Horario específico reservado
- `OneToOne` → `ConversacionChat`: Chat asociado a esta reserva

---

### 4.5 Entidades de Interacción Social

#### 4.5.1 **Comentario** (Tabla: `comentario`)
**Propósito**: Reseñas y opiniones de clientes sobre actividades.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idComentario` | Long | PK, AUTO_INCREMENT | Identificador único |
| `id_cliente` | Long | FK → Cliente, NOT NULL | Cliente que comentó |
| `id_actividad` | Long | FK → Actividad, NOT NULL | Actividad comentada |
| `texto` | TEXT | NOT NULL | Contenido del comentario |
| `calificacion` | Integer | NOT NULL | Puntuación (ej: 1-5 estrellas) |
| `fechaComentario` | LocalDateTime | DEFAULT NOW | Fecha del comentario |

**Relaciones**:
- `ManyToOne` → `Cliente`: Autor del comentario
- `ManyToOne` → `Actividad`: Actividad evaluada

---

#### 4.5.2 **Favorito** (Tabla: `favorito`)
**Propósito**: Lista de actividades favoritas de cada cliente.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idFavorito` | Long | PK, AUTO_INCREMENT | Identificador único |
| `id_cliente` | Long | FK → Cliente, NOT NULL | Cliente propietario |
| `id_actividad` | Long | FK → Actividad, NOT NULL | Actividad marcada |
| `createdAt` | LocalDateTime | DEFAULT NOW | Fecha en que se marcó |

**Restricciones**:
- `UNIQUE(id_cliente, id_actividad)`: Un cliente no puede tener duplicados

**Relaciones**:
- `ManyToOne` → `Cliente`: Propietario de la lista
- `ManyToOne` → `Actividad`: Actividad favorita

---

### 4.6 Entidades de Mensajería

#### 4.6.1 **ConversacionChat** (Tabla: `conversacion_chat`)
**Propósito**: Conversación privada entre un cliente y un colaborador vinculada a una reserva.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idConversacion` | Long | PK, AUTO_INCREMENT | Identificador único |
| `id_reserva` | Long | FK → Reserva, UNIQUE, NOT NULL | Reserva que originó la conversación |
| `id_cliente` | Long | FK → Cliente, NOT NULL | Cliente participante |
| `id_colaborador` | Long | FK → Colaborador, NOT NULL | Colaborador participante |
| `createdAt` | LocalDateTime | DEFAULT NOW | Fecha de creación |
| `updatedAt` | LocalDateTime | DEFAULT NOW | Última actualización |

**Relaciones**:
- `OneToOne` → `Reserva`: Reserva asociada
- `ManyToOne` → `Cliente`: Cliente participante
- `ManyToOne` → `Colaborador`: Colaborador participante
- `OneToMany` → `MensajeChat`: Mensajes de la conversación

---

#### 4.6.2 **MensajeChat** (Tabla: `mensaje_chat`)
**Propósito**: Mensaje individual dentro de una conversación.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `idMensaje` | Long | PK, AUTO_INCREMENT | Identificador único |
| `id_conversacion` | Long | FK → ConversacionChat, NOT NULL | Conversación a la que pertenece |
| `id_remitente` | Long | FK → Usuario, NOT NULL | Usuario que envió el mensaje |
| `contenido` | TEXT | NOT NULL | Contenido del mensaje |
| `fechaEnvio` | LocalDateTime | DEFAULT NOW | Fecha y hora de envío |

**Relaciones**:
- `ManyToOne` → `ConversacionChat`: Conversación contenedora
- `ManyToOne` → `Usuario`: Remitente del mensaje

---

### 4.7 Entidades de Planes del Día

#### 4.7.1 **Plan** (Tabla: `planes`)
**Propósito**: Representa una ruta turística temática que agrupa múltiples actividades en un itinerario sugerido.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | Long | PK, AUTO_INCREMENT | Identificador único |
| `titulo` | String(200) | NOT NULL | Título del plan |
| `descripcion` | TEXT | NULL | Descripción del itinerario |
| `imagenPortada` | String(255) | NULL | Imagen principal del plan |
| `duracionEstimada` | String(50) | NULL | Duración estimada (ej: "8 horas", "1 día completo") |
| `tipo` | String(50) | NULL | Tipo de plan (ej: "Cultural", "Aventura", "Gastronómico") |
| `id_cliente_creador` | Long | FK → Cliente, NULL | Cliente que creó el plan (si aplica) |
| `id_colaborador_creador` | Long | FK → Colaborador, NULL | Colaborador que creó el plan (si aplica) |
| `fechaCreacion` | LocalDateTime | NOT NULL, DEFAULT NOW | Fecha de creación |
| `publico` | Boolean | NOT NULL, DEFAULT TRUE | Si es visible en vista pública |
| `vistas` | Integer | DEFAULT 0 | Contador de vistas (para popularidad) |

**Restricciones**:
- Solo uno de `id_cliente_creador` o `id_colaborador_creador` puede ser no-null (creador polimórfico)

**Relaciones**:
- `ManyToOne` → `Cliente`: Cliente creador (opcional)
- `ManyToOne` → `Colaborador`: Colaborador creador (opcional)
- `OneToMany` → `PlanActividad`: Actividades incluidas en el plan

---

#### 4.7.2 **PlanActividad** (Tabla: `plan_actividades`)
**Propósito**: Relación entre un plan y las actividades que lo componen, con orden y detalles personalizados.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | Long | PK, AUTO_INCREMENT | Identificador único |
| `id_plan` | Long | FK → Plan, NOT NULL | Plan al que pertenece |
| `id_actividad` | Long | FK → Actividad, NOT NULL | Actividad incluida |
| `orden` | Integer | NOT NULL | Orden en el itinerario (1, 2, 3...) |
| `horaSugerida` | String(20) | NULL | Hora sugerida para esta actividad (ej: "9:00 AM") |
| `notaPersonalizada` | TEXT | NULL | Nota o tip del creador para esta parada |

**Relaciones**:
- `ManyToOne` → `Plan`: Plan contenedor
- `ManyToOne` → `Actividad`: Actividad incluida en el plan

---

### 4.8 Entidades de Preguntas Frecuentes

#### 4.8.1 **PreguntaFrecuenteActividad** (Tabla: `pregunta_frecuente_actividad`)
**Propósito**: Preguntas frecuentes específicas de cada actividad para resolver dudas comunes de clientes.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | Long | PK, AUTO_INCREMENT | Identificador único |
| `id_actividad` | Long | FK → Actividad, NOT NULL | Actividad a la que pertenece la FAQ |
| `pregunta` | String(500) | NOT NULL | Pregunta frecuente |
| `respuesta` | TEXT | NOT NULL | Respuesta a la pregunta |
| `createdAt` | LocalDateTime | DEFAULT NOW | Fecha de creación |

**Relaciones**:
- `ManyToOne` → `Actividad`: Actividad propietaria de las FAQs

---

### 4.9 Entidades de Documentos

#### 4.9.1 **Documento** (Tabla: `documento`)
**Propósito**: Almacenamiento de archivos adjuntos asociados a usuarios.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | Long | PK, AUTO_INCREMENT | Identificador único |
| `nombreArchivo` | String | NOT NULL | Nombre del archivo subido |
| `tipoMime` | String | NOT NULL | Tipo MIME del archivo |
| `ruta` | String | NOT NULL | Ubicación física del archivo |
| `usuario_id` | Long | FK → Usuario, NULL | Usuario propietario (opcional) |

**Relaciones**:
- `ManyToOne` → `Usuario`: Usuario dueño del documento

---

### 4.10 Entidades de Configuración de Pagos

#### 4.10.1 **ConfiguracionPagos** (Tabla: `configuracion_pagos`)
**Propósito**: Almacena configuración global de la pasarela de pagos ePayco para el sistema.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | Long | PK, AUTO_INCREMENT | Identificador único |
| `publicKey` | String(255) | NOT NULL | Clave pública de ePayco |
| `privateKey` | String(255) | NOT NULL | Clave privada de ePayco |
| `pCustId` | String(255) | NOT NULL | ID del cliente en ePayco |
| `pKey` | String(255) | NOT NULL | Clave P de ePayco |
| `testMode` | Boolean | DEFAULT TRUE | Si está en modo sandbox (true) o producción (false) |
| `callbackUrl` | String(500) | NOT NULL | URL de callback para confirmación de pago |
| `responseUrl` | String(500) | NOT NULL | URL de respuesta exitosa |
| `confirmationUrl` | String(500) | NOT NULL | URL de confirmación de webhook |
| `updatedAt` | LocalDateTime | DEFAULT NOW | Última actualización de credenciales |

---

## 5. Relaciones del Sistema

### 5.1 Relaciones Jerárquicas
```
Usuario (1) ─── (1) Cliente
Usuario (1) ─── (1) Colaborador  
Usuario (1) ─── (1) Admin
```

### 5.2 Relaciones de Contenido
```
Colaborador (1) ─── (N) Actividad
Actividad (1) ─── (N) Disponibilidad
Actividad (1) ─── (N) ImagenActividad
Actividad (1) ─── (N) PatronDisponibilidad
```

### 5.3 Relaciones de Interacción
```
Cliente (1) ─── (N) Reserva ─── (1) Disponibilidad
Cliente (1) ─── (N) Comentario ─── (1) Actividad
Cliente (1) ─── (N) Favorito ─── (1) Actividad
```

### 5.4 Relaciones de Comunicación
```
Reserva (1) ─── (1) ConversacionChat
ConversacionChat (1) ─── (N) MensajeChat
Usuario (1) ─── (N) MensajeChat
```

### 5.5 Relaciones de Planes del Día
```
Cliente (1) ─── (N) Plan
Colaborador (1) ─── (N) Plan
Plan (1) ─── (N) PlanActividad ─── (1) Actividad
```

### 5.6 Relaciones de Preguntas Frecuentes
```
Actividad (1) ─── (N) PreguntaFrecuenteActividad
```

### 5.7 Relaciones de Pagos
```
Reserva (1) ─── (0..1) Transaccion de Pago
Reserva.ref_payco ─── Transacción en ePayco
```

### 6.1 Registro y Autenticación
**Datos Almacenados**:
- Usuario con credenciales encriptadas
- Token de verificación de email
- Fecha y hora de registro
- Rol asignado (Cliente, Colaborador, Admin)

**Entidades Involucradas**: `Usuario`, `Rol`, `Cliente`/`Colaborador`/`Admin`

---

### 6.2 Publicación de Actividad
**Datos Almacenados**:
- Información básica (título, descripción, precio)
- Ubicación geográfica (latitud, longitud)
- Imágenes (principal + galería)
- Normas, condiciones e inclusiones
- Idioma de la actividad
- Patrón de disponibilidad recurrente

**Entidades Involucradas**: `Actividad`, `ImagenActividad`, `Idioma`, `Categoria`, `PatronDisponibilidad`

---

### 6.3 Búsqueda y Descubrimiento
**Datos Consultados**:
- Actividades filtradas por categoría, precio, ubicación
- Calificaciones promedio y número de comentarios
- Disponibilidad en fechas específicas
- Historial de vistas (para personalización)

**Entidades Involucradas**: `Actividad`, `Categoria`, `Comentario`, `Disponibilidad`

---

### 6.4 Proceso de Reserva
**Datos Almacenados**:
- Reserva con cliente, actividad y disponibilidad
- Actualización de cupos disponibles
- Estado de la reserva (Pendiente, Confirmada, Completada, Cancelada)
- Fecha y hora de la reserva

**Entidades Involucradas**: `Reserva`, `Disponibilidad`, `Cliente`, `Actividad`

---

### 6.5 Sistema de Mensajería
**Datos Almacenados**:
- Conversación única por reserva
- Mensajes con remitente, contenido y timestamp
- Participantes (cliente y colaborador)
- Historial completo de comunicación

**Entidades Involucradas**: `ConversacionChat`, `MensajeChat`, `Reserva`

---

### 6.6 Reseñas y Calificaciones
**Datos Almacenados**:
- Comentario con texto y calificación numérica
- Cliente autor y actividad evaluada
- Fecha del comentario
- Actualización de calificación promedio de la actividad

**Entidades Involucradas**: `Comentario`, `Cliente`, `Actividad`

---

### 6.7 Personalización y Recomendaciones
**Datos Almacenados**:
- Preferencias del cliente (onboarding):
  - País de origen
  - Grupo de viaje
  - Rango de precio
  - Disponibilidad semanal
  - Categorías preferidas
- Historial de actividades vistas

### 6.8 Sistema de Pagos ePayco
**Datos Almacenados**:
- Referencias de transacción (ref_payco) en cada reserva
- Estado del pago (Pendiente, Confirmado, Fallido, Reembolsado)
- Precio total de la reserva (con comisión incluida)
- Configuración global de credenciales de ePayco

**Entidades Involucradas**: `Reserva`, `ConfiguracionPagos`

**Flujo**:
1. Cliente selecciona disponibilidad
2. Se crea Reserva en estado `PENDIENTE` con `estadoPago = PENDIENTE`
3. ePayco procesa transacción
4. Webhook confirma pago y actualiza `ref_payco` y `estadoPago`
5. Reserva se marca como `CONFIRMADA`

### 6.9 Preguntas Frecuentes por Actividad
**Datos Almacenados**:
- Pregunta y respuesta específicas de cada actividad
- Fecha de creación
- Relación con actividad

**Entidades Involucradas**: `PreguntaFrecuenteActividad`, `Actividad`

**Beneficios**:
- Reduce dudas comunes en clientes antes de reservar
- Mejora la experiencia del usuario
- Reduce carga de mensajes al colaborador

---

## 7. Consideraciones de Integridad y Validaciones

### 7.1 Validaciones de Negocio

| Validación | Entidad | Regla |
|------------|---------|-------|
| Email único | Usuario | No puede haber dos usuarios con el mismo email |
| Cédula única | Cliente | Cada cliente tiene cédula única |
| Favoritos únicos | Favorito | UNIQUE(id_cliente, id_actividad) - no duplicados |
| Cupos válidos | Disponibilidad | cuposDisponibles ≤ cuposTotales |
| Precio positivo | Actividad, Disponibilidad | precio > 0 |
| Calificación rango | Comentario | 1 ≤ calificacion ≤ 5 |
| Solo un creador | Plan | (id_cliente_creador NULL OR id_colaborador_creador NULL) |
| Orden secuencial | PlanActividad | orden ≥ 1 |
| Comisión rango | Admin | 0 < porcentajeComision ≤ 100 |

---

## 8. Optimizaciones de Consultas

### 8.1 Índices Recomendados

```sql
-- Búsqueda rápida por email
CREATE INDEX idx_usuario_email ON usuarios(email);

-- Búsqueda de actividades por categoría
CREATE INDEX idx_actividad_categoria ON actividad(id_categoria);

-- Búsqueda de actividades por colaborador
CREATE INDEX idx_actividad_colaborador ON actividad(id_colaborador);

-- Búsqueda de disponibilidades por fecha
CREATE INDEX idx_disponibilidad_fecha ON disponibilidad(fecha);

-- Búsqueda de reservas por cliente
CREATE INDEX idx_reserva_cliente ON reserva(id_cliente);

-- Búsqueda de comentarios por actividad
CREATE INDEX idx_comentario_actividad ON comentario(id_actividad);

-- Búsqueda de reservas por disponibilidad
CREATE INDEX idx_reserva_disponibilidad ON reserva(id_disponibilidad);

-- Búsqueda de planes públicos
CREATE INDEX idx_plan_publico ON planes(publico, fechaCreacion);

-- Búsqueda de mensajes por conversación
CREATE INDEX idx_mensaje_conversacion ON mensaje_chat(id_conversacion, fechaEnvio);
```

### 8.2 Estrategias de N+1

**Problema**: Obtener actividades con sus comentarios genera N+1 consultas.

**Solución**: Usar `LEFT JOIN FETCH` o `@Query` personalizado:
```java
@Query("""
    SELECT DISTINCT a FROM Actividad a 
    LEFT JOIN FETCH a.comentarios
    WHERE a.id_categoria = :categoriaId
""")
List<Actividad> findWithComments(@Param("categoriaId") Long categoriaId);
```

---

## 9. Seguridad de Datos

### 9.1 Campos Sensibles

| Campo | Entidad | Protección |
|-------|---------|------------|
| `password` | Usuario | Encriptado con BCrypt |
| `tokenVerificacion` | Usuario | Token único generado aleatoriamente |
| `privateKey` | ConfiguracionPagos | Nunca expuesto, solo en servidor |
| `ref_payco` | Reserva | Verificado contra firma digital de ePayco |

### 9.2 Auditoría

**Campos de auditoría recomendados**:
- `createdAt`: Fecha de creación (presente en la mayoría de entidades)
- `updatedAt`: Última modificación (presente en algunas)
- `createdBy`: Usuario que creó (futuro)
- `modifiedBy`: Usuario que modificó (futuro)

---

## 10. Historial de Cambios - v2.0

### Cambios Principales (v1.0 → v2.0)

| Fecha | Entidad/Campo | Cambio |
|-------|---|--------|
| 24 de abril | Plan | ✅ Agregar tabla `planes` y `plan_actividades` |
| 24 de abril | Reserva | ✅ Agregar campos `precioTotal`, `ref_payco`, `estadoPago` |
| 22 de abril | PreguntaFrecuenteActividad | ✅ Agregar tabla para FAQs |
| 22 de abril | ConfiguracionPagos | ✅ Agregar tabla para configuración ePayco |
| 26 de mayo | Múltiples | ✅ Actualizar índices y optimizaciones |

---

## 11. Notas Técnicas

### 11.1 Estrategia DDL

- **Modo**: `update` (Hibernate genera `ALTER TABLE` automáticamente)
- **Riesgo**: Datos pueden perderse si se ejecuta con `drop-and-create`
- **Recomendación**: Usar en desarrollo; en producción usar Flyway o Liquibase

### 11.2 Conexión a Base de Datos

```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/eta_db
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

### 11.3 Configuración de TimeZone

```properties
# Para que LocalDateTime y LocalDate funcionen correctamente
spring.datasource.url=jdbc:mysql://localhost:3306/eta_db?serverTimezone=America/Bogota
```

---

*Documento actualizado: 26 de mayo de 2026 — Alineado con PRD v2.0*
- Actividades favoritas
- Historial de reservas

**Entidades Involucradas**: `Cliente`, `Favorito`, `Actividad`, `Reserva`

---

### 6.8 Creación y Gestión de Planes del Día
**Datos Almacenados**:
- Plan con título, descripción y tipo temático
- Imagen de portada del plan
- Creador polimórfico (Cliente o Colaborador)
- Lista ordenada de actividades con:
  - Orden en el itinerario
  - Hora sugerida para cada actividad
  - Notas personalizadas por el creador
- Contador de vistas para popularidad
- Estado de visibilidad (público/privado)

**Consultas Realizadas**:
- Planes públicos más recientes (Top 5)
- Planes creados por un cliente específico
- Planes creados por un colaborador específico
- Detalle de plan con todas sus actividades ordenadas
- Incremento de contador de vistas

**Entidades Involucradas**: `Plan`, `PlanActividad`, `Actividad`, `Cliente`, `Colaborador`

---

## 7. Estrategias de Almacenamiento

### 7.1 Generación de Claves Primarias
- **Estrategia**: `IDENTITY` (auto-incremento nativo de MySQL)
- Todas las entidades usan `Long` para IDs

### 7.2 Manejo de Texto Largo
- **Campos de Texto**: `@Lob` para `TEXT` en MySQL
- Usado en: `descripcion`, `normas`, `incluye`, `condiciones`, `contenido`

### 7.3 Almacenamiento de Imágenes
- **Estrategia**: Ruta/nombre de archivo en base de datos
- Archivos físicos almacenados en sistema de archivos
- Límite de upload: 10MB por archivo

### 7.4 Datos Temporales
- **Tipo**: `LocalDateTime`, `LocalDate`, `LocalTime`
- No se almacenan zonas horarias (timezone-agnostic)
- Automático con `@PrePersist` para `createdAt`

### 7.5 Enumeraciones
- **Almacenamiento**: `@Enumerated(EnumType.STRING)`
- Almacenados como texto para legibilidad y mantenibilidad
- Ejemplos: `GrupoViaje`, `RangoPrecio`, `DisponibilidadSemana`

---

## 8. Seguridad y Privacidad

### 8.1 Datos Sensibles
- **Contraseñas**: Nunca almacenadas en texto plano (encriptadas con BCrypt)
- **Tokens de Verificación**: Temporales con fecha de expiración
- **Documentos**: Asociados a usuarios con control de acceso

### 8.2 Restricciones de Unicidad
- Email único por usuario
- Cédula única por cliente
- Una conversación por reserva
- Una actividad favorita por cliente (no duplicados)

### 8.3 Integridad Referencial
- Cascadas configuradas con `CascadeType.ALL` donde aplica
- `orphanRemoval = true` para entidades dependientes
- Eliminación lógica vs física según caso de uso

### 8.4 Verificación de Email
- Token de verificación almacenado temporalmente
- Expiración controlada mediante timestamp
- Campo `emailVerificado` para validar acceso

---

## 9. Optimizaciones y Rendimiento

### 9.1 Lazy Loading
- Relaciones `@ManyToOne` y `@OneToMany` configuradas con `FetchType.LAZY`
- Evita carga innecesaria de relaciones complejas
- Mejora performance en listados y búsquedas

### 9.2 Índices Implícitos
- Claves primarias indexadas automáticamente
- Claves foráneas indexadas por defecto
- Campos UNIQUE generan índices

### 9.3 Contadores Desnormalizados
- `totalVistas` en Actividad para algoritmos de personalización
- `totalTendencia` para ranking de actividades populares
- Evita COUNT queries costosos

---

## 10. Consideraciones de Escalabilidad

### 10.1 Preparación para Crecimiento
- Separación de perfil de Usuario facilita escalar tipos de cuenta
- Disponibilidades generadas desde patrones para eficiencia
- Favoritos y comentarios listos para sistemas de recomendación

### 10.2 Puntos de Extensión
- `Beneficio`: Tabla preparada para relación muchos-a-muchos con Actividad
- `Documento`: Genérico para múltiples tipos de archivos
- Campos `preferencias` (TEXT) permiten almacenar JSON estructurado

### 10.3 Auditoría y Trazabilidad
- Timestamps de creación (`createdAt`) en todas las entidades principales
- Timestamps de actualización (`updatedAt`) donde aplica
- Historial completo de mensajes en conversaciones

---

## 11. Mantenimiento y Actualizaciones

### 11.1 Estrategia de Migración
- `spring.jpa.hibernate.ddl-auto=update`
- Cambios de esquema aplicados automáticamente
- **Recomendación para producción**: Migrar a herramientas como Flyway o Liquibase

### 11.2 Logging SQL
- SQL formateado habilitado en desarrollo
- Facilita debugging de queries
- Desactivar en producción por performance

### 11.3 Respaldos
- Base de datos MySQL permite dumps regulares
- Estrategia de backup debe incluir archivos físicos (uploads)

---

## 12. Apéndices

### 12.1 Enums Definidos

#### GrupoViaje
```java
SOLO("Solo")
PAREJA("En pareja")
FAMILIA("Con familia")
AMIGOS("Con amigos")
VARIOS("Varía")
```

#### RangoPrecio
```java
ECONOMICO("Económico (< $50k)")
MODERADO("Moderado ($50k–$150k)")
PREMIUM("Premium (> $150k)")
```

#### DisponibilidadSemana
```java
FINDE("Fines de semana")
ENTRE_SEMANA("Entre semana")
AMBOS("Ambos")
```

### 12.2 Estados del Sistema

#### Estado de Reserva
- `Pendiente`: Recién creada, pendiente de confirmación
- `Confirmada`: Aceptada por el colaborador
- `Completada`: Actividad realizada
- `Cancelada`: Cancelada por cliente o colaborador

#### Estado de Disponibilidad
- `DISPONIBLE`: Acepta reservas
- `CANCELADO`: No disponible temporalmente
- `COMPLETADO`: Sesión ya realizada

#### Estado de Patrón de Disponibilidad
- `ACTIVO`: Generando disponibilidades
- `INACTIVO`: Desactivado temporalmente

---

## 13. Conclusión

Este documento especifica la arquitectura completa de base de datos de **ETA App**, diseñada para soportar un marketplace de experiencias turísticas escalable y con funcionalidades avanzadas de personalización. El modelo relacional garantiza:

✅ **Integridad de datos** mediante relaciones y restricciones bien definidas  
✅ **Flexibilidad** para evolucionar conforme crezcan las necesidades del negocio  
✅ **Performance** mediante estrategias de carga lazy y contadores desnormalizados  
✅ **Seguridad** con encriptación de datos sensibles y control de acceso basado en roles  
✅ **Trazabilidad** mediante timestamps y relaciones completas de auditoría

La base de datos está lista para soportar las operaciones core del negocio y preparada para integraciones futuras con sistemas de pago, notificaciones, y análisis de datos.
