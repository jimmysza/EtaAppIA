/*
package maineta.eta.ta.eta.config;

import java.io.FileNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Manejo de archivos demasiado grandes
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException ex, RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("error", "El archivo es demasiado grande. El tamaño máximo permitido es 10 MB.");
        // Redirige a una página genérica; ajusta según tu flujo
        return "redirect:/colaborador/actividades";
    }

    // Manejo de acceso denegado (Spring Security)
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("error", "No tienes permisos para acceder a este recurso.");
        model.addAttribute("statusCode", HttpStatus.FORBIDDEN.value());
        return "error/error"; // Plantilla personalizada: templates/error/error.html
    }

    // Manejo de archivo no encontrado (por ejemplo, imagen eliminada)
    @ExceptionHandler(FileNotFoundException.class)
    public String handleFileNotFound(FileNotFoundException ex, Model model) {
        model.addAttribute("error", "El recurso solicitado no fue encontrado.");
        model.addAttribute("statusCode", HttpStatus.NOT_FOUND.value());
        return "error/error";
    }

    // Manejo genérico de errores
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        // Opcional: loggear el error real (usa un logger en producción)
        ex.printStackTrace();
        
        model.addAttribute("error", "Ocurrió un error inesperado. Por favor, inténtalo más tarde.");
        model.addAttribute("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return "error/error";
    }
}*/
