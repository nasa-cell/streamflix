package streamflix.ia;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import streamflix.modelo.Pelicula;

/** Convierte una película en las características que usa el árbol de decisión. */
public final class Caracteristicas {

    public static final List<String> NOMBRES = List.of(
            "Género principal", "Género secundario", "Tipo", "Duración", "Año", "Clasificación", "Plataforma");

    private Caracteristicas() {}

    public static Map<String, String> de(Pelicula p) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Género principal", p.generoPrincipal());
        m.put("Género secundario", p.generos().size() > 1 ? p.generos().get(1) : "Ninguno");
        m.put("Tipo", p.tipo());
        m.put("Duración", duracion(p));
        m.put("Año", String.valueOf(p.anio()));
        m.put("Clasificación", p.aptaParaNinos() ? "Apta para todos" : (p.clasificacion().equals("+13") ? "+13" : "+14 o más"));
        m.put("Plataforma", p.plataformas().isEmpty() ? "Ninguna" : p.plataformas().get(0).nombre());
        return m;
    }

    private static String duracion(Pelicula p) {
        if ("Serie".equals(p.tipo())) return p.duracionMin() <= 30 ? "Capítulos cortos" : "Capítulos largos";
        if (p.duracionMin() < 110) return "Corta (menos de 1 h 50)";
        if (p.duracionMin() <= 140) return "Media (1 h 50 a 2 h 20)";
        return "Larga (más de 2 h 20)";
    }
}
