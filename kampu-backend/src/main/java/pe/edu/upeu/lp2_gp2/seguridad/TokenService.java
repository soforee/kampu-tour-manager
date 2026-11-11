package pe.edu.upeu.lp2_gp2.seguridad;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tokens de sesión opacos (aleatorios) guardados en memoria. El frontend los envía en
 * "Authorization: Bearer <token>". Al reiniciar el backend, las sesiones se pierden y
 * hay que volver a iniciar sesión.
 */
@Component
public class TokenService {
    private static final Duration DURACION = Duration.ofHours(12);
    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<String, SesionUsuario> sesiones = new ConcurrentHashMap<>();

    public SesionUsuario crear(Long usuarioId, Long clienteId, String username, String nombre, String rol) {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        SesionUsuario s = new SesionUsuario(token, usuarioId, clienteId, username, nombre, rol, Instant.now().plus(DURACION));
        sesiones.put(token, s);
        return s;
    }

    public SesionUsuario obtener(String token) {
        if (token == null) return null;
        SesionUsuario s = sesiones.get(token);
        if (s == null) return null;
        if (s.expira().isBefore(Instant.now())) {
            sesiones.remove(token);
            return null;
        }
        return s;
    }

    public void eliminar(String token) {
        if (token != null) sesiones.remove(token);
    }
}
