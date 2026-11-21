package streamflix.modelo;

/** Respuesta de la encuesta de satisfacción (valores de 1 a 5). */
public record Encuesta(int perfilId, int satisfaccion, int acierto, int facilidad,
                       boolean recomendaria, String comentario) {}
