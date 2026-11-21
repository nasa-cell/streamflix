package streamflix.servicio;

/**
 * Error que se le muestra tal cual a la persona: dice qué pasó y, si aplica,
 * en qué campo del formulario está el problema.
 */
public class ErrorUsuario extends RuntimeException {

    private final String campo;
    private final int codigoHttp;
    private final java.util.Map<String, Object> datos;

    public ErrorUsuario(String mensaje) {
        this(mensaje, null, 400);
    }

    public ErrorUsuario(String mensaje, String campo) {
        this(mensaje, campo, 400);
    }

    public ErrorUsuario(String mensaje, String campo, int codigoHttp) {
        this(mensaje, campo, codigoHttp, java.util.Map.of());
    }

    /** Con datos extra para la página, por ejemplo cuántos intentos quedan. */
    public ErrorUsuario(String mensaje, String campo, int codigoHttp, java.util.Map<String, Object> datos) {
        super(mensaje);
        this.campo = campo;
        this.codigoHttp = codigoHttp;
        this.datos = datos;
    }

    public java.util.Map<String, Object> getDatos() {
        return datos;
    }

    public String getCampo() {
        return campo;
    }

    public int getCodigoHttp() {
        return codigoHttp;
    }
}
