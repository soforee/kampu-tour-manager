package pe.edu.upeu.lp2_gp2.config;

import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.edu.upeu.lp2_gp2.entity.UsuarioSistema;
import pe.edu.upeu.lp2_gp2.repository.UsuarioSistemaRepository;

@Component
public class UsuarioInicialConfig {
    private final UsuarioSistemaRepository repo;
    private final PasswordEncoder encoder;

    public UsuarioInicialConfig(UsuarioSistemaRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @PostConstruct
    public void crearUsuariosIniciales() {
        if (repo.count() > 0) return;
        repo.save(nuevo("admin", encoder.encode("admin123"), "ADMIN"));
        repo.save(nuevo("ventas", encoder.encode("ventas123"), "VENTAS"));
        System.out.println("USUARIOS_SISTEMA vacía: se crearon los usuarios admin y ventas.");
    }

    private UsuarioSistema nuevo(String username, String hash, String rol) {
        UsuarioSistema u = new UsuarioSistema();
        u.setUsername(username);
        u.setPasswordHash(hash);
        u.setRol(rol);
        u.setEstado("ACTIVO");
        return u;
    }
}
