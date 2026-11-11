package pe.edu.upeu.lp2_gp2.dto;

// Respuesta del login y del registro. token va en "Authorization: Bearer <token>".
public record SesionDTO(
        Long usuarioId,
        Long clienteId,
        String username,
        String nombre,
        String rol,
        // Empresa de transporte del usuario PROVEEDOR (null en los demás roles)
        Long proveedorId,
        String proveedor,
        String estadoProveedor,
        String estado,
        String token,
        // Acciones permitidas (PKG_SEGURIDAD_ROLES.permisos): el frontend arma menú, rutas y botones con esto
        java.util.List<String> permisos
) {
}
