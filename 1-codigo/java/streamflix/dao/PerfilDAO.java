package streamflix.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import streamflix.modelo.Perfil;

/** Consultas SQL de perfiles y de sus gustos. */
public class PerfilDAO {

    public List<Perfil> listar(int usuarioId) throws SQLException {
        List<Perfil> lista = new ArrayList<>();
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM perfiles WHERE usuario_id = ? ORDER BY id")) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(leer(c, rs));
            }
        }
        return lista;
    }

    public Perfil obtener(int id) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM perfiles WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(c, rs) : null;
            }
        }
    }

    public boolean existeNombre(int usuarioId, String nombre, int excepto) throws SQLException {
        String sql = "SELECT 1 FROM perfiles WHERE usuario_id = ? AND LOWER(nombre) = LOWER(?) AND id <> ?";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, nombre);
            ps.setInt(3, excepto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int crear(Perfil p) throws SQLException {
        String sql = "INSERT INTO perfiles (usuario_id, nombre, avatar, color, infantil, idioma) VALUES (?,?,?,?,?,?)";
        try (Connection c = Conexion.abrir()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, p.usuarioId());
                ps.setString(2, p.nombre());
                ps.setString(3, p.avatar());
                ps.setString(4, p.color());
                ps.setBoolean(5, p.infantil());
                ps.setString(6, p.idioma());
                ps.executeUpdate();
                int id;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    id = rs.getInt(1);
                }
                guardarGustos(c, id, p.gustos());
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void editar(Perfil p) throws SQLException {
        String sql = "UPDATE perfiles SET nombre = ?, avatar = ?, color = ?, infantil = ?, idioma = ? WHERE id = ? AND usuario_id = ?";
        try (Connection c = Conexion.abrir()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, p.nombre());
                ps.setString(2, p.avatar());
                ps.setString(3, p.color());
                ps.setBoolean(4, p.infantil());
                ps.setString(5, p.idioma());
                ps.setInt(6, p.id());
                ps.setInt(7, p.usuarioId());
                ps.executeUpdate();
                try (PreparedStatement borrar = c.prepareStatement("DELETE FROM perfil_gustos WHERE perfil_id = ?")) {
                    borrar.setInt(1, p.id());
                    borrar.executeUpdate();
                }
                guardarGustos(c, p.id(), p.gustos());
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public boolean borrar(int id, int usuarioId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM perfiles WHERE id = ? AND usuario_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, usuarioId);
            return ps.executeUpdate() == 1;
        }
    }

    private void guardarGustos(Connection c, int perfilId, List<String> gustos) throws SQLException {
        String sql = "INSERT INTO perfil_gustos (perfil_id, genero_id) SELECT ?, id FROM generos WHERE nombre = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (String g : gustos) {
                ps.setInt(1, perfilId);
                ps.setString(2, g);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Perfil leer(Connection c, ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        List<String> gustos = new ArrayList<>();
        String sql = "SELECT g.nombre FROM perfil_gustos pg JOIN generos g ON g.id = pg.genero_id WHERE pg.perfil_id = ? ORDER BY g.nombre";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet g = ps.executeQuery()) {
                while (g.next()) gustos.add(g.getString(1));
            }
        }
        return new Perfil(id, rs.getInt("usuario_id"), rs.getString("nombre"), rs.getString("avatar"),
                rs.getString("color"), rs.getBoolean("infantil"), rs.getString("idioma"), gustos);
    }
}
