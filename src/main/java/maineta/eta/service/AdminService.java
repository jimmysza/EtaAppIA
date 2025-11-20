package maineta.eta.service;
import java.util.Optional;

import maineta.eta.entity.Admin;
import maineta.eta.entity.Usuario;

public interface AdminService {
    Admin registrarAdmin(Admin admin);
    Admin buscarAdminPorId(long idAdmin);
    /*Optional<Admin> obtenerPorUsuario(Usuario usuario);*/

}
