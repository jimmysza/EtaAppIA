package maineta.eta.service;

import java.io.IOException;
import java.net.MalformedURLException;

import org.springframework.core.io.Resource;  // ✅ Permite manejar archivos como recursos en Spring
import org.springframework.web.multipart.MultipartFile; // ✅ Representa un archivo subido en un formulario

/**
 * 🔹 Interfaz IUploadFileService
 *
 * Define los métodos necesarios para gestionar archivos subidos al servidor.
 * Sirve como contrato para cualquier clase que implemente esta lógica.
 *
 * 📌 Operaciones principales:
 *  - Cargar un archivo desde el sistema (load).
 *  - Guardar una copia de un archivo subido (copy).
 *  - Eliminar un archivo físico del sistema (delete).
 */
public interface IUploadFileService {

    /**
     * 🔹 Cargar un archivo desde el sistema de archivos.
     *
     * @param filename Nombre del archivo que se desea recuperar.
     * @return Un objeto Resource que representa el archivo cargado.
     * @throws MalformedURLException Si la ruta del archivo no es válida.
     *
     * 📌 Ejemplo de uso: Mostrar imágenes almacenadas en el servidor.
     */
    Resource load(String filename) throws MalformedURLException;

    /**
     * 🔹 Guardar un archivo en el sistema (hacer una copia del archivo subido).
     *
     * @param file Archivo subido desde un formulario (tipo MultipartFile).
     * @return El nombre del archivo que se guardó en el servidor.
     * @throws IOException Si ocurre un error al copiar el archivo.
     *
     * 📌 Ejemplo de uso: Guardar una imagen de perfil o archivo adjunto.
     */
    String copy(MultipartFile file) throws IOException;

    /**
     * 🔹 Eliminar un archivo del sistema.
     *
     * @param filename Nombre del archivo que se quiere eliminar.
     * @return true si el archivo fue eliminado, false si no existía o no se pudo eliminar.
     *
     * 📌 Ejemplo de uso: Cuando un usuario borra una actividad que tenía imagen asociada.
     */
    boolean delete(String filename);
}

