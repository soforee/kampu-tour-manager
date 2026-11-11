package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

// Vista VW_RESUMEN_PAQUETES_POR_ROL: filas y montos según el rol de la sesión
@Entity
@Immutable
@Table(name = "VW_RESUMEN_PAQUETES_POR_ROL")
public class VistaResumenPaquete {
    @Id
    @Column(name = "salida_id")
    private Long salidaId;

    @Column(name = "paquete_id")
    private Long paqueteId;

    @Column(name = "paquete_nombre")
    private String paqueteNombre;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "estado_salida")
    private String estadoSalida;

    @Column(name = "estado_ejecucion")
    private String estadoEjecucion;

    @Column(name = "cupos_maximos")
    private Integer cuposMaximos;

    @Column(name = "cupos_disponibles")
    private Integer cuposDisponibles;

    @Column(name = "pasajeros")
    private Integer pasajeros;

    @Column(name = "reservas_activas")
    private Integer reservasActivas;

    @Column(name = "operaciones")
    private Integer operaciones;

    @Column(name = "guia")
    private String guia;

    @Column(name = "monto_vendido")
    private BigDecimal montoVendido;

    @Column(name = "monto_cobrado")
    private BigDecimal montoCobrado;

    public VistaResumenPaquete() {
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

    public String getPaqueteNombre() {
        return paqueteNombre;
    }

    public void setPaqueteNombre(String paqueteNombre) {
        this.paqueteNombre = paqueteNombre;
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

    public String getEstadoSalida() {
        return estadoSalida;
    }

    public void setEstadoSalida(String estadoSalida) {
        this.estadoSalida = estadoSalida;
    }

    public String getEstadoEjecucion() {
        return estadoEjecucion;
    }

    public void setEstadoEjecucion(String estadoEjecucion) {
        this.estadoEjecucion = estadoEjecucion;
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

    public Integer getPasajeros() {
        return pasajeros;
    }

    public void setPasajeros(Integer pasajeros) {
        this.pasajeros = pasajeros;
    }

    public Integer getReservasActivas() {
        return reservasActivas;
    }

    public void setReservasActivas(Integer reservasActivas) {
        this.reservasActivas = reservasActivas;
    }

    public Integer getOperaciones() {
        return operaciones;
    }

    public void setOperaciones(Integer operaciones) {
        this.operaciones = operaciones;
    }

    public String getGuia() {
        return guia;
    }

    public void setGuia(String guia) {
        this.guia = guia;
    }

    public BigDecimal getMontoVendido() {
        return montoVendido;
    }

    public void setMontoVendido(BigDecimal montoVendido) {
        this.montoVendido = montoVendido;
    }

    public BigDecimal getMontoCobrado() {
        return montoCobrado;
    }

    public void setMontoCobrado(BigDecimal montoCobrado) {
        this.montoCobrado = montoCobrado;
    }
}
