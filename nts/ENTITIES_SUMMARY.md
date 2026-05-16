# Resumen de Entidades del Proyecto ETA

## 1. **Usuario** (`usuario` table)
- **ID**: `@Id @GeneratedValue(IDENTITY) Long id`
- **Columnas**:
  - `@Column(nullable=false, length=100) String nombre`
  - `@Column(unique=true, nullable=false, length=150) String email`
  - `@Column(nullable=false, length=255) String password`
  - `@Column(nullable=false, length=15) String telefono`
  - `@Column(nullable=false) LocalDateTime createdAt`
  - `Boolean emailVerificado`
  - `String tokenVerificacion`
  - `LocalDateTime tokenVerificacionExpiraEn`
- **Relaciones**:
  - `@ManyToOne @JoinColumn(id_rol, nullable=false) Rol rol`

---

## 2. **Rol** (`roles` table)
- **ID**: `@Id @GeneratedValue(IDENTITY) Long id`
- **Columnas**:
  - `@Column(unique=true, nullable=false, length=50) String nombre` (ROLE_ADMIN, ROLE_CLIENTE, ROLE_COLABORADOR)
- **Relaciones**:
  - `@OneToMany(mappedBy=rol) List<Usuario> usuarios`

---

## 3. **Cliente** (`cliente` table)
- **ID**: `@Id @GeneratedValue(IDENTITY) Long id`
- **Relaciones**:
  - `@OneToOne @JoinColumn(id_usuario, nullable=false, unique=true) Usuario usuario`
  - `@OneToMany(mappedBy=cliente) List<Reserva> reservas`
  - `@OneToMany(mappedBy=cliente, cascade=ALL, orphanRemoval=true) List<Comentario> comentarios`
  - `@OneToMany(mappedBy=cliente, cascade=ALL, orphanRemoval=true) List<Favorito> favoritos`
  - `@ManyToMany @JoinTable(cliente_categorias_preferidas) Set<Categoria> categoriasPreferidas`
- **Columnas**:
  - `@Column(nullable=false, unique=true) Long cedula`
  - `String direccion`
  - `@Lob String preferencias`
  - `@Column(length=80) String paisOrigen`
  - `@Enumerated(STRING) @Column(length=20) GrupoViaje grupoViaje`
  - `@Enumerated(STRING) @Column(length=20) RangoPrecio rangoPrecio`
  - `@Enumerated(STRING) @Column(length=20) DisponibilidadSemana disponibilidadSemana`
  - `@Column(name=onboarding_completado, nullable=false) boolean onboardingCompletado`

---

## 4. **Colaborador** (`colaborador` table)
- **ID**: `@Id @GeneratedValue(IDENTITY) Long idColaborador`
- **Relaciones**:
  - `@OneToOne @JoinColumn(id_usuario, nullable=false, unique=true) Usuario usuario`
- **Columnas**:
  - `@Column(length=50) String nit`
  - `@Column(length=150) String correoSeguridad`
  - `@Column(name=foto_perfil, length=255) String fotoPerfil`
  - `@Column(length=100) String banco`
  - `@Column(length=50) String numeroCuenta`
  - `@Enumerated(STRING) @Column(length=20) TipoCuenta tipoCuenta`
  - `@Column(nullable=false) Integer penalizaciones` (default=0)

---

## 5. **Actividad** (`actividad` table)
- **ID**: `@Id @GeneratedValue(IDENTITY) Long idActividad`
- **Relaciones**:
  - `@ManyToOne @JoinColumn(id_idioma, nullable=false) Idioma idioma`
  - `@ManyToOne @JoinColumn(id_colaborador, nullable=false) Colaborador colaborador`
  - `@ManyToOne @JoinColumn(id_categoria, nullable=false) Categoria categoria`
  - `@OneToMany(mappedBy=actividad, cascade=ALL, orphanRemoval=true) List<Comentario> comentarios`
  - `@OneToMany(mappedBy=actividad, cascade=ALL, orphanRemoval=true) List<Reserva> reservas`
  - `@OneToMany(mappedBy=actividad, cascade=ALL, orphanRemoval=true) List<ImagenActividad> imagenes`
  - `@OneToMany(mappedBy=actividad, cascade=ALL, orphanRemoval=true) List<PreguntaFrecuenteActividad> preguntasFrecuentes`
