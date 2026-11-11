package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.dto.LoginRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.RegistroRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.SesionDTO;

public interface IAuthService {
    SesionDTO login(LoginRequestDTO dto);
    SesionDTO registrar(RegistroRequestDTO dto);
    void logout(String token);

    /** Sesión vigente del token con sus permisos actualizados (al recargar la página) */
    SesionDTO sesion(String token);
}
