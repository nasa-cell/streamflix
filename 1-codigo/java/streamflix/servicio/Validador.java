package streamflix.servicio;

import java.util.regex.Pattern;

/**
 * Reglas de cada dato del formulario. Son las mismas que revisa la página
 * mientras se escribe, pero aquí se vuelven a revisar porque nunca se debe
 * confiar solo en el navegador.
 */
public final class Validador {

    private static final Pattern NOMBRE = Pattern.compile("^[A-Za-zÁÉÍÓÚáéíóúÑñÜü ]{2,40}$");
    private static final Pattern USUARIO = Pattern.compile("^[A-Za-z0-9]{4,20}$");
    private static final Pattern CORREO = Pattern.compile("^[A-Za-z0-9.]{6,30}@gmail\\.com$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DNI = Pattern.compile("^[0-9]{8}$");
    private static final Pattern CELULAR = Pattern.compile("^9[0-9]{8}$");
    private static final Pattern CLAVE = Pattern.compile("^(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z0-9]{8,20}$");
    private static final Pattern PERFIL = Pattern.compile("^[A-Za-z0-9ÁÉÍÓÚáéíóúÑñ]{2,15}$");

    private Validador() {}

    public static String nombreCompleto(String v) {
        v = recortar(v);
        if (v.isEmpty()) throw new ErrorUsuario("Escribe tu nombre completo.", "nombreCompleto");
        if (!NOMBRE.matcher(v).matches())
            throw new ErrorUsuario("El nombre solo lleva letras y espacios, de 2 a 40 letras.", "nombreCompleto");
        return v.replaceAll(" +", " ");
    }

    public static String usuario(String v) {
        v = recortar(v);
        if (!USUARIO.matcher(v).matches())
            throw new ErrorUsuario("El usuario lleva de 4 a 20 letras o números, sin espacios ni símbolos.", "usuario");
        return v;
    }

    public static String correo(String v) {
        v = recortar(v).toLowerCase();
        if (!CORREO.matcher(v).matches())
            throw new ErrorUsuario("El correo debe ser un Gmail, por ejemplo fabian23@gmail.com (6 a 30 letras o números antes de @).", "correo");
        return v;
    }

    public static String dni(String v) {
        v = recortar(v);
        if (!DNI.matcher(v).matches()) throw new ErrorUsuario("El DNI tiene exactamente 8 números.", "dni");
        return v;
    }

    public static String celular(String v) {
        v = recortar(v);
        if (!CELULAR.matcher(v).matches())
            throw new ErrorUsuario("El celular tiene 9 números y empieza con 9.", "celular");
        return v;
    }

    public static int edad(Object v) {
        int edad;
        try {
            edad = Integer.parseInt(recortar(String.valueOf(v)));
        } catch (NumberFormatException e) {
            throw new ErrorUsuario("La edad solo lleva números.", "edad");
        }
        if (edad < 13 || edad > 99) throw new ErrorUsuario("La edad debe estar entre 13 y 99 años.", "edad");
        return edad;
    }

    public static String clave(String v) {
        v = v == null ? "" : v;
        if (!CLAVE.matcher(v).matches())
            throw new ErrorUsuario("La contraseña lleva de 8 a 20 caracteres, con letras y números juntos (sin símbolos).", "clave");
        return v;
    }

    public static String nombrePerfil(String v) {
        v = recortar(v);
        if (!PERFIL.matcher(v).matches())
            throw new ErrorUsuario("El nombre del perfil lleva de 2 a 15 letras o números, sin espacios.", "nombre");
        return v;
    }

    public static String comentario(String v) {
        v = recortar(v);
        if (v.length() < 10) throw new ErrorUsuario("El comentario es muy corto: escribe al menos 10 letras.", "texto");
        if (v.length() > 300) throw new ErrorUsuario("El comentario es muy largo: máximo 300 letras.", "texto");
        return v;
    }

    public static int deUnoACinco(Object v, String campo, String nombre) {
        int n;
        try {
            n = (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new ErrorUsuario("Falta elegir " + nombre + ".", campo);
        }
        if (n < 1 || n > 5) throw new ErrorUsuario("Elige " + nombre + " de 1 a 5.", campo);
        return n;
    }

    private static String recortar(String v) {
        return v == null ? "" : v.trim();
    }
}
