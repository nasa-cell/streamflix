package streamflix.controlador;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import streamflix.modelo.Perfil;
import streamflix.servicio.CatalogoServicio;
import streamflix.servicio.PerfilServicio;
import streamflix.servicio.Sesion;

/** Rutas /api/perfiles: listar, crear, editar, borrar y elegir perfil. */
public class PerfilControlador extends Controlador {

    private final PerfilServicio perfiles = new PerfilServicio();
    private final CatalogoServicio catalogo = new CatalogoServicio();

    @Override
    protected boolean cambiaDatos(String ruta) {
        return java.util.List.of("/crear", "/editar", "/borrar").contains(ruta);
    }

    @Override
    protected void atender(HttpExchange ex, String ruta) throws Exception {
        Sesion s = sesion(ex);
        int usuario = s.getUsuarioId();
        switch (ruta) {
            case "/" -> ok(ex, "perfiles", perfiles.listar(usuario), "maximo", PerfilServicio.MAXIMO_PERFILES,
                    "avatares", PerfilServicio.avatares(), "colores", PerfilServicio.colores(),
                    "generos", catalogo.generos(), "actual", s.getPerfilId());
            case "/uno" -> ok(ex, "perfil", perfiles.obtenerDelUsuario(entero(parametros(ex).get("id"), "id"), usuario));
            case "/crear" -> {
                exigirMetodo(ex, "POST");
                Perfil p = perfiles.crear(usuario, leerJson(ex));
                ok(ex, "perfil", p, "mensaje", "Perfil " + p.nombre() + " creado.");
            }
            case "/editar" -> {
                exigirMetodo(ex, "POST");
                Map<String, Object> d = leerJson(ex);
                Perfil p = perfiles.editar(usuario, entero(d.get("id"), "id"), d);
                ok(ex, "perfil", p, "mensaje", "Cambios guardados.");
            }
            case "/borrar" -> {
                exigirMetodo(ex, "POST");
                int id = entero(leerJson(ex).get("id"), "id");
                perfiles.borrar(usuario, id);
                if (s.getPerfilId() == id) s.setPerfilId(0);
                ok(ex, "mensaje", "Perfil borrado.");
            }
            case "/elegir" -> {
                exigirMetodo(ex, "POST");
                Perfil p = perfiles.obtenerDelUsuario(entero(leerJson(ex).get("id"), "id"), usuario);
                s.setPerfilId(p.id());
                ok(ex, "perfil", p);
            }
            case "/actual" -> ok(ex, "perfil", perfilActual(ex));
            default -> throw rutaDesconocida(ruta);
        }
    }
}
