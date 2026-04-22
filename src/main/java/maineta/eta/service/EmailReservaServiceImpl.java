package maineta.eta.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import maineta.eta.entity.Reserva;

@Slf4j
@Service
public class EmailReservaServiceImpl implements EmailReservaService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:no-reply@eta.local}")
    private String mailFrom;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public EmailReservaServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void enviarEmailConfirmacionReserva(Reserva reserva) {
        try {
            String emailDestino = reserva.getCliente().getUsuario().getEmail();
            String nombreCliente = reserva.getCliente().getUsuario().getNombre();
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(mailFrom);
            helper.setTo(emailDestino);
            helper.setSubject("✅ Reserva Confirmada - " + reserva.getActividad().getTitulo());
            
            // Crear contexto Thymeleaf con datos de la reserva
            Context context = new Context(new Locale("es", "CO"));
            context.setVariable("nombreCliente", nombreCliente);
            context.setVariable("reserva", reserva);
            context.setVariable("actividad", reserva.getActividad());
            context.setVariable("disponibilidad", reserva.getDisponibilidad());
            context.setVariable("fechaReserva", formatearFecha(reserva.getFechaReserva().toLocalDate()));
            context.setVariable("fechaActividad", formatearFecha(reserva.getDisponibilidad().getFecha()));
            context.setVariable("appBaseUrl", appBaseUrl);
            
            // Generar HTML desde template
            String htmlContent = templateEngine.process("emails/confirmacion-reserva", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Email de confirmación enviado a: {}", emailDestino);
            
        } catch (MessagingException | MailException e) {
            log.error("Error al enviar email de confirmación de reserva ID {}: {}", 
                     reserva.getIdReserva(), e.getMessage(), e);
            // No lanzamos excepción para no interrumpir el flujo de la reserva
        }
    }

    @Override
    public void enviarEmailRecordatorioReserva(Reserva reserva) {
        try {
            String emailDestino = reserva.getCliente().getUsuario().getEmail();
            String nombreCliente = reserva.getCliente().getUsuario().getNombre();
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(mailFrom);
            helper.setTo(emailDestino);
            helper.setSubject("⏰ Recordatorio: Tu actividad es mañana - " + reserva.getActividad().getTitulo());
            
            // Crear contexto Thymeleaf
            Context context = new Context(new Locale("es", "CO"));
            context.setVariable("nombreCliente", nombreCliente);
            context.setVariable("reserva", reserva);
            context.setVariable("actividad", reserva.getActividad());
            context.setVariable("disponibilidad", reserva.getDisponibilidad());
            context.setVariable("fechaActividad", formatearFecha(reserva.getDisponibilidad().getFecha()));
            context.setVariable("appBaseUrl", appBaseUrl);
            
            // Generar HTML desde template
            String htmlContent = templateEngine.process("emails/recordatorio-reserva", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Email de recordatorio enviado a: {} para reserva ID {}", 
                    emailDestino, reserva.getIdReserva());
            
        } catch (MessagingException | MailException e) {
            log.error("Error al enviar email de recordatorio para reserva ID {}: {}", 
                     reserva.getIdReserva(), e.getMessage(), e);
        }
    }

    /**
     * Formatea una fecha en formato legible en español.
     */
    private String formatearFecha(LocalDate fecha) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", 
                                                                  new Locale("es", "CO"));
        return fecha.format(formatter);
    }
}
