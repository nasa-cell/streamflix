package streamflix.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

/** Consultas SQL de la tabla mi_lista. */
public class ListaDAO {

    public void agregar(int perfilId, int peliculaId) throws SQLException {
        ejecutar(Conexion.sql("INSERT IGNORE INTO mi_lista (perfil_id, pelicula_id) VALUES (?,?)",
                "INSERT INTO mi_lista (perfil_id, pelicula_id) VALUES (?,?) ON CONFLICT DO NOTHING"), perfilId, peliculaId);
    }

    public void quitar(int perfilId, int peliculaId) throws SQLException {
        ejecutar("DELETE FROM mi_lista WHERE perfil_id = ? AND pelicula_id = ?", perfilId, peliculaId);
    }

    public boolean contiene(int perfilId, int peliculaId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM mi_lista WHERE perfil_id = ? AND pelicula_id = ?")) {
            ps.setInt(1, perfilId);
            ps.setInt(2, peliculaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Set<Integer> dePerfil(int perfilId) throws SQLException {
        Set<Integer> ids = new LinkedHashSet<>();
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT pelicula_id FROM mi_lista WHERE perfil_id = ? ORDER BY agregado_en DESC")) {
            ps.setInt(1, perfilId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    private void ejecutar(String sql, int perfilId, int peliculaId) throws SQLException {
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, perfilId);
            ps.setInt(2, peliculaId);
            ps.executeUpdate();
        }
    }
}
