package streamflix;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import streamflix.dao.ArchivosDatosReales;
import streamflix.dao.ArchivosDatosReales.Conjunto;
import streamflix.ia.CombinadorSenales;
import streamflix.ia.DatosEntrenamiento;
import streamflix.ia.FactorizacionMatrices;
import streamflix.ia.PesosAprendidos;
import streamflix.ia.RecomendadorHibrido;
import streamflix.ia.ResultadoMetodo;
import streamflix.ia.ValidacionCruzada;
import streamflix.util.Json;

/**
 * Prueba el algoritmo con datos reales (MovieLens y anime de MyAnimeList)
 * usando la misma validación cruzada de 5 partes que la app.
 * Guarda los resultados en 3-pruebas/resultados/datos-reales.json, que la página
 * «Cómo funciona la IA» muestra. Se ejecuta con probar-datos-reales.bat.
 */
public class PruebasDatosReales {

    public static void main(String[] args) throws Exception {
        List<Conjunto> conjuntos = new ArrayList<>();
        if (ArchivosDatosReales.hayMovieLens()) conjuntos.add(ArchivosDatosReales.movieLens());
        if (ArchivosDatosReales.hayAnime()) conjuntos.add(ArchivosDatosReales.anime());
        if (conjuntos.isEmpty()) {
            System.out.println("No hay datos reales. Ejecuta primero 3-pruebas/datos-reales/descargar.bat");
            return;
        }
        int repeticiones = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Conjunto c : conjuntos) {
            System.out.printf("%n== %s: %,d calificaciones, %,d personas, %,d títulos%n",
                    c.nombre(), c.calificaciones().size(), c.usuarios(), c.peliculas().size());
            long t = System.nanoTime();
            List<ResultadoMetodo> metodos = new ValidacionCruzada().ejecutarRepetida(c.peliculas(), c.calificaciones(),
                    Map.of(), Map.of(), Map.of(), 5, repeticiones);
            double segundos = (System.nanoTime() - t) / 1e9;
            for (ResultadoMetodo r : metodos) {
                System.out.printf("   %-40s MAE %.3f  Precisión@3 %.1f%%%n", r.nombre(), r.mae(), r.precision() * 100);
            }

            DatosEntrenamiento datos = new DatosEntrenamiento(c.peliculas(), CombinadorSenales.soloEstrellas(c.calificaciones()), Map.of());
            RecomendadorHibrido h = new RecomendadorHibrido(datos, 42).aprenderPesos();
            FactorizacionMatrices f = h.factorizacion();
            Map<String, Object> pesos = new LinkedHashMap<>();
            for (int k = 0; k < PesosAprendidos.NOMBRES.size(); k++) {
                pesos.put(PesosAprendidos.NOMBRES.get(k), Math.round(h.pesos().pesos()[k] * 1000) / 1000.0);
            }
            salida.add(Json.objeto("nombre", c.nombre(), "fuente", c.fuente(), "nota", c.nota(),
                    "calificaciones", c.calificaciones().size(), "usuarios", c.usuarios(), "titulos", c.peliculas().size(),
                    "partes", 5, "repeticiones", repeticiones, "segundos", Math.round(segundos * 10) / 10.0,
                    "metodos", metodos, "curva", f.curva(), "mejorEpoca", f.mejorEpoca(),
                    "factores", f.factores(), "regularizacion", f.regularizacion(), "pesos", pesos));
            System.out.printf("   Tardó %.1f s. Factorización: %d rasgos, regularización %.2f, mejor vuelta %d%n",
                    segundos, f.factores(), f.regularizacion(), f.mejorEpoca());
        }
        Path archivo = Path.of("3-pruebas", "resultados", "datos-reales.json");
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, Json.escribir(Json.objeto("conjuntos", salida)), StandardCharsets.UTF_8);
        System.out.println("\nGuardado en " + archivo.toAbsolutePath());
    }
}
