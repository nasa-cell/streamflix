package streamflix.modelo;

import java.util.List;
import java.util.Map;
import streamflix.util.ConvertibleAJson;
import streamflix.util.Json;

public record Perfil(int id, int usuarioId, String nombre, String avatar, String color,
                     boolean infantil, String idioma, List<String> gustos) implements ConvertibleAJson {
    @Override
    public Map<String, Object> aMapa() {
        return Json.objeto("id", id, "nombre", nombre, "avatar", avatar, "color", color,
                "infantil", infantil, "idioma", idioma, "gustos", gustos);
    }
}
