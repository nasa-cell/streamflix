package streamflix.servicio;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import streamflix.modelo.Pelicula;
import streamflix.modelo.Perfil;

/** Catálogo de películas y series: listado, búsqueda y filtros. */
public class CatalogoServicio {


    public Map<Integer, Pelicula> todas() throws SQLException {
        return CacheDatos.peliculas();
    }

    public List<String> generos() throws SQLException {
        return new streamflix.dao.PeliculaDAO().generos();
    }

    public Pelicula obtener(int id, Perfil perfil) throws SQLException {
        Pelicula p = CacheDatos.peliculas().get(id);
        if (p == null) throw new ErrorUsuario("Esa película no existe en el catálogo.", null, 404);
        if (perfil != null && perfil.infantil() && !p.aptaParaNinos())
            throw new ErrorUsuario("Este es un perfil infantil y esa película es para mayores (" + p.clasificacion() + ").", null, 403);
        return p;
    }

    /** Busca por título, género o descripción, sin importar tildes ni mayúsculas. */
    public List<Pelicula> buscar(String texto, Integer anio, String tipo, String genero, Perfil perfil) throws SQLException {
        String buscado = normalizar(texto);
        List<Pelicula> resultado = new ArrayList<>();
        for (Pelicula p : CacheDatos.peliculas().values()) {
            if (perfil != null && perfil.infantil() && !p.aptaParaNinos()) continue;
            if (anio != null && p.anio() != anio) continue;
            if (tipo != null && !tipo.isBlank() && !p.tipo().equals(tipo)) continue;
            if (genero != null && !genero.isBlank() && !p.generos().contains(genero)) continue;
            if (!buscado.isEmpty()) {
                String donde = normalizar(p.titulo() + " " + String.join(" ", p.generos()) + " " + p.descripcion());
                boolean todas = true;
                for (String palabra : buscado.split(" +")) {
                    if (!donde.contains(palabra)) { todas = false; break; }
                }
                if (!todas) continue;
            }
            resultado.add(p);
        }
        return resultado;
    }

    static String normalizar(String s) {
        if (s == null) return "";
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().trim();
    }
}
