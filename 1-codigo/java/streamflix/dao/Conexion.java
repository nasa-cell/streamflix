package streamflix.dao;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import streamflix.util.Carpeta;

/**
 * Abre conexiones a MySQL con los datos de 1-codigo/configuracion/conexion.properties.
 * En la nube (Render) usa PostgreSQL de Neon con la variable DATABASE_URL.
 * Guarda hasta 20 conexiones abiertas para reutilizarlas (pool): así muchas
 * personas a la vez no obligan a abrir una conexión nueva en cada pedido.
 */
public final class Conexion {

    private static final int MAXIMO_LIBRES = 20;
    private static final Properties PROPIEDADES = new Properties();
    private static final BlockingQueue<Connection> LIBRES = new ArrayBlockingQueue<>(MAXIMO_LIBRES);

    /** true cuando la base es PostgreSQL (versión en la nube). */
    public static final boolean POSTGRES;

    static {
        String nube = System.getenv("DATABASE_URL");
        Path archivo = Carpeta.ruta("1-codigo", "configuracion", "conexion.properties");
        if (Files.exists(archivo)) {
            try (Reader in = new InputStreamReader(Files.newInputStream(archivo), StandardCharsets.UTF_8)) {
                PROPIEDADES.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer " + archivo, e);
            }
        } else if (nube == null || nube.isBlank()) {
            throw new IllegalStateException("No se encontró " + archivo
                    + ". Abre la app desde la carpeta streamflix con iniciar.bat.");
        }
        if (nube != null && !nube.isBlank()) usarPostgres(nube.trim());
        String puerto = System.getenv("PORT");
        if (puerto != null && !puerto.isBlank()) PROPIEDADES.setProperty("puerto", puerto.trim());
        POSTGRES = PROPIEDADES.getProperty("url", "").startsWith("jdbc:postgresql:");
    }

    /** Convierte postgresql://usuario:clave@servidor/base?sslmode=require al formato de Java. */
    private static void usarPostgres(String direccion) {
        URI u = URI.create(direccion.replaceFirst("^postgres(ql)?://", "http://"));
        String[] cuenta = u.getRawUserInfo().split(":", 2);
        String consulta = (u.getRawQuery() == null ? "sslmode=require" : u.getRawQuery())
                + "&options=-c%20TimeZone%3DAmerica/Lima";
        String servidor = u.getHost() + (u.getPort() > 0 ? ":" + u.getPort() : "");
        PROPIEDADES.setProperty("url", "jdbc:postgresql://" + servidor + u.getRawPath() + "?" + consulta);
        PROPIEDADES.setProperty("usuario", URLDecoder.decode(cuenta[0], StandardCharsets.UTF_8));
        PROPIEDADES.setProperty("clave", cuenta.length > 1 ? URLDecoder.decode(cuenta[1], StandardCharsets.UTF_8) : "");
    }

    private Conexion() {}

    /** Elige la consulta según la base: la de MySQL (en la PC) o la de PostgreSQL (en la nube). */
    public static String sql(String mysql, String postgres) {
        return POSTGRES ? postgres : mysql;
    }

    public static String propiedad(String clave, String porDefecto) {
        return PROPIEDADES.getProperty(clave, porDefecto);
    }

    /** Entrega una conexión; al cerrarla con try-with-resources vuelve al pool. */
    public static Connection abrir() throws SQLException {
        Connection real = LIBRES.poll();
        while (real != null && !real.isValid(1)) {
            cerrarSinError(real);
            real = LIBRES.poll();
        }
        if (real == null) {
            real = DriverManager.getConnection(PROPIEDADES.getProperty("url"),
                    PROPIEDADES.getProperty("usuario"), PROPIEDADES.getProperty("clave"));
        }
        return envolver(real);
    }

    private static Connection envolver(Connection real) {
        boolean[] devuelta = {false};
        return (Connection) Proxy.newProxyInstance(Conexion.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, metodo, argumentos) -> {
                    if (metodo.getName().equals("close")) {
                        if (devuelta[0]) return null;
                        devuelta[0] = true;
                        devolver(real);
                        return null;
                    }
                    try {
                        return metodo.invoke(real, argumentos);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private static void devolver(Connection real) {
        try {
            if (real.isClosed()) return;
            if (!real.getAutoCommit()) {
                real.rollback();
                real.setAutoCommit(true);
            }
            if (!LIBRES.offer(real)) real.close();
        } catch (SQLException e) {
            cerrarSinError(real);
        }
    }

    private static void cerrarSinError(Connection c) {
        try {
            c.close();
        } catch (SQLException ignorada) {
            // la conexión ya estaba rota
        }
    }
}
