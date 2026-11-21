package streamflix.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Consultas SQL de la tabla vistas (películas que cada perfil ya vio). */
public class VistaDAO {

    public void marcar(int perfilId, int peliculaId, int minutos) throws SQLException {
        String sql = "INSERT INTO vistas (perfil_id, pelicula_id, minutos) VALUES (?,?,?) " + Conexion.sql(
                "ON DUPLICATE KEY UPDATE visto_en = NOW()",
                "ON CONFLICT (perfil_id, pelicula_id) DO UPDATE SET visto_en = LOCALTIMESTAMP");
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, perfilId);
            ps.setInt(2, peliculaId);
            ps.setInt(3, minutos);
            ps.executeUpdate();
        }
    }

    public void quitar(int perfilId, int peliculaId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM vistas WHERE perfil_id = ? AND pelicula_id = ?")) {
            ps.setInt(1, perfilId);
            ps.setInt(2, peliculaId);
            ps.executeUpdate();
        }
    }

    public boolean existe(int perfilId, int peliculaId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM vistas WHERE perfil_id = ? AND pelicula_id = ?")) {
            ps.setInt(1, perfilId);
            ps.setInt(2, peliculaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Películas vistas por un perfil, de la más reciente a la más antigua. */
    public Set<Integer> dePerfil(int perfilId) throws SQLException {
        Set<Integer> ids = new LinkedHashSet<>();
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT pelicula_id FROM vistas WHERE perfil_id = ? ORDER BY visto_en DESC, pelicula_id")) {
            ps.setInt(1, perfilId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    /** Minutos vistos por perfil y película (sirve para medir el tiempo de visualización). */
    public Map<Integer, Map<Integer, Integer>> minutos() throws SQLException {
        Map<Integer, Map<Integer, Integer>> m = new HashMap<>();
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT perfil_id, pelicula_id, minutos FROM vistas");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.computeIfAbsent(rs.getInt(1), k -> new HashMap<>()).put(rs.getInt(2), rs.getInt(3));
        }
        return m;
    }
}
