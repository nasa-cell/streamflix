package streamflix.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import streamflix.modelo.Pelicula;
import streamflix.modelo.Plataforma;

/** Consultas SQL del catálogo: películas, géneros y plataformas. */
public class PeliculaDAO {

    public Map<Integer, Pelicula> todas() throws SQLException {
        Map<Integer, List<String>> generos = new HashMap<>();
        Map<Integer, List<Plataforma>> plataformas = new HashMap<>();
        Map<Integer, double[]> notas = new HashMap<>();
        Map<Integer, Pelicula> peliculas = new LinkedHashMap<>();
        try (Connection c = Conexion.abrir()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT pg.pelicula_id, g.nombre FROM pelicula_generos pg JOIN generos g ON g.id = pg.genero_id ORDER BY pg.pelicula_id, pg.orden");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) generos.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>()).add(rs.getString(2));
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT pp.pelicula_id, p.id, p.nombre, p.enlace, p.color FROM pelicula_plataformas pp JOIN plataformas p ON p.id = pp.plataforma_id ORDER BY pp.pelicula_id, p.id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    plataformas.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>())
                            .add(new Plataforma(rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5)));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT pelicula_id, AVG(estrellas), COUNT(*) FROM calificaciones GROUP BY pelicula_id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notas.put(rs.getInt(1), new double[]{rs.getDouble(2), rs.getInt(3)});
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM peliculas ORDER BY anio DESC, id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    double[] n = notas.getOrDefault(id, new double[]{0, 0});
                    peliculas.put(id, new Pelicula(id, rs.getString("titulo"), rs.getInt("anio"), rs.getString("tipo"),
                            rs.getInt("duracion_min"), rs.getString("clasificacion"), rs.getString("descripcion"),
                            rs.getString("portada"), rs.getString("trailer"), rs.getString("trailer_idioma"), generos.getOrDefault(id, List.of()),
                            plataformas.getOrDefault(id, List.of()), n[0], (int) n[1]));
                }
            }
        }
        return peliculas;
    }

    public List<String> generos() throws SQLException {
        List<String> lista = new ArrayList<>();
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT nombre FROM generos ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(rs.getString(1));
        }
        return lista;
    }
}
