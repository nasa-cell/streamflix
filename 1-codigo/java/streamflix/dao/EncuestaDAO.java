package streamflix.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import streamflix.modelo.Encuesta;
import streamflix.util.Json;

/** Guarda y resume la encuesta de satisfacción. */
public class EncuestaDAO {

    public void guardar(Encuesta e) throws SQLException {
        String sql = "INSERT INTO encuestas (perfil_id, satisfaccion, acierto, facilidad, recomendaria, comentario) VALUES (?,?,?,?,?,?)";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, e.perfilId());
            ps.setInt(2, e.satisfaccion());
            ps.setInt(3, e.acierto());
            ps.setInt(4, e.facilidad());
            ps.setBoolean(5, e.recomendaria());
            ps.setString(6, e.comentario());
            ps.executeUpdate();
        }
    }

    public Map<String, Object> resumen() throws SQLException {
        String sql = "SELECT COUNT(*), AVG(satisfaccion), AVG(acierto), AVG(facilidad), AVG(CASE WHEN recomendaria THEN 1 ELSE 0 END) * 100 FROM encuestas";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return Json.objeto("respuestas", rs.getInt(1),
                    "satisfaccion", redondear(rs.getDouble(2)), "acierto", redondear(rs.getDouble(3)),
                    "facilidad", redondear(rs.getDouble(4)), "recomendaria", Math.round(rs.getDouble(5)));
        }
    }

    private double redondear(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
