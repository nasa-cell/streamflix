package streamflix.servicio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Guarda en memoria las sesiones abiertas, identificadas por un token. */
public final class GestorSesiones {

    private static final Map<String, Sesion> SESIONES = new ConcurrentHashMap<>();

    private GestorSesiones() {}

    public static String abrir(int usuarioId) {
        String token = Cifrado.token();
        SESIONES.put(token, new Sesion(usuarioId));
        return token;
    }

    public static Sesion buscar(String token) {
        return token == null ? null : SESIONES.get(token);
    }

    /** Cierra todas las sesiones de una cuenta (al cambiar la contraseña). */
    public static void cerrarDeUsuario(int usuarioId) {
        SESIONES.values().removeIf(s -> s.getUsuarioId() == usuarioId);
    }

    public static void cerrar(String token) {
        if (token != null) SESIONES.remove(token);
    }
}
