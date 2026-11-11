package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

// Bitácora AUDITORIA (solo lectura desde JPA). La escriben:
//   origen BD : TRG_AUDITORIA_ESTADO_PAQUETE y SP_REGISTRAR_CLIENTE_EXTERNO
//   origen APP: auditoria.AuditoriaHibernateListener y auditoria.AuditoriaService
// Las longitudes deben coincidir con la tabla: rol_usuario es clave de partición (ORA-14060 si Hibernate intenta cambiarla).
@Entity
@Immutable
@Table(name = "AUDITORIA")
public class Auditoria {
    @Id
    @Column(name = "auditoria_id")
    private Long auditoriaId;

    @Column(name = "accion", length = 20)
    private String accion;

    @Column(name = "entidad", length = 30)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "campo", length = 40)
    private String campo;

    @Column(name = "valor_anterior", length = 400)
    private String valorAnterior;

    @Column(name = "valor_nuevo", length = 400)
    private String valorNuevo;

    @Column(name = "detalle", length = 1000)
    private String detalle;

    @Column(name = "usuario", length = 100)
    private String usuario;

    @Column(name = "rol_usuario", length = 20)
    private String rolUsuario;

    @Column(name = "origen", length = 10)
    private String origen;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    public Auditoria() {
    }

    public Long getAuditoriaId() { return auditoriaId; }
    public String getAccion() { return accion; }
    public String getEntidad() { return entidad; }
    public Long getEntidadId() { return entidadId; }
    public String getCampo() { return campo; }
    public String getValorAnterior() { return valorAnterior; }
    public String getValorNuevo() { return valorNuevo; }
    public String getDetalle() { return detalle; }
    public String getUsuario() { return usuario; }
    public String getRolUsuario() { return rolUsuario; }
    public String getOrigen() { return origen; }
    public LocalDateTime getFecha() { return fecha; }
}
