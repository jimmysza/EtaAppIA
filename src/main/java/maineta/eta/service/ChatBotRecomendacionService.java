package maineta.eta.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import maineta.eta.dto.ChatRecomendacionDTO;
import maineta.eta.dto.ChatRecomendacionDTO.FiltrosRecomendadosDTO;
import maineta.eta.entity.Categoria;
import maineta.eta.entity.Idioma;

/**
 * Servicio especializado en detectar intenciones de búsqueda en los mensajes del usuario
 * y generar recomendaciones estructuradas con filtros pre-aplicados.
 */
@Service
public class ChatBotRecomendacionService {

    private final CategoriaService categoriaService;
    private final IdiomaService idiomaService;

    public ChatBotRecomendacionService(CategoriaService categoriaService, IdiomaService idiomaService) {
        this.categoriaService = categoriaService;
        this.idiomaService = idiomaService;
    }

    /**
     * Analiza el mensaje del usuario y genera una recomendación estructurada
     * si detecta una intención de búsqueda.
     */
    public ChatRecomendacionDTO analizarYGenerarRecomendacion(String mensajeUsuario, String respuestaBot) {
        if (mensajeUsuario == null || mensajeUsuario.isBlank()) {
            return ChatRecomendacionDTO.sinRecomendacion(respuestaBot);
        }

        String mensajeLower = mensajeUsuario.toLowerCase();
        
        // Detectar categoría
        Categoria categoriaDetectada = detectarCategoria(mensajeLower);
        
        // Detectar idioma
        Idioma idiomaDetectado = detectarIdioma(mensajeLower);
        
        // Detectar rango de precio
        BigDecimal[] rangoPrecio = detectarRangoPrecio(mensajeLower);
        
        // Detectar palabras clave genéricas
        String palabraClave = extraerPalabraClave(mensajeLower);

        // Si no detectamos nada relevante, no hay recomendación
        if (categoriaDetectada == null && idiomaDetectado == null && 
            rangoPrecio[0] == null && rangoPrecio[1] == null && palabraClave == null) {
            return ChatRecomendacionDTO.sinRecomendacion(respuestaBot);
        }

        // Construir filtros recomendados
        FiltrosRecomendadosDTO filtros = new FiltrosRecomendadosDTO();
        
        if (categoriaDetectada != null) {
            filtros.setCategoriaId(categoriaDetectada.getIdCategoria());
            filtros.setCategoriaNombre(categoriaDetectada.getNombre());
        }
        
        if (idiomaDetectado != null) {
            filtros.setIdiomaId(idiomaDetectado.getIdIdioma());
            filtros.setIdiomaNombre(idiomaDetectado.getNombre());
        }
        
        if (rangoPrecio[0] != null) {
            filtros.setPrecioMin(rangoPrecio[0]);
        }
        
        if (rangoPrecio[1] != null) {
            filtros.setPrecioMax(rangoPrecio[1]);
        }
        
        if (palabraClave != null) {
            filtros.setNombre(palabraClave);
        }

        // Generar texto del botón
        String textoBoton = generarTextoBoton(categoriaDetectada, idiomaDetectado, palabraClave);
        filtros.setTextoBoton(textoBoton);

        return ChatRecomendacionDTO.conRecomendacion(respuestaBot, filtros);
    }

    /**
     * Detecta la categoría basándose en palabras clave del mensaje.
     */
    private Categoria detectarCategoria(String mensaje) {
        List<Categoria> todasCategorias = categoriaService.listarCategorias();
        
        for (Categoria categoria : todasCategorias) {
            String nombreCategoria = categoria.getNombre().toLowerCase();
            
            // Coincidencia exacta o parcial
            if (mensaje.contains(nombreCategoria)) {
                return categoria;
            }
            
            // Sinónimos por categoría
            if (categoria.getNombre().equalsIgnoreCase("Gastronomía") && 
                (mensaje.contains("comer") || mensaje.contains("comida") || 
                 mensaje.contains("restaurante") || mensaje.contains("gastronómica"))) {
                return categoria;
            }
            
            if (categoria.getNombre().equalsIgnoreCase("Aventura") && 
                (mensaje.contains("adrenalina") || mensaje.contains("emoción") || 
                 mensaje.contains("aventurera"))) {
                return categoria;
            }
            
            if (categoria.getNombre().equalsIgnoreCase("Cultural") && 
                (mensaje.contains("cultura") || mensaje.contains("historia") || 
                 mensaje.contains("museos") || mensaje.contains("histórica"))) {
                return categoria;
            }
            
            if (categoria.getNombre().equalsIgnoreCase("Naturaleza") && 
                (mensaje.contains("natural") || mensaje.contains("ecológica") || 
                 mensaje.contains("playa") || mensaje.contains("mar"))) {
                return categoria;
            }
            
            if (categoria.getNombre().equalsIgnoreCase("Deportes") && 
                (mensaje.contains("deporte") || mensaje.contains("ejercicio") || 
                 mensaje.contains("fitness"))) {
                return categoria;
            }
            
            if (categoria.getNombre().equalsIgnoreCase("Vida Nocturna") && 
                (mensaje.contains("noche") || mensaje.contains("fiesta") || 
                 mensaje.contains("bar") || mensaje.contains("disco"))) {
                return categoria;
            }
            
            if (categoria.getNombre().equalsIgnoreCase("Familiar") && 
                (mensaje.contains("niños") || mensaje.contains("familia") || 
                 mensaje.contains("infantil"))) {
                return categoria;
            }

            if (categoria.getNombre().equalsIgnoreCase("Romántico") && 
                (mensaje.contains("pareja") || mensaje.contains("romántica") || 
                 mensaje.contains("amor"))) {
                return categoria;
            }
        }
        
        return null;
    }

