package maineta.eta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import maineta.eta.entity.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Admin findByIdAdmin(long idAdmin);
    Optional<Admin> findTopByOrderByIdAdminAsc();
}
