package pe.edu.upeu.lp2_gp2.util;

public final class Texto {
    private Texto() {
    }

    public static String nz(String s) {
        return s == null ? "" : s;
    }

    public static String limpio(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
