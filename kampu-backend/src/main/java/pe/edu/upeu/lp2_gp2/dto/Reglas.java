package pe.edu.upeu.lp2_gp2.dto;

import java.time.LocalDate;
import java.util.Map;

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

    // VEHICULO.placa es VARCHAR2(10)
    public static final String PLACA = "^\\s*([A-Za-z0-9-]{3,10})?\\s*$";

    // PROVEEDORES.ruc: chk_prov_ruc = '^(10|15|17|20)[0-9]{9}$'
    public static final String RUC = "^(10|15|17|20)[0-9]{9}$";
    public static final String MSG_RUC = "El RUC tiene 11 dígitos y empieza con 10, 15, 17 o 20.";

    public static final String CODIGO = "^$|^[A-Za-z0-9 ._/#-]*$";

    public static final LocalDate FECHA_MINIMA = LocalDate.of(1900, 1, 1);

    // Solo los dígitos del teléfono: así se compara si está repetido
    // (igual que TRANSLATE(telefono, '0+-() ', '0') de los índices únicos)
    public static String digitos(String telefono) {
        return telefono == null ? "" : telefono.replaceAll("\\D", "");
    }

    public static long idOMenosUno(Long id) {
        return id == null ? -1L : id;
    }

    public static boolean documentoValido(String tipo, String numero) {
        if (tipo == null || numero == null || numero.isBlank()) return true;
        String n = numero.trim();
        if ("DNI".equals(tipo)) return n.matches("\\d{8}");
        if ("CE".equals(tipo)) return n.matches("[A-Za-z0-9]{8,12}");
        if ("PASAPORTE".equals(tipo)) return n.matches("[A-Za-z0-9]{6,12}");
        if ("RUC".equals(tipo)) return n.matches(RUC);
        return true;
    }

    public static final String MSG_DOCUMENTO =
            "Número de documento no válido: el DNI tiene 8 dígitos; el CE, de 8 a 12 letras o números; el pasaporte, de 6 a 12 letras o números; el RUC, 11 dígitos.";

    public static boolean telefonoValido(String telefono) {
        if (telefono == null || telefono.isBlank()) return true;
        int digitos = 0;
        for (int i = 0; i < telefono.length(); i++) {
            if (Character.isDigit(telefono.charAt(i))) digitos++;
        }
        return digitos >= 6 && digitos <= 15;
    }

    // ---------- Teléfono con código de país: "+51 987654321" ----------
    // Cantidad de dígitos del número nacional (sin el código) según el país.
    // Debe coincidir con core/utils/paises.ts del frontend.
    public record PaisTelefono(String nombre, int min, int max) {
    }

    public static final Map<String, PaisTelefono> PAISES_TELEFONO = Map.ofEntries(
            Map.entry("51", new PaisTelefono("Perú", 9, 9)),
            Map.entry("54", new PaisTelefono("Argentina", 10, 11)),
            Map.entry("591", new PaisTelefono("Bolivia", 8, 8)),
            Map.entry("55", new PaisTelefono("Brasil", 10, 11)),
            Map.entry("56", new PaisTelefono("Chile", 9, 9)),
            Map.entry("57", new PaisTelefono("Colombia", 10, 10)),
            Map.entry("506", new PaisTelefono("Costa Rica", 8, 8)),
            Map.entry("53", new PaisTelefono("Cuba", 8, 8)),
            Map.entry("593", new PaisTelefono("Ecuador", 8, 9)),
            Map.entry("502", new PaisTelefono("Guatemala", 8, 8)),
            Map.entry("52", new PaisTelefono("México", 10, 10)),
            Map.entry("507", new PaisTelefono("Panamá", 7, 8)),
            Map.entry("595", new PaisTelefono("Paraguay", 8, 9)),
            Map.entry("598", new PaisTelefono("Uruguay", 8, 8)),
            Map.entry("58", new PaisTelefono("Venezuela", 10, 10)),
            Map.entry("1", new PaisTelefono("Estados Unidos / Canadá", 10, 10)),
            Map.entry("49", new PaisTelefono("Alemania", 10, 11)),
            Map.entry("61", new PaisTelefono("Australia", 9, 9)),
            Map.entry("86", new PaisTelefono("China", 11, 11)),
            Map.entry("82", new PaisTelefono("Corea del Sur", 9, 10)),
            Map.entry("34", new PaisTelefono("España", 9, 9)),
            Map.entry("33", new PaisTelefono("Francia", 9, 9)),
            Map.entry("91", new PaisTelefono("India", 10, 10)),
            Map.entry("972", new PaisTelefono("Israel", 9, 9)),
            Map.entry("39", new PaisTelefono("Italia", 9, 10)),
            Map.entry("81", new PaisTelefono("Japón", 10, 10)),
            Map.entry("31", new PaisTelefono("Países Bajos", 9, 9)),
            Map.entry("351", new PaisTelefono("Portugal", 9, 9)),
            Map.entry("44", new PaisTelefono("Reino Unido", 10, 10)),
            Map.entry("7", new PaisTelefono("Rusia", 10, 10)),
            Map.entry("41", new PaisTelefono("Suiza", 9, 9))
    );

    public static final String TELEFONO_PAIS = "^\\+\\d{1,3} \\d{7,11}$";
    public static final String MSG_TELEFONO_PAIS = "Elige el código de país y escribe solo los dígitos del número.";

    // Devuelve el mensaje de error, o null si el teléfono es válido para su país.
    public static String errorTelefonoPais(String telefono) {
        if (telefono == null || telefono.isBlank()) return "El teléfono es obligatorio.";
        String t = telefono.trim();
        if (!t.matches(TELEFONO_PAIS)) return MSG_TELEFONO_PAIS;
        int espacio = t.indexOf(' ');
        String codigo = t.substring(1, espacio);
        String numero = t.substring(espacio + 1);
        PaisTelefono pais = PAISES_TELEFONO.get(codigo);
        if (pais == null) return "El código de país +" + codigo + " no está en la lista de países admitidos.";
        int n = numero.length();
        if (n < pais.min() || n > pais.max()) {
            String cantidad = pais.min() == pais.max() ? pais.min() + " dígitos" : "entre " + pais.min() + " y " + pais.max() + " dígitos";
            return "Para " + pais.nombre() + " (+" + codigo + ") el número debe tener " + cantidad + ".";
        }
        return null;
    }
}
