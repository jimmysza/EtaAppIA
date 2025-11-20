package maineta.eta.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import maineta.eta.entity.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Admin findByIdAdmin(long idAdmin);
}
