package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

// Vista VW_PAQUETES_CLIENTE: solo filas del cliente de la sesión (PKG_SEGURIDAD_ROLES.cliente_actual)
@Entity
@Immutable
@Table(name = "VW_PAQUETES_CLIENTE")
public class VistaPaqueteCliente {
    @Id
    @Column(name = "detalle_id")
    private Long detalleId;

    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "salida_id")
    private Long salidaId;

    @Column(name = "paquete_id")
    private Long paqueteId;

    @Column(name = "paquete_nombre")
    private String paqueteNombre;

    @Column(name = "imagen")
    private String imagen;

    @Column(name = "dias")
    private Integer dias;

    @Column(name = "noches")
    private Integer noches;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "pasajeros")
    private Integer pasajeros;

    @Column(name = "fecha_reserva")
    private LocalDateTime fechaReserva;

    @Column(name = "estado_reserva")
    private String estadoReserva;

    @Column(name = "estado_ejecucion")
    private String estadoEjecucion;

    @Column(name = "estado_pago")
    private String estadoPago;

    @Column(name = "monto_total")
    private BigDecimal montoTotal;

    @Column(name = "monto_pagado")
    private BigDecimal montoPagado;

    @Column(name = "saldo")
    private BigDecimal saldo;

    @Column(name = "monto_por_validar")
    private BigDecimal montoPorValidar;

    public VistaPaqueteCliente() {
    }

    public Long getDetalleId() {
        return detalleId;
    }

    public void setDetalleId(Long detalleId) {
        this.detalleId = detalleId;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public void setReservaId(Long reservaId) {
        this.reservaId = reservaId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
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

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Integer getDias() {
        return dias;
    }

    public void setDias(Integer dias) {
        this.dias = dias;
    }

    public Integer getNoches() {
        return noches;
    }

    public void setNoches(Integer noches) {
        this.noches = noches;
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

    public Integer getPasajeros() {
        return pasajeros;
    }

    public void setPasajeros(Integer pasajeros) {
        this.pasajeros = pasajeros;
    }

    public LocalDateTime getFechaReserva() {
        return fechaReserva;
    }

    public void setFechaReserva(LocalDateTime fechaReserva) {
        this.fechaReserva = fechaReserva;
    }

    public String getEstadoReserva() {
        return estadoReserva;
    }

    public void setEstadoReserva(String estadoReserva) {
        this.estadoReserva = estadoReserva;
    }

    public String getEstadoEjecucion() {
        return estadoEjecucion;
    }

    public void setEstadoEjecucion(String estadoEjecucion) {
        this.estadoEjecucion = estadoEjecucion;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public BigDecimal getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(BigDecimal montoPagado) {
        this.montoPagado = montoPagado;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public BigDecimal getMontoPorValidar() {
        return montoPorValidar;
    }

    public void setMontoPorValidar(BigDecimal montoPorValidar) {
        this.montoPorValidar = montoPorValidar;
    }
}
