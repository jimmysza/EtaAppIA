package maineta.eta.service;

import java.math.BigDecimal;

import maineta.eta.entity.Admin;

public interface AdminService {
    Admin registrarAdmin(Admin admin);
    Admin buscarAdminPorId(long idAdmin);
    Admin obtenerAdminPrincipal();
    Admin actualizarPorcentajeComision(BigDecimal porcentajeComision);
    Admin actualizarHorasCancelacion(Integer horasCancelacion);
    /*Optional<Admin> obtenerPorUsuario(Usuario usuario);*/

}
