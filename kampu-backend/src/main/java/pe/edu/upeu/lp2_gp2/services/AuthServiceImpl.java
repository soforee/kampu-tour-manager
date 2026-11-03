package pe.edu.upeu.lp2_gp2.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.LoginRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.LoginResponseDTO;
import pe.edu.upeu.lp2_gp2.dto.UsuarioDTO;
import pe.edu.upeu.lp2_gp2.entity.UsuarioSistema;
import pe.edu.upeu.lp2_gp2.exception.ForbiddenException;
import pe.edu.upeu.lp2_gp2.exception.UnauthorizedException;
import pe.edu.upeu.lp2_gp2.repository.UsuarioSistemaRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthServiceImpl implements IAuthService {
    private record Sesion(Long usuarioId, LocalDateTime vence) {
    }

    private final UsuarioSistemaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    @Value("${tour.sesion-horas:12}")
    private int sesionHoras;

    public AuthServiceImpl(UsuarioSistemaRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        UsuarioSistema u = usuarioRepository.findByUsernameIgnoreCase(dto.username().trim())
                .orElseThrow(() -> new UnauthorizedException("Usuario o contraseña incorrectos."));
        if (!passwordEncoder.matches(dto.password(), u.getPasswordHash())) {
            throw new UnauthorizedException("Usuario o contraseña incorrectos.");
        }
        if (!"ACTIVO".equals(u.getEstado())) {
            throw new ForbiddenException("Tu usuario está inactivo. Contacta al administrador.");
        }
        String token = UUID.randomUUID().toString();
        sesiones.put(token, new Sesion(u.getUsuarioId(), LocalDateTime.now().plusHours(sesionHoras)));
        sesiones.values().removeIf(s -> s.vence().isBefore(LocalDateTime.now()));
        return new LoginResponseDTO(token, UsuarioDTO.desde(u));
    }

    @Override
    public void logout(String token) {
        if (token != null) sesiones.remove(token);
    }

    @Override
    public Optional<Long> usuarioDeToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        Sesion s = sesiones.get(token);
        if (s == null) return Optional.empty();
        if (s.vence().isBefore(LocalDateTime.now())) {
            sesiones.remove(token);
            return Optional.empty();
        }
        return Optional.of(s.usuarioId());
    }
}
