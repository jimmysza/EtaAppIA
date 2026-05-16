package maineta.eta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import maineta.eta.entity.PagoIntento;

/**
 * Repositorio para gestionar los intentos de pago de Wompi.
 */
@Repository
public interface PagoIntentoRepository extends JpaRepository<PagoIntento, Long> {

    /**
     * Busca un intento de pago por su referencia única.
     * 
     * @param reference Referencia del pago (formato: "ETA-{idDispo}-{idCliente}-{timestamp}")
     * @return Optional con el intento de pago si existe
     */
    Optional<PagoIntento> findByReference(String reference);

    /**
     * Busca un intento de pago por el ID de transacción de Wompi.  
     * 
     * @param wompiTransactionId ID de transacción asignado por Wompi
     * @return Optional con el intento de pago si existe
     */
    Optional<PagoIntento> findByWompiTransactionId(String wompiTransactionId);
}
