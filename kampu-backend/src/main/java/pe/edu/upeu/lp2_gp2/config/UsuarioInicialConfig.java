package pe.edu.upeu.lp2_gp2.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import pe.edu.upeu.lp2_gp2.entity.UsuarioSistema;
import pe.edu.upeu.lp2_gp2.repository.UsuarioSistemaRepository;

@Component
public class UsuarioInicialConfig {
    private final UsuarioSistemaRepository repo;
    private final JdbcTemplate jdbc;

    public UsuarioInicialConfig(UsuarioSistemaRepository repo, JdbcTemplate jdbc) {
        this.repo = repo;
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void crearUsuariosIniciales() {
        verificarEsquema();
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

    // Antes de usar las tablas: si el esquema al que se conectó la aplicación no tiene los scripts
    // instalados, se detiene con un mensaje claro (usuario, contenedor y servicio) en lugar de un ORA-00942.
    private void verificarEsquema() {
        var datos = jdbc.queryForMap("SELECT USER AS usuario, SYS_CONTEXT('USERENV', 'CON_NAME') AS contenedor, "
                + "SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS servicio, "
                + "(SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = 'USUARIOS_SISTEMA') AS tablas, "
                + "(SELECT COUNT(*) FROM USER_OBJECTS WHERE OBJECT_NAME = 'PKG_SEGURIDAD_ROLES' AND OBJECT_TYPE = 'PACKAGE BODY' AND STATUS = 'VALID') AS paquete "
                + "FROM dual");
        boolean tablas = ((Number) datos.get("TABLAS")).intValue() > 0;
        boolean paquete = ((Number) datos.get("PAQUETE")).intValue() > 0;
        if (tablas && paquete) return;
        String donde = "usuario " + datos.get("USUARIO") + ", contenedor " + datos.get("CONTENEDOR") + ", servicio " + datos.get("SERVICIO");
        String mensaje = "La base de datos a la que se conectó la aplicación (" + donde + ") no tiene "
                + (tablas ? "el paquete PKG_SEGURIDAD_ROLES válido" : "las tablas del sistema") + ". "
                + "Ejecuta 01_schema.sql, 02_data.sql y 03_particionamiento_roles.sql conectado como ese mismo usuario y servicio, "
                + "o cambia DB_URL para apuntar a donde ya los ejecutaste "
                + "(Oracle XE 21c: jdbc:oracle:thin:@localhost:1521/XEPDB1 · Oracle 23 Free: .../FREEPDB1)."
                + ("CDB$ROOT".equals(datos.get("CONTENEDOR")) ? " Ojo: estás en el contenedor raíz (CDB$ROOT); los scripts van en la PDB." : "");
        System.err.println("\n*** " + mensaje + "\n");
        throw new IllegalStateException(mensaje);
    }
}
