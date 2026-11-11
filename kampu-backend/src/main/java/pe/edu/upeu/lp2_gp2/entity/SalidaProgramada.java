package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.hibernate.annotations.DynamicUpdate;

// fecha_fin = inicio + días del paquete - 1 y cupos_disponibles: los calcula el backend
@Entity
@DynamicUpdate
@Table(name = "SALIDAS_PROGRAMADAS")
public class SalidaProgramada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salida_id")
    private Long salidaId;

    @Column(name = "paquete_id", nullable = false)
    private Long paqueteId;

    // PERSONAL con rol GUIA o AMBOS
    @Column(name = "guia_id")
    private Long guiaId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "cupos_maximos", nullable = false)
    private Integer cuposMaximos;

    @Column(name = "cupos_disponibles", nullable = false)
    private Integer cuposDisponibles;

    @Column(name = "estado", length = 20)
    private String estado;

    public SalidaProgramada() {
    }

    public Long getSalidaId() {
        return salidaId;
    }

    public void setSalidaId(Long salidaId) {
        this.salidaId = salidaId;
    }

    public Long getPaqueteId() {
        return paqueteId;
    }

    public void setPaqueteId(Long paqueteId) {
        this.paqueteId = paqueteId;
    }

    public Long getGuiaId() {
        return guiaId;
    }

    public void setGuiaId(Long guiaId) {
        this.guiaId = guiaId;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getCuposMaximos() {
        return cuposMaximos;
    }

    public void setCuposMaximos(Integer cuposMaximos) {
        this.cuposMaximos = cuposMaximos;
    }

    public Integer getCuposDisponibles() {
        return cuposDisponibles;
    }

    public void setCuposDisponibles(Integer cuposDisponibles) {
        this.cuposDisponibles = cuposDisponibles;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
