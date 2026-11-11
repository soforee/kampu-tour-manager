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
    EN_RUTA("En ruta", 4, true, false),
    FINALIZADO("Finalizado", 5, true, true),
    CANCELADO("Cancelado", 6, false, true);

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
            case PROGRAMADO -> Set.of(ASIGNADO, CANCELADO);
            case ASIGNADO -> Set.of(PROGRAMADO, EMBARQUE, CANCELADO);   // PROGRAMADO = desasignar
            case EMBARQUE -> Set.of(EN_RUTA, CANCELADO);
            case EN_RUTA -> Set.of(FINALIZADO);
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
