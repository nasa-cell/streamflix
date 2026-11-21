package streamflix.servicio;

import java.sql.SQLException;
import java.util.Map;
import streamflix.dao.UsuarioDAO;
import streamflix.modelo.Usuario;

/** Registro e inicio de sesión. */
public class CuentaServicio {

    private final UsuarioDAO usuarios = new UsuarioDAO();

    public Usuario registrar(Map<String, Object> datos) throws SQLException {
        String nombre = Validador.nombreCompleto(texto(datos, "nombreCompleto"));
        String usuario = Validador.usuario(texto(datos, "usuario"));
        String correo = Validador.correo(texto(datos, "correo"));
        String dni = Validador.dni(texto(datos, "dni"));
        String celular = Validador.celular(texto(datos, "celular"));
        int edad = Validador.edad(datos.get("edad"));
        String clave = Validador.clave(texto(datos, "clave"));

        revisarQueNoExista("usuario", usuario, "Ese usuario ya está registrado. Prueba con otro, por ejemplo " + usuario + "1.");
        revisarQueNoExista("correo", correo, "Ese Gmail ya tiene una cuenta. Si es tuyo, ingresa con él.");
        revisarQueNoExista("dni", dni, "Ese DNI ya está registrado en otra cuenta.");
        revisarQueNoExista("celular", celular, "Ese celular ya está registrado en otra cuenta.");

        Usuario nuevo = new Usuario(0, nombre, usuario, correo, dni, celular, edad);
        int id = usuarios.insertar(nuevo, Cifrado.cifrar(clave));
        return new Usuario(id, nombre, usuario, correo, dni, celular, edad);
    }

    public boolean disponible(String campo, String valor) throws SQLException {
        return !usuarios.existe(campo, valor.trim());
    }

    /** Intentos permitidos antes de bloquear la cuenta. */
    public static final int INTENTOS = 3;
    /** Espera de cada bloqueo seguido: 30 s, 1 min, 5 min y luego 15 min. */
    private static final int[] ESPERAS = {30, 60, 300, 900};
    private static final int MINUTOS_CODIGO = 10;
    private static final int SEGUNDOS_ENTRE_CODIGOS = 60;
    /** Permisos para cambiar la contraseña tras comprobar el código: token → [usuario, vence]. */
    private static final Map<String, long[]> PERMISOS = new java.util.concurrent.ConcurrentHashMap<>();

    public Usuario ingresar(String usuarioOCorreo, String clave) throws SQLException {
        if (usuarioOCorreo == null || usuarioOCorreo.isBlank())
            throw new ErrorUsuario("Escribe tu usuario o tu Gmail.", "login");
        if (clave == null || clave.isBlank())
            throw new ErrorUsuario("Escribe tu contraseña.", "clave");
        UsuarioDAO.EstadoCuenta e = usuarios.buscarEstado(usuarioOCorreo.trim());
        if (e == null)
            throw new ErrorUsuario("No existe una cuenta con ese usuario o Gmail. Revisa si está bien escrito o crea una cuenta.", "login", 401);
        int id = e.usuario().id();
        if (e.segundosBloqueo() > 0) throw bloqueada(e.segundosBloqueo());
        if (!Cifrado.coincide(clave, e.claveHash())) {
            int intentos = e.intentos() + 1;
            if (intentos >= INTENTOS) {
                int espera = ESPERAS[Math.min(e.bloqueos(), ESPERAS.length - 1)];
                usuarios.registrarFallo(id, 0, e.bloqueos() + 1, espera);
                throw bloqueada(espera);
            }
            usuarios.registrarFallo(id, intentos, e.bloqueos(), 0);
            int quedan = INTENTOS - intentos;
            throw new ErrorUsuario("La contraseña no es correcta. Te " + (quedan == 1 ? "queda 1 intento." : "quedan " + quedan + " intentos."),
                    "clave", 401, Map.of("intentosQuedan", quedan));
        }
        if (e.intentos() > 0 || e.bloqueos() > 0) usuarios.limpiarIntentos(id);
        return e.usuario();
    }

    private static ErrorUsuario bloqueada(long segundos) {
        return new ErrorUsuario("Fallaste " + INTENTOS + " veces. Por seguridad la cuenta está bloqueada " + tiempo(segundos)
                + ". Si no recuerdas la contraseña, usa «¿Olvidaste tu contraseña?».", "clave", 429,
                Map.of("bloqueado", true, "segundos", segundos, "intentosQuedan", 0));
    }

