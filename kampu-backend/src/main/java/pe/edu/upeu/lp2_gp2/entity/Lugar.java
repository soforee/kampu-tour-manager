package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

// Orígenes y destinos de las operaciones (aeropuertos, hoteles, atractivos...)
@Entity
@Table(name = "LUGARES")
public class Lugar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lugar_id")
    private Long lugarId;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "tipo_lugar", nullable = false, length = 20)
    private String tipoLugar;

    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;

    // Coordenadas reales (WGS84): se usan para calcular la duración de las rutas
    @Column(name = "latitud", precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 9, scale = 6)
    private BigDecimal longitud;

    public Lugar() {
    }

    public Long getLugarId() {
        return lugarId;
    }

    public void setLugarId(Long lugarId) {
        this.lugarId = lugarId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipoLugar() {
        return tipoLugar;
    }

    public void setTipoLugar(String tipoLugar) {
        this.tipoLugar = tipoLugar;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }
}
