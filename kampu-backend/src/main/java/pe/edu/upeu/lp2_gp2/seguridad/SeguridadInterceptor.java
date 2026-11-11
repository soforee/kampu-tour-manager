package pe.edu.upeu.lp2_gp2.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pe.edu.upeu.lp2_gp2.exception.AccesoDenegadoException;
import pe.edu.upeu.lp2_gp2.exception.NoAutenticadoException;
import pe.edu.upeu.lp2_gp2.repository.PkgSeguridadRepository;

/**
 * Seguridad por rol de toda la API (/api/v1/**):
 *  1. Lee el token "Authorization: Bearer ..." y fija la sesión de la petición (SesionActual).
 *  2. Traduce la petición a una acción (VER_RESERVAS, REGISTRAR_PAGO, ...) y pregunta a
 *     PKG_SEGURIDAD_ROLES.puede(rol, acción) si el rol la tiene: 401 sin sesión, 403 sin permiso.
 */
@Component
public class SeguridadInterceptor implements HandlerInterceptor {
    private final TokenService tokens;
    private final PkgSeguridadRepository seguridad;

    public SeguridadInterceptor(TokenService tokens, PkgSeguridadRepository seguridad) {
        this.tokens = tokens;
        this.seguridad = seguridad;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        SesionActual.limpiar();
        String metodo = req.getMethod();
        if ("OPTIONS".equals(metodo)) return true;

        String ruta = req.getRequestURI().substring(req.getContextPath().length());
        if (ruta.startsWith("/api/v1")) ruta = ruta.substring("/api/v1".length());

        String cabecera = req.getHeader("Authorization");
        String token = cabecera != null && cabecera.startsWith("Bearer ") ? cabecera.substring(7).trim() : null;
        SesionUsuario sesion = tokens.obtener(token);
        if (sesion != null) SesionActual.set(sesion);

        if (esPublica(metodo, ruta)) return true;
        if (sesion == null) {
            throw new NoAutenticadoException(token == null ? "Inicia sesión para continuar." : "Tu sesión venció. Vuelve a iniciar sesión.");
        }
        String accion = accion(metodo, ruta);
        if (accion == null) return true;   // cualquier usuario con sesión (cerrar sesión, ver su sesión)
        if (!seguridad.puede(sesion.rol(), accion)) {
            throw new AccesoDenegadoException("Tu rol (" + sesion.rol() + ") no tiene permiso para "
                    + accion.toLowerCase().replace('_', ' ') + ".");
        }
        // Empresa proveedora: gestiona su flota y personal, pero los lugares los administra Kampu
        if (sesion.esProveedor() && !"GET".equals(metodo) && ruta.startsWith("/lugares")) {
            throw new AccesoDenegadoException("Los lugares los administra Kampu Tours.");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        SesionActual.limpiar();
    }

    // Catálogo público (página de inicio), login, registro de clientes y afiliación de empresas
    private boolean esPublica(String metodo, String ruta) {
        if (ruta.equals("/auth/login") || ruta.equals("/auth/registro") || ruta.equals("/auth/afiliacion")) return true;
        boolean lectura = "GET".equals(metodo);
        return lectura && (ruta.equals("/paquetes") || ruta.matches("/paquetes/\\d+")
                || ruta.equals("/salidas") || ruta.matches("/salidas/\\d+"));
    }

    // Acción de PKG_SEGURIDAD_ROLES que exige cada petición
    private String accion(String metodo, String ruta) {
        boolean lectura = "GET".equals(metodo);
        if (ruta.startsWith("/auth/")) return null;
        if (ruta.startsWith("/portal")) {
            return "POST".equals(metodo) && ruta.endsWith("/pagos") ? "INFORMAR_PAGO" : "VER_PAQUETES_PROPIOS";
        }
        if (ruta.startsWith("/usuarios")) return lectura ? "VER_USUARIOS" : "GESTIONAR_USUARIOS";
        if (ruta.startsWith("/auditoria")) return "VER_AUDITORIA";
        if (ruta.startsWith("/clientes")) return lectura ? "VER_CLIENTES" : "GESTIONAR_CLIENTES";
        if (ruta.startsWith("/reservas") || ruta.startsWith("/pasajeros")) return lectura ? "VER_RESERVAS" : "GESTIONAR_RESERVAS";
        if (ruta.startsWith("/pagos")) return lectura ? "VER_MONTOS" : "REGISTRAR_PAGO";
        if (ruta.matches("/salidas/\\d+/ejecucion")) return "ACTUALIZAR_EJECUCION";
        if (ruta.matches("/salidas/\\d+/manifiesto")) return "VER_RESUMEN";
        if (ruta.startsWith("/paquetes") || ruta.startsWith("/salidas")) return lectura ? "VER_LOGISTICA" : "GESTIONAR_CATALOGO";
        if (ruta.startsWith("/proveedores") || ruta.startsWith("/personal") || ruta.startsWith("/vehiculos")
                || ruta.startsWith("/lugares")) {
            return lectura ? "VER_LOGISTICA" : "GESTIONAR_LOGISTICA";
        }
        if (ruta.matches("/operaciones/\\d+/respuesta")) return "RESPONDER_SERVICIO";   // la empresa acepta o rechaza
        if (ruta.startsWith("/operaciones")) return lectura ? "VER_OPERACIONES" : "GESTIONAR_OPERACIONES";
        if (ruta.startsWith("/resumen-paquetes")) return "VER_RESUMEN";
        return "SOLO_ADMIN";
    }
}
