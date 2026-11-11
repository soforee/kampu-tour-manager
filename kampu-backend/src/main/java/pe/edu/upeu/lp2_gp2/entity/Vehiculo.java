package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "VEHICULOS")
public class Vehiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehiculo_id")
    private Long vehiculoId;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "placa", nullable = false, length = 10)
    private String placa;

    @Column(name = "tipo_vehiculo", nullable = false, length = 20)
    private String tipoVehiculo;

    @Column(name = "marca_modelo", length = 100)
    private String marcaModelo;

    @Column(name = "capacidad_pax", nullable = false)
    private Integer capacidadPax;

    @Column(name = "prox_mantenimiento")
    private LocalDate proxMantenimiento;

    @Column(name = "estado", length = 20)
    private String estado;

    public Vehiculo() {
    }

    public Long getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(Long vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getTipoVehiculo() {
        return tipoVehiculo;
    }

    public void setTipoVehiculo(String tipoVehiculo) {
        this.tipoVehiculo = tipoVehiculo;
    }

    public String getMarcaModelo() {
        return marcaModelo;
    }

    public void setMarcaModelo(String marcaModelo) {
        this.marcaModelo = marcaModelo;
    }

    public Integer getCapacidadPax() {
        return capacidadPax;
    }

    public void setCapacidadPax(Integer capacidadPax) {
        this.capacidadPax = capacidadPax;
    }

    public LocalDate getProxMantenimiento() {
        return proxMantenimiento;
    }

    public void setProxMantenimiento(LocalDate proxMantenimiento) {
        this.proxMantenimiento = proxMantenimiento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
