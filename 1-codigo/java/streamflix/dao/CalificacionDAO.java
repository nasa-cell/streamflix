package streamflix.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import streamflix.modelo.Calificacion;

/** Consultas SQL de la tabla calificaciones (estrellas). */
public class CalificacionDAO {

    public void guardar(int perfilId, int peliculaId, int estrellas) throws SQLException {
        String sql = "INSERT INTO calificaciones (perfil_id, pelicula_id, estrellas) VALUES (?,?,?) "
                + Conexion.sql("ON DUPLICATE KEY UPDATE estrellas = VALUES(estrellas), fecha = NOW()",
                "ON CONFLICT (perfil_id, pelicula_id) DO UPDATE SET estrellas = EXCLUDED.estrellas, fecha = LOCALTIMESTAMP");
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, perfilId);
            ps.setInt(2, peliculaId);
            ps.setInt(3, estrellas);
            ps.executeUpdate();
        }
    }

    public int estrellas(int perfilId, int peliculaId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT estrellas FROM calificaciones WHERE perfil_id = ? AND pelicula_id = ?")) {
            ps.setInt(1, perfilId);
            ps.setInt(2, peliculaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Calificacion> todas() throws SQLException {
        List<Calificacion> lista = new ArrayList<>();
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT perfil_id, pelicula_id, estrellas FROM calificaciones ORDER BY perfil_id, pelicula_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new Calificacion(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
        }
        return lista;
    }
}
