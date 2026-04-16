# SPECIFICACIÓN TÉCNICA – INTEGRACIÓN MODELO PREDICTIVO ETA

## 📌 CONTEXTO

El sistema ETA es una plataforma de gestión de actividades turísticas desarrollada con Spring Boot y Thymeleaf.

El sistema cuenta con una base de datos estructurada que incluye entidades como:
- actividades
- disponibilidad
- reservas

Se ha desarrollado un modelo predictivo en Weka para clasificar el nivel de ocupación de actividades turísticas, el cual ya fue entrenado y exportado como archivo `.model`.

---

## 🎯 OBJETIVO

Integrar el modelo predictivo dentro del backend del sistema para:

- Predecir el nivel de ocupación de una actividad
- Utilizar datos reales de la base de datos
- Exponer la predicción mediante un endpoint REST

---

## 🧠 MODELO PREDICTIVO

### Tipo:
Clasificación multiclase

### Variable objetivo:
nivel_ocupacion

### Clases:
- baja
- media
- alta
- agotado

---

## 📊 VARIABLES DE ENTRADA (FEATURES)

Las variables deben construirse a partir de la base de datos:

1. categoria → actividad.categoria
2. rango_precio → actividad.precio (transformado)
3. rango_hora → disponibilidad.hora_inicio (transformado)
4. tipo_dia → disponibilidad.fecha (transformado)
5. rango_cupos → disponibilidad.cupos_totales (transformado)

---

## 🔁 TRANSFORMACIONES NECESARIAS

### 1. RANGO PRECIO
- bajo: < 50000
- medio: 50000 – 150000
- alto: > 150000

---

### 2. RANGO HORA
Convertir hora_inicio (TIME) a:

- mañana: 06–12
- tarde: 12–18
- noche: 18–24

---

### 3. TIPO DÍA
A partir de fecha:

- fin_semana: sábado o domingo
- entre_semana: lunes a viernes

---

### 4. RANGO CUPOS
Basado en cupos_totales:

- bajo: 1–10
- medio: 11–30
- alto: >30

---

## ⚙️ IMPLEMENTACIÓN BACKEND

### 1. DEPENDENCIA WEKA

Agregar en pom.xml:

```xml
<dependency>
    <groupId>nz.ac.waikato.cms.weka</groupId>
    <artifactId>weka-stable</artifactId>
    <version>3.8.6</version>
</dependency>