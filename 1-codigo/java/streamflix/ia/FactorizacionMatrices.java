package streamflix.ia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import streamflix.util.ConvertibleAJson;
import streamflix.util.Json;

/**
 * Factorización de matrices (técnica popularizada por el Netflix Prize).
 * Cada perfil y cada película se resumen en unos pocos números ("gustos ocultos").
 * La nota que un perfil le daría a una película es:
 *   promedio global + sesgo del perfil + sesgo de la película + (gustos del perfil · rasgos de la película)
 * Se entrena por vueltas (épocas) con descenso de gradiente estocástico.
 * Un 10 % de los datos se guarda aparte para saber cuándo parar (parada temprana)
 * y así no memorizar los datos de entrenamiento.
 */
public class FactorizacionMatrices {

    /** Error al entrenar y en la parte apartada, en cada vuelta. */
    public record Vuelta(int epoca, double errorEntrenamiento, double errorValidacion) implements ConvertibleAJson {
        @Override
        public Map<String, Object> aMapa() {
            return Json.objeto("epoca", epoca, "entrenamiento", Math.round(errorEntrenamiento * 1000) / 1000.0,
                    "validacion", Math.round(errorValidacion * 1000) / 1000.0);
        }
    }

    private static final double APRENDIZAJE = 0.01;
    private static final int MAXIMO_EPOCAS = 150;
    private static final int PACIENCIA = 5;

    private final Map<Integer, Integer> indicePerfil = new HashMap<>();
    private final Map<Integer, Integer> indicePelicula = new HashMap<>();
    private final int factores;
    private final double regularizacion;
    private double media;
    private double[] sesgoPerfil, sesgoPelicula;
    private double[][] gustos, rasgos;
    private final List<Vuelta> curva = new ArrayList<>();
    private int mejorEpoca;
    private double mejorError = Double.MAX_VALUE;

    private FactorizacionMatrices(int factores, double regularizacion) {
        this.factores = factores;
        this.regularizacion = regularizacion;
    }

    /**
     * Ajuste automático: prueba varias combinaciones de cantidad de rasgos y de
     * regularización, y se queda con la que menos se equivoca en la parte apartada.
     */
    public static FactorizacionMatrices entrenarAjustada(DatosEntrenamiento datos, long semilla) {
        FactorizacionMatrices mejor = null;
        for (int f : new int[]{10, 30}) {
            for (double reg : new double[]{0.05, 0.12, 0.25}) {
                FactorizacionMatrices m = entrenar(datos, f, reg, semilla);
                if (mejor == null || m.mejorError < mejor.mejorError) mejor = m;
            }
        }
        return mejor;
    }

    public static FactorizacionMatrices entrenar(DatosEntrenamiento datos, int factores, double regularizacion, long semilla) {
        FactorizacionMatrices m = new FactorizacionMatrices(factores, regularizacion);
        m.ajustar(datos, semilla);
        return m;
    }

    private void ajustar(DatosEntrenamiento datos, long semilla) {
        List<double[]> todas = new ArrayList<>();
        for (int perfil : datos.perfiles()) {
            int u = indicePerfil.computeIfAbsent(perfil, k -> indicePerfil.size());
            for (Map.Entry<Integer, Double> e : datos.valoresDe(perfil).entrySet()) {
                int i = indicePelicula.computeIfAbsent(e.getKey(), k -> indicePelicula.size());
                todas.add(new double[]{u, i, e.getValue()});
            }
        }
        Random azar = new Random(semilla);
        Collections.shuffle(todas, azar);
        int corte = todas.size() >= 50 ? todas.size() / 10 : 0;
        List<double[]> validacion = todas.subList(0, corte);
        List<double[]> entrenamiento = new ArrayList<>(todas.subList(corte, todas.size()));

        double suma = 0;
        for (double[] t : entrenamiento) suma += t[2];
        media = entrenamiento.isEmpty() ? 3 : suma / entrenamiento.size();
        sesgoPerfil = new double[indicePerfil.size()];
        sesgoPelicula = new double[indicePelicula.size()];
        gustos = inicial(indicePerfil.size(), azar);
        rasgos = inicial(indicePelicula.size(), azar);

        double[] mejorSP = null, mejorSI = null;
        double[][] mejorG = null, mejorR = null;
        int sinMejorar = 0;
        for (int epoca = 1; epoca <= MAXIMO_EPOCAS; epoca++) {
            Collections.shuffle(entrenamiento, azar);
            for (double[] t : entrenamiento) paso((int) t[0], (int) t[1], t[2]);
            double errEnt = rmse(entrenamiento);
            double errVal = validacion.isEmpty() ? errEnt : rmse(validacion);
            curva.add(new Vuelta(epoca, errEnt, errVal));
            if (errVal < mejorError - 1e-4) {
                mejorError = errVal;
                mejorEpoca = epoca;
                mejorSP = sesgoPerfil.clone();
                mejorSI = sesgoPelicula.clone();
                mejorG = copia(gustos);
                mejorR = copia(rasgos);
                sinMejorar = 0;
            } else if (++sinMejorar >= PACIENCIA) {
                break;
            }
        }
        if (mejorSP != null) {
            sesgoPerfil = mejorSP;
            sesgoPelicula = mejorSI;
            gustos = mejorG;
            rasgos = mejorR;
        }
    }

    private void paso(int u, int i, double real) {
        double error = real - crudo(u, i);
        sesgoPerfil[u] += APRENDIZAJE * (error - regularizacion * sesgoPerfil[u]);
        sesgoPelicula[i] += APRENDIZAJE * (error - regularizacion * sesgoPelicula[i]);
        double[] g = gustos[u], r = rasgos[i];
        for (int f = 0; f < factores; f++) {
            double gf = g[f];
            g[f] += APRENDIZAJE * (error * r[f] - regularizacion * gf);
            r[f] += APRENDIZAJE * (error * gf - regularizacion * r[f]);
        }
    }

    private double crudo(int u, int i) {
        double s = media + sesgoPerfil[u] + sesgoPelicula[i];
        double[] g = gustos[u], r = rasgos[i];
        for (int f = 0; f < factores; f++) s += g[f] * r[f];
        return s;
    }

    private double rmse(List<double[]> datos) {
        double s = 0;
        for (double[] t : datos) {
            double e = limitar(crudo((int) t[0], (int) t[1])) - t[2];
            s += e * e;
        }
        return Math.sqrt(s / Math.max(1, datos.size()));
    }

    /** Estrellas estimadas de 1 a 5. Si no conoce al perfil o la película usa lo que sí sabe. */
    public double predecir(int perfilId, int peliculaId) {
        Integer u = indicePerfil.get(perfilId), i = indicePelicula.get(peliculaId);
        if (u != null && i != null) return limitar(crudo(u, i));
        double s = media;
        if (u != null) s += sesgoPerfil[u];
        if (i != null) s += sesgoPelicula[i];
        return limitar(s);
    }

    public List<Vuelta> curva() {
        return curva;
    }

    public int mejorEpoca() {
        return mejorEpoca;
    }

    public int factores() {
        return factores;
    }

    public double regularizacion() {
        return regularizacion;
    }

    private double[][] inicial(int filas, Random azar) {
        double[][] m = new double[filas][factores];
        for (double[] fila : m) for (int f = 0; f < factores; f++) fila[f] = azar.nextGaussian() * 0.1;
        return m;
    }

    private static double[][] copia(double[][] m) {
        double[][] c = new double[m.length][];
        for (int k = 0; k < m.length; k++) c[k] = m[k].clone();
        return c;
    }

    private static double limitar(double v) {
        return Math.max(1, Math.min(5, v));
    }
}
