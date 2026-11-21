package streamflix.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Gustos declarados por los perfiles y sus nombres, para la parte de IA. */
public class GustoDAO {

    /** Perfil -> géneros que eligió al crear su perfil. */
    public Map<Integer, Set<String>> declarados() throws SQLException {
        Map<Integer, Set<String>> m = new HashMap<>();
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT pg.perfil_id, g.nombre FROM perfil_gustos pg JOIN generos g ON g.id = pg.genero_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.computeIfAbsent(rs.getInt(1), k -> new LinkedHashSet<>()).add(rs.getString(2));
        }
        return m;
    }

    /** Perfil -> nombre visible, por ejemplo "Lucía (lucia45)". */
    public Map<Integer, String> nombresDePerfiles() throws SQLException {
        Map<Integer, String> m = new HashMap<>();
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT p.id, CONCAT(p.nombre, ' (', u.usuario, ')') FROM perfiles p JOIN usuarios u ON u.id = p.usuario_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.put(rs.getInt(1), rs.getString(2));
        }
        return m;
    }
}
