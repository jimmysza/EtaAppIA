# Configuración de Notificaciones por Email

## Descripción
El sistema ahora envía automáticamente dos tipos de correos electrónicos para las reservas:

1. **Email de Confirmación**: Se envía inmediatamente después de que un cliente realiza una reserva
2. **Email de Recordatorio**: Se envía automáticamente 24 horas antes de la fecha de la actividad

## Configuración SMTP

### Opción 1: Gmail (Recomendado para desarrollo)

1. **Generar App Password** (no usar tu contraseña normal):
   - Ve a https://myaccount.google.com/apppasswords
   - Inicia sesión con tu cuenta de Gmail
   - Selecciona "Correo" y "Otro (nombre personalizado)"
   - Escribe "ETA App" y haz clic en "Generar"
   - Copia la contraseña de 16 caracteres generada

2. **Configurar variables de entorno**:
   
   **En Windows (PowerShell):**
   ```powershell
   $env:SMTP_USERNAME="tu-email@gmail.com"
   $env:SMTP_PASSWORD="xxxx xxxx xxxx xxxx"
   ```

   **En Windows (CMD):**
   ```cmd
   set SMTP_USERNAME=tu-email@gmail.com
   set SMTP_PASSWORD=xxxx xxxx xxxx xxxx
   ```

   **En Linux/Mac:**
   ```bash
   export SMTP_USERNAME="tu-email@gmail.com"
   export SMTP_PASSWORD="xxxx xxxx xxxx xxxx"
   ```

3. **Para producción**, agrega las variables de entorno en tu servidor:
   - Heroku: `heroku config:set SMTP_USERNAME=xxx SMTP_PASSWORD=xxx`
   - AWS: Agrega en el archivo de configuración de Elastic Beanstalk
   - Docker: Agrega en docker-compose.yml o variables de entorno del contenedor

### Opción 2: SendGrid (Recomendado para producción)

SendGrid ofrece 100 emails gratis por día.

1. Crea una cuenta en https://sendgrid.com
2. Ve a Settings → API Keys → Create API Key
3. Copia la API Key generada
4. Modifica `application.properties`:
   ```properties
   spring.mail.host=smtp.sendgrid.net
   spring.mail.port=587
   spring.mail.username=apikey
   spring.mail.password=${SMTP_PASSWORD}
   ```
5. Configura la variable de entorno:
   ```bash
   export SMTP_PASSWORD="tu-sendgrid-api-key"
   ```

### Opción 3: Mailgun

1. Crea una cuenta en https://www.mailgun.com
2. Obtén tus credenciales SMTP
3. Modifica `application.properties`:
   ```properties
   spring.mail.host=smtp.mailgun.org
   spring.mail.port=587
   spring.mail.username=postmaster@tu-dominio.mailgun.org
   spring.mail.password=${SMTP_PASSWORD}
   ```

## Configurar URL Base de la Aplicación

Para que los links en los emails funcionen correctamente:

**Desarrollo:**
```bash
export APP_BASE_URL="http://localhost:8080"
```

**Producción:**
```bash
export APP_BASE_URL="https://tu-dominio.com"
```

## Verificar Configuración

### Probar Email de Confirmación

1. Inicia la aplicación
2. Realiza una nueva reserva como cliente
3. Deberías recibir un email de confirmación en la cuenta del cliente

### Probar Email de Recordatorio (Modo Manual)

Para probar sin esperar 24 horas:

1. Abre `RecordatorioReservaScheduler.java`
2. Descomenta la anotación del método `enviarRecordatoriosPrueba()`:
   ```java
   @Scheduled(fixedRate = 300000) // Cada 5 minutos
   public void enviarRecordatoriosPrueba() { ... }
   ```
3. Crea una reserva con disponibilidad para **mañana**
4. Espera 5 minutos y verifica que llegue el email
5. **Importante**: Vuelve a comentar la anotación después de probar

### Logs de Email

