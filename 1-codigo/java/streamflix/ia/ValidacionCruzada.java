package streamflix.ia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import streamflix.modelo.Calificacion;
import streamflix.modelo.Pelicula;

/**
 * Validación cruzada de k partes (k-fold cross-validation).
 * Las calificaciones se mezclan y se dividen en k partes. En cada vuelta una
 * parte se esconde (prueba) y los modelos aprenden con el resto (entrenamiento);
 * luego se mide qué tan bien adivinan lo escondido. Al final se promedian las k vueltas.
 * Las vistas y comentarios de las películas escondidas también se esconden,
 * para que el modelo no "copie" la respuesta.
 */
public class ValidacionCruzada {

    /** Cuántas películas de prueba se recomiendan a cada perfil para medir la precisión. */
    public static final int TOP = 3;

    public static final String AZAR = "Al azar (referencia)";
    public static final String ACTUAL = "Sistema actual (más populares)";
    public static final String COLABORATIVO = "Colaborativo por usuarios";
    public static final String POR_ITEMS = "Colaborativo por películas";
    public static final String CONTENIDO = "Análisis de contenido";
    public static final String ARBOL = "Árbol de decisión";
    public static final String FACTORIZACION = "Factorización de matrices";
    public static final String HIBRIDO_FIJO = "Híbrido anterior (pesos fijos)";
    public static final String SOLO_ESTRELLAS = "Híbrido mejorado solo con estrellas";
    public static final String HIBRIDO = "Híbrido mejorado (pesos aprendidos)";

    private interface Predictor {
        double estrellas(int perfilId, Pelicula p);
    }

    private static final class Acumulado {
        double errorAbs, errorCuad, n, precision, satisfaccion, minutos, usuarios, nanos;
        final List<Double> porParte = new ArrayList<>();
    }

    /**
     * Validación cruzada repetida: corre la de k partes varias veces, cada vez
     * repartiendo las calificaciones al azar de otra forma, y promedia.
     * Así el resultado no depende de la suerte de un solo reparto.
     */
    public List<ResultadoMetodo> ejecutarRepetida(Map<Integer, Pelicula> peliculas, List<Calificacion> calificaciones,
                                                  Map<Integer, Set<String>> gustos, Map<Integer, Map<Integer, Integer>> minutos,
                                                  Map<Long, String> comentarios, int k, int repeticiones) {
        List<List<ResultadoMetodo>> vueltas = new ArrayList<>();
        for (int r = 0; r < repeticiones; r++) {
            vueltas.add(ejecutar(peliculas, calificaciones, gustos, minutos, comentarios, k, 42 + r));
        }
        List<ResultadoMetodo> promedio = new ArrayList<>();
        for (int m = 0; m < vueltas.get(0).size(); m++) {
            double mae = 0, rmse = 0, pre = 0, sat = 0, min = 0, ms = 0;
            List<Double> porVuelta = new ArrayList<>();
            for (List<ResultadoMetodo> v : vueltas) {
                ResultadoMetodo x = v.get(m);
                mae += x.mae(); rmse += x.rmse(); pre += x.precision(); sat += x.satisfaccion(); min += x.minutos(); ms += x.milisegundos();
                porVuelta.add(x.precision());
            }
            int n = vueltas.size();
            promedio.add(new ResultadoMetodo(vueltas.get(0).get(m).nombre(), mae / n, rmse / n, pre / n, sat / n, min / n, ms / n, porVuelta));
        }
        return promedio;
    }

