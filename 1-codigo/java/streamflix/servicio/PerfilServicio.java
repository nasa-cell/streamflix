package streamflix.servicio;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import streamflix.dao.PeliculaDAO;
import streamflix.dao.PerfilDAO;
import streamflix.modelo.Perfil;

/** Crear, editar, borrar y elegir perfiles (como en las plataformas de streaming). */
public class PerfilServicio {

    public static final int MAXIMO_PERFILES = 5;
    private static final List<String> AVATARES = List.of("🦊", "🐱", "🐼", "🐯", "🐸", "🦁", "🐧", "🐲", "🐨", "🦄");
    private static final List<String> COLORES = List.of("#ff6fae", "#ff9e5e", "#ffe066", "#63e6be", "#74c0fc", "#b197fc");
    private static final List<String> IDIOMAS = List.of("Español", "Inglés", "Japonés");

    private final PerfilDAO perfiles = new PerfilDAO();
    private final PeliculaDAO peliculas = new PeliculaDAO();

    public List<Perfil> listar(int usuarioId) throws SQLException {
        return perfiles.listar(usuarioId);
    }

    public Perfil obtenerDelUsuario(int perfilId, int usuarioId) throws SQLException {
        Perfil p = perfiles.obtener(perfilId);
        if (p == null || p.usuarioId() != usuarioId)
            throw new ErrorUsuario("Ese perfil no existe o no es de tu cuenta.", null, 404);
        return p;
    }

    public Perfil crear(int usuarioId, Map<String, Object> datos) throws SQLException {
        if (perfiles.listar(usuarioId).size() >= MAXIMO_PERFILES)
            throw new ErrorUsuario("Ya tienes " + MAXIMO_PERFILES + " perfiles, que es el máximo. Borra uno para crear otro.");
        Perfil p = leerDatos(0, usuarioId, datos);
        int id = perfiles.crear(p);
        return perfiles.obtener(id);
    }

    public Perfil editar(int usuarioId, int perfilId, Map<String, Object> datos) throws SQLException {
        obtenerDelUsuario(perfilId, usuarioId);
        Perfil p = leerDatos(perfilId, usuarioId, datos);
        perfiles.editar(p);
        return perfiles.obtener(perfilId);
    }

    public void borrar(int usuarioId, int perfilId) throws SQLException {
        obtenerDelUsuario(perfilId, usuarioId);
        if (perfiles.listar(usuarioId).size() <= 1)
            throw new ErrorUsuario("No puedes borrar tu único perfil. Crea otro primero.");
        perfiles.borrar(perfilId, usuarioId);
    }

    private Perfil leerDatos(int perfilId, int usuarioId, Map<String, Object> datos) throws SQLException {
        String nombre = Validador.nombrePerfil(String.valueOf(datos.getOrDefault("nombre", "")));
        if (perfiles.existeNombre(usuarioId, nombre, perfilId))
            throw new ErrorUsuario("Ya tienes un perfil llamado " + nombre + ". Usa otro nombre.", "nombre", 409);

        String avatar = String.valueOf(datos.getOrDefault("avatar", AVATARES.get(0)));
        if (!AVATARES.contains(avatar)) avatar = AVATARES.get(0);
        String color = String.valueOf(datos.getOrDefault("color", COLORES.get(0)));
        if (!COLORES.contains(color)) color = COLORES.get(0);
        String idioma = String.valueOf(datos.getOrDefault("idioma", "Español"));
        if (!IDIOMAS.contains(idioma)) idioma = "Español";
        boolean infantil = Boolean.TRUE.equals(datos.get("infantil"));

        Set<String> validos = new LinkedHashSet<>(peliculas.generos());
        List<String> gustos = new ArrayList<>();
        if (datos.get("gustos") instanceof List<?> lista) {
            for (Object g : lista) {
                String s = String.valueOf(g);
                if (validos.contains(s) && !gustos.contains(s)) gustos.add(s);
            }
        }
        if (gustos.size() < 3) throw new ErrorUsuario("Elige al menos 3 gustos (llevas " + gustos.size() + ").", "gustos");
        if (gustos.size() > 6) throw new ErrorUsuario("Elige como máximo 6 gustos (llevas " + gustos.size() + ").", "gustos");
        return new Perfil(perfilId, usuarioId, nombre, avatar, color, infantil, idioma, gustos);
    }

    public static List<String> avatares() {
        return AVATARES;
    }

    public static List<String> colores() {
        return COLORES;
    }
}
