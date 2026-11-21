package streamflix.ia;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import streamflix.modelo.Pelicula;

/**
 * Datos con los que aprenden los algoritmos: la matriz perfil x película con
 * qué tanto le gustó (1 a 5, desde estrellas, vistas y comentarios), el
 * catálogo y los gustos que cada perfil declaró.
 */
public class DatosEntrenamiento {

    private final Map<Integer, Pelicula> peliculas;
    private final Map<Integer, Map<Integer, Double>> valorPorPerfil = new HashMap<>();
    private final Map<Integer, Map<Integer, Double>> valorPorPelicula = new HashMap<>();
    private final Map<Integer, Set<String>> gustos;
    private final Map<Integer, Double> promedioPerfil = new HashMap<>();
    private final Map<String, Integer> conteoPorFuente = new HashMap<>();
    private final double promedioGlobal;

    public DatosEntrenamiento(Map<Integer, Pelicula> peliculas, Collection<Senal> senales,
                              Map<Integer, Set<String>> gustos) {
        this.peliculas = peliculas;
        this.gustos = gustos;
        double suma = 0;
        for (Senal s : senales) {
            valorPorPerfil.computeIfAbsent(s.perfilId(), k -> new HashMap<>()).put(s.peliculaId(), s.valor());
            valorPorPelicula.computeIfAbsent(s.peliculaId(), k -> new HashMap<>()).put(s.perfilId(), s.valor());
            conteoPorFuente.merge(s.fuente(), 1, Integer::sum);
            suma += s.valor();
        }
        promedioGlobal = senales.isEmpty() ? 3.0 : suma / senales.size();
        for (Map.Entry<Integer, Map<Integer, Double>> e : valorPorPerfil.entrySet()) {
            double s = 0;
            for (double v : e.getValue().values()) s += v;
            promedioPerfil.put(e.getKey(), s / e.getValue().size());
        }
    }

    public Map<Integer, Pelicula> peliculas() {
        return peliculas;
    }

    public Map<Integer, Double> valoresDe(int perfilId) {
        return valorPorPerfil.getOrDefault(perfilId, Collections.emptyMap());
    }

    public Map<Integer, Double> valoresDePelicula(int peliculaId) {
        return valorPorPelicula.getOrDefault(peliculaId, Collections.emptyMap());
    }

    public Set<Integer> perfiles() {
        return valorPorPerfil.keySet();
    }

    public Set<String> gustosDe(int perfilId) {
        return gustos.getOrDefault(perfilId, Collections.emptySet());
    }

    public double promedioDe(int perfilId) {
        return promedioPerfil.getOrDefault(perfilId, promedioGlobal);
    }

    public double promedioGlobal() {
        return promedioGlobal;
    }

    public Map<String, Integer> conteoPorFuente() {
        return conteoPorFuente;
    }
}
