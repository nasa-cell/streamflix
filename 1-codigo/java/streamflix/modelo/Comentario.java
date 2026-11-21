package streamflix.modelo;

import java.util.Map;
import streamflix.util.ConvertibleAJson;
import streamflix.util.Json;

public record Comentario(int id, int perfilId, String perfilNombre, String avatar, String color,
                         String texto, String fecha, int estrellas) implements ConvertibleAJson {
    @Override
    public Map<String, Object> aMapa() {
        return Json.objeto("id", id, "perfilId", perfilId, "perfil", perfilNombre, "avatar", avatar,
                "color", color, "texto", texto, "fecha", fecha, "estrellas", estrellas);
    }
}
