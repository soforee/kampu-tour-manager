package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.LoginRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.UsuarioDTO;
import pe.edu.upeu.lp2_gp2.entity.UsuarioSistema;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.repository.UsuarioSistemaRepository;

@Service
public class AuthServiceImpl implements IAuthService {
    private final UsuarioSistemaRepository usuarioRepository;

    public AuthServiceImpl(UsuarioSistemaRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO login(LoginRequestDTO dto) {
        UsuarioSistema u = usuarioRepository.findByUsernameIgnoreCase(dto.username().trim())
                .orElseThrow(() -> new BusinessException("Usuario o contraseña incorrectos."));
        if (!dto.password().equals(u.getPasswordHash())) {
            throw new BusinessException("Usuario o contraseña incorrectos.");
        }
        if (!"ACTIVO".equals(u.getEstado())) {
            throw new BusinessException("Tu usuario está inactivo. Contacta al administrador.");
        }
        return UsuarioDTO.desde(u);
    }
}