    /**
     * Detecta el idioma mencionado en el mensaje.
     */
    private Idioma detectarIdioma(String mensaje) {
        List<Idioma> todosIdiomas = idiomaService.listarIdiomas();
        
        for (Idioma idioma : todosIdiomas) {
            String nombreIdioma = idioma.getNombre().toLowerCase();
            
            if (mensaje.contains(nombreIdioma)) {
                return idioma;
            }
            
            // Sinónimos
            if (idioma.getNombre().equalsIgnoreCase("Inglés") && 
                (mensaje.contains("english") || mensaje.contains("ingles"))) {
                return idioma;
            }
            
            if (idioma.getNombre().equalsIgnoreCase("Francés") && 
                (mensaje.contains("french") || mensaje.contains("frances"))) {
                return idioma;
            }
        }
        
        return null;
    }

    /**
     * Detecta rango de precios mencionado en el mensaje.
     * Retorna [precioMin, precioMax]. Null si no se encuentra.
     */
    private BigDecimal[] detectarRangoPrecio(String mensaje) {
        BigDecimal[] rango = new BigDecimal[2]; // [min, max]
        
        // Patrones comunes: "menos de 100k", "menos de $100,000", "bajo 150000"
        Pattern patronMenosDe = Pattern.compile("(menos|bajo|menor)\\s+(de\\s+)?\\$?([0-9,]+)k?");
        Matcher matcherMenos = patronMenosDe.matcher(mensaje);
        
        if (matcherMenos.find()) {
            String numero = matcherMenos.group(3).replaceAll(",", "");
            BigDecimal valor = new BigDecimal(numero);
            
            // Si termina en "k", multiplicar por 1000
            if (matcherMenos.group().contains("k")) {
                valor = valor.multiply(new BigDecimal("1000"));
            }
            
            rango[1] = valor; // Precio máximo
        }
        
        // Patrones: "entre $50 y $100", "de 50000 a 100000"
        Pattern patronRango = Pattern.compile("(entre|de)\\s+\\$?([0-9,]+)k?\\s+(y|a)\\s+\\$?([0-9,]+)k?");
        Matcher matcherRango = patronRango.matcher(mensaje);
        
        if (matcherRango.find()) {
            String numeroMin = matcherRango.group(2).replaceAll(",", "");
            String numeroMax = matcherRango.group(4).replaceAll(",", "");
            
            BigDecimal min = new BigDecimal(numeroMin);
            BigDecimal max = new BigDecimal(numeroMax);
            
            if (matcherRango.group().contains("k")) {
                min = min.multiply(new BigDecimal("1000"));
                max = max.multiply(new BigDecimal("1000"));
            }
            
            rango[0] = min;
            rango[1] = max;
        }
        
        // Patrones: "barato", "económico"
        if (mensaje.contains("barato") || mensaje.contains("económico") || 
            mensaje.contains("economica") || mensaje.contains("low cost")) {
            rango[1] = new BigDecimal("100000"); // Menos de 100k
        }
        
        // Patrones: "premium", "lujoso", "exclusivo"
        if (mensaje.contains("premium") || mensaje.contains("lujo") || 
            mensaje.contains("exclusiv")) {
            rango[0] = new BigDecimal("200000"); // Más de 200k
        }
        
        return rango;
    }

    /**
     * Extrae palabra clave genérica del mensaje para búsqueda por nombre.
     */
    private String extraerPalabraClave(String mensaje) {
        // Palabras clave de búsqueda comunes
        String[] keywords = {
            "buceo", "snorkel", "kayak", "surf", "paseo", "tour", "cata",
            "taller", "clase", "show", "concierto", "museo", "castillo",
            "playa", "isla", "barco", "lancha", "crucero", "pesca",
            "fotografía", "yoga", "masaje", "spa", "ciclismo", "senderismo"
        };
        
        for (String keyword : keywords) {
            if (mensaje.contains(keyword)) {
                return keyword;
            }
        }
        
        return null;
    }

    /**
     * Genera el texto del botón de acción para la recomendación.
     */
    private String generarTextoBoton(Categoria categoria, Idioma idioma, String palabraClave) {
        if (categoria != null) {
            return "Ver actividades de " + categoria.getNombre();
        }
        
        if (idioma != null) {
            return "Ver actividades en " + idioma.getNombre();
        }
        
        if (palabraClave != null) {
            return "Buscar '" + palabraClave + "'";
        }
        
        return "Ver todas las actividades";
    }
}
