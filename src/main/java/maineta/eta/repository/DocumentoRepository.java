package maineta.eta.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import maineta.eta.entity.Documento;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

}
