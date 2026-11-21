package streamflix.ia;

/** Otro perfil parecido y qué tan parecido es (de 0 a 1). */
public record Vecino(int perfilId, double similitud, int enComun) {}
