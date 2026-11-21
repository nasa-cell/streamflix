package streamflix.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import streamflix.util.Json;

/** Consultas SQL de la tabla historial_ia: cómo cambia la precisión a medida que llegan datos. */
public class HistorialDAO {

    public void guardar(int calificaciones, int perfiles, double mae, double precision, String nota) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO historial_ia (calificaciones, perfiles, mae, precision_top, nota) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, calificaciones);
            ps.setInt(2, perfiles);
            ps.setDouble(3, mae);
            ps.setDouble(4, precision);
            ps.setString(5, nota);
            ps.executeUpdate();
        }
    }

    /** Calificaciones que había en la última medición, o -1 si todavía no se midió nunca. */
    public int ultimaCantidad() throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT calificaciones FROM historial_ia ORDER BY id DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    public List<Map<String, Object>> todos() throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT " + Conexion.sql("DATE_FORMAT(fecha, '%d/%m/%Y %H:%i')", "TO_CHAR(fecha, 'DD/MM/YYYY HH24:MI')")
                     + ", calificaciones, perfiles, mae, precision_top, nota FROM historial_ia ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(Json.objeto("fecha", rs.getString(1), "calificaciones", rs.getInt(2), "perfiles", rs.getInt(3),
                        "mae", rs.getDouble(4), "precision", rs.getDouble(5), "nota", rs.getString(6)));
            }
        }
        return lista;
    }
}
