package streamflix.ia;

import java.util.Map;

/**
 * Sistema actual de StreamFlix: recomienda a todos lo mismo, las películas
 * mejor calificadas en general (promedio bayesiano para que una película con
 * un solo voto de 5 estrellas no quede primera). Sirve de punto de comparación.
 */
public class SistemaPopular {

    private static final double VOTOS_MINIMOS = 5;
    private final DatosEntrenamiento datos;

    public SistemaPopular(DatosEntrenamiento datos) {
        this.datos = datos;
    }

    public double predecir(int peliculaId) {
        Map<Integer, Double> votos = datos.valoresDePelicula(peliculaId);
        double suma = 0;
        for (double v : votos.values()) suma += v;
        return (suma + VOTOS_MINIMOS * datos.promedioGlobal()) / (votos.size() + VOTOS_MINIMOS);
    }

    /** Popularidad de 0 a 1: mezcla la nota y cuánta gente la vio. */
    public double popularidad(int peliculaId) {
        int votos = datos.valoresDePelicula(peliculaId).size();
        double nota = (predecir(peliculaId) - 1) / 4;
        return 0.7 * nota + 0.3 * Math.min(1, votos / 30.0);
    }
}
