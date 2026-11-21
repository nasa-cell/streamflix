package streamflix.modelo;

import java.util.Map;
import streamflix.util.ConvertibleAJson;
import streamflix.util.Json;

public record Usuario(int id, String nombreCompleto, String usuario, String correo,
                      String dni, String celular, int edad) implements ConvertibleAJson {
    @Override
    public Map<String, Object> aMapa() {
        return Json.objeto("id", id, "nombreCompleto", nombreCompleto, "usuario", usuario, "correo", correo);
    }
}
