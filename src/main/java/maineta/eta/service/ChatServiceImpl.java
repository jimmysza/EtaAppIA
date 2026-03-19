package maineta.eta.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.ConversacionChat;
import maineta.eta.entity.MensajeChat;
import maineta.eta.entity.Reserva;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.ConversacionChatRepository;
import maineta.eta.repository.MensajeChatRepository;
import maineta.eta.repository.ReservaRepository;

@Service
public class ChatServiceImpl implements ChatService {

    private final ConversacionChatRepository conversacionChatRepository;
    private final MensajeChatRepository mensajeChatRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioService usuarioService;
    private final ClienteService clienteService;
    private final ColaboradorService colaboradorService;

    public ChatServiceImpl(
            ConversacionChatRepository conversacionChatRepository,
            MensajeChatRepository mensajeChatRepository,
            ReservaRepository reservaRepository,
            UsuarioService usuarioService,
            ClienteService clienteService,
            ColaboradorService colaboradorService) {
        this.conversacionChatRepository = conversacionChatRepository;
        this.mensajeChatRepository = mensajeChatRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioService = usuarioService;
        this.clienteService = clienteService;
        this.colaboradorService = colaboradorService;
    }

    @Override
    public List<ConversacionChat> listarConversacionesCliente(String emailUsuario) {
        Usuario usuario = usuarioService.obtenerPorEmail(emailUsuario);
        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        return conversacionChatRepository.findByCliente_IdOrderByUpdatedAtDesc(cliente.getId());
    }

    @Override
    public List<ConversacionChat> listarConversacionesColaborador(String emailUsuario) {
        Usuario usuario = usuarioService.obtenerPorEmail(emailUsuario);
        Colaborador colaborador = colaboradorService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new EntityNotFoundException("Colaborador no encontrado"));

        return conversacionChatRepository.findByColaborador_IdColaboradorOrderByUpdatedAtDesc(colaborador.getIdColaborador());
    }

    @Override
    @Transactional
    public ConversacionChat obtenerOCrearConversacionDesdeReservaCliente(Long idReserva, String emailUsuario) {
        Reserva reserva = reservaRepository.findByIdReservaAndCliente_Usuario_Email(idReserva, emailUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada para este cliente"));

        return obtenerOCrearPorReserva(reserva);
    }

    @Override
    @Transactional
    public ConversacionChat obtenerOCrearConversacionDesdeReservaColaborador(Long idReserva, String emailUsuario) {
        Reserva reserva = reservaRepository.findByIdReservaAndActividad_Colaborador_Usuario_Email(idReserva, emailUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada para este colaborador"));

        return obtenerOCrearPorReserva(reserva);
    }

    private ConversacionChat obtenerOCrearPorReserva(Reserva reserva) {
        return conversacionChatRepository.findByReserva_IdReserva(reserva.getIdReserva())
                .orElseGet(() -> {
                    ConversacionChat conversacion = new ConversacionChat();
                    conversacion.setReserva(reserva);
                    conversacion.setCliente(reserva.getCliente());
                    conversacion.setColaborador(reserva.getActividad().getColaborador());
                    return conversacionChatRepository.save(conversacion);
                });
    }

    @Override
    public List<MensajeChat> listarMensajes(@NonNull Long idConversacion, String emailUsuario) {
        ConversacionChat conversacion = obtenerConversacionValida(idConversacion, emailUsuario);
        return mensajeChatRepository.findByConversacion_IdConversacionOrderByFechaEnvioAsc(conversacion.getIdConversacion());
    }

    @Override
    @Transactional
    public MensajeChat enviarMensaje(@NonNull Long idConversacion, String contenido, String emailUsuario) {
        ConversacionChat conversacion = obtenerConversacionValida(idConversacion, emailUsuario);

        if (contenido == null || contenido.trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacio");
        }

        Usuario remitente = usuarioService.obtenerPorEmail(emailUsuario);

        MensajeChat mensaje = new MensajeChat();
        mensaje.setConversacion(conversacion);
        mensaje.setRemitente(remitente);
        mensaje.setContenido(contenido.trim());

        MensajeChat guardado = mensajeChatRepository.save(mensaje);

        conversacion.setUpdatedAt(LocalDateTime.now());
        conversacionChatRepository.save(conversacion);

        return guardado;
    }

    private ConversacionChat obtenerConversacionValida(@NonNull Long idConversacion, String emailUsuario) {
        ConversacionChat conversacion = conversacionChatRepository.findById(idConversacion)
                .orElseThrow(() -> new EntityNotFoundException("Conversacion no encontrada"));

        String emailCliente = conversacion.getCliente().getUsuario().getEmail();
        String emailColaborador = conversacion.getColaborador().getUsuario().getEmail();

        if (!emailUsuario.equals(emailCliente) && !emailUsuario.equals(emailColaborador)) {
            throw new SecurityException("No tienes permisos para acceder a esta conversacion");
        }

        return conversacion;
    }
}
