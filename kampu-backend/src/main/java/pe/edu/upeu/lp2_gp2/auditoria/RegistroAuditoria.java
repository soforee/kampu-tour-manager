package pe.edu.upeu.lp2_gp2.auditoria;

/**
 * Una fila de la bitácora AUDITORIA.
 *
 * @param accion        CREAR, ACTUALIZAR, ELIMINAR, REGISTRO, LOGIN, LOGOUT o LOGIN_FALLIDO (chk_aud_accion)
 * @param entidad       RESERVA, PAGO, CLIENTE, SESION... (máx. 30)
 * @param entidadId     id del registro afectado (puede ser null, p. ej. un login fallido)
 * @param campo         columna modificada (solo en ACTUALIZAR)
 * @param valorAnterior valor antes del cambio
 * @param valorNuevo    valor después del cambio
 * @param detalle       resumen legible (máx. 1000)
 * @param usuario       quién lo hizo
 * @param rol           ADMIN, VENDEDOR, OPERADOR, CLIENTE o SISTEMA (clave de partición)
 */
public record RegistroAuditoria(
        String accion,
        String entidad,
        Long entidadId,
        String campo,
        String valorAnterior,
        String valorNuevo,
        String detalle,
        String usuario,
        String rol
) {
    static final String SQL = "INSERT INTO AUDITORIA (accion, entidad, entidad_id, campo, valor_anterior, valor_nuevo, "
            + "detalle, usuario, rol_usuario, origen) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'APP')";

    private static final java.util.Set<String> ROLES = java.util.Set.of("ADMIN", "VENDEDOR", "OPERADOR", "CLIENTE");

    /** Normaliza longitudes y el rol para que la fila siempre cumpla los CHECK de la tabla */
    Object[] parametros() {
        return new Object[]{
                accion, corta(entidad, 30), entidadId, corta(campo, 40), corta(valorAnterior, 400), corta(valorNuevo, 400),
                corta(detalle, 1000), corta(usuario == null ? "sistema" : usuario, 100),
                rol != null && ROLES.contains(rol) ? rol : "SISTEMA"
        };
    }

    static String corta(String texto, int max) {
        if (texto == null) return null;
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}
