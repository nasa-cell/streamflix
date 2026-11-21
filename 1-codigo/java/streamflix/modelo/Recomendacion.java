package streamflix.modelo;

import java.util.Map;
import streamflix.util.ConvertibleAJson;
import streamflix.util.Json;

/**
 * Película recomendada con su puntaje final (0 a 1) y la parte que aportó
 * cada técnica: colaborativo, contenido y árbol de decisión.
 */
public record Recomendacion(Pelicula pelicula, double puntaje, double colaborativo,
                            double contenido, double arbol, String motivo) implements ConvertibleAJson {
    @Override
    public Map<String, Object> aMapa() {
        Map<String, Object> m = pelicula.aMapa();
        m.put("puntaje", Math.round(puntaje * 100));
        m.put("colaborativo", Math.round(colaborativo * 100));
        m.put("contenido", Math.round(contenido * 100));
        m.put("arbol", Math.round(arbol * 100));
        m.put("motivo", motivo);
        return m;
    }
}
