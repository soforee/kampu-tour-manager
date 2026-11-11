package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Cliente;

import java.time.LocalDateTime;

public record ClienteDTO(
        Long clienteId,
        String nombreCompleto,
        String tipoDocumento,
        String numeroDocumento,
        String email,
        String telefono,
        // true si el cliente ya se registró en el portal web (tiene contraseña)
        boolean cuentaWeb,
        String estadoCuenta,
        LocalDateTime fechaRegistro
) {
    public static ClienteDTO desde(Cliente c) {
        return new ClienteDTO(c.getClienteId(), c.getNombreCompleto(), c.getTipoDocumento(), c.getNumeroDocumento(),
                c.getEmail() == null ? "" : c.getEmail(), c.getTelefono() == null ? "" : c.getTelefono(),
                c.getPasswordHash() != null, c.getEstadoCuenta(), c.getFechaRegistro());
    }
}
