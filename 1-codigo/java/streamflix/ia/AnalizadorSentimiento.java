package streamflix.ia;

import java.text.Normalizer;
import java.util.Map;
import java.util.Set;

/**
 * Lee un comentario y calcula si es positivo o negativo (de -1 a 1) con un
 * diccionario de palabras en español. Entiende negaciones simples:
 * "no me gustó" cuenta como negativo.
 */
public final class AnalizadorSentimiento {

    private static final Map<String, Double> PALABRAS = Map.ofEntries(
            Map.entry("encanto", 1.0), Map.entry("encantaron", 1.0), Map.entry("excelente", 1.0),
            Map.entry("increible", 0.9), Map.entry("increibles", 0.9), Map.entry("mejor", 0.8),
            Map.entry("mejores", 0.8), Map.entry("genial", 0.9), Map.entry("hermosa", 0.8),
            Map.entry("bonita", 0.7), Map.entry("buena", 0.6), Map.entry("bueno", 0.6),
            Map.entry("buenos", 0.6), Map.entry("buenas", 0.6), Map.entry("gusto", 0.6),
            Map.entry("entretenida", 0.6), Map.entry("recomendable", 0.8), Map.entry("recomiendo", 0.8),
            Map.entry("divertida", 0.7), Map.entry("emocionante", 0.8), Map.entry("volveria", 0.7),
            Map.entry("perfecta", 1.0), Map.entry("obra", 0.5), Map.entry("bien", 0.4),
            Map.entry("aburrida", -0.9), Map.entry("aburrido", -0.9), Map.entry("floja", -0.7),
            Map.entry("mala", -0.9), Map.entry("malo", -0.9), Map.entry("pesima", -1.0),
            Map.entry("horrible", -1.0), Map.entry("lenta", -0.5), Map.entry("lentas", -0.5),
            Map.entry("larga", -0.3), Map.entry("decepcion", -0.8), Map.entry("convencio", 0.5),
            Map.entry("odie", -1.0), Map.entry("peor", -0.9), Map.entry("perdida", -0.7));

    private static final Set<String> NEGACIONES = Set.of("no", "nunca", "nada", "ni", "tampoco");

    private AnalizadorSentimiento() {}

    public static double analizar(String texto) {
        if (texto == null || texto.isBlank()) return 0;
        String limpio = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-zñ ]", " ");
        String[] palabras = limpio.trim().split(" +");
        double suma = 0;
        int encontradas = 0;
        for (int i = 0; i < palabras.length; i++) {
            Double valor = PALABRAS.get(palabras[i]);
            if (valor == null) continue;
            boolean negada = (i > 0 && NEGACIONES.contains(palabras[i - 1]))
                    || (i > 1 && NEGACIONES.contains(palabras[i - 2]));
            suma += negada ? -valor : valor;
            encontradas++;
        }
        if (encontradas == 0) return 0;
        return Math.max(-1, Math.min(1, suma / encontradas));
    }

    public static String etiqueta(double sentimiento) {
        if (sentimiento > 0.2) return "positivo";
        if (sentimiento < -0.2) return "negativo";
        return "neutral";
    }
}
