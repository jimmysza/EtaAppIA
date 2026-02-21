package maineta.eta.specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Categoria;
import maineta.eta.entity.Idioma;

/**
 * 🔹 Clase de especificaciones para filtros dinámicos de Actividad.
 * 
 * Permite construir consultas complejas combinando múltiples filtros
 * de manera dinámica usando JPA Criteria API.
 */
public class ActividadSpecification {

    /**
     * Genera una Specification con los filtros proporcionados.
     * 
     * @param titulo      Filtro por título (búsqueda parcial, case-insensitive)
     * @param idiomaId    Filtro por ID de idioma
     * @param categoriaId Filtro por ID de categoría
     * @param precioMin   Precio mínimo
     * @param precioMax   Precio máximo
     * @return Specification<Actividad> con los predicados combinados
     */
    public static Specification<Actividad> filtrar(
            String titulo,
            Long idiomaId,
            Long categoriaId,
            BigDecimal precioMin,
            BigDecimal precioMax) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // 🔎 FILTRO POR TITULO
            if (titulo != null && !titulo.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("titulo")),
                                "%" + titulo.toLowerCase() + "%"));
            }

            // 🌍 FILTRO POR IDIOMA (JOIN)
            if (idiomaId != null) {
                Join<Actividad, Idioma> idiomaJoin = root.join("idioma", JoinType.INNER);

                predicates.add(
                        cb.equal(idiomaJoin.get("idIdioma"), idiomaId));
            }

            // 🏷 FILTRO POR CATEGORIA (JOIN)
            if (categoriaId != null) {
                Join<Actividad, Categoria> categoriaJoin = root.join("categoria", JoinType.INNER);

                predicates.add(
                        cb.equal(categoriaJoin.get("idCategoria"), categoriaId));
            }

            // 💰 FILTRO POR RANGO DE PRECIO
            if (precioMin != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("precio"),
                                precioMin));
            }

            if (precioMax != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("precio"),
                                precioMax));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
