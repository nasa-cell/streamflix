package streamflix.modelo;

import java.util.Map;
import streamflix.util.ConvertibleAJson;
import streamflix.util.Json;

public record Plataforma(int id, String nombre, String enlace, String color) implements ConvertibleAJson {
    @Override
    public Map<String, Object> aMapa() {
        return Json.objeto("id", id, "nombre", nombre, "enlace", enlace, "color", color);
    }
}
