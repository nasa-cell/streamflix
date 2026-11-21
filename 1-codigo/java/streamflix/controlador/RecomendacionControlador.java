package streamflix.controlador;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import streamflix.modelo.Perfil;
import streamflix.servicio.InteraccionServicio;
import streamflix.servicio.RecomendacionServicio;

/** Rutas /api/recomendaciones: inicio personalizado, mis gustos y métricas de la IA. */
public class RecomendacionControlador extends Controlador {

    private final RecomendacionServicio recomendaciones = new RecomendacionServicio();
    private final InteraccionServicio interacciones = new InteraccionServicio();

    @Override
    protected void atender(HttpExchange ex, String ruta) throws Exception {
        Perfil perfil = perfilActual(ex);
        switch (ruta) {
            case "/inicio" -> {
                Map<String, Object> r = recomendaciones.inicio(perfil);
                r.put("perfil", perfil);
                r.put("ok", true);
                responder(ex, 200, r);
            }
            case "/mis-gustos" -> {
                Map<String, Object> r = recomendaciones.misGustos(perfil);
                r.put("perfil", perfil);
                r.put("ok", true);
                responder(ex, 200, r);
            }
            case "/explorar" -> {
                Map<String, String> q = parametros(ex);
                ok(ex, "explorar", recomendaciones.explorar(perfil, q.get("tipo"), q.get("genero")));
            }
            case "/categorias" -> ok(ex, "categorias", recomendaciones.categorias(perfil));
            case "/metricas" -> {
                boolean recalcular = "1".equals(parametros(ex).get("recalcular"));
                Map<String, Object> r = new java.util.LinkedHashMap<>(recomendaciones.metricas(recalcular));
                r.put("encuesta", interacciones.resumenEncuestas());
                r.put("historial", new streamflix.dao.HistorialDAO().todos());
                r.put("cadaCuanto", streamflix.servicio.AprendizajeContinuo.CADA);
                java.nio.file.Path reales = streamflix.util.Carpeta.ruta("3-pruebas", "resultados", "datos-reales.json");
                if (java.nio.file.Files.exists(reales)) {
                    r.put("datosReales", streamflix.util.Json.leerObjeto(java.nio.file.Files.readString(reales)).get("conjuntos"));
                }
                r.put("ok", true);
                responder(ex, 200, r);
            }
            default -> throw rutaDesconocida(ruta);
        }
    }
}