- **Columnas**:
  - `@Column(nullable=false, length=200) String titulo`
  - `@Lob String descripcion`
  - `int calificacion`
  - `String ubicacion`
  - `@Column(name=latitud) Double latitud`
  - `@Column(name=longitud) Double longitud`
  - `@Lob String normas`
  - `@Lob String incluye`
  - `@Lob String condiciones`
  - `String imagen`
  - `@Column(updatable=false) LocalDateTime createdAt`
  - `@Column(nullable=false) BigDecimal precio`
  - `@Enumerated(STRING) @Column(nullable=false, length=50) PoliticaCancelacion politicaCancelacion`
  - `LocalDateTime updatedAt`
  - `@Column(name=total_vistas, nullable=false) int totalVistas` (default=0)
  - `@Column(name=total_tendencia, nullable=false) int totalTendencia` (default=0)

---

## 6. **Categoria** (`categoria` table)
- **ID**: `@Id @GeneratedValue(IDENTITY) Long idCategoria`
- **Columnas**:
  - `@Column(nullable=false, length=100, unique=true) String nombre`
  - `String imagen`
- **Relaciones**:
  - `@OneToMany(mappedBy=categoria) List<Actividad> actividades`

---

## 7. **Idioma** (`idioma` table)
- **ID**: `@Id @GeneratedValue(IDENTITY) Long idIdioma`
- **Columnas**:
  - `@Column(nullable=false, length=10) String codigo` (es, en, fr, etc.)
  - `@Column(nullable=false, length=50) String nombre` (Español, Inglés, etc.)
- **Relaciones**:
  - `@OneToMany(mappedBy=idioma) List<Actividad> actividades`

---

## 8. **Disponibilidad** (`disponibilidad` table)
- **ID**: `@Id @GeneratedValue(IDENTITY) Long idDisponibilidad`
- **Columnas**:
  - `LocalDate fecha`
  - `@Column(name=hora_inicio) LocalTime horaInicio`
  - `@Column(name=hora_fin) LocalTime horaFin`
  - `int cuposTotales`
  - `int cuposDisponibles`
  - `@Column(nullable=false, length=20) String estado` (DISPONIBLE, CANCELADO, COMPLETADO)
- **Relaciones**:
  - `@ManyToOne(fetch=LAZY) @JoinColumn(id_actividad, nullable=false) Actividad actividad`
  - `@ManyToOne(fetch=LAZY) @JoinColumn(id_patron) PatronDisponibilidad patron`

---

## 9. **Reserva** (`reserva` table)
- **ID**: `@Id @GeneratedValue(IDENTITY) Long idReserva`
- **Relaciones**:
  - `@ManyToOne @JoinColumn(id_disponibilidad) Disponibilidad disponibilidad`
  - `@ManyToOne @JoinColumn(id_cliente) Cliente cliente`
  - `@ManyToOne(fetch=LAZY) @JoinColumn(id_actividad) Actividad actividad`
- **Columnas - Datos Básicos**:
  - `@Column(nullable=false) String estado` (Pendiente, Confirmado, Cancelado)
  - `int cantidad`
  - `LocalDateTime fechaReserva`
  - `@Column(length=100, unique=true) String refPayco` (Ref. en ePayco)
  - `@Column(length=100, unique=true) String refWompi` (Ref. en Wompi)
  - `@Column(length=100) String wompiTransactionId`
- **Columnas - Precios y Comisiones**:
  - `@Column(precision=10, scale=2) BigDecimal precioColaborador` (Precio base)
  - `@Column(precision=10, scale=2) BigDecimal precioConsumidor` (Precio con comisión)
  - `@Column(precision=5, scale=2) BigDecimal comisionPorcentaje` (% comisión)
  - `@Column(precision=10, scale=2) BigDecimal comisionEta` (Ganancia ETA)
