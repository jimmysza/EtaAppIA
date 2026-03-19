package maineta.eta.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.apache.tika.Tika;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import maineta.eta.entity.Documento;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.DocumentoRepository;

@Service
public class DocumentoService {

    private final DocumentoRepository repo;

    public DocumentoService(DocumentoRepository repo) {
        this.repo = repo;
    }

    public void guardarDocumento(MultipartFile archivo, Usuario usuario) throws Exception {

        // ✅ Validar tamaño (5MB)
        if (archivo.getSize() > 5 * 1024 * 1024) {
            throw new Exception("El archivo supera los 5MB");
        }

        // ✅ Detectar MIME real
        Tika tika = new Tika();
        String mimeReal = tika.detect(archivo.getInputStream());

        List<String> permitidos = List.of(
                "application/pdf",
                "image/jpeg",
                "image/png"
        );

        if (!permitidos.contains(mimeReal)) {
            throw new Exception("Tipo de archivo no permitido");
        }

        // ✅ Guardar archivo
        String ruta = "uploads/" + UUID.randomUUID() + "_" + archivo.getOriginalFilename();
        Files.copy(archivo.getInputStream(), Paths.get(ruta));

        // ✅ Guardar en BD
        Documento doc = new Documento();
        doc.setNombreArchivo(archivo.getOriginalFilename());
        doc.setTipoMime(mimeReal);
        doc.setRuta(ruta);
        doc.setUsuario(usuario);

        repo.save(doc);
    }
}