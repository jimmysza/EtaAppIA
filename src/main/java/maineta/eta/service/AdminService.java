package maineta.eta.service;

import maineta.eta.entity.Admin;

public interface AdminService {
    Admin registrarAdmin(Admin admin);
    Admin buscarAdminPorId(long idAdmin);
    /*Optional<Admin> obtenerPorUsuario(Usuario usuario);*/

}