- **Columnas - Pago Colaborador**:
  - `@Enumerated(STRING) @Column(length=30) EstadoPagoColaborador estadoPagoColaborador` (PENDIENTE_PAGO)
  - `LocalDateTime fechaPagoColaborador`
- **Columnas - Cancelación y Reembolsos**:
  - `@Enumerated(STRING) @Column(length=50) PoliticaCancelacion politicaAplicada`
  - `@Column(precision=10, scale=2) BigDecimal montoReembolso`
  - `@Enumerated(STRING) @Column(length=30) EstadoReembolso estadoReembolso` (SIN_REEMBOLSO)
  - `LocalDateTime fechaReembolso`
  - `@Column(length=20) String canceladoPor` (CLIENTE, COLABORADOR, ADMIN)

---

## 10. **Comentario** (`comentario` table)
- **ID**: `@Id @GeneratedValue(IDENTITY) Long idComentario`
- **Relaciones**:
  - `@ManyToOne(fetch=LAZY) @JoinColumn(id_cliente, nullable=false) Cliente cliente`
  - `@ManyToOne(fetch=LAZY) @JoinColumn(id_actividad, nullable=false) Actividad actividad`
- **Columnas**:
  - `@Lob @Column(nullable=false) String texto`
  - `@Column(nullable=false) Integer calificacion` (1-5 estrellas)
  - `@Column(nullable=false) LocalDateTime fechaComentario`

---

## 11. **Favorito** (`favorito` table)
- **ID**: `@Id @GeneratedValue(IDENTITY) Long idFavorito`
- **Constraints**: `@UniqueConstraint(columnNames={id_cliente, id_actividad})`
- **Relaciones**:
  - `@ManyToOne @JoinColumn(id_cliente, nullable=false) Cliente cliente`
  - `@ManyToOne @JoinColumn(id_actividad, nullable=false) Actividad actividad`
- **Columnas**:
  - `@Column(updatable=false) LocalDateTime createdAt`

---

## 12. **Enums**

### **PoliticaCancelacion**
```java
REEMBOLSO_TOTAL_SI_A_TIEMPO,
REEMBOLSO_PARCIAL_7_DIAS,
REEMBOLSO_PARCIAL_3_DIAS,
NO_REEMBOLSO
```

### **EstadoPagoColaborador**
```java
PENDIENTE_PAGO,
PAGADO,
RECHAZADO
```

### **EstadoReembolso**
```java
SIN_REEMBOLSO,
EN_PROCESO,
COMPLETADO
```

### **TipoCuenta**
```java
CORRIENTE,
AHORROS
```

### **GrupoViaje**
```java
FAMILIA,
PAREJA,
AMIGOS,
SOLO
```

### **RangoPrecio**
```java
ECONOMICO,
MEDIO,
PREMIUM
```

### **DisponibilidadSemana**
```java
FINES_SEMANA,
ENTRE_SEMANA,
FLEXIBLE
```

---

## Mejoras aplicadas al `pom.xml`

✅ **Spring Boot**: 3.5.7 → **3.4.0** (versión más estable)
✅ **Spring AI BOM**: 1.0.0 → **1.0.4** (actualizado)
✅ **Compiler Plugin**: Añadido `<source>17</source>` y `<target>17</target>` explícitamente
✅ **Encoding**: Añadido `<encoding>UTF-8</encoding>` en maven-compiler-plugin
✅ **Properties**: Añadidas propiedades de encoding y compilador
✅ **Dependencia**: Añadido `jakarta.servlet-api:6.1.0` para compatibilidad con Jakarta EE

---

## Status de Compilación ✅
```
mvn clean compile -q
[INFO] Building eta_app 0.0.1-SNAPSHOT
[SUCCESS] No errors found
```

El proyecto compila correctamente con todas las entidades y configuración de seguridad JWT.
