package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.dto.LoginRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.UsuarioDTO;

public interface IAuthService {
    UsuarioDTO login(LoginRequestDTO dto);
}
