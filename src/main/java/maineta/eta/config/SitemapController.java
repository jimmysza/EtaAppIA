/*
package maineta.eta.config;

import maineta.eta.service.ActividadService;
import maineta.eta.service.CategoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SitemapController {

    private final ActividadService actividadService;
    private final CategoriaService categoriaService;

    public SitemapController(ActividadService actividadService,
                             CategoriaService categoriaService) {
        this.actividadService = actividadService;
        this.categoriaService = categoriaService;
    }

    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public String sitemap() {

        String baseUrl = "https://tudominio.com";

        StringBuilder xml = new StringBuilder();
        xml.append("""
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
        """);

        // HOME
        xml.append(url(baseUrl + "/", "daily", "1.0"));

        // ACTIVIDADES
        actividadService.findAllIndexables().forEach(a -> {
            xml.append(url(
                baseUrl + "/actividad/" + a.getSlug(),
                "weekly",
                "0.9"
            ));
        });

        // CATEGORÍAS (solo indexables)
        categoriaService.findIndexables().forEach(c -> {
            xml.append(url(
                baseUrl + "/actividades/" + c.getSlug(),
                "weekly",
                "0.6"
            ));
        });

        xml.append("</urlset>");
        return xml.toString();
    }

    private String url(String loc, String freq, String priority) {
        return """
            <url>
              <loc>%s</loc>
              <changefreq>%s</changefreq>
              <priority>%s</priority>
            </url>
        """.formatted(loc, freq, priority);
    }
}
*/
