# PRD - Base de Datos ETA App

## 1. Introducción

### 1.1 Propósito del Documento
Este documento especifica la arquitectura, estructura y almacenamiento de datos de la plataforma **ETA App**, una aplicación que conecta turistas (clientes) con proveedores de experiencias locales (colaboradores). Define todas las entidades, relaciones, tipos de datos y estrategias de persistencia que soportan las funcionalidades core del negocio.

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
└─────────────┘       └──────┬───────┘       └─────────────┘
                             │
                ┌────────────┼────────────┐
                │            │            │
                ▼            ▼            ▼
         ┌──────────┐ ┌──────────┐ ┌──────────┐
         │Disponib. │ │Comentario│ │ImagenAct.│
         └─────┬────┘ └──────────┘ └──────────┘
               │
               ▼
         ┌──────────┐       ┌──────────────┐
         │  Reserva │──────►│Conversación  │
         │          │       │    Chat      │
         └──────────┘       └──────┬───────┘
                                   │
                                   ▼
                            ┌──────────────┐
                            │ MensajeChat  │
                            └──────────────┘
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
| `estado` | String | DEFAULT 'Pendiente' | Estado de la reserva |
| `cantidad` | Integer | NOT NULL | Número de cupos reservados |
| `fechaReserva` | LocalDateTime | NOT NULL | Fecha en que se realizó la reserva |

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

### 4.7 Entidades de Documentos

#### 4.7.1 **Documento** (Tabla: `documento`)
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

---

## 6. Casos de Uso de Almacenamiento

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
- Actividades favoritas
- Historial de reservas

**Entidades Involucradas**: `Cliente`, `Favorito`, `Actividad`, `Reserva`

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
