# ⚡ INICIO RÁPIDO: ePayco + ngrok

## 🎯 LO QUE NECESITAS HACER

### 1️⃣ VERIFICAR CREDENCIALES DE EPAYCO

**Las credenciales actuales pueden ser de ejemplo/tutorial.**

✅ **Ve a https://dashboard.epayco.co**
✅ **Copia TUS credenciales reales** (Configuración → Llaves API)
✅ **Actualiza `application.properties`** con tus credenciales

Ver detalles en: [EPAYCO_CREDENCIALES.md](EPAYCO_CREDENCIALES.md)

---

### 2️⃣ INSTALAR NGROK

```powershell
# Opción 1: Con Chocolatey
choco install ngrok

# Opción 2: Con winget
winget install ngrok

# Opción 3: Descarga manual
# https://ngrok.com/download
```

**Autenticar ngrok (solo una vez):**
```powershell
ngrok config add-authtoken TU_AUTHTOKEN_DE_NGROK
```

Obtén tu authtoken en: https://dashboard.ngrok.com/signup

---

### 3️⃣ INICIAR TODO

#### Opción A: Manual (paso a paso)

```powershell
# Terminal 1: Iniciar Spring Boot
mvnw spring-boot:run

# Terminal 2: Iniciar ngrok
ngrok http 8080

# Copiar la URL de ngrok (ej: https://abc123.ngrok-free.app)
# Actualizar checkout.html línea ~663:
const baseUrl = "https://abc123.ngrok-free.app";

# Probar en: http://localhost:8080
```

#### Opción B: Script automático (RECOMENDADO)

```powershell
# 1. Iniciar Spring Boot primero
mvnw spring-boot:run

# 2. En otra terminal, ejecutar script
.\start-epayco-dev.ps1
```

El script:
- ✅ Inicia ngrok automáticamente
- ✅ Captura la URL pública
- ✅ Actualiza checkout.html con la URL
- ✅ Muestra resumen de URLs importantes

---

### 4️⃣ PROBAR EPAYCO

1. Ve a http://localhost:8080
2. Inicia sesión como cliente
3. Busca una actividad
4. Selecciona fecha y horario
5. Clic en **"Pagar con ePayco"**

**Tarjeta de prueba exitosa:**
- Número: `4575623182290326`
- Fecha: `12/28`
- CVV: `123`

---

## 🐛 SI ALGO FALLA

### Error: "En este momento no es posible realizar esta transacción"

**Causa más probable:** Credenciales inválidas

1. Ve a https://dashboard.epayco.co
2. Copia tus credenciales reales
3. Actualiza `application.properties`
4. Reinicia Spring Boot

---

### Error: "ngrok not found"

1. Instala ngrok: `choco install ngrok`
2. O descarga de: https://ngrok.com/download
3. Autentica: `ngrok config add-authtoken TU_TOKEN`

---

### Widget se cierra inmediatamente

1. Abre la consola del navegador (F12)
2. Busca el log: `ePayco configuration: {...}`
3. Verifica que:
   - `key` tenga valor (no esté vacío)
   - `confirmation` apunte a ngrok (no localhost)
   - `amount` sea un número string (ej: "118000")

---

### Webhook no llega

1. Abre http://127.0.0.1:4040 (dashboard de ngrok)
2. Verifica que llegue `POST /cliente/pago/confirmacion`
3. Si no llega, verifica que `baseUrl` en checkout.html sea tu URL de ngrok

---

## 📚 DOCUMENTACIÓN COMPLETA

- [SETUP_NGROK_EPAYCO.md](SETUP_NGROK_EPAYCO.md) - Guía paso a paso completa
- [EPAYCO_CREDENCIALES.md](EPAYCO_CREDENCIALES.md) - Verificar y actualizar credenciales

---

## ✅ CHECKLIST

- [ ] Credenciales de ePayco actualizadas en `application.properties`
- [ ] Ngrok instalado y autenticado
- [ ] Spring Boot corriendo (`mvnw spring-boot:run`)
- [ ] Ngrok corriendo (`ngrok http 8080`)
- [ ] URL de ngrok copiada y actualizada en `checkout.html`
- [ ] Página recargada (F5)
- [ ] Widget de ePayco se abre correctamente
- [ ] Tarjeta de prueba funciona
- [ ] Webhook llega (visible en terminal de ngrok)

---

## 🚀 RESUMEN DE 30 SEGUNDOS

```powershell
# 1. Actualiza tus credenciales en application.properties
# 2. Inicia Spring Boot
mvnw spring-boot:run

# 3. En otra terminal, ejecuta
.\start-epayco-dev.ps1

# 4. Prueba en http://localhost:8080
```

**¿Dudas?** Lee [SETUP_NGROK_EPAYCO.md](SETUP_NGROK_EPAYCO.md) para más detalles.
