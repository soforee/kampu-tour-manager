package pe.edu.upeu.lp2_gp2.auditoria;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;
import pe.edu.upeu.lp2_gp2.entity.Auditoria;
import pe.edu.upeu.lp2_gp2.entity.HistorialOperacion;
import pe.edu.upeu.lp2_gp2.seguridad.SesionActual;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

/**
 * Auditoría automática de TODA escritura hecha con JPA (todas las pantallas del sistema):
 *   - CREAR      : una fila con el resumen de los datos guardados.
 *   - ACTUALIZAR : una fila POR CAMPO modificado, con valor anterior y nuevo.
 *   - ELIMINAR   : una fila con los datos que tenía el registro.
 * Se escribe con la misma conexión de Hibernate: queda en la misma transacción que el cambio.
 *
 * No se duplica lo que ya audita Oracle: RESERVAS.estado_reserva y estado_pago los registra
 * TRG_AUDITORIA_ESTADO_PAQUETE. Las contraseñas nunca se guardan en claro.
 */
@Component
public class AuditoriaHibernateListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    // Tabla -> nombre de la entidad en la bitácora
    private static final Map<String, String> NOMBRES = Map.ofEntries(
            Map.entry("RESERVAS", "RESERVA"), Map.entry("PAGOS", "PAGO"), Map.entry("CLIENTES", "CLIENTE"),
            Map.entry("PAQUETES_TOUR", "PAQUETE"), Map.entry("SALIDAS_PROGRAMADAS", "SALIDA"),
            Map.entry("DETALLE_RESERVA", "DETALLE_RESERVA"), Map.entry("PASAJEROS_RESERVA", "PASAJERO"),
            Map.entry("PROVEEDORES", "PROVEEDOR"), Map.entry("PERSONAL", "PERSONAL"), Map.entry("VEHICULOS", "VEHICULO"),
            Map.entry("LUGARES", "LUGAR"), Map.entry("OPERACIONES_TRANSPORTE", "OPERACION"),
            Map.entry("USUARIOS_SISTEMA", "USUARIO"));

    // Ya auditados por Oracle o que son bitácoras en sí mismas
    private static final Set<Class<?>> EXCLUIDAS = Set.of(Auditoria.class, HistorialOperacion.class);
    private static final Set<String> CAMPOS_TRIGGER = Set.of("RESERVA.ESTADO_RESERVA", "RESERVA.ESTADO_PAGO");
    private static final Set<String> SECRETOS = Set.of("PASSWORD_HASH");

    private final EntityManagerFactory emf;

    public AuditoriaHibernateListener(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @PostConstruct
    void registrar() {
        EventListenerRegistry registro = emf.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry().getService(EventListenerRegistry.class);
        registro.appendListeners(EventType.POST_INSERT, this);
        registro.appendListeners(EventType.POST_UPDATE, this);
        registro.appendListeners(EventType.POST_DELETE, this);
    }

    @Override
    public void onPostInsert(PostInsertEvent e) {
        if (excluida(e.getEntity())) return;
        guardar(e.getSession(), "CREAR", e.getPersister(), e.getId(), null, null, null,
                "Alta: " + resumen(e.getPersister(), e.getState()));
    }

    @Override
    public void onPostUpdate(PostUpdateEvent e) {
        if (excluida(e.getEntity()) || e.getOldState() == null) return;
        EntityPersister p = e.getPersister();
        String entidad = entidad(p);
        int[] sucios = e.getDirtyProperties();
        if (sucios == null) return;
        for (int i : sucios) {
            String campo = columna(p, i);
            if (CAMPOS_TRIGGER.contains(entidad + "." + campo)) continue;
            Object antes = e.getOldState()[i];
            Object despues = e.getState()[i];
            if (Objects.equals(texto(antes), texto(despues))) continue;
            boolean secreto = SECRETOS.contains(campo);
            guardar(e.getSession(), "ACTUALIZAR", p, e.getId(), campo,
                    secreto ? "******" : texto(antes), secreto ? "******" : texto(despues),
                    secreto ? "Cambio de contraseña" : null);
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent e) {
        if (excluida(e.getEntity())) return;
        guardar(e.getSession(), "ELIMINAR", e.getPersister(), e.getId(), null, null, null,
                "Baja: " + resumen(e.getPersister(), e.getDeletedState()));
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    // ---------------------------------------------------------------------

    private void guardar(SharedSessionContractImplementor sesion, String accion, EntityPersister p, Object id,
                         String campo, String antes, String despues, String detalle) {
        RegistroAuditoria r = new RegistroAuditoria(accion, entidad(p), id instanceof Number n ? n.longValue() : null,
                campo, antes, despues, detalle, SesionActual.usuario(), SesionActual.rol());
        try {
            AuditoriaService.insertar(sesion.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection(), r);
        } catch (SQLException ex) {
            // Sin bitácora no se confirma el cambio (falla la transacción completa)
            throw new IllegalStateException("No se pudo registrar la auditoría (¿ejecutaste 01_schema.sql actualizado?): "
                    + ex.getMessage(), ex);
        }
    }

    private static boolean excluida(Object entidad) {
        return entidad == null || EXCLUIDAS.contains(entidad.getClass());
    }

    private static String entidad(EntityPersister p) {
        String tabla = p instanceof AbstractEntityPersister a ? a.getTableName().toUpperCase() : p.getEntityName();
        return NOMBRES.getOrDefault(tabla, tabla);
    }

    private static String columna(EntityPersister p, int i) {
        if (p instanceof AbstractEntityPersister a) {
            String[] cols = a.getPropertyColumnNames(i);
            if (cols != null && cols.length > 0) return cols[0].toUpperCase();
        }
        return p.getPropertyNames()[i].toUpperCase();
    }

    // "campo=valor, ..." sin nulos ni secretos
    private static String resumen(EntityPersister p, Object[] estado) {
        if (estado == null) return "";
        StringJoiner j = new StringJoiner(", ");
        for (int i = 0; i < estado.length; i++) {
            String campo = columna(p, i);
            String valor = texto(estado[i]);
            if (valor == null || valor.isEmpty() || SECRETOS.contains(campo)) continue;
            j.add(campo.toLowerCase() + "=" + valor);
        }
        return j.toString();
    }

    private static String texto(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal b) return b.stripTrailingZeros().toPlainString();
        if (v instanceof Boolean b) return b ? "SI" : "NO";
        return v.toString();
    }
}
