package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Proveedores de transporte y personal. es_propio = 1: Kampu Tours (único, uq_prov_propio).
@Entity
@DynamicUpdate
@Table(name = "PROVEEDORES")
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proveedor_id")
    private Long proveedorId;

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ruc", nullable = false, length = 11)
    private String ruc;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "es_propio", nullable = false)
    private Integer esPropio;

    @Column(name = "estado", length = 20)
    private String estado;

    public Proveedor() {
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Integer getEsPropio() {
        return esPropio;
    }

    public void setEsPropio(Integer esPropio) {
        this.esPropio = esPropio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
