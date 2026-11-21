package streamflix.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import streamflix.modelo.Comentario;

/** Consultas SQL de la tabla comentarios. */
public class ComentarioDAO {

    public void guardar(int perfilId, int peliculaId, String texto) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("INSERT INTO comentarios (perfil_id, pelicula_id, texto) VALUES (?,?,?)")) {
            ps.setInt(1, perfilId);
            ps.setInt(2, peliculaId);
            ps.setString(3, texto);
            ps.executeUpdate();
        }
    }

    /** Borra un comentario solo si pertenece al perfil que lo pide. */
    /** Textos que el perfil ya escribió en esa película (para el límite y evitar repetidos). */
    public List<String> textosDe(int perfilId, int peliculaId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT texto FROM comentarios WHERE perfil_id=? AND pelicula_id=?")) {
            ps.setInt(1, perfilId);
            ps.setInt(2, peliculaId);
            List<String> r = new java.util.ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) r.add(rs.getString(1));
            }
            return r;
        }
    }

    public boolean borrar(int comentarioId, int perfilId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM comentarios WHERE id = ? AND perfil_id = ?")) {
            ps.setInt(1, comentarioId);
            ps.setInt(2, perfilId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Texto de los comentarios por par perfil-película (para que la IA lea su sentimiento). */
    public Map<Long, String> textosPorPar() throws SQLException {
        Map<Long, String> m = new HashMap<>();
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT perfil_id, pelicula_id, texto FROM comentarios ORDER BY fecha");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.merge(rs.getInt(1) * 1_000_000L + rs.getInt(2), rs.getString(3), (a, b) -> a + " " + b);
        }
        return m;
    }

    public List<Comentario> dePelicula(int peliculaId) throws SQLException {
        String sql = "SELECT co.id, co.perfil_id, p.nombre, p.avatar, p.color, co.texto, "
                + Conexion.sql("DATE_FORMAT(co.fecha, '%d/%m/%Y')", "TO_CHAR(co.fecha, 'DD/MM/YYYY')") + ", COALESCE(ca.estrellas, 0) "
                + "FROM comentarios co JOIN perfiles p ON p.id = co.perfil_id "
                + "LEFT JOIN calificaciones ca ON ca.perfil_id = co.perfil_id AND ca.pelicula_id = co.pelicula_id "
                + "WHERE co.pelicula_id = ? ORDER BY co.fecha DESC, co.id DESC LIMIT 50";
        List<Comentario> lista = new ArrayList<>();
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, peliculaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Comentario(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getString(6), rs.getString(7), rs.getInt(8)));
                }
            }
        }
        return lista;
    }
}