    public List<ResultadoMetodo> ejecutar(Map<Integer, Pelicula> peliculas, List<Calificacion> calificaciones,
                                          Map<Integer, Set<String>> gustos,
                                          Map<Integer, Map<Integer, Integer>> minutos,
                                          Map<Long, String> comentarios, int k, long semilla) {
        List<Calificacion> mezcladas = new ArrayList<>(calificaciones);
        Collections.shuffle(mezcladas, new Random(semilla));
        boolean senalesExtra = !minutos.isEmpty() || !comentarios.isEmpty();
        Map<String, Acumulado> acumulados = new LinkedHashMap<>();

        for (int parte = 0; parte < k; parte++) {
            List<Calificacion> entrenamiento = new ArrayList<>(), prueba = new ArrayList<>();
            for (int i = 0; i < mezcladas.size(); i++) (i % k == parte ? prueba : entrenamiento).add(mezcladas.get(i));

            Set<Long> escondidas = new HashSet<>();
            for (Calificacion c : prueba) escondidas.add(Senal.par(c.perfilId(), c.peliculaId()));
            DatosEntrenamiento datos = new DatosEntrenamiento(peliculas,
                    CombinadorSenales.combinar(peliculas, entrenamiento, minutos, comentarios, escondidas), gustos);
            RecomendadorHibrido hibrido = new RecomendadorHibrido(datos, semilla + parte).aprenderPesos();
            Map<String, Predictor> predictores = new LinkedHashMap<>();
            Random azar = new Random(semilla * 31 + parte);
            predictores.put(AZAR, (u, p) -> 1 + 4 * azar.nextDouble());
            predictores.put(ACTUAL, (u, p) -> hibrido.popular().predecir(p.id()));
            predictores.put(COLABORATIVO, (u, p) -> {
                double v = hibrido.colaborativo().predecir(u, p.id());
                return Double.isNaN(v) ? datos.promedioDe(u) : v;
            });
            predictores.put(POR_ITEMS, (u, p) -> {
                double v = hibrido.porItems().predecir(u, p.id());
                return Double.isNaN(v) ? datos.promedioDe(u) : v;
            });
            predictores.put(CONTENIDO, (u, p) -> hibrido.contenido().predecir(u, p));
            predictores.put(ARBOL, (u, p) -> 1 + 4 * hibrido.arbolDe(u).predecir(Caracteristicas.de(p)));
            predictores.put(FACTORIZACION, (u, p) -> hibrido.factorizacion().predecir(u, p.id()));
            predictores.put(HIBRIDO_FIJO, hibrido::predecirEstrellasPesosFijos);
            if (senalesExtra) {
                RecomendadorHibrido soloEstrellas = new RecomendadorHibrido(
                        new DatosEntrenamiento(peliculas, CombinadorSenales.soloEstrellas(entrenamiento), gustos), semilla + parte).aprenderPesos();
                predictores.put(SOLO_ESTRELLAS, soloEstrellas::predecirEstrellas);
            }
            predictores.put(HIBRIDO, hibrido::predecirEstrellas);
            for (String m : predictores.keySet()) acumulados.computeIfAbsent(m, x -> new Acumulado());

            Map<Integer, List<Calificacion>> pruebaPorPerfil = new HashMap<>();
            for (Calificacion c : prueba) pruebaPorPerfil.computeIfAbsent(c.perfilId(), x -> new ArrayList<>()).add(c);

            for (Map.Entry<String, Predictor> m : predictores.entrySet()) {
                Acumulado a = acumulados.get(m.getKey());
                double precisionParte = 0, usuariosParte = 0;
                long inicio = System.nanoTime();
                for (Map.Entry<Integer, List<Calificacion>> e : pruebaPorPerfil.entrySet()) {
                    int perfil = e.getKey();
                    List<double[]> predichas = new ArrayList<>();
                    for (Calificacion c : e.getValue()) {
                        double estimada = m.getValue().estrellas(perfil, peliculas.get(c.peliculaId()));
                        double error = estimada - c.estrellas();
                        a.errorAbs += Math.abs(error);
                        a.errorCuad += error * error;
                        a.n++;
                        predichas.add(new double[]{estimada, c.estrellas(), c.peliculaId()});
                    }
                    if (predichas.size() > TOP) {
                        List<double[]> orden = OrdenRecursivo.ordenar(predichas, (x, y) -> Double.compare(y[0], x[0]));
                        double aciertos = 0, estrellas = 0, mins = 0;
                        for (int i = 0; i < TOP; i++) {
                            double[] x = orden.get(i);
                            if (x[1] >= 4) aciertos++;
                            estrellas += x[1];
                            mins += minutos.getOrDefault(perfil, Map.of()).getOrDefault((int) x[2], 0);
                        }
                        a.precision += aciertos / TOP;
                        a.satisfaccion += estrellas / TOP;
                        a.minutos += mins / TOP;
                        a.usuarios++;
                        precisionParte += aciertos / TOP;
                        usuariosParte++;
                    }
                }
                a.nanos += System.nanoTime() - inicio;
                a.porParte.add(usuariosParte == 0 ? 0 : precisionParte / usuariosParte);
            }
        }

        List<ResultadoMetodo> resultados = new ArrayList<>();
        for (Map.Entry<String, Acumulado> e : acumulados.entrySet()) {
            Acumulado a = e.getValue();
            resultados.add(new ResultadoMetodo(e.getKey(), a.errorAbs / a.n, Math.sqrt(a.errorCuad / a.n),
                    a.precision / a.usuarios, a.satisfaccion / a.usuarios, a.minutos / a.usuarios,
                    a.nanos / 1e6, a.porParte));
        }
        return resultados;
    }
}
