package streamflix.ia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filtrado colaborativo basado en usuarios: si perfiles parecidos a ti
 * calificaron bien una película, es probable que a ti también te guste.
 */
public class FiltradoColaborativo {

    private final DatosEntrenamiento datos;
    private final int cantidadVecinos;
    private final Map<Integer, List<Vecino>> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public FiltradoColaborativo(DatosEntrenamiento datos, int cantidadVecinos) {
        this.datos = datos;
        this.cantidadVecinos = cantidadVecinos;
    }

    /** Los perfiles más parecidos, de mayor a menor similitud. */
    public List<Vecino> vecinos(int perfilId) {
        return cache.computeIfAbsent(perfilId, id -> {
            Map<Integer, Double> mias = datos.valoresDe(id);
            double miMedia = datos.promedioDe(id);
            List<Vecino> lista = new ArrayList<>();
            for (int otro : datos.perfiles()) {
                if (otro == id) continue;
                Map<Integer, Double> suyas = datos.valoresDe(otro);
                double s = Similitud.pearson(mias, miMedia, suyas, datos.promedioDe(otro));
                if (s > 0.05) lista.add(new Vecino(otro, s, Similitud.enComun(mias, suyas)));
            }
            List<Vecino> ordenados = OrdenRecursivo.ordenar(lista, (a, b) -> Double.compare(b.similitud(), a.similitud()));
            return ordenados.subList(0, Math.min(cantidadVecinos, ordenados.size()));
        });
    }

    /**
     * Estrellas que el perfil le daría a la película. Devuelve NaN si ningún
     * vecino la calificó (entonces no se puede predecir por este método).
     */
    public double predecir(int perfilId, int peliculaId) {
        Map<Integer, Double> quienes = datos.valoresDePelicula(peliculaId);
        double num = 0, den = 0;
        for (Vecino v : vecinos(perfilId)) {
            Double e = quienes.get(v.perfilId());
            if (e == null) continue;
            num += v.similitud() * (e - datos.promedioDe(v.perfilId()));
            den += v.similitud();
        }
        if (den == 0) return Double.NaN;
        return limitar(datos.promedioDe(perfilId) + num / den);
    }

    /** El vecino más parecido que le dio 4 o 5 estrellas a esa película, o null. */
    public Vecino mejorVecinoQueLaDisfruto(int perfilId, int peliculaId) {
        Map<Integer, Double> quienes = datos.valoresDePelicula(peliculaId);
        for (Vecino v : vecinos(perfilId)) {
            Double e = quienes.get(v.perfilId());
            if (e != null && e >= 4) return v;
        }
        return null;
    }

    static double limitar(double estrellas) {
        return Math.max(1, Math.min(5, estrellas));
    }
}
