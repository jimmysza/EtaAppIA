package maineta.eta.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import maineta.eta.dto.ColaboradorPublicoDTO;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Usuario;

@Repository
public interface ColaboradorRepository extends JpaRepository<Colaborador,Integer> {

    Optional<Colaborador> findById(Integer integer);
    Optional<Colaborador> findByIdColaborador(Long idColaborador);
    Optional<Colaborador> findByNit(String nit);
    Optional<Colaborador> findByCorreoSeguridad(String correo);

    Optional<Colaborador> findByUsuario(Usuario usuario);

    @Query("""
            select new maineta.eta.dto.ColaboradorPublicoDTO(
                c.idColaborador,
                u.nombre,
                count(distinct a.idActividad),
                count(r.idReserva),
                coalesce(sum(r.cantidad), 0),
                coalesce(avg(distinct a.calificacion), 0.0)
            )
            from Colaborador c
            join c.usuario u
            left join Actividad a on a.colaborador = c
            left join Reserva r on r.actividad = a
            group by c.idColaborador, u.nombre
            order by count(r.idReserva) desc, coalesce(sum(r.cantidad), 0) desc,
                     count(distinct a.idActividad) desc, u.nombre asc
            """)
    List<ColaboradorPublicoDTO> findColaboradoresDestacados(Pageable pageable);

    @Query("""
            select new maineta.eta.dto.ColaboradorPublicoDTO(
                c.idColaborador,
                u.nombre,
                count(distinct a.idActividad),
                count(r.idReserva),
                coalesce(sum(r.cantidad), 0),
                coalesce(avg(distinct a.calificacion), 0.0)
            )
            from Colaborador c
            join c.usuario u
            left join Actividad a on a.colaborador = c
            left join Reserva r on r.actividad = a
            where c.idColaborador = :idColaborador
            group by c.idColaborador, u.nombre
            """)
    Optional<ColaboradorPublicoDTO> findResumenPublicoById(@Param("idColaborador") Long idColaborador);
}
