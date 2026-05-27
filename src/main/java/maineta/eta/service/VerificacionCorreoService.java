package maineta.eta.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import maineta.eta.entity.Usuario;
import maineta.eta.repository.UsuarioRepository;

@Service
public class VerificacionCorreoService {

    public enum EstadoVerificacion {
        VERIFICADA,
        YA_VERIFICADA,
        TOKEN_INVALIDO,
        TOKEN_EXPIRADO
    }

    private static final int HORAS_VIGENCIA_TOKEN = 24;

    private final UsuarioRepository usuarioRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${app.mail.from:onboarding@resend.dev}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    public VerificacionCorreoService(UsuarioRepository usuarioRepository, JavaMailSender mailSender,
            TemplateEngine templateEngine) {
        this.usuarioRepository = usuarioRepository;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void prepararVerificacion(Usuario usuario) {
        usuario.setEmailVerificado(Boolean.FALSE);
        usuario.setTokenVerificacion(UUID.randomUUID().toString());
        usuario.setTokenVerificacionExpiraEn(LocalDateTime.now().plusHours(HORAS_VIGENCIA_TOKEN));
    }

    public void enviarCorreoVerificacion(Usuario usuario) {
        if (smtpUsername == null || smtpUsername.isBlank() || smtpPassword == null || smtpPassword.isBlank()) {
            throw new IllegalStateException(
                    "SMTP no configurado: define spring.mail.username y spring.mail.password (o SMTP_USERNAME y SMTP_PASSWORD).");
        }

        if (usuario.getTokenVerificacion() == null || usuario.getTokenVerificacion().isBlank()) {
            throw new IllegalArgumentException("El usuario no tiene token de verificacion generado");
        }

        String link = appBaseUrl + "/registro/verificar?token=" + usuario.getTokenVerificacion();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(usuario.getEmail());
            helper.setSubject("Verifica tu cuenta en ETA");

            Context context = new Context();
            context.setVariable("nombreUsuario", usuario.getNombre());
            context.setVariable("enlaceVerificacion", link);
            context.setVariable("horasVigencia", HORAS_VIGENCIA_TOKEN);
            context.setVariable("appBaseUrl", appBaseUrl);

            String htmlContent = templateEngine.process("emails/verificacion-cuenta", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            String detalle = e.getMessage();
            throw new RuntimeException(
                    "No se pudo enviar el correo de verificacion. Revisa SMTP (host/puerto/usuario/clave). Detalle: "
                            + detalle,
                    e);
        }
    }

    @Transactional
    public EstadoVerificacion verificarCuenta(String token) {
        if (token == null || token.isBlank()) {
            return EstadoVerificacion.TOKEN_INVALIDO;
        }

        Usuario usuario = usuarioRepository.findByTokenVerificacion(token).orElse(null);
        if (usuario == null) {
            return EstadoVerificacion.TOKEN_INVALIDO;
        }

        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            return EstadoVerificacion.YA_VERIFICADA;
        }

        LocalDateTime expira = usuario.getTokenVerificacionExpiraEn();
        if (expira == null || expira.isBefore(LocalDateTime.now())) {
            return EstadoVerificacion.TOKEN_EXPIRADO;
        }

        usuario.setEmailVerificado(Boolean.TRUE);
        usuario.setTokenVerificacion(null);
        usuario.setTokenVerificacionExpiraEn(null);
        usuarioRepository.save(usuario);

        return EstadoVerificacion.VERIFICADA;
    }
}
