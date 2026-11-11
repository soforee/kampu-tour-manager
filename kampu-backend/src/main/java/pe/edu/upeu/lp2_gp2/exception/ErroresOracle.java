package pe.edu.upeu.lp2_gp2.exception;

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduce los errores de Oracle a mensajes para el usuario:
 *  - ORA-20000..20999: RAISE_APPLICATION_ERROR de los triggers, paquetes y procedimientos
 *    de 01_schema_v4.sql; su texto ya está en español y se devuelve tal cual.
 *  - ORA-00001 / 02290 / 02291 / 02292: restricciones UNIQUE, CHECK y FK, con un
 *    mensaje por nombre de restricción.
 */
public final class ErroresOracle {
    private ErroresOracle() {
    }

    private static final Pattern RESTRICCION = Pattern.compile("\\(([A-Za-z0-9_$#]+)\\.([A-Za-z0-9_$#]+)\\)");

    // Nombres de restricciones e índices únicos de 01_schema_v4.sql
    private static final Map<String, String> MENSAJES = Map.ofEntries(
            // UNIQUE
            Map.entry("UQ_CLI_EMAIL", "Ese correo ya está registrado para otro cliente."),
            Map.entry("UQ_CLI_NUMERO_DOC", "Ese número de documento ya está registrado para otro cliente."),
            Map.entry("UQ_CLI_TIPO_DOC", "Ya existe un cliente con ese tipo y número de documento."),
            Map.entry("UQ_CLI_TELEFONO", "Ese teléfono ya está registrado para otro cliente."),
            Map.entry("UQ_PROV_PROPIO", "Solo puede haber un proveedor propio (Kampu Tours)."),
            Map.entry("UQ_PROV_TELEFONO", "Ese teléfono ya está registrado para otro proveedor."),
            Map.entry("UQ_PERS_TELEFONO", "Ese teléfono ya está registrado para otra persona del personal."),
            Map.entry("UQ_PERS_LICENCIA", "Ya existe una persona con ese número de licencia."),
            Map.entry("UQ_DET_RESERVA_SALIDA", "La reserva ya incluye esa salida."),
            Map.entry("UQ_PAS_DETALLE_DOC", "El documento de un pasajero está repetido en la reserva."),
            // CHECK
            Map.entry("CHK_PROV_RUC", "El RUC tiene 11 dígitos y empieza con 10, 15, 17 o 20."),
            Map.entry("CHK_USU_ROL", "Rol no válido (ADMIN, VENDEDOR u OPERADOR)."),
            Map.entry("CHK_PAQ_DIAS", "Las noches no pueden ser más que los días."),
            Map.entry("CHK_PERS_LICENCIA", "Quien conduce necesita número, categoría y vencimiento de licencia."),
            Map.entry("CHK_LUG_COORDS", "Indica la latitud y la longitud juntas (o deja ambas vacías)."),
            Map.entry("CHK_OP_OD", "El origen y el destino deben ser distintos."),
            Map.entry("CHK_OP_HORAS", "La llegada debe ser posterior a la hora de salida."),
            Map.entry("CHK_SAL_CUPOS", "Los cupos disponibles no pueden ser negativos ni superar los cupos máximos."),
            Map.entry("CHK_RES_MONTOS", "El monto pagado no puede superar el total de la reserva."),
            Map.entry("CHK_PAG_TC", "El tipo de cambio de un pago en soles debe ser 1."),
            // FOREIGN KEY
            Map.entry("FK_OP_VEH", "El vehículo indicado no existe."),
            Map.entry("FK_OP_CON", "El conductor indicado no existe."),
            Map.entry("FK_OP_SAL", "La salida indicada no existe."),
            Map.entry("FK_SAL_GUIA", "El guía indicado no existe."),
            Map.entry("FK_PERS_PROV", "El proveedor indicado no existe."),
            Map.entry("FK_VEH_PROV", "El proveedor indicado no existe.")
    );

    /** Mensaje para el usuario, o null si la excepción no viene de Oracle. */
    public static String mensaje(Throwable e) {
        SQLException sql = buscarSql(e);
        if (sql == null) return null;
        int codigo = sql.getErrorCode();
        String texto = sql.getMessage() == null ? "" : sql.getMessage();
        if (codigo >= 20000 && codigo <= 20999) {
            return primeraLinea(texto).replaceFirst("^ORA-2\\d{4}:\\s*", "");
        }
        String restriccion = restriccion(texto);
        if (restriccion != null && MENSAJES.containsKey(restriccion)) return MENSAJES.get(restriccion);
        return switch (codigo) {
            case 1 -> "Ya existe un registro con esos datos.";
            case 2290 -> "Los datos no cumplen una regla de la base de datos" + (restriccion == null ? "." : " (" + restriccion + ").");
            case 2291 -> "Un dato relacionado no existe" + (restriccion == null ? "." : " (" + restriccion + ").");
            case 2292 -> "El registro está en uso por otros datos y no se puede eliminar.";
            case 1400 -> "Falta un dato obligatorio.";
            default -> null;
        };
    }

    private static SQLException buscarSql(Throwable e) {
        SQLException ultima = null;
        for (Throwable t = e; t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof SQLException s) ultima = s;
        }
        return ultima;
    }

    private static String primeraLinea(String texto) {
        int salto = texto.indexOf('\n');
        return (salto < 0 ? texto : texto.substring(0, salto)).trim();
    }

    private static String restriccion(String texto) {
        Matcher m = RESTRICCION.matcher(texto);
        return m.find() ? m.group(2).toUpperCase() : null;
    }
}
