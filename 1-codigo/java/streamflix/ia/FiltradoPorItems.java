package streamflix.ia;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtrado colaborativo por películas ("quienes vieron esto también vieron…").
 * Dos películas se parecen si las mismas personas las califican de forma parecida
 * (coseno ajustado: a cada nota se le resta el promedio de quien la puso).
 * Para adivinar la nota de una película se miran las películas más parecidas
 * que el perfil ya calificó.
 */
public class FiltradoPorItems {

    private static final int VECINAS = 20;
    private static final double ENCOGIMIENTO = 10;
    private static final int MAXIMO_GUARDADAS = 1_500_000;

    private final DatosEntrenamiento datos;
    private final Map<Long, Double> guardadas = new ConcurrentHashMap<>();

    public FiltradoPorItems(DatosEntrenamiento datos) {
        this.datos = datos;
    }

    /** Estrellas estimadas de 1 a 5, o NaN si no hay películas parecidas calificadas. */
    public double predecir(int perfilId, int peliculaId) {
        Map<Integer, Double> mias = datos.valoresDe(perfilId);
        if (mias.isEmpty()) return Double.NaN;
        double media = datos.promedioDe(perfilId);
        List<double[]> parecidas = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : mias.entrySet()) {
            if (e.getKey() == peliculaId) continue;
            double s = similitud(peliculaId, e.getKey());
            if (s > 0) parecidas.add(new double[]{s, e.getValue()});
        }
        if (parecidas.isEmpty()) return Double.NaN;
        List<double[]> orden = OrdenRecursivo.ordenar(parecidas, (a, b) -> Double.compare(b[0], a[0]));
        double arriba = 0, abajo = 0;
        for (int k = 0; k < Math.min(VECINAS, orden.size()); k++) {
            arriba += orden.get(k)[0] * (orden.get(k)[1] - media);
            abajo += orden.get(k)[0];
        }
        return Math.max(1, Math.min(5, media + arriba / abajo));
    }

    /** Coseno ajustado entre dos películas, reducido si las comparten pocas personas. */
    public double similitud(int a, int b) {
        int menor = Math.min(a, b), mayor = Math.max(a, b);
        long clave = ((long) menor << 32) | (mayor & 0xffffffffL);
        Double guardada = guardadas.get(clave);
        if (guardada != null) return guardada;
        Map<Integer, Double> ra = datos.valoresDePelicula(a), rb = datos.valoresDePelicula(b);
        if (ra.size() > rb.size()) {
            Map<Integer, Double> t = ra;
            ra = rb;
            rb = t;
        }
        double prod = 0, na = 0, nb = 0;
        int comunes = 0;
        for (Map.Entry<Integer, Double> e : ra.entrySet()) {
            Double vb = rb.get(e.getKey());
            if (vb == null) continue;
            double m = datos.promedioDe(e.getKey());
            double x = e.getValue() - m, y = vb - m;
            prod += x * y;
            na += x * x;
            nb += y * y;
            comunes++;
        }
        double s = (na == 0 || nb == 0) ? 0 : prod / Math.sqrt(na * nb) * comunes / (comunes + ENCOGIMIENTO);
        if (guardadas.size() < MAXIMO_GUARDADAS) guardadas.put(clave, s);
        return s;
    }
}
