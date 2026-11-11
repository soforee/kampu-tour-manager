package pe.edu.upeu.lp2_gp2.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import pe.edu.upeu.lp2_gp2.entity.UsuarioSistema;
import pe.edu.upeu.lp2_gp2.repository.UsuarioSistemaRepository;

@Component
public class UsuarioInicialConfig {
    private final UsuarioSistemaRepository repo;

    public UsuarioInicialConfig(UsuarioSistemaRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void crearUsuariosIniciales() {
        if (repo.count() > 0) return;
        repo.save(nuevo("admin", "admin123", "ADMIN"));
        // Roles permitidos por chk_usu_rol: ADMIN, VENDEDOR, OPERADOR
        repo.save(nuevo("ventas", "ventas123", "VENDEDOR"));
        repo.save(nuevo("operador", "operador123", "OPERADOR"));
        System.out.println("USUARIOS_SISTEMA vacía: se crearon los usuarios admin, ventas y operador.");
    }

    private UsuarioSistema nuevo(String username, String password, String rol) {
        UsuarioSistema u = new UsuarioSistema();
        u.setUsername(username);
        u.setPasswordHash(password);
        u.setRol(rol);
        u.setEstado("ACTIVO");
        return u;
    }
}
