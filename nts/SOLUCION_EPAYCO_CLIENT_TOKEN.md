# 🔴 SOLUCIÓN: "Client Token is not configured"

## 🎯 LO QUE HICE

### ✅ Cambios Aplicados

1. **Agregué validaciones en `checkout.html`:**
   - Verificación de que `ePayco` esté cargado antes de configurar el widget
   - Validación de que `epaycoPublicKey` no esté vacía
   - Logs de debug para diagnosticar el problema

2. **Moví event listeners dentro de `DOMContentLoaded`:**
   - Asegura que los elementos del DOM existan antes de registrar eventos
   - Previene errores de `null` en `btnPagar`

3. **Agregué logs de debug automáticos:**
   - Al cargar la página, la consola mostrará:
     - Valores recibidos desde el backend (epaycoPublicKey, clienteId, etc.)
     - Estado de carga del script de ePayco
     - Configuración que se enviará a ePayco

---

## 🔍 DIAGNÓSTICO

### PASO 1: Abrir la Consola del Navegador

1. Presiona **F12** en tu navegador
2. Ve a la pestaña **Console**
3. Recarga la página de checkout (**F5**)

### PASO 2: Buscar estos logs

**✅ Si todo está bien, deberías ver:**

```
📦 Datos recibidos desde Thymeleaf:
  epaycoPublicKey: "2bd9054390390165a8e15290060c20e1"
  epaycoTest: true
  clienteId: 123
  precio: 100000
  actividadId: 5

✅ ePayco script cargado correctamente

✅ ePayco está disponible y listo para usar
```

**❌ Si ves estos errores:**

```
❌ CRITICAL: ePayco script no está cargado
```

**SOLUCIÓN:** Verifica tu conexión a internet o desactiva bloqueadores de anuncios que puedan estar bloqueando `checkout.epayco.co`.

---

```
❌ ePayco Public Key no está configurada
```

**SOLUCIÓN:** Verifica que `application.properties` tenga:
```properties
epayco.public.key=${EPAYCO_PUBLIC_KEY:2bd9054390390165a8e15290060c20e1}
```

Y reinicia Spring Boot.

---

### PASO 3: Hacer clic en "Pagar con ePayco"

Cuando hagas clic, deberías ver:

```
🔑 Configurando ePayco con:
  publicKey: "2bd9054390390165a8e15290060c20e1"
  testMode: true
  total: 118000
  clienteId: 123
```

---

## 🐛 CAUSAS COMUNES DEL ERROR

### 1️⃣ Bloqueador de Anuncios

**Problema:** Extensiones como uBlock Origin o AdBlock pueden bloquear el script de ePayco.

**Solución:**
1. Desactiva temporalmente el bloqueador en tu sitio
2. O agrega `checkout.epayco.co` a la lista blanca

**Verificar:**
```
Ve a la pestaña Network (F12 → Network)
Busca: checkout.js
Si está en rojo o dice "blocked" → Es el bloqueador
```

---

### 2️⃣ Problema de Red/CORS

**Problema:** No se puede cargar el script desde `https://checkout.epayco.co/checkout.js`

**Solución:**
1. Verifica tu conexión a internet
2. Prueba abrir en el navegador: https://checkout.epayco.co/checkout.js
3. Si da error 404 o timeout → Problema de red o ePayco está caído

**Verificar en consola:**
```
Failed to load resource: net::ERR_BLOCKED_BY_CLIENT
o
Failed to load resource: net::ERR_NAME_NOT_RESOLVED
```

---

### 3️⃣ Public Key Vacía o Incorrecta

**Problema:** `epaycoPublicKey` está vacía, `null` o `undefined`

**Solución:**

#### A. Verifica `application.properties`
```properties
epayco.public.key=${EPAYCO_PUBLIC_KEY:2bd9054390390165a8e15290060c20e1}
```

Debe tener un valor por defecto después de los `:` o una variable de entorno configurada.

#### B. Reinicia Spring Boot
```powershell
# Detén la aplicación (Ctrl+C)
mvnw spring-boot:run
```

#### C. Verifica en la consola del navegador
```javascript
// Abre la consola (F12) y escribe:
console.log(epaycoPublicKey);

// Debe mostrar:
"2bd9054390390165a8e15290060c20e1"

// NO debe mostrar:
undefined
null
""
```

---

### 4️⃣ Script de ePayco Cargado Después del Código

**Problema:** El script `checkout.js` se carga después de que tu código intenta usar `ePayco`.

**Solución:** Ya está solucionado en el código actual:
- El script está en el `<head>` para que cargue primero
- Los event listeners están dentro de `DOMContentLoaded`
- Hay validación de que `ePayco` exista antes de usarlo

---

### 5️⃣ Credenciales de Prueba Inválidas

**Problema:** Las credenciales de ePayco no son válidas o no son tuyas.

**Solución:**

1. Ve a https://dashboard.epayco.co
2. Inicia sesión con TU cuenta
3. Ve a **Configuración → Llaves API**
4. Copia TUS credenciales reales
5. Actualiza `application.properties`:

