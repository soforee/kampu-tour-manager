package pe.edu.upeu.lp2_gp2.auditoria;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.lp2_gp2.seguridad.SesionActual;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Escribe en la bitácora AUDITORIA (origen = 'APP').
 *
 * - registrar(...)            : dentro de la transacción en curso (si el cambio se deshace, su registro también).
 * - registrarIndependiente(...): en una transacción propia; para eventos que deben quedar aunque la
 *                                operación falle (login fallido) o que ocurren fuera de una transacción.
 * - insertar(Connection, ...) : lo usa AuditoriaHibernateListener con la conexión de Hibernate.
 */
@Service
public class AuditoriaService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate nueva;

    public AuditoriaService(JdbcTemplate jdbc, PlatformTransactionManager tx) {
        this.jdbc = jdbc;
        this.nueva = new TransactionTemplate(tx);
        this.nueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Con el usuario y rol de la sesión de la petición */
    public void registrar(String accion, String entidad, Long entidadId, String detalle) {
        registrar(new RegistroAuditoria(accion, entidad, entidadId, null, null, null, detalle,
                SesionActual.usuario(), SesionActual.rol()));
    }

    public void registrar(RegistroAuditoria r) {
        jdbc.update(RegistroAuditoria.SQL, r.parametros());
    }

    public void registrarIndependiente(RegistroAuditoria r) {
        nueva.executeWithoutResult(estado -> registrar(r));
    }

    static void insertar(Connection con, RegistroAuditoria r) throws SQLException {
        Object[] p = r.parametros();
        try (PreparedStatement ps = con.prepareStatement(RegistroAuditoria.SQL)) {
            for (int i = 0; i < p.length; i++) {
                if (p[i] == null) ps.setNull(i + 1, i == 2 ? Types.NUMERIC : Types.VARCHAR);
                else if (p[i] instanceof Long l) ps.setLong(i + 1, l);
                else ps.setString(i + 1, p[i].toString());
            }
            ps.executeUpdate();
        }
    }
}
