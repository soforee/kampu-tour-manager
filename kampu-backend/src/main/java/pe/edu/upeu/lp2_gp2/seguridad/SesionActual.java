package pe.edu.upeu.lp2_gp2.seguridad;

/**
 * Sesión de la petición en curso (la fija SeguridadInterceptor). La usan los servicios
 * y SesionOracleDataSource, que la pasa a PKG_SEGURIDAD_ROLES en cada conexión.
 */
public final class SesionActual {
    private static final ThreadLocal<SesionUsuario> ACTUAL = new ThreadLocal<>();

    private SesionActual() {
    }

    public static SesionUsuario get() {
        return ACTUAL.get();
    }

    public static void set(SesionUsuario sesion) {
        ACTUAL.set(sesion);
    }

    public static void limpiar() {
        ACTUAL.remove();
    }

    public static String usuario() {
        SesionUsuario s = ACTUAL.get();
        return s == null ? "sistema" : s.username();
    }

    public static String rol() {
        SesionUsuario s = ACTUAL.get();
        return s == null ? null : s.rol();
    }
}
