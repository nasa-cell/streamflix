package streamflix.controlador;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import streamflix.modelo.Pelicula;
import streamflix.modelo.Perfil;
import streamflix.servicio.CatalogoServicio;
import streamflix.servicio.InteraccionServicio;

/** Rutas /api/interaccion: ya la vi, mi lista, estrellas, comentarios y encuesta. */
public class InteraccionControlador extends Controlador {

    private final InteraccionServicio interacciones = new InteraccionServicio();
    private final CatalogoServicio catalogo = new CatalogoServicio();

    @Override
    protected boolean cambiaDatos(String ruta) {
        return java.util.List.of("/visto", "/calificar", "/comentar", "/borrar-comentario").contains(ruta);
    }

    @Override
    protected void atender(HttpExchange ex, String ruta) throws Exception {
        exigirMetodo(ex, "POST");
        Perfil perfil = perfilActual(ex);
        Map<String, Object> d = leerJson(ex);
        switch (ruta) {
            case "/visto" -> {
                Pelicula p = catalogo.obtener(entero(d.get("peliculaId"), "película"), perfil);
                boolean visto = interacciones.cambiarVisto(perfil.id(), p, Boolean.TRUE.equals(d.get("visto")));
                ok(ex, "visto", visto, "mensaje", visto ? "Marcada como vista." : "Ya no figura como vista.");
            }
            case "/lista" -> {
                Pelicula p = catalogo.obtener(entero(d.get("peliculaId"), "película"), perfil);
                boolean en = interacciones.cambiarLista(perfil.id(), p.id(), Boolean.TRUE.equals(d.get("agregar")));
                ok(ex, "enLista", en, "mensaje", en ? "Agregada a Mi lista." : "Quitada de Mi lista.");
            }
            case "/calificar" -> {
                Pelicula p = catalogo.obtener(entero(d.get("peliculaId"), "película"), perfil);
                interacciones.calificar(perfil.id(), p, d.get("estrellas"));
                ok(ex, "mensaje", "¡Gracias! Tus recomendaciones se actualizaron.");
            }
            case "/comentar" -> {
                Pelicula p = catalogo.obtener(entero(d.get("peliculaId"), "película"), perfil);
                interacciones.comentar(perfil.id(), p.id(), d.get("texto"));
                ok(ex, "comentarios", interacciones.comentarios(p.id()), "mensaje", "Comentario publicado.");
            }
            case "/borrar-comentario" -> {
                interacciones.borrarComentario(perfil.id(), entero(d.get("id"), "comentario"));
                int peli = entero(d.get("peliculaId"), "película");
                ok(ex, "comentarios", interacciones.comentarios(peli), "mensaje", "Comentario borrado.");
            }
            case "/encuesta" -> {
                interacciones.guardarEncuesta(perfil.id(), d);
                ok(ex, "mensaje", "¡Gracias por responder la encuesta!", "resumen", interacciones.resumenEncuestas());
            }
            default -> throw rutaDesconocida(ruta);
        }
    }
}
