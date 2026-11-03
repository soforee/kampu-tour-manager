package pe.edu.upeu.lp2_gp2.dto;

import java.time.LocalDate;

public final class Reglas {
    private Reglas() {
    }

    public static final String NOMBRE_PERSONA = "^\\s*(\\p{L}[\\p{L} .'-]*)?\\s*$";
    public static final String MSG_NOMBRE_PERSONA = "solo admite letras, espacios y los signos . ' -";

    public static final String TEXTO = "^$|^[\\p{L}\\p{N} .,;:'\"()&/+#°ºª¡!¿?%-]*$";

    public static final String NACIONALIDAD = "^$|^\\s*\\p{L}[\\p{L} ]*\\s*$";

    public static final String TELEFONO = "^$|^\\s*\\+?[0-9][0-9 ()-]*\\s*$";
    public static final String MSG_TELEFONO = "El teléfono solo admite números y los signos + ( ) - (entre 6 y 15 dígitos).";

    public static final String EMAIL = "^$|^\\s*[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}\\s*$";

    public static final String PLACA = "^\\s*([A-Za-z0-9-]{3,15})?\\s*$";

    public static final String CODIGO = "^$|^[A-Za-z0-9 ._/#-]*$";

    public static final LocalDate FECHA_MINIMA = LocalDate.of(1900, 1, 1);

    public static boolean documentoValido(String tipo, String numero) {
        if (tipo == null || numero == null || numero.isBlank()) return true;
        String n = numero.trim();
        if ("DNI".equals(tipo)) return n.matches("\\d{8}");
        if ("CE".equals(tipo)) return n.matches("[A-Za-z0-9]{8,12}");
        if ("PASAPORTE".equals(tipo)) return n.matches("[A-Za-z0-9]{6,12}");
        return true;
    }

    public static final String MSG_DOCUMENTO =
            "Número de documento no válido: el DNI tiene 8 dígitos; el CE, de 8 a 12 letras o números; el pasaporte, de 6 a 12 letras o números.";

    public static boolean telefonoValido(String telefono) {
        if (telefono == null || telefono.isBlank()) return true;
        int digitos = 0;
        for (int i = 0; i < telefono.length(); i++) {
            if (Character.isDigit(telefono.charAt(i))) digitos++;
        }
        return digitos >= 6 && digitos <= 15;
    }
}