Los logs de envío de email aparecen en la consola:
```
INFO  - Email de confirmación enviado a: cliente@example.com
INFO  - Recordatorio enviado para reserva #123
```

Si hay errores:
```
ERROR - Error al enviar email de confirmación: Authentication failed
```

## Horario de Recordatorios

Los recordatorios se envían automáticamente todos los días a las **9:00 AM** (hora del servidor).

Para cambiar el horario, modifica el cron en `RecordatorioReservaScheduler.java`:
```java
@Scheduled(cron = "0 0 9 * * ?")  // 9:00 AM
```

**Ejemplos de expresiones cron:**
- `0 0 9 * * ?` - 9:00 AM todos los días
- `0 0 8 * * ?` - 8:00 AM todos los días
- `0 30 19 * * ?` - 7:30 PM todos los días
- `0 0 9,18 * * ?` - 9:00 AM y 6:00 PM todos los días

## Troubleshooting

### Error: "Authentication failed"
- Verifica que estés usando **App Password** de Gmail, no tu contraseña normal
- Verifica que las credenciales en las variables de entorno sean correctas
- Asegúrate de que la autenticación de 2 factores esté activada en Gmail

### Error: "Connection timeout"
- Verifica tu conexión a internet
- Si estás detrás de un firewall corporativo, puede estar bloqueando el puerto 587
- Intenta cambiar el puerto a 465 (SSL) en Gmail

### No llegan los emails
- Revisa la carpeta de SPAM
- Verifica que el email del remitente (`spring.mail.username`) sea válido
- Revisa los logs de la aplicación para ver errores

### Los emails se envían pero los links no funcionan
- Verifica que `APP_BASE_URL` esté configurado correctamente
- En producción debe ser `https://tu-dominio.com` (sin barra final)

## Templates de Email

Los templates HTML están en:
- `src/main/resources/templates/emails/confirmacion-reserva.html`
- `src/main/resources/templates/emails/recordatorio-reserva.html`

Puedes personalizarlos editando el HTML y CSS inline.

## Desactivar Emails (Solo para desarrollo)

Si necesitas desactivar temporalmente los emails durante el desarrollo, puedes:

1. **Comentar el envío en ClienteController**:
   ```java
   // emailReservaService.enviarEmailConfirmacionReserva(reservaGuardada);
   ```

2. **Desactivar la tarea programada**:
   Comenta `@EnableScheduling` en `EtaAppApplication.java`

## Limitaciones

### Gmail
- Máximo 500 emails/día con cuenta gratuita
- Máximo 100 destinatarios por email

### SendGrid
- 100 emails/día gratis
- Planes de pago desde $14.95/mes (40,000 emails/mes)

### Mailgun
- 5,000 emails/mes gratis primeros 3 meses
- Luego $35/mes (50,000 emails)

## Archivos Modificados/Creados

### Creados:
- `EmailReservaService.java` - Interface del servicio
- `EmailReservaServiceImpl.java` - Implementación con lógica de envío
- `RecordatorioReservaScheduler.java` - Tarea programada para recordatorios
- `templates/emails/confirmacion-reserva.html` - Template de confirmación
- `templates/emails/recordatorio-reserva.html` - Template de recordatorio

### Modificados:
- `ClienteController.java` - Integrado envío de email de confirmación
- `ReservaRepository.java` - Agregado query para buscar reservas por fecha
- `EtaAppApplication.java` - Habilitado @EnableScheduling
- `application.properties` - Agregada configuración SMTP

## Próximos Pasos Recomendados

1. ✅ Configurar variables de entorno SMTP
2. ✅ Probar email de confirmación
3. ✅ Probar email de recordatorio
4. 📧 Personalizar templates con logo y colores de la marca
5. 📊 Implementar tracking de emails (aperturas, clicks)
6. 💳 Agregar email de pago confirmado (si implementan pasarela de pago)
7. ❌ Agregar email de cancelación de reserva
