package streamflix.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Guarda el último resultado de la validación cruzada (en JSON) junto con una "firma"
 * de los datos usados. Así no hay que volver a calcularlo cada vez que el servidor arranca:
 * solo se recalcula cuando cambian las calificaciones, lo visto o los comentarios.
 */
public class MetricasDAO {

    private static volatile boolean tablaLista = false;

    private void prepararTabla(Connection c) throws SQLException {
        if (tablaLista) return;
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS metricas_ia (id INT PRIMARY KEY, firma VARCHAR(60) NOT NULL, datos TEXT NOT NULL)");
        }
        tablaLista = true;
    }

    /** Firma de los datos: cantidad de calificaciones, vistas, comentarios y gustos, y suma de estrellas. */
    public String firmaActual() throws SQLException {
        try (Connection c = Conexion.abrir(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT (SELECT COUNT(*) FROM calificaciones), (SELECT COUNT(*) FROM vistas), "
                     + "(SELECT COUNT(*) FROM comentarios), (SELECT COALESCE(SUM(estrellas), 0) FROM calificaciones), "
                     + "(SELECT COUNT(*) FROM perfil_gustos)")) {
            rs.next();
            return rs.getLong(1) + "-" + rs.getLong(2) + "-" + rs.getLong(3) + "-" + rs.getLong(4) + "-" + rs.getLong(5);
        }
    }

    /** JSON guardado si la firma coincide; null si no hay o si los datos cambiaron. */
    public String leer(String firma) throws SQLException {
        try (Connection c = Conexion.abrir()) {
            prepararTabla(c);
            try (PreparedStatement ps = c.prepareStatement("SELECT datos FROM metricas_ia WHERE id = 1 AND firma = ?")) {
                ps.setString(1, firma);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        }
    }

    public void guardar(String firma, String json) throws SQLException {
        try (Connection c = Conexion.abrir()) {
            prepararTabla(c);
            c.setAutoCommit(false);
            try (PreparedStatement borrar = c.prepareStatement("DELETE FROM metricas_ia WHERE id = 1");
                 PreparedStatement poner = c.prepareStatement("INSERT INTO metricas_ia (id, firma, datos) VALUES (1, ?, ?)")) {
                borrar.executeUpdate();
                poner.setString(1, firma);
                poner.setString(2, json);
                poner.executeUpdate();
                c.commit();
            }
        }
    }
}
