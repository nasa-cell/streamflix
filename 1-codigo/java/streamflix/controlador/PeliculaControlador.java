package streamflix.controlador;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import streamflix.modelo.Pelicula;
import streamflix.modelo.Perfil;
import streamflix.servicio.CatalogoServicio;
import streamflix.servicio.ErrorUsuario;
import streamflix.servicio.InteraccionServicio;

/** Rutas /api/peliculas: búsqueda con filtros y detalle de una película. */
public class PeliculaControlador extends Controlador {

    private final CatalogoServicio catalogo = new CatalogoServicio();
    private final InteraccionServicio interacciones = new InteraccionServicio();

    @Override
    protected void atender(HttpExchange ex, String ruta) throws Exception {
        Perfil perfil = perfilActual(ex);
        Map<String, String> p = parametros(ex);
        switch (ruta) {
            case "/buscar" -> {
                String texto = p.getOrDefault("q", "").trim();
                if (texto.length() > 60) throw new ErrorUsuario("La búsqueda es muy larga: máximo 60 letras.", "q");
                Integer anio = null;
                if (!p.getOrDefault("anio", "").isBlank()) {
                    anio = entero(p.get("anio"), "año");
                    if (anio != 2025 && anio != 2026) throw new ErrorUsuario("Solo hay películas de 2025 y 2026.", "anio");
                }
                ok(ex, "resultados", catalogo.buscar(texto, anio, p.get("tipo"), p.get("genero"), perfil),
                        "generos", catalogo.generos());
            }
            case "/detalle" -> {
                int id = entero(p.get("id"), "id");
                Pelicula peli = catalogo.obtener(id, perfil);
                ok(ex, "pelicula", peli, "estado", interacciones.estado(perfil.id(), id),
                        "comentarios", interacciones.comentarios(id), "perfilId", perfil.id());
            }
            default -> throw rutaDesconocida(ruta);
        }
    }
}
