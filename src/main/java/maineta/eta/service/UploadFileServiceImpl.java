package maineta.eta.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 🔹 Implementación de IUploadFileService
 *
 * Esta clase maneja la lógica real para:
 *  - Crear la carpeta de uploads.
 *  - Guardar archivos subidos.
 *  - Cargar archivos guardados.
 *  - Eliminar archivos del sistema.
 *
 * Usa UUID para evitar que dos archivos con el mismo nombre se sobrescriban.
 */
@Service
public class UploadFileServiceImpl implements IUploadFileService {

    // 📂 Carpeta raíz donde se almacenarán los archivos
    private final Path rootFolder = Paths.get("uploads");

    /**
     * 🔹 Constructor
     * Se asegura de que la carpeta "uploads" exista al iniciar el servicio.
     * Si no existe, la crea automáticamente.
     */
    public UploadFileServiceImpl() {
        try {
            if (!Files.exists(rootFolder)) {
                Files.createDirectories(rootFolder);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error creando carpeta uploads: " + e.getMessage());
        }
    }

    /**
     * 🔹 Cargar un archivo desde el sistema y devolverlo como Resource.
     *
     * @param filename Nombre del archivo.
     * @return Resource que representa el archivo.
     * @throws MalformedURLException si la ruta no es válida.
     */
    @Override
    public Resource load(String filename) throws MalformedURLException {
        Path filePath = rootFolder.resolve(filename).toAbsolutePath(); // Ruta completa
        Resource resource = new UrlResource(filePath.toUri()); // Convertir a recurso accesible

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("No se pudo leer el archivo: " + filename);
        }
    }

    /**
     * 🔹 Guardar un archivo subido al servidor.
     *
     * Se usa un UUID para generar un nombre único y evitar colisiones.
     *
     * @param file Archivo que viene desde un formulario.
     * @return Nombre único con el que se guardó el archivo.
     * @throws IOException si ocurre un error al copiar.
     */
    @Override
    public String copy(MultipartFile file) throws IOException {

        String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        Path filePath = rootFolder.resolve(uniqueFileName).toAbsolutePath(); // Ruta destino

        Files.copy(file.getInputStream(), filePath); // Guardar físicamente
        
        return uniqueFileName;
    }

    /**
     * 🔹 Eliminar un archivo físico del sistema.
     *
     * @param filename Nombre del archivo a eliminar.
     * @return true si se eliminó, false si no existía o no se pudo borrar.
     */
    @Override
    public boolean delete(String filename) {
        if (filename != null && !filename.isEmpty()) {
            Path filePath = rootFolder.resolve(filename).toAbsolutePath();
            File file = filePath.toFile();
            if (file.exists() && file.canRead()) {
                return file.delete();
            }
        }
        return false;
    }
}
