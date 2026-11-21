package streamflix.modelo;

/** Una calificación de 1 a 5 estrellas que un perfil dio a una película. */
public record Calificacion(int perfilId, int peliculaId, int estrellas) {}
