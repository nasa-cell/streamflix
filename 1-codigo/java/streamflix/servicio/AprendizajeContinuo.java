package streamflix.servicio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import streamflix.dao.CalificacionDAO;
import streamflix.dao.ComentarioDAO;
import streamflix.dao.GustoDAO;
import streamflix.dao.HistorialDAO;
import streamflix.dao.VistaDAO;
import streamflix.ia.ResultadoMetodo;
import streamflix.ia.ValidacionCruzada;
import streamflix.modelo.Calificacion;
import streamflix.modelo.Pelicula;

/**
 * La IA sigue aprendiendo con el uso. El modelo ya se vuelve a entrenar con cada
 * calificación, comentario o vista; además, cada vez que llegan suficientes
 * calificaciones nuevas, esta clase vuelve a medir la precisión (en segundo plano,
 * sin hacer esperar a nadie) y la guarda en historial_ia para ver cómo mejora.
 */
public final class AprendizajeContinuo {

    /** Cada cuántas calificaciones nuevas se vuelve a medir. */
    public static final int CADA = 20;
    private static final AtomicBoolean midiendo = new AtomicBoolean(false);
    private static final HistorialDAO historial = new HistorialDAO();

    private AprendizajeContinuo() {}

    /** Se llama después de cada entrenamiento; si toca medir, lanza la medición en otro hilo. */
    public static void revisar(int calificacionesActuales) {
        if (midiendo.get()) return;
        int ultima;
        try {
            ultima = historial.ultimaCantidad();
        } catch (Exception e) {
            return;
        }
        if (ultima >= 0 && Math.abs(calificacionesActuales - ultima) < CADA) return;
        if (!midiendo.compareAndSet(false, true)) return;
        Thread hilo = new Thread(() -> {
            try {
                medir(ultima < 0);
            } catch (Exception e) {
                System.err.println("No se pudo medir el aprendizaje: " + e.getMessage());
            } finally {
                midiendo.set(false);
            }
        }, "aprendizaje-continuo");
        hilo.setDaemon(true);
        hilo.setPriority(Thread.MIN_PRIORITY);
        hilo.start();
    }

    /**
     * La primera vez mide con el 25, 50, 75 y 100 % de las calificaciones (elegidas al azar)
     * para que se vea desde el inicio cómo mejora con más datos. Después mide solo el total.
     */
    private static void medir(boolean primeraVez) throws Exception {
        Map<Integer, Pelicula> peliculas = CacheDatos.peliculas();
        List<Calificacion> todas = new CalificacionDAO().todas();
        Map<Integer, Set<String>> gustos = new GustoDAO().declarados();
        Map<Integer, Map<Integer, Integer>> minutos = new VistaDAO().minutos();
        Map<Long, String> comentarios = new ComentarioDAO().textosPorPar();
        if (primeraVez) {
            List<Calificacion> mezcla = new ArrayList<>(todas);
            Collections.shuffle(mezcla, new Random(2025));
            for (int parte : new int[]{25, 50, 75}) {
                List<Calificacion> sub = mezcla.subList(0, mezcla.size() * parte / 100);
                guardar(peliculas, sub, gustos, minutos, comentarios, "Inicio: con el " + parte + " % de los datos de ejemplo");
            }
            guardar(peliculas, todas, gustos, minutos, comentarios, "Inicio: con todos los datos de ejemplo");
        } else {
            int antes = historial.ultimaCantidad();
            int diferencia = todas.size() - antes;
            String nota = diferencia >= 0 ? "Llegaron " + diferencia + " calificaciones nuevas"
                    : "Se borraron " + (-diferencia) + " calificaciones";
            guardar(peliculas, todas, gustos, minutos, comentarios, nota);
        }
    }

    private static void guardar(Map<Integer, Pelicula> peliculas, List<Calificacion> calificaciones,
                                Map<Integer, Set<String>> gustos, Map<Integer, Map<Integer, Integer>> minutos,
                                Map<Long, String> comentarios, String nota) throws Exception {
        Set<Long> pares = new HashSet<>();
        Set<Integer> perfiles = new HashSet<>();
        for (Calificacion c : calificaciones) {
            pares.add(c.perfilId() * 1_000_000L + c.peliculaId());
            perfiles.add(c.perfilId());
        }
        Map<Integer, Map<Integer, Integer>> minSub = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Integer>> e : minutos.entrySet()) {
            for (Map.Entry<Integer, Integer> v : e.getValue().entrySet()) {
                if (pares.contains(e.getKey() * 1_000_000L + v.getKey())) {
                    minSub.computeIfAbsent(e.getKey(), k -> new HashMap<>()).put(v.getKey(), v.getValue());
                }
            }
        }
        Map<Long, String> comSub = new HashMap<>();
        for (Map.Entry<Long, String> e : comentarios.entrySet()) if (pares.contains(e.getKey())) comSub.put(e.getKey(), e.getValue());

        List<ResultadoMetodo> r = new ValidacionCruzada().ejecutarRepetida(peliculas, calificaciones, gustos, minSub, comSub, 5, 2);
        for (ResultadoMetodo m : r) {
            if (m.nombre().equals(ValidacionCruzada.HIBRIDO)) {
                historial.guardar(calificaciones.size(), perfiles.size(),
                        Math.round(m.mae() * 1000) / 1000.0, Math.round(m.precision() * 1000) / 10.0, nota);
            }
        }
    }
}
