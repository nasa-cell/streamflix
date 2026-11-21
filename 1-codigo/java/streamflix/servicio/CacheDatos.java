package streamflix.servicio;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import streamflix.dao.PeliculaDAO;
import streamflix.modelo.Pelicula;

/**
 * Guarda en memoria el catálogo para no leerlo de MySQL en cada pedido.
 * Cuando alguien califica, comenta, ve algo o cambia sus gustos se llama a
 * invalidar() y el siguiente pedido vuelve a leer los datos frescos.
 */
public final class CacheDatos {

    private static final AtomicLong VERSION = new AtomicLong();
    private static volatile Map<Integer, Pelicula> peliculas;
    private static volatile long versionPeliculas = -1;

    private CacheDatos() {}

    public static void invalidar() {
        VERSION.incrementAndGet();
    }

    public static long version() {
        return VERSION.get();
    }

    public static Map<Integer, Pelicula> peliculas() throws SQLException {
        long v = VERSION.get();
        if (versionPeliculas != v) {
            synchronized (CacheDatos.class) {
                if (versionPeliculas != v) {
                    peliculas = new PeliculaDAO().todas();
                    versionPeliculas = v;
                }
            }
        }
        return peliculas;
    }
}
