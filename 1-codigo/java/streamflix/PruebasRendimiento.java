package streamflix;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import streamflix.dao.CalificacionDAO;
import streamflix.dao.ComentarioDAO;
import streamflix.dao.GustoDAO;
import streamflix.dao.PeliculaDAO;
import streamflix.dao.VistaDAO;
import streamflix.ia.DatosEntrenamiento;
import streamflix.ia.OrdenRecursivo;
import streamflix.ia.RecomendadorHibrido;
import streamflix.ia.ResultadoMetodo;
import streamflix.ia.Senal;
import streamflix.ia.ValidacionCruzada;
import streamflix.modelo.Calificacion;
import streamflix.modelo.Pelicula;
import streamflix.util.Json;

/**
 * Pruebas de rendimiento para el informe:
 *  1. Validación cruzada con los datos reales de la base.
 *  2. Tiempo para recomendar con 1 000, 5 000 y 10 000 perfiles ficticios.
 *  3. Ordenamiento recursivo (merge sort) frente al de Java.
 * Guarda todo en 3-pruebas/resultados/resultados.json.
 */
public class PruebasRendimiento {

    public static void main(String[] args) throws Exception {
        Map<Integer, Pelicula> peliculas = new PeliculaDAO().todas();
        List<Calificacion> calificaciones = new CalificacionDAO().todas();
        Map<Integer, Set<String>> gustos = new GustoDAO().declarados();
        Map<String, Object> resultado = new LinkedHashMap<>();

        System.out.println("1) Validación cruzada de 5 partes repetida 5 veces...");
        new ValidacionCruzada().ejecutar(peliculas, calificaciones, gustos, new VistaDAO().minutos(), new ComentarioDAO().textosPorPar(), 5, 7);
        long t = System.nanoTime();
        List<ResultadoMetodo> cv = new ValidacionCruzada().ejecutarRepetida(peliculas, calificaciones, gustos,
                new VistaDAO().minutos(), new ComentarioDAO().textosPorPar(), 5, 5);
        resultado.put("validacionCruzada", cv);
        resultado.put("segundosValidacion", Math.round((System.nanoTime() - t) / 1e7) / 100.0);
        for (ResultadoMetodo r : cv) System.out.println("   " + r.aMapa());

        System.out.println("2) Escalabilidad con perfiles ficticios...");
        List<Map<String, Object>> escala = new ArrayList<>();
        for (int n : new int[]{1000, 5000, 10000}) {
            List<Senal> senales = sinteticas(n, new ArrayList<>(peliculas.keySet()), 2025 + n);
            long t0 = System.nanoTime();
            DatosEntrenamiento datos = new DatosEntrenamiento(peliculas, senales, Map.of());
            RecomendadorHibrido h = new RecomendadorHibrido(datos);
            double cargar = (System.nanoTime() - t0) / 1e6;
            Random azar = new Random(1);
            int muestras = 50;
            long t1 = System.nanoTime();
            for (int i = 0; i < muestras; i++) {
                int perfil = azar.nextInt(n) + 1;
                h.recomendar(perfil, peliculas.values(), datos.valoresDe(perfil).keySet(), false, 10, Map.of());
            }
            double porPerfil = (System.nanoTime() - t1) / 1e6 / muestras;
            escala.add(Json.objeto("perfiles", n, "calificaciones", senales.size(),
                    "msCargar", Math.round(cargar), "msPorPerfil", Math.round(porPerfil * 10) / 10.0));
            System.out.printf("   %,d perfiles: cargar %.0f ms, recomendar %.1f ms por perfil%n", n, cargar, porPerfil);
        }
        resultado.put("escalabilidad", escala);

        System.out.println("3) Ordenamiento recursivo...");
        List<Map<String, Object>> orden = new ArrayList<>();
        for (int n : new int[]{1000, 10000, 100000}) {
            List<Double> numeros = new ArrayList<>();
            Random azar = new Random(n);
            for (int i = 0; i < n; i++) numeros.add(azar.nextDouble());
            OrdenRecursivo.ordenar(numeros, Double::compare);
            long a = System.nanoTime();
            List<Double> r1 = OrdenRecursivo.ordenar(numeros, Double::compare);
            double msRec = (System.nanoTime() - a) / 1e6;
            List<Double> copia = new ArrayList<>(numeros);
            long b = System.nanoTime();
            Collections.sort(copia);
            double msJava = (System.nanoTime() - b) / 1e6;
            orden.add(Json.objeto("elementos", n, "msRecursivo", Math.round(msRec * 100) / 100.0,
                    "msJava", Math.round(msJava * 100) / 100.0, "correcto", r1.equals(copia)));
            System.out.printf("   %,d elementos: recursivo %.2f ms, Java %.2f ms, correcto=%s%n", n, msRec, msJava, r1.equals(copia));
        }
        resultado.put("ordenamiento", orden);

        resultado.put("datos", Json.objeto("peliculas", peliculas.size(), "calificaciones", calificaciones.size(),
                "perfiles", new HashSet<>(calificaciones.stream().map(Calificacion::perfilId).toList()).size()));
        Path salida = Path.of("3-pruebas", "resultados", "resultados.json");
        Files.createDirectories(salida.getParent());
        Files.writeString(salida, Json.escribir(resultado), StandardCharsets.UTF_8);
        System.out.println("Resultados guardados en " + salida.toAbsolutePath());
    }

    /** Perfiles ficticios con gustos por grupos de películas, para medir la velocidad. */
    private static List<Senal> sinteticas(int perfiles, List<Integer> ids, long semilla) {
        Random azar = new Random(semilla);
        List<Senal> s = new ArrayList<>();
        Map<Integer, Integer> grupo = new HashMap<>();
        for (int id : ids) grupo.put(id, azar.nextInt(6));
        for (int p = 1; p <= perfiles; p++) {
            int favorito = azar.nextInt(6);
            Set<Integer> vistas = new HashSet<>();
            int cuantas = 12 + azar.nextInt(16);
            while (vistas.size() < cuantas) vistas.add(ids.get(azar.nextInt(ids.size())));
            for (int id : vistas) {
                double base = grupo.get(id) == favorito ? 4.3 : 2.6;
                s.add(new Senal(p, id, Math.max(1, Math.min(5, base + azar.nextGaussian() * 0.8)), Senal.ESTRELLAS));
            }
        }
        return s;
    }
}
