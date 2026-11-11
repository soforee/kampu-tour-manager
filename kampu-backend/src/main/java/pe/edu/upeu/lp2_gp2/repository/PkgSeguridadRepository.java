package pe.edu.upeu.lp2_gp2.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PKG_SEGURIDAD_ROLES: la matriz de permisos por rol vive en la base de datos.
 * Se cachea 5 minutos por (rol, acción) para no consultar Oracle en cada petición.
 */
@Repository
public class PkgSeguridadRepository {
    private static final long VIGENCIA_MS = 5 * 60 * 1000;
    private record Entrada(boolean permitido, long hasta) {
    }

    private final JdbcTemplate jdbc;
    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    public PkgSeguridadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** PKG_SEGURIDAD_ROLES.puede(rol, acción) */
    public boolean puede(String rol, String accion) {
        if (rol == null) return false;
        String clave = rol + "|" + accion;
        Entrada e = cache.get(clave);
        if (e != null && e.hasta() > System.currentTimeMillis()) return e.permitido();
        Integer r = jdbc.queryForObject("SELECT PKG_SEGURIDAD_ROLES.puede(?, ?) FROM dual", Integer.class, rol, accion);
        boolean permitido = r != null && r == 1;
        cache.put(clave, new Entrada(permitido, System.currentTimeMillis() + VIGENCIA_MS));
        return permitido;
    }

    /** PKG_SEGURIDAD_ROLES.permisos(rol): acciones permitidas al rol (misma caché de 5 minutos) */
    public java.util.List<String> permisos(String rol) {
        if (rol == null) return java.util.List.of();
        String clave = "PERMISOS|" + rol;
        Lista l = listas.get(clave);
        if (l != null && l.hasta() > System.currentTimeMillis()) return l.acciones();
        String texto = jdbc.queryForObject("SELECT PKG_SEGURIDAD_ROLES.permisos(?) FROM dual", String.class, rol);
        java.util.List<String> acciones = texto == null || texto.isBlank()
                ? java.util.List.of() : java.util.List.of(texto.split(","));
        listas.put(clave, new Lista(acciones, System.currentTimeMillis() + VIGENCIA_MS));
        return acciones;
    }

    private record Lista(java.util.List<String> acciones, long hasta) {
    }

    private final Map<String, Lista> listas = new ConcurrentHashMap<>();
}
