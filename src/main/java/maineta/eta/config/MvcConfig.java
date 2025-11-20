package maineta.eta.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración personalizada de Spring MVC.
 * Se utiliza para mapear rutas de recursos estáticos que no están en la carpeta "static" de Spring.
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    /**
     * Este método permite registrar "manejadores de recursos estáticos".
     * En este caso, le decimos a Spring dónde buscar los archivos subidos.
     *
     * @param registry registro de manejadores de recursos estáticos
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cuando se acceda a una URL que empiece por "/uploads/**"
        // Spring buscará los archivos dentro de la carpeta "uploads/" ubicada en el proyecto.
        // Ejemplo: http://localhost:8080/uploads/imagen.jpg -> buscará en uploads/imagen.jpg
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/"); 
                // "file:" indica que la ruta es del sistema de archivos local
    }
}