```properties
epayco.public.key=TU_PUBLIC_KEY_REAL_AQUI
epayco.private.key=TU_PRIVATE_KEY_REAL_AQUI
epayco.client.id=TU_CLIENT_ID_REAL_AQUI
epayco.test=true
```

6. Reinicia Spring Boot

⚠️ **IMPORTANTE:** Las credenciales `2bd9054390390165a8e15290060c20e1` son de ejemplo. Si las copiaste de un tutorial, NO funcionarán en producción.

---

## ✅ CHECKLIST DE VERIFICACIÓN

Usa esta lista para diagnosticar:

- [ ] **Script de ePayco está cargando**
  ```
  F12 → Network → Buscar "checkout.js" → Status 200
  ```

- [ ] **Consola muestra log de Thymeleaf**
  ```
  📦 Datos recibidos desde Thymeleaf: { epaycoPublicKey: "..." }
  ```

- [ ] **epaycoPublicKey NO está vacía**
  ```
  console.log(epaycoPublicKey); // → Debe mostrar la key
  ```

- [ ] **ePayco está disponible**
  ```
  ✅ ePayco está disponible y listo para usar
  ```

- [ ] **Al hacer clic en "Pagar" muestra configuración**
  ```
  🔑 Configurando ePayco con: { publicKey: "...", testMode: true }
  ```

- [ ] **No hay errores rojos en consola**
  ```
  Verifica que no haya errores de "undefined" o "is not a function"
  ```

---

## 🚀 PRUEBA RÁPIDA

### Test 1: Verificar que ePayco esté cargado

Abre la consola del navegador (F12) en la página de checkout y escribe:

```javascript
typeof ePayco
```

**✅ Debe devolver:** `"object"`  
**❌ Si devuelve:** `"undefined"` → El script no está cargado

---

### Test 2: Verificar Public Key

En la misma consola, escribe:

```javascript
epaycoPublicKey
```

**✅ Debe devolver:** `"2bd9054390390165a8e15290060c20e1"` (o tu key real)  
**❌ Si devuelve:** `undefined` → El backend no está pasando la variable

---

### Test 3: Intentar configurar ePayco manualmente

En la consola, escribe:

```javascript
const handler = ePayco.checkout.configure({
    key: "2bd9054390390165a8e15290060c20e1",
    test: true
});

console.log(handler);
```

**✅ Debe devolver:** Un objeto con métodos (`open`, etc.)  
**❌ Si da error:** El problema está en las credenciales de ePayco

---

## 📋 SOLUCIONES POR SÍNTOMA

### Síntoma: "Client Token is not configured"

**Causas posibles:**
1. ✅ Public key vacía → Ver **PASO 3** (verificar application.properties)
2. ✅ Script no cargado → Ver **PASO 1** (bloqueador de anuncios)
3. ✅ Credenciales inválidas → Ver **PASO 5** (obtener credenciales reales)

---

### Síntoma: Widget no se abre

**Causas posibles:**
1. ✅ ePayco undefined → Ver **Test 1**
2. ✅ Error en configuración → Revisar consola por errores rojos
3. ✅ Botón no tiene event listener → Ya solucionado (movido a DOMContentLoaded)

---

### Síntoma: Widget se abre pero dice "Error"

**Causas posibles:**
1. ✅ Credenciales incorrectas → Obtén TUS credenciales de https://dashboard.epayco.co
2. ✅ Amount inválido → Verifica que `total` sea un string numérico (ej: "118000")
3. ✅ URL de confirmación incorrecta → Debe ser ngrok URL, no localhost

---

## 🎯 SIGUIENTE PASO

### Si el error persiste después de verificar todo:

1. **Copia y pega en la consola del navegador:**
   ```javascript
   console.log({
       ePaycoDisponible: typeof ePayco !== 'undefined',
       publicKey: epaycoPublicKey,
       testMode: epaycoTest,
       clienteId: clienteId
   });
   ```

2. **Envíame la salida** para diagnosticar el problema exacto

3. **Verifica la pestaña Network (F12):**
   - Busca `checkout.js`
   - ¿Está cargando con status 200?
   - ¿Hay algún error en rojo?

---

## 📚 RECURSOS ADICIONALES

- [Documentación oficial de ePayco](https://docs.epayco.co)
- [Dashboard de ePayco](https://dashboard.epayco.co)
- [SETUP_NGROK_EPAYCO.md](SETUP_NGROK_EPAYCO.md) - Configuración de ngrok
- [EPAYCO_CREDENCIALES.md](EPAYCO_CREDENCIALES.md) - Obtener credenciales

---

## 💡 TIP FINAL

El error "Client Token is not configured" es un **warning interno de ePayco**, no necesariamente un error crítico. Si el widget se abre correctamente después del warning, puedes ignorarlo.

**Pero si el widget NO se abre:**
1. Verifica que `epaycoPublicKey` tenga valor
2. Obtén TUS credenciales reales de https://dashboard.epayco.co
3. Actualiza `application.properties`
4. Reinicia Spring Boot
5. Recarga la página (F5)
