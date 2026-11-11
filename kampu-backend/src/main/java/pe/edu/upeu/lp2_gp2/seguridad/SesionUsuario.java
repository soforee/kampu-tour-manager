package pe.edu.upeu.lp2_gp2.seguridad;

import java.time.Instant;

/**
 * Sesión de la aplicación: usuario interno (usuarioId) o cliente del portal (clienteId).
 * rol: ADMIN, VENDEDOR, OPERADOR o CLIENTE.
 */
public record SesionUsuario(
        String token,
        Long usuarioId,
        Long clienteId,
        String username,
        String nombre,
        String rol,
        Instant expira
) {
    public boolean esCliente() {
        return "CLIENTE".equals(rol);
    }
}
