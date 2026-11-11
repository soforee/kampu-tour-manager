package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Proveedores de transporte y personal. es_propio = 1: Kampu Tours (único, uq_prov_propio).
// Estado PENDIENTE: empresa que se afilió desde la web y espera la aprobación de Kampu.
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

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "email", length = 100)
    private String email;

    // Persona de contacto de la empresa
    @Column(name = "contacto", length = 150)
    private String contacto;

    // Servicios que ofrece: TRASLADO, TOUR, INTERPROVINCIAL (separados por coma)
    @Column(name = "servicios", nullable = false, length = 60)
    private String servicios;

    // Ciudades donde opera, separadas por coma (null = todo el país)
    @Column(name = "zonas", length = 300)
    private String zonas;

    @Column(name = "fecha_afiliacion")
    private java.time.LocalDate fechaAfiliacion;

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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public String getServicios() {
        return servicios;
    }

    public void setServicios(String servicios) {
        this.servicios = servicios;
    }

    public String getZonas() {
        return zonas;
    }

    public void setZonas(String zonas) {
        this.zonas = zonas;
    }

    public java.time.LocalDate getFechaAfiliacion() {
        return fechaAfiliacion;
    }

    public void setFechaAfiliacion(java.time.LocalDate fechaAfiliacion) {
        this.fechaAfiliacion = fechaAfiliacion;
    }
}
