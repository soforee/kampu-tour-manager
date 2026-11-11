package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "DETALLE_RESERVA")
public class DetalleReserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detalle_id")
    private Long detalleId;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "salida_id", nullable = false)
    private Long salidaId;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "descuento", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuento;

    // Columna virtual
    @Column(name = "subtotal", insertable = false, updatable = false)
    private BigDecimal subtotal;

    public DetalleReserva() {
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

    public Long getSalidaId() {
        return salidaId;
    }

    public void setSalidaId(Long salidaId) {
        this.salidaId = salidaId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
