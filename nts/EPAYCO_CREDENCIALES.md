# Credenciales de ePayco - Verificación

## 🔑 TUS CREDENCIALES ACTUALES

```
Public Key:  2bd9054390390165a8e15290060c20e1
Private Key: de47b68c79ebc23acc5f039985c18692
Client ID:   1579987
Modo:        TEST (true)
```

---

## ⚠️ IMPORTANTE: ¿SON TUYAS?

Estas credenciales **deben ser de tu cuenta de ePayco**. Si las copiaste de un tutorial o ejemplo, **NO funcionarán**.

### ✅ Cómo obtener tus credenciales reales:

1. Ve a https://dashboard.epayco.co
2. Inicia sesión con tu cuenta
3. Ve a **Configuración → Llaves API**
4. Encontrarás:
   - **Public Key** (comienza con números/letras)
   - **Private Key** (comienza con números/letras)
   - **Client ID** (número)

### 🧪 Modo TEST vs PRODUCCIÓN

**Sandbox/Test:**
- Usas credenciales de prueba
- No se cobran tarjetas reales
- Puedes usar tarjetas de prueba
- Ideal para desarrollo

**Producción:**
- Usas credenciales reales
- Se cobran tarjetas de verdad
- Solo tarjetas reales funcionan
- Para cuando tu app esté lista

---

## 🔍 VERIFICAR SI TUS CREDENCIALES SON VÁLIDAS

### Opción 1: Desde el Dashboard de ePayco
1. Ve a https://dashboard.epayco.co/configuracion/llaves
2. Compara las llaves que ves con las de tu `application.properties`
3. Si NO coinciden, actualiza `application.properties`

### Opción 2: Prueba con curl
```bash
curl -X POST https://secure.epayco.co/validation/v1/reference/create \
  -H "Content-Type: application/json" \
  -d '{
    "public_key": "2bd9054390390165a8e15290060c20e1",
    "test": "true"
  }'
```

**Si la respuesta es `{"success": true}` → Credenciales válidas**  
**Si la respuesta es `{"success": false}` → Credenciales inválidas**

---

## 🛠️ ACTUALIZAR CREDENCIALES (si es necesario)

Si descubres que las credenciales están mal:

### 1. Edita `application.properties`
```properties
epayco.public.key=TU_PUBLIC_KEY_REAL_AQUI
epayco.private.key=TU_PRIVATE_KEY_REAL_AQUI
epayco.client.id=TU_CLIENT_ID_REAL_AQUI
epayco.test=true
```

### 2. Edita `.vscode/launch.json` (para debug mode)
```json
"EPAYCO_PUBLIC_KEY": "TU_PUBLIC_KEY_REAL_AQUI",
"EPAYCO_PRIVATE_KEY": "TU_PRIVATE_KEY_REAL_AQUI",
"EPAYCO_CLIENT_ID": "TU_CLIENT_ID_REAL_AQUI"
```

### 3. Reinicia la aplicación
```powershell
# Detén la app (Ctrl+C)
mvnw spring-boot:run
```

---

## 🎯 CHECKLIST DE CONFIGURACIÓN EPAYCO

- [ ] **Cuenta creada en ePayco** (https://dashboard.epayco.co)
- [ ] **Llaves API copiadas** desde el dashboard
- [ ] **Public Key actualizada** en application.properties
- [ ] **Private Key actualizada** en application.properties
- [ ] **Client ID actualizado** en application.properties
- [ ] **Modo test activado** (`epayco.test=true`)
- [ ] **Aplicación reiniciada** tras cambios

---

## 🔐 SEGURIDAD

⚠️ **NUNCA subas credenciales reales a GitHub**

### Opción 1: Variables de entorno (RECOMENDADO)
```properties
epayco.public.key=${EPAYCO_PUBLIC_KEY:default_value}
epayco.private.key=${EPAYCO_PRIVATE_KEY:default_value}
epayco.client.id=${EPAYCO_CLIENT_ID:default_value}
```

### Opción 2: Archivo .env (no versionado)
1. Crea `.env` en la raíz del proyecto
2. Agrega a `.gitignore`:
   ```
   .env
   application-local.properties
   ```

---

## 📋 RESUMEN

**Para que ePayco funcione necesitas:**

1. ✅ **Credenciales válidas de TU cuenta de ePayco**
2. ✅ **Credenciales en `application.properties`**
3. ✅ **Modo test activado** (`epayco.test=true`)
4. ✅ **Ngrok con URL pública** (ver SETUP_NGROK_EPAYCO.md)
5. ✅ **URL de ngrok en `baseUrl` de checkout.html**

**Si el widget dice "no es posible realizar esta transacción":**
- 🔑 Verifica que las credenciales sean tuyas (no de un tutorial)
- 🌐 Verifica que `confirmation` apunte a ngrok (no localhost)
- 🧪 Verifica que estés en modo test con credenciales de test
- 📊 Revisa el dashboard de ePayco por alertas/bloqueos

---

**¿Las credenciales actuales son de un tutorial?**
Ve a https://dashboard.epayco.co y obtén las tuyas propias. ePayco NO permite usar credenciales de otras cuentas por seguridad.
