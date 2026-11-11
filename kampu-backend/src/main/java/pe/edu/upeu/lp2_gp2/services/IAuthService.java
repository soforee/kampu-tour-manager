package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.dto.AfiliacionRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.LoginRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.RegistroRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.SesionDTO;

public interface IAuthService {
    SesionDTO login(LoginRequestDTO dto);
    SesionDTO registrar(RegistroRequestDTO dto);
    SesionDTO afiliar(AfiliacionRequestDTO dto);
    void logout(String token);

    /** Sesión vigente del token con sus permisos actualizados (al recargar la página) */
    SesionDTO sesion(String token);
}
