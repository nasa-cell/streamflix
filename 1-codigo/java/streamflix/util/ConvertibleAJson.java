package streamflix.util;

import java.util.Map;

/** Objetos del modelo que saben convertirse en un mapa para enviarse como JSON. */
public interface ConvertibleAJson {
    Map<String, Object> aMapa();
}
