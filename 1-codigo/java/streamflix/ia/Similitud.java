package streamflix.ia;

import java.util.Map;

/** Medidas de parecido entre dos perfiles o entre un perfil y una película. */
public final class Similitud {

    /** A partir de cuántas películas en común se confía del todo en el parecido. */
    private static final int MINIMO_CONFIABLE = 5;

    private Similitud() {}

    /**
     * Correlación de Pearson entre dos perfiles usando solo las películas que
     * ambos calificaron. Se reduce si tienen pocas películas en común.
     */
    public static double pearson(Map<Integer, Double> a, double mediaA, Map<Integer, Double> b, double mediaB) {
        double num = 0, denA = 0, denB = 0;
        int comunes = 0;
        Map<Integer, Double> chico = a.size() <= b.size() ? a : b;
        Map<Integer, Double> grande = chico == a ? b : a;
        for (Map.Entry<Integer, Double> e : chico.entrySet()) {
            Double otra = grande.get(e.getKey());
            if (otra == null) continue;
            double da = (chico == a ? e.getValue() : otra) - mediaA;
            double db = (chico == a ? otra : e.getValue()) - mediaB;
            num += da * db;
            denA += da * da;
            denB += db * db;
            comunes++;
        }
        if (comunes < 2 || denA == 0 || denB == 0) return 0;
        double r = num / Math.sqrt(denA * denB);
        return r * Math.min(comunes, MINIMO_CONFIABLE) / MINIMO_CONFIABLE;
    }

    public static int enComun(Map<Integer, Double> a, Map<Integer, Double> b) {
        int n = 0;
        for (Integer k : a.keySet()) if (b.containsKey(k)) n++;
        return n;
    }

    /** Coseno entre dos vectores de géneros (resultado de -1 a 1). */
    public static double coseno(Map<String, Double> a, Map<String, Double> b) {
        double num = 0, na = 0, nb = 0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            na += e.getValue() * e.getValue();
            Double v = b.get(e.getKey());
            if (v != null) num += e.getValue() * v;
        }
        for (double v : b.values()) nb += v * v;
        if (na == 0 || nb == 0) return 0;
        return num / Math.sqrt(na * nb);
    }
}
