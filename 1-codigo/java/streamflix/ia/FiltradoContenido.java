package streamflix.ia;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import streamflix.modelo.Pelicula;

/**
 * Análisis de contenido: arma un "vector de gustos" del perfil con los géneros
 * de lo que calificó (sumando si le gustó, restando si no) más los gustos que
 * eligió al crear su perfil, y lo compara con los géneros de cada película.
 */
public class FiltradoContenido {

    private static final double PESO_GENERO_PRINCIPAL = 1.0;
    private static final double PESO_GENERO_SECUNDARIO = 0.6;
    private static final double PESO_GUSTO_DECLARADO = 1.2;

    private final DatosEntrenamiento datos;
    private final Map<Integer, Map<String, Double>> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public FiltradoContenido(DatosEntrenamiento datos) {
        this.datos = datos;
    }

    public static Map<String, Double> vectorDePelicula(Pelicula p) {
        Map<String, Double> v = new LinkedHashMap<>();
        List<String> g = p.generos();
        for (int i = 0; i < g.size(); i++) v.put(g.get(i), i == 0 ? PESO_GENERO_PRINCIPAL : PESO_GENERO_SECUNDARIO);
        return v;
    }

    public Map<String, Double> vectorDePerfil(int perfilId) {
        return cache.computeIfAbsent(perfilId, id -> {
            Map<String, Double> v = new HashMap<>();
            double media = datos.promedioDe(id);
            for (Map.Entry<Integer, Double> e : datos.valoresDe(id).entrySet()) {
                Pelicula p = datos.peliculas().get(e.getKey());
                if (p == null) continue;
                double peso = e.getValue() - Math.min(media, 3.5);
                for (Map.Entry<String, Double> g : vectorDePelicula(p).entrySet()) {
                    v.merge(g.getKey(), peso * g.getValue(), Double::sum);
                }
            }
            for (String g : datos.gustosDe(id)) v.merge(g, PESO_GUSTO_DECLARADO, Double::sum);
            return v;
        });
    }

    /** Parecido entre el perfil y la película, de -1 (nada que ver) a 1 (justo su estilo). */
    public double puntaje(int perfilId, Pelicula p) {
        return Similitud.coseno(vectorDePerfil(perfilId), vectorDePelicula(p));
    }

    public double predecir(int perfilId, Pelicula p) {
        return FiltradoColaborativo.limitar(datos.promedioDe(perfilId) + 1.6 * puntaje(perfilId, p));
    }

    /** Géneros que más le gustan al perfil, de mayor a menor, en porcentaje. */
    public Map<String, Integer> generosFavoritos(int perfilId, int cuantos) {
        Map<String, Double> v = vectorDePerfil(perfilId);
        double max = 0;
        for (double x : v.values()) max = Math.max(max, x);
        List<Map.Entry<String, Double>> orden = OrdenRecursivo.ordenar(
                List.copyOf(v.entrySet()), (a, b) -> Double.compare(b.getValue(), a.getValue()));
        Map<String, Integer> r = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : orden) {
            if (r.size() >= cuantos || e.getValue() <= 0) break;
            r.put(e.getKey(), (int) Math.round(100 * e.getValue() / max));
        }
        return r;
    }
}
