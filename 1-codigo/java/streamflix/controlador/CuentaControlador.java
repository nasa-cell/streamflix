package streamflix.controlador;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import streamflix.modelo.Usuario;
import streamflix.servicio.CuentaServicio;
import streamflix.servicio.ErrorUsuario;
import streamflix.servicio.GestorSesiones;
import streamflix.servicio.Sesion;

/** Rutas /api/cuenta: registrarse, ingresar (con límite de intentos), recuperar la contraseña, salir y revisar datos repetidos. */
public class CuentaControlador extends Controlador {

    private final CuentaServicio cuentas = new CuentaServicio();

    @Override
    protected void atender(HttpExchange ex, String ruta) throws Exception {
        switch (ruta) {
            case "/registro" -> {
                exigirMetodo(ex, "POST");
                Usuario u = cuentas.registrar(leerJson(ex));
                abrirSesion(ex, u);
                ok(ex, "usuario", u, "mensaje", "¡Cuenta creada! Ahora crea tu primer perfil.");
            }
            case "/ingresar" -> {
                exigirMetodo(ex, "POST");
                Map<String, Object> d = leerJson(ex);
                Usuario u = cuentas.ingresar(String.valueOf(d.getOrDefault("login", "")), String.valueOf(d.getOrDefault("clave", "")));
                abrirSesion(ex, u);
                ok(ex, "usuario", u);
            }
            case "/recuperar/pedir" -> {
                exigirMetodo(ex, "POST");
                Map<String, Object> d = leerJson(ex);
                ok(ex, "envio", cuentas.pedirCodigo(String.valueOf(d.getOrDefault("login", ""))));
            }
            case "/recuperar/comprobar" -> {
                exigirMetodo(ex, "POST");
                Map<String, Object> d = leerJson(ex);
                ok(ex, "permiso", cuentas.comprobarCodigo(String.valueOf(d.getOrDefault("login", "")),
                        String.valueOf(d.getOrDefault("codigo", ""))));
            }
            case "/recuperar/cambiar" -> {
                exigirMetodo(ex, "POST");
                Map<String, Object> d = leerJson(ex);
                Usuario u = cuentas.cambiarClave(String.valueOf(d.getOrDefault("permiso", "")),
                        String.valueOf(d.getOrDefault("clave", "")), String.valueOf(d.getOrDefault("clave2", "")));
                abrirSesion(ex, u);
                ok(ex, "usuario", u, "mensaje", "Tu contraseña cambió. Se cerraron las sesiones de otros equipos.");
            }
            case "/salir" -> {
                exigirMetodo(ex, "POST");
                GestorSesiones.cerrar(token(ex));
                ex.getResponseHeaders().add("Set-Cookie", COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
                ok(ex, "mensaje", "Cerraste sesión.");
            }
            case "/sesion" -> {
                Sesion s = GestorSesiones.buscar(token(ex));
                if (s == null) {
                    ok(ex, "conectado", false);
                } else {
                    ok(ex, "conectado", true, "usuario", cuentas.obtener(s.getUsuarioId()), "perfilId", s.getPerfilId());
                }
            }
            case "/disponible" -> {
                Map<String, String> p = parametros(ex);
                String campo = p.getOrDefault("campo", "");
                if (!campo.matches("usuario|correo|dni|celular")) throw new ErrorUsuario("Ese campo no se revisa.");
                ok(ex, "disponible", cuentas.disponible(campo, p.getOrDefault("valor", "")));
            }
            default -> throw rutaDesconocida(ruta);
        }
    }

    private void abrirSesion(HttpExchange ex, Usuario u) {
        String token = GestorSesiones.abrir(u.id());
        ex.getResponseHeaders().add("Set-Cookie", COOKIE + "=" + token + "; Path=/; HttpOnly; SameSite=Lax");
    }
}
