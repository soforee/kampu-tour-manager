package pe.edu.upeu.lp2_gp2.exception;

// 401: falta iniciar sesión o la sesión venció
public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