    /** Paso 1: busca la cuenta y envía un código de 6 números al Gmail. */
    public Map<String, Object> pedirCodigo(String usuarioOCorreo) throws SQLException {
        if (usuarioOCorreo == null || usuarioOCorreo.isBlank())
            throw new ErrorUsuario("Escribe tu Gmail o tu usuario.", "login");
        UsuarioDAO.EstadoCuenta e = usuarios.buscarEstado(usuarioOCorreo.trim());
        if (e == null)
            throw new ErrorUsuario("No existe ninguna cuenta con ese Gmail o usuario. Revisa que esté bien escrito o crea una cuenta.", "login", 404);
        long falta = SEGUNDOS_ENTRE_CODIGOS - e.segundosDesdePedido();
        if (falta > 0)
            throw new ErrorUsuario("Ya te enviamos un código. Podrás pedir otro en " + falta + " segundos.", "login", 429,
                    Map.of("segundos", falta));
        String codigo = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
        usuarios.guardarCodigo(e.usuario().id(), Cifrado.cifrar(codigo), MINUTOS_CODIGO);
        Usuario u = e.usuario();
        String nombre = u.nombreCompleto().split(" ")[0];
        boolean enviado = Correo.enviar(u.correo(), CorreoCodigo.asunto(codigo),
                CorreoCodigo.texto(nombre, codigo, MINUTOS_CODIGO), CorreoCodigo.html(nombre, codigo, MINUTOS_CODIGO));
        if (!enviado) System.out.println("Código de recuperación para " + u.usuario() + " (" + u.correo() + "): " + codigo);
        return Map.of("correo", ocultar(u.correo()), "minutos", MINUTOS_CODIGO, "segundosOtro", SEGUNDOS_ENTRE_CODIGOS,
                "enviado", enviado);
    }

    /** Paso 2: comprueba el código. Tres intentos; al tercero el código se anula. */
    public String comprobarCodigo(String usuarioOCorreo, String codigo) throws SQLException {
        UsuarioDAO.EstadoCuenta e = usuarios.buscarEstado(String.valueOf(usuarioOCorreo).trim());
        if (e == null || e.codigoHash() == null)
            throw new ErrorUsuario("Primero pide un código.", "codigo", 400, Map.of("anulado", true));
        if (e.segundosCodigo() <= 0) {
            usuarios.borrarCodigo(e.usuario().id());
            throw new ErrorUsuario("El código venció. Pide uno nuevo.", "codigo", 400, Map.of("anulado", true));
        }
        if (codigo == null || !codigo.matches("[0-9]{6}"))
            throw new ErrorUsuario("El código tiene 6 números.", "codigo");
        if (!Cifrado.coincide(codigo, e.codigoHash())) {
            int intentos = e.codigoIntentos() + 1;
            if (intentos >= INTENTOS) {
                usuarios.borrarCodigo(e.usuario().id());
                throw new ErrorUsuario("Fallaste " + INTENTOS + " veces: el código se anuló. Pide uno nuevo.", "codigo", 400,
                        Map.of("anulado", true));
            }
            usuarios.fallarCodigo(e.usuario().id(), intentos);
            int quedan = INTENTOS - intentos;
            throw new ErrorUsuario("Código incorrecto. Te " + (quedan == 1 ? "queda 1 intento." : "quedan " + quedan + " intentos."),
                    "codigo", 400, Map.of("intentosQuedan", quedan));
        }
        usuarios.borrarCodigo(e.usuario().id());
        String permiso = Cifrado.token();
        PERMISOS.put(permiso, new long[]{e.usuario().id(), System.currentTimeMillis() + MINUTOS_CODIGO * 60_000L});
        return permiso;
    }

    /** Paso 3: guarda la nueva contraseña y cierra las sesiones abiertas en otros equipos. */
    public Usuario cambiarClave(String permiso, String clave, String repetida) throws SQLException {
        long[] p = permiso == null ? null : PERMISOS.get(permiso);
        if (p == null || p[1] < System.currentTimeMillis()) {
            if (permiso != null) PERMISOS.remove(permiso);
            throw new ErrorUsuario("Pasó mucho tiempo. Vuelve a pedir un código.", "permiso", 400, Map.of("anulado", true));
        }
        String nueva = Validador.clave(clave);
        if (!nueva.equals(repetida)) throw new ErrorUsuario("Las dos contraseñas no son iguales.", "clave2");
        int id = (int) p[0];
        usuarios.cambiarClave(id, Cifrado.cifrar(nueva));
        PERMISOS.remove(permiso);
        GestorSesiones.cerrarDeUsuario(id);
        return usuarios.obtener(id);
    }

    /** fabian23@gmail.com → fa******@gmail.com */
    static String ocultar(String correo) {
        int arroba = correo.indexOf('@');
        if (arroba < 0) return correo;
        int ver = Math.min(2, arroba);
        return correo.substring(0, ver) + "*".repeat(Math.min(6, Math.max(3, arroba - ver))) + correo.substring(arroba);
    }

    static String tiempo(long segundos) {
        if (segundos < 60) return segundos + " segundos";
        long m = (segundos + 59) / 60;
        return m == 1 ? "1 minuto" : m + " minutos";
    }

    public Usuario obtener(int id) throws SQLException {
        return usuarios.obtener(id);
    }

    private void revisarQueNoExista(String campo, String valor, String mensaje) throws SQLException {
        if (usuarios.existe(campo, valor)) throw new ErrorUsuario(mensaje, campo, 409);
    }

    private static String texto(Map<String, Object> datos, String clave) {
        Object v = datos.get(clave);
        return v == null ? "" : String.valueOf(v);
    }
}
