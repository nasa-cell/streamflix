package streamflix.ia;

import java.util.Map;

/**
 * Un caso para entrenar el árbol: las características de una película,
 * si le gustó al perfil (4 o 5 estrellas) y cuánto pesa este caso.
 */
public record Ejemplo(Map<String, String> atributos, boolean gusta, double peso) {}
