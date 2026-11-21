package streamflix.ia;

/**
 * Qué tanto le gustó una película a un perfil, en escala de 1 a 5, sacado de
 * sus estrellas, de cuánto la vio y de lo que comentó.
 */
public record Senal(int perfilId, int peliculaId, double valor, String fuente) {

    public static final String ESTRELLAS = "estrellas";
    public static final String VISTA = "vista";
    public static final String COMENTARIO = "comentario";

    public static long par(int perfilId, int peliculaId) {
        return perfilId * 1_000_000L + peliculaId;
    }
}
