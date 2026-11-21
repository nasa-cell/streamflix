package streamflix.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import streamflix.util.Carpeta;

/**
 * Crea las tablas y los datos de ejemplo cuando la base todavía está vacía.
 * Nunca borra nada: si la base ya tiene la tabla peliculas, la deja como está.
 */
public final class PrepararBase {

    /** Resultado de preparar, usado por el instalador para mostrar el aviso correcto. */
    public enum Resultado { CREADA, YA_EXISTIA, CLAVE_INCORRECTA, SIN_CONEXION }

    private PrepararBase() {}

    public static Resultado preparar() {
        try {
            return Conexion.POSTGRES ? prepararPostgres() : prepararMysql();
        } catch (SQLException e) {
            System.out.println("No se pudo preparar la base: " + e.getMessage());
            // 28000 y 28P01: usuario o contraseña incorrectos
            String estado = e.getSQLState() == null ? "" : e.getSQLState();
            return estado.startsWith("28") ? Resultado.CLAVE_INCORRECTA : Resultado.SIN_CONEXION;
        } catch (IOException e) {
            System.out.println("No se encontraron los archivos de la base: " + e.getMessage());
            return Resultado.SIN_CONEXION;
        }
    }

    private static Resultado prepararPostgres() throws SQLException, IOException {
        try (Connection c = Conexion.abrir(); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT to_regclass('public.peliculas') IS NOT NULL")) {
                rs.next();
                if (rs.getBoolean(1)) return Resultado.YA_EXISTIA;
            }
            c.setAutoCommit(false);
            st.execute(leer("nube-postgres", "1-crear-tablas.sql"));
            st.execute(leer("nube-postgres", "2-datos-ejemplo.sql"));
            c.commit();
            System.out.println("Base de la nube creada con los datos de ejemplo.");
            return Resultado.CREADA;
        }
    }

    private static Resultado prepararMysql() throws SQLException, IOException {
        // Se conecta al servidor MySQL sin elegir base, para poder crearla si falta.
        String original = Conexion.propiedad("url", "");
        String base = original.replaceFirst("^jdbc:mysql://[^/?]+/([^?]*).*$", "$1");
        String url = original.replaceFirst("(jdbc:mysql://[^/?]+)/[^?]*", "$1/");
        url += (url.contains("?") ? "&" : "?") + "allowMultiQueries=true";
        try (Connection c = DriverManager.getConnection(url, Conexion.propiedad("usuario", "root"),
                Conexion.propiedad("clave", ""));
             Statement st = c.createStatement()) {
            try (var ps = c.prepareStatement("SELECT COUNT(*) FROM information_schema.tables "
                    + "WHERE table_schema = ? AND table_name = 'peliculas'")) {
                ps.setString(1, base);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) return Resultado.YA_EXISTIA;
                }
            }
            st.execute(leer("1-crear-tablas.sql"));
            st.execute(leer("2-datos-ejemplo.sql"));
            System.out.println("Base streamflix creada en MySQL con los datos de ejemplo.");
            return Resultado.CREADA;
        }
    }

    private static String leer(String... partes) throws IOException {
        String[] ruta = new String[partes.length + 1];
        ruta[0] = "2-base-de-datos";
        System.arraycopy(partes, 0, ruta, 1, partes.length);
        return Files.readString(Carpeta.ruta(ruta), StandardCharsets.UTF_8);
    }
}
