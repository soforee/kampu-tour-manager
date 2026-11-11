package pe.edu.upeu.lp2_gp2.services;

import java.util.List;
import java.util.Set;

/**
 * Máquina de estados de las operaciones de transporte (v4: en el backend, no en tablas).
 * CHECK chk_op_est de OPERACIONES_TRANSPORTE admite exactamente estos valores.
 */
public enum EstadoOperacion {
    PROGRAMADO("Programado", 1, false, false),
    ASIGNADO("Asignado", 2, true, false),
    EMBARQUE("Embarque", 3, true, false),
    EN_ESPERA_SALIDA("En espera de salida", 4, true, false),
    EN_RUTA("En ruta", 5, true, false),
    LLEGADA_PROXIMA("Llegada próxima", 6, true, false),
    FINALIZADO("Finalizado", 7, true, true),
    // Retrasado: la salida se reprograma (hora_salida_estimada); conserva sus recursos si los tenía
    RETRASADO("Retrasado", 8, false, false),
    CANCELADO("Cancelado", 9, false, true);

    private final String nombre;
    private final int orden;
    private final boolean requiereRecursos;
    private final boolean esFinal;

    EstadoOperacion(String nombre, int orden, boolean requiereRecursos, boolean esFinal) {
        this.nombre = nombre;
        this.orden = orden;
        this.requiereRecursos = requiereRecursos;
        this.esFinal = esFinal;
    }

    /** Transiciones permitidas desde este estado */
    public Set<EstadoOperacion> siguientes() {
        return switch (this) {
            case PROGRAMADO -> Set.of(ASIGNADO, RETRASADO, CANCELADO);
            case ASIGNADO -> Set.of(PROGRAMADO, EMBARQUE, RETRASADO, CANCELADO);   // PROGRAMADO = desasignar
            case EMBARQUE -> Set.of(EN_ESPERA_SALIDA, EN_RUTA, RETRASADO, CANCELADO);
            case EN_ESPERA_SALIDA -> Set.of(EN_RUTA, RETRASADO, CANCELADO);
            case RETRASADO -> Set.of(PROGRAMADO, ASIGNADO, EMBARQUE, EN_ESPERA_SALIDA, EN_RUTA, CANCELADO);
            case EN_RUTA -> Set.of(LLEGADA_PROXIMA, FINALIZADO);
            case LLEGADA_PROXIMA -> Set.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> Set.of();
        };
    }

    public List<String> siguientesOrdenados() {
        return siguientes().stream().sorted((a, b) -> Integer.compare(a.orden, b.orden)).map(Enum::name).toList();
    }

    public boolean puedePasarA(EstadoOperacion destino) {
        return siguientes().contains(destino);
    }

    public String nombre() {
        return nombre;
    }

    public int orden() {
        return orden;
    }

    public boolean requiereRecursos() {
        return requiereRecursos;
    }

    /** Ocupa vehículo y conductor (cuenta para cruces y horas): todo menos CANCELADO */
    public boolean ocupaRecursos() {
        return this != CANCELADO;
    }

    /** Ya salió del punto de origen (en ruta o por llegar) */
    public boolean enTransito() {
        return this == EN_RUTA || this == LLEGADA_PROXIMA;
    }

    /** Aún no sale: se puede asignar, reasignar o reprogramar */
    public boolean antesDeSalir() {
        return this == PROGRAMADO || this == ASIGNADO || this == RETRASADO;
    }

    public boolean esFinal() {
        return esFinal;
    }

    public static EstadoOperacion de(String codigo) {
        try {
            return valueOf(codigo.trim().toUpperCase());
        } catch (RuntimeException e) {
            throw new pe.edu.upeu.lp2_gp2.exception.BusinessException("Estado de operación inexistente: " + codigo);
        }
    }
}
