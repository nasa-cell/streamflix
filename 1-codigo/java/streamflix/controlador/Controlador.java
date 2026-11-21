package streamflix.controlador;

import streamflix.servicio.CacheDatos;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import streamflix.dao.PerfilDAO;
import streamflix.modelo.Perfil;
import streamflix.servicio.ErrorUsuario;
import streamflix.servicio.GestorSesiones;
import streamflix.servicio.Sesion;
import streamflix.util.Json;

/**
 * Base de todos los controladores: lee lo que manda la página, responde en
 * JSON y convierte cualquier error en un mensaje claro que diga qué pasó.
 */
public abstract class Controlador implements HttpHandler {

    protected static final String COOKIE = "sf_sesion";
    private static final int TAMANO_MAXIMO = 64 * 1024;

    /** True si la ruta guarda algo que la IA usa: así se vuelve a entrenar. */
    protected boolean cambiaDatos(String ruta) {
        return false;
    }

    /** Cada controlador atiende sus rutas aquí. */
    protected abstract void atender(HttpExchange ex, String ruta) throws Exception;

    @Override
    public final void handle(HttpExchange ex) throws IOException {
        try {
            String prefijo = ex.getHttpContext().getPath();
            String ruta = ex.getRequestURI().getPath().substring(prefijo.length());
            if (ruta.isEmpty()) ruta = "/";
            atender(ex, ruta);
            if (cambiaDatos(ruta)) CacheDatos.invalidar();
        } catch (ErrorUsuario e) {
            Map<String, Object> cuerpo = new java.util.LinkedHashMap<>(e.getDatos());
            cuerpo.putAll(Json.objeto("ok", false, "error", e.getMessage(), "campo", e.getCampo()));
            responder(ex, e.getCodigoHttp(), cuerpo);
        } catch (SQLException e) {
            responder(ex, 500, Json.objeto("ok", false, "error", traducir(e), "detalle", e.getMessage()));
        } catch (IllegalArgumentException e) {
            responder(ex, 400, Json.objeto("ok", false, "error", "Los datos enviados no se entendieron: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            responder(ex, 500, Json.objeto("ok", false,
                    "error", "Error inesperado en el servidor (" + e.getClass().getSimpleName() + "). Revisa la ventana negra de iniciar.bat.",
                    "detalle", String.valueOf(e.getMessage())));
        } finally {
            ex.close();
        }
    }

    /** Explica en palabras simples qué pasó con MySQL. */
    static String traducir(SQLException e) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        int codigo = e.getErrorCode();
        if (estado.startsWith("08") || e.getClass().getSimpleName().contains("Communications"))
            return "No se pudo conectar con MySQL. Enciende el servicio MySQL84 (Servicios de Windows) y vuelve a intentar.";
        if (codigo == 1045)
            return "MySQL rechazó la contraseña. Revisa usuario y clave en 1-codigo/configuracion/conexion.properties.";
        if (codigo == 1049)
            return "La base de datos streamflix no existe. Ejecuta 2-base-de-datos/1-crear-tablas.sql y 2-datos-ejemplo.sql en Workbench.";
        if (e instanceof SQLIntegrityConstraintViolationException || codigo == 1062)
            return "Ese dato ya está registrado y no se puede repetir.";
        if (codigo == 3819)
            return "Un dato no cumple las reglas de la base (mínimo, máximo o formato).";
        return "La base de datos respondió con un error (" + codigo + "). Intenta de nuevo.";
    }

    protected void exigirMetodo(HttpExchange ex, String metodo) {
        if (!ex.getRequestMethod().equalsIgnoreCase(metodo))
            throw new ErrorUsuario("Esta acción se pide con " + metodo + ".", null, 405);
    }

    protected Map<String, Object> leerJson(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] datos = in.readNBytes(TAMANO_MAXIMO + 1);
            if (datos.length > TAMANO_MAXIMO) throw new ErrorUsuario("Lo que enviaste es demasiado grande.", null, 413);
            return Json.leerObjeto(new String(datos, StandardCharsets.UTF_8));
        }
    }

    protected Map<String, String> parametros(HttpExchange ex) {
        Map<String, String> m = new HashMap<>();
        String q = ex.getRequestURI().getRawQuery();
        if (q == null) return m;
        for (String par : q.split("&")) {
            String[] kv = par.split("=", 2);
            m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    protected int entero(Object valor, String nombre) {
        try {
            return (int) Double.parseDouble(String.valueOf(valor));
        } catch (NumberFormatException | NullPointerException e) {
            throw new ErrorUsuario("Falta el dato " + nombre + " o no es un número.");
        }
    }

    protected String token(HttpExchange ex) {
        String cookies = ex.getRequestHeaders().getFirst("Cookie");
        if (cookies == null) return null;
        for (String c : cookies.split(";")) {
            String[] kv = c.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(COOKIE)) return kv[1];
        }
        return null;
    }

    protected Sesion sesion(HttpExchange ex) {
        Sesion s = GestorSesiones.buscar(token(ex));
        if (s == null) throw new ErrorUsuario("Tu sesión terminó o no has ingresado. Vuelve a ingresar.", "sesion", 401);
        return s;
    }

    protected Perfil perfilActual(HttpExchange ex) throws SQLException {
        Sesion s = sesion(ex);
        if (!s.tienePerfil()) throw new ErrorUsuario("Primero elige quién está viendo.", "perfil", 428);
        Perfil p = new PerfilDAO().obtener(s.getPerfilId());
        if (p == null) {
            s.setPerfilId(0);
            throw new ErrorUsuario("Ese perfil ya no existe. Elige otro.", "perfil", 428);
        }
        return p;
    }

    protected void responder(HttpExchange ex, int codigo, Object cuerpo) throws IOException {
        byte[] bytes = Json.escribir(cuerpo).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    protected void ok(HttpExchange ex, Object... claveValor) throws IOException {
        Map<String, Object> m = Json.objeto(claveValor);
        m.put("ok", true);
        responder(ex, 200, m);
    }

    protected ErrorUsuario rutaDesconocida(String ruta) {
        return new ErrorUsuario("No existe la ruta " + ruta + ".", null, 404);
    }
}
