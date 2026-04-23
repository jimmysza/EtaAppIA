# Guía: Configurar ngrok para ePayco (Modo Test)

## 📋 CHECKLIST DE LO QUE TIENES

✅ **Credenciales de ePayco en application.properties**  
✅ **Modo test activado** (`epayco.test=true`)  
✅ **Widget de ePayco integrado en checkout.html**  
✅ **Webhook endpoint público** (`POST /cliente/pago/confirmacion`)  
✅ **SecurityConfig permite webhook sin autenticación**  

---

## 🚀 PASO 1: INSTALAR NGROK

### Opción A: Con Chocolatey (Windows)
```powershell
choco install ngrok
```

### Opción B: Descarga manual
1. Ve a https://ngrok.com/download
2. Descarga el `.zip` para Windows
3. Extrae `ngrok.exe` a una carpeta (ej: `C:\ngrok\`)
4. Agrega esa carpeta al PATH de Windows

### Opción C: Con winget
```powershell
winget install ngrok
```

---

## 🔑 PASO 2: CREAR CUENTA EN NGROK (Gratis)

1. Ve a https://dashboard.ngrok.com/signup
2. Crea una cuenta gratuita
3. Copia tu **authtoken** del dashboard

### Autenticar ngrok (solo una vez)
```powershell
ngrok config add-authtoken TU_AUTHTOKEN_AQUI
```

---

## ▶️ PASO 3: INICIAR TU APLICACIÓN

Abre una terminal en `C:\Users\jaime\Documents\Uni\EtaAppIA`:

```powershell
mvnw spring-boot:run
```

O desde VS Code, ejecuta la configuración de debug **"EtaAppApplication"**.

**Espera a que veas:**
```
Started EtaAppApplication in X.XXX seconds
```

---

## 🌐 PASO 4: INICIAR NGROK

**Abre OTRA terminal** (sin cerrar la del paso 3) y ejecuta:

```powershell
ngrok http 8080
```

**Verás algo como esto:**

```
Session Status                online
Account                       TuNombre (Plan: Free)
Version                       3.x.x
Region                        United States (us)
Latency                       -
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://abc123xyz.ngrok-free.app -> http://localhost:8080
```

---

## 📝 PASO 5: COPIAR LA URL DE NGROK

**Copia la URL** que empieza con `https://` (en el ejemplo: `https://abc123xyz.ngrok-free.app`)

⚠️ **IMPORTANTE:** Esta URL cambia cada vez que reinicias ngrok (en plan gratuito).

---

## ✏️ PASO 6: ACTUALIZAR checkout.html

Abre `src/main/resources/templates/cliente/checkout.html` y busca la línea ~663:

```javascript
const baseUrl = "https://elbow-pope-rebuff.ngrok-free.dev"; // ← CAMBIAR AQUÍ
```

**Reemplázala** con tu nueva URL de ngrok:

```javascript
const baseUrl = "https://abc123xyz.ngrok-free.app"; // ← Tu URL de ngrok
```

**Guarda el archivo** (NO necesitas reiniciar Spring Boot, Thymeleaf recarga automáticamente).

---

## 🧪 PASO 7: PROBAR EPAYCO

1. Ve a tu app: http://localhost:8080
2. Inicia sesión como cliente
3. Busca una actividad y ve al checkout
4. Selecciona fecha, horario y cantidad
5. Clic en **"Pagar con ePayco"**

### ✅ Si todo está bien:
- Se abre el widget de ePayco
- Muestra tu nombre, email y teléfono pre-llenados
- Acepta datos de tarjeta de prueba

### ❌ Si sigue fallando:
Revisa la consola del navegador (F12) y busca el log:

```javascript
ePayco configuration: {
  key: "2bd9054390390165a8e15290060c20e1",
  test: true,
  amount: "118000",
  confirmation: "https://abc123xyz.ngrok-free.app/cliente/pago/confirmacion"
}
```

**Verifica que `confirmation` sea tu URL de ngrok + `/cliente/pago/confirmacion`**

---

## 💳 PASO 8: DATOS DE TARJETA DE PRUEBA (ePayco Test Mode)

**Tarjeta de prueba exitosa:**
- Número: `4575623182290326`
- Fecha: Cualquier fecha futura (ej: `12/28`)
- CVV: `123`
- Cuotas: `1`

**Tarjeta de prueba rechazada:**
- Número: `4151611527583283`
- Fecha: `12/28`
- CVV: `123`

---

## 🔍 PASO 9: VERIFICAR WEBHOOK

Cuando completes el pago en ePayco, **abre la terminal donde está ngrok** y verás:

```
POST /cliente/pago/confirmacion  200 OK
GET  /cliente/pago/respuesta     302 Found
```

**Si no ves el POST:**
- ePayco no pudo enviar el webhook
- Verifica que tu URL de ngrok esté correcta en `baseUrl`
- Revisa en el dashboard de ngrok: http://127.0.0.1:4040

---

## 📊 MONITOREO EN TIEMPO REAL

**Dashboard de ngrok:** http://127.0.0.1:4040

Aquí puedes ver:
- Todas las peticiones HTTP que llegan a tu app
- Headers y body de cada request
- Respuestas de tu servidor
- **Muy útil para debug de webhooks**

---

## 🐛 TROUBLESHOOTING

### Error: "En este momento no es posible realizar esta transacción"

**Causa 1: Public key inválida**
- Verifica que `2bd9054390390165a8e15290060c20e1` sea tu public key real de ePayco
- Si es de pruebas, debe coincidir con tu cuenta de sandbox
- Si es de producción, debe coincidir con tu cuenta real

**Causa 2: Cuenta de ePayco no activada**
- Ve a https://dashboard.epayco.co
- Verifica que tu cuenta esté activa
- Revisa si tienes alguna alerta o configuración pendiente

**Causa 3: Cliente ID incorrecto**
- Verifica que `1579987` sea tu Client ID real
- Debe coincidir con el que ves en tu dashboard de ePayco

**Causa 4: URL de confirmación incorrecta**
- Debe ser: `https://TU_NGROK.ngrok-free.app/cliente/pago/confirmacion`
- NO debe ser: `http://localhost:8080/...` (no es accesible para ePayco)

### Error: "ngrok not found"
- Asegúrate de que `ngrok.exe` esté en el PATH de Windows
- O ejecuta ngrok desde la carpeta donde lo descargaste: `.\ngrok.exe http 8080`

### Error: "Account has reached connection limit"
- Plan gratuito de ngrok permite solo 1 túnel simultáneo
- Cierra otros túneles de ngrok con `Ctrl+C`
- Reinicia ngrok con `ngrok http 8080`

### Widget se cierra inmediatamente
- Revisa la consola del navegador (F12)
- Busca errores de CORS o configuración
- Verifica que `epaycoPublicKey` tenga valor (no esté vacío)

---

## 🎯 RESUMEN RÁPIDO

```powershell
# Terminal 1: Iniciar aplicación
cd C:\Users\jaime\Documents\Uni\EtaAppIA
mvnw spring-boot:run

# Terminal 2: Iniciar ngrok
ngrok http 8080

# Copiar URL de ngrok (ej: https://abc123.ngrok-free.app)
# Actualizar checkout.html línea ~663:
const baseUrl = "https://abc123.ngrok-free.app";

# Probar en navegador:
# http://localhost:8080
```

---

## 🔄 CADA VEZ QUE REINICIES NGROK

1. **Detén ngrok** (Ctrl+C)
2. **Vuelve a iniciarlo** (`ngrok http 8080`)
3. **Copia la NUEVA URL** (cambia cada vez en plan gratuito)
4. **Actualiza `baseUrl` en checkout.html**
5. **Recarga la página** (F5)

⚠️ **Para URL fija:** Actualiza a ngrok PRO ($8/mes) y configura un dominio estático.

---

## ✅ CHECKLIST FINAL

- [ ] Ngrok instalado y autenticado
- [ ] Aplicación corriendo en puerto 8080
- [ ] Ngrok corriendo (`ngrok http 8080`)
- [ ] URL de ngrok copiada
- [ ] `baseUrl` actualizado en checkout.html
- [ ] Página recargada (F5)
- [ ] Widget de ePayco se abre correctamente
- [ ] Datos de tarjeta de prueba ingresados
- [ ] Webhook recibido (visible en terminal de ngrok)
- [ ] Reserva creada en base de datos

---

## 📚 RECURSOS

- **Documentación ePayco:** https://docs.epayco.co
- **Ngrok Dashboard:** https://dashboard.ngrok.com
- **Tarjetas de prueba:** https://docs.epayco.co/payments/test-cards
- **Monitoreo de webhooks:** http://127.0.0.1:4040

---

## 🚀 PRODUCCIÓN (cuando esté listo)

1. Despliega tu app en un servidor con dominio público (ej: https://etaapp.com)
2. Actualiza `baseUrl` a tu dominio real
3. Cambia `epayco.test=false` en application.properties
4. Usa credenciales de producción de ePayco
5. Ya NO necesitas ngrok

---

**¿Dudas?** Revisa los logs en:
- Terminal de Spring Boot (errores del backend)
- Consola del navegador F12 (errores del frontend)
- Dashboard de ngrok http://127.0.0.1:4040 (tráfico HTTP)
