package maineta.eta.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${spring.mail.username:no-reply@eta.local}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    public VerificacionCorreoService(UsuarioRepository usuarioRepository, JavaMailSender mailSender) {
        this.usuarioRepository = usuarioRepository;
        this.mailSender = mailSender;
    }

    public void prepararVerificacion(Usuario usuario) {
        usuario.setEmailVerificado(Boolean.FALSE);
        usuario.setTokenVerificacion(UUID.randomUUID().toString());
        usuario.setTokenVerificacionExpiraEn(LocalDateTime.now().plusHours(HORAS_VIGENCIA_TOKEN));
    }

    public void enviarCorreoVerificacion(Usuario usuario) {
        if (smtpUsername == null || smtpUsername.isBlank() || smtpPassword == null || smtpPassword.isBlank()) {
            throw new IllegalStateException(
                    "SMTP no configurado: define spring.mail.username y spring.mail.password (o SMTP_USERNAME y SMTP_PASSWORD)."
            );
        }

        if (usuario.getTokenVerificacion() == null || usuario.getTokenVerificacion().isBlank()) {
            throw new IllegalArgumentException("El usuario no tiene token de verificacion generado");
        }

        String link = appBaseUrl + "/registro/verificar?token=" + usuario.getTokenVerificacion();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(usuario.getEmail());
        message.setSubject("Verifica tu cuenta en ETA");
        message.setText(
                "Hola " + usuario.getNombre() + ",\n\n"
                        + "Gracias por registrarte. Para activar tu cuenta, haz clic en este enlace:\n"
                        + link + "\n\n"
                        + "Este enlace expirara en " + HORAS_VIGENCIA_TOKEN + " horas.\n\n"
                        + "Si no solicitaste esta cuenta, ignora este mensaje.");

        try {
            mailSender.send(message);
        } catch (MailException e) {
            String detalle = e.getMostSpecificCause() != null && e.getMostSpecificCause().getMessage() != null
                    ? e.getMostSpecificCause().getMessage()
                    : e.getMessage();
            throw new RuntimeException(
                    "No se pudo enviar el correo de verificacion. Revisa SMTP (host/puerto/usuario/clave). Detalle: "
                            + detalle,
                    e
            );
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
