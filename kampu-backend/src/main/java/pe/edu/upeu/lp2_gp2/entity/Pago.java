package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.DynamicUpdate;

// TRG_VALIDAR_PAGO_PAQUETE impide pagar reservas cerradas o aprobar más que el total
@Entity
@DynamicUpdate
@Table(name = "PAGOS")
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pago_id")
    private Long pagoId;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda", length = 10)
    private String moneda;

    @Column(name = "tipo_cambio", nullable = false, precision = 8, scale = 4)
    private BigDecimal tipoCambio;

    // Columna virtual: ROUND(monto * tipo_cambio, 2)
    @Column(name = "monto_pen", insertable = false, updatable = false)
    private BigDecimal montoPen;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Column(name = "codigo_transaccion", length = 100)
    private String codigoTransaccion;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "estado_transaccion", length = 20)
    private String estadoTransaccion;

    // ADMIN / VENDEDOR / CLIENTE: clave de partición de PAGOS
    @Column(name = "registrado_por", length = 20)
    private String registradoPor;

    public Pago() {
    }

    public Long getPagoId() {
        return pagoId;
    }

    public void setPagoId(Long pagoId) {
        this.pagoId = pagoId;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public void setReservaId(Long reservaId) {
        this.reservaId = reservaId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public BigDecimal getTipoCambio() {
        return tipoCambio;
    }

    public void setTipoCambio(BigDecimal tipoCambio) {
        this.tipoCambio = tipoCambio;
    }

    public BigDecimal getMontoPen() {
        return montoPen;
    }

    public void setMontoPen(BigDecimal montoPen) {
        this.montoPen = montoPen;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getCodigoTransaccion() {
        return codigoTransaccion;
    }

    public void setCodigoTransaccion(String codigoTransaccion) {
        this.codigoTransaccion = codigoTransaccion;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getEstadoTransaccion() {
        return estadoTransaccion;
    }

    public void setEstadoTransaccion(String estadoTransaccion) {
        this.estadoTransaccion = estadoTransaccion;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(String registradoPor) {
        this.registradoPor = registradoPor;
    }
}
