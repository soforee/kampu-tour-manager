package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.hibernate.annotations.DynamicUpdate;

// Guías y conductores (rol GUIA, CONDUCTOR o AMBOS) de un proveedor.
@Entity
@DynamicUpdate
@Table(name = "PERSONAL")
public class Personal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personal_id")
    private Long personalId;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "rol", nullable = false, length = 20)
    private String rol;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "num_licencia", length = 20)
    private String numLicencia;

    @Column(name = "cat_licencia", length = 10)
    private String catLicencia;

    @Column(name = "venc_licencia")
    private LocalDate vencLicencia;

    @Column(name = "estado", length = 20)
    private String estado;

    public Personal() {
    }

    public Long getPersonalId() {
        return personalId;
    }

    public void setPersonalId(Long personalId) {
        this.personalId = personalId;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getNumLicencia() {
        return numLicencia;
    }

    public void setNumLicencia(String numLicencia) {
        this.numLicencia = numLicencia;
    }

    public String getCatLicencia() {
        return catLicencia;
    }

    public void setCatLicencia(String catLicencia) {
        this.catLicencia = catLicencia;
    }

    public LocalDate getVencLicencia() {
        return vencLicencia;
    }

    public void setVencLicencia(LocalDate vencLicencia) {
        this.vencLicencia = vencLicencia;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
