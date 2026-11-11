package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "HISTORIAL_OPERACIONES")
public class HistorialOperacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historial_id")
    private Long historialId;

    @Column(name = "operacion_id", nullable = false)
    private Long operacionId;

    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private String estadoNuevo;

    @Column(name = "fecha_cambio")
    private LocalDateTime fechaCambio;

    @Column(name = "usuario", length = 50)
    private String usuario;

    @Column(name = "observacion", length = 200)
    private String observacion;

    public HistorialOperacion() {
    }

    public Long getHistorialId() {
        return historialId;
    }

    public void setHistorialId(Long historialId) {
        this.historialId = historialId;
    }

    public Long getOperacionId() {
        return operacionId;
    }

    public void setOperacionId(Long operacionId) {
        this.operacionId = operacionId;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(String estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
