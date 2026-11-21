package streamflix.servicio;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import streamflix.dao.CalificacionDAO;
import streamflix.dao.ComentarioDAO;
import streamflix.dao.EncuestaDAO;
import streamflix.dao.ListaDAO;
import streamflix.dao.VistaDAO;
import streamflix.modelo.Comentario;
import streamflix.modelo.Encuesta;
import streamflix.modelo.Pelicula;
import streamflix.util.Json;

/** Lo que hace un perfil con una película: verla, calificarla, comentarla o guardarla. */
public class InteraccionServicio {

    public static final int MAXIMO_COMENTARIOS = 3;

    private final VistaDAO vistas = new VistaDAO();
    private final ListaDAO lista = new ListaDAO();
    private final CalificacionDAO calificaciones = new CalificacionDAO();
    private final ComentarioDAO comentarios = new ComentarioDAO();
    private final EncuestaDAO encuestas = new EncuestaDAO();

    public Map<String, Object> estado(int perfilId, int peliculaId) throws SQLException {
        return Json.objeto(
                "visto", vistas.existe(perfilId, peliculaId),
                "enLista", lista.contiene(perfilId, peliculaId),
                "estrellas", calificaciones.estrellas(perfilId, peliculaId));
    }

    public boolean cambiarVisto(int perfilId, Pelicula p, boolean visto) throws SQLException {
        if (visto) {
            int minutos = "Serie".equals(p.tipo()) ? p.duracionMin() * 8 : p.duracionMin();
            vistas.marcar(perfilId, p.id(), minutos);
        } else {
            vistas.quitar(perfilId, p.id());
        }
        return visto;
    }

    public boolean cambiarLista(int perfilId, int peliculaId, boolean agregar) throws SQLException {
        if (agregar) lista.agregar(perfilId, peliculaId);
        else lista.quitar(perfilId, peliculaId);
        return agregar;
    }

    /** Calificar también marca la película como vista. */
    public void calificar(int perfilId, Pelicula p, Object estrellas) throws SQLException {
        int e = Validador.deUnoACinco(estrellas, "estrellas", "las estrellas");
        calificaciones.guardar(perfilId, p.id(), e);
        if (!vistas.existe(perfilId, p.id())) cambiarVisto(perfilId, p, true);
    }

    public void comentar(int perfilId, int peliculaId, Object texto) throws SQLException {
        String limpio = Validador.comentario(texto == null ? "" : texto.toString());
        List<String> previos = comentarios.textosDe(perfilId, peliculaId);
        if (previos.size() >= MAXIMO_COMENTARIOS)
            throw new ErrorUsuario("Ya escribiste " + MAXIMO_COMENTARIOS + " comentarios en este título, que es el máximo. Borra uno para escribir otro.", "texto");
        for (String t : previos) {
            if (t.equalsIgnoreCase(limpio))
                throw new ErrorUsuario("Ya publicaste ese mismo comentario aquí. Escribe algo distinto.", "texto");
        }
        comentarios.guardar(perfilId, peliculaId, limpio);
    }

    public void borrarComentario(int perfilId, int comentarioId) throws SQLException {
        if (!comentarios.borrar(comentarioId, perfilId))
            throw new ErrorUsuario("Solo puedes borrar tus propios comentarios.", null, 403);
    }

    public List<Comentario> comentarios(int peliculaId) throws SQLException {
        return comentarios.dePelicula(peliculaId);
    }

    public void guardarEncuesta(int perfilId, Map<String, Object> d) throws SQLException {
        int s = Validador.deUnoACinco(d.get("satisfaccion"), "satisfaccion", "qué tan satisfecho estás");
        int a = Validador.deUnoACinco(d.get("acierto"), "acierto", "qué tanto acertamos");
        int f = Validador.deUnoACinco(d.get("facilidad"), "facilidad", "qué tan fácil es usarla");
        Object rec = d.get("recomendaria");
        if (!(rec instanceof Boolean)) throw new ErrorUsuario("Responde si recomendarías StreamFlix.", "recomendaria");
        String comentario = d.get("comentario") == null ? null : d.get("comentario").toString().trim();
        if (comentario != null && comentario.length() > 200)
            throw new ErrorUsuario("El comentario de la encuesta es de máximo 200 letras.", "comentarioEncuesta");
        encuestas.guardar(new Encuesta(perfilId, s, a, f, (Boolean) rec, comentario == null || comentario.isEmpty() ? null : comentario));
    }

    public Map<String, Object> resumenEncuestas() throws SQLException {
        return encuestas.resumen();
    }
}
