package pe.edu.upeu.lp2_gp2.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.DynamicUpdate;

// Tráfico: transiciones de estado y validaciones en OperacionTransporteServiceImpl
@Entity
@DynamicUpdate
@Table(name = "OPERACIONES_TRANSPORTE")
public class OperacionTransporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operacion_id")
    private Long operacionId;

    @Column(name = "codigo_operacion", nullable = false, length = 20)
    private String codigoOperacion;

    @Column(name = "salida_id")
    private Long salidaId;

    @Column(name = "origen_id", nullable = false)
    private Long origenId;

    @Column(name = "destino_id", nullable = false)
    private Long destinoId;

    @Column(name = "vehiculo_id")
    private Long vehiculoId;

    // PERSONAL con rol CONDUCTOR o AMBOS
    @Column(name = "conductor_id")
    private Long conductorId;

    @Column(name = "pax_programados", nullable = false)
    private Integer paxProgramados;

    @Column(name = "hora_salida", nullable = false)
    private LocalDateTime horaSalida;

    @Column(name = "hora_llegada", nullable = false)
    private LocalDateTime horaLlegada;

    @Column(name = "estado", length = 20)
    private String estado;

    @Column(name = "observaciones", length = 200)
    private String observaciones;

    // TRASLADO, TOUR o INTERPROVINCIAL
    @Column(name = "tipo_servicio", nullable = false, length = 20)
    private String tipoServicio;

    // Vía o referencia de la ruta (texto libre)
    @Column(name = "ruta", length = 150)
    private String ruta;

    // Tipo de vehículo exigido (null = cualquiera con capacidad)
    @Column(name = "tipo_vehiculo_req", length = 20)
    private String tipoVehiculoReq;

    // Empresa preferida para la asignación automática (null = la que convenga)
    @Column(name = "proveedor_pref_id")
    private Long proveedorPrefId;

    // Nueva hora de salida cuando la operación está RETRASADA
    @Column(name = "hora_salida_estimada")
    private LocalDateTime horaSalidaEstimada;

    // Respuesta de la empresa al servicio: PENDIENTE, ACEPTADO o RECHAZADO
    @Column(name = "confirmacion", length = 20)
    private String confirmacion;

    @Column(name = "motivo_rechazo", length = 200)
    private String motivoRechazo;

    // Ids de las empresas que rechazaron el servicio (no se les vuelve a asignar)
    @Column(name = "proveedores_rechazo", length = 200)
    private String proveedoresRechazo;

    public OperacionTransporte() {
    }

    public Long getOperacionId() {
        return operacionId;
    }

    public void setOperacionId(Long operacionId) {
        this.operacionId = operacionId;
    }

    public String getCodigoOperacion() {
        return codigoOperacion;
    }

    public void setCodigoOperacion(String codigoOperacion) {
        this.codigoOperacion = codigoOperacion;
    }

    public Long getSalidaId() {
        return salidaId;
    }

    public void setSalidaId(Long salidaId) {
        this.salidaId = salidaId;
    }

    public Long getOrigenId() {
        return origenId;
    }

    public void setOrigenId(Long origenId) {
        this.origenId = origenId;
    }

    public Long getDestinoId() {
        return destinoId;
    }

    public void setDestinoId(Long destinoId) {
        this.destinoId = destinoId;
    }

    public Long getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(Long vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public Long getConductorId() {
        return conductorId;
    }

    public void setConductorId(Long conductorId) {
        this.conductorId = conductorId;
    }

    public Integer getPaxProgramados() {
        return paxProgramados;
    }

    public void setPaxProgramados(Integer paxProgramados) {
        this.paxProgramados = paxProgramados;
    }

    public LocalDateTime getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(LocalDateTime horaSalida) {
        this.horaSalida = horaSalida;
    }

    public LocalDateTime getHoraLlegada() {
        return horaLlegada;
    }

    public void setHoraLlegada(LocalDateTime horaLlegada) {
        this.horaLlegada = horaLlegada;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(String tipoServicio) {
        this.tipoServicio = tipoServicio;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public String getTipoVehiculoReq() {
        return tipoVehiculoReq;
    }

    public void setTipoVehiculoReq(String tipoVehiculoReq) {
        this.tipoVehiculoReq = tipoVehiculoReq;
    }

    public Long getProveedorPrefId() {
        return proveedorPrefId;
    }

    public void setProveedorPrefId(Long proveedorPrefId) {
        this.proveedorPrefId = proveedorPrefId;
    }

    public LocalDateTime getHoraSalidaEstimada() {
        return horaSalidaEstimada;
    }

    public void setHoraSalidaEstimada(LocalDateTime horaSalidaEstimada) {
        this.horaSalidaEstimada = horaSalidaEstimada;
    }

    public String getConfirmacion() {
        return confirmacion;
    }

    public void setConfirmacion(String confirmacion) {
        this.confirmacion = confirmacion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getProveedoresRechazo() {
        return proveedoresRechazo;
    }

    public void setProveedoresRechazo(String proveedoresRechazo) {
        this.proveedoresRechazo = proveedoresRechazo;
    }
}
