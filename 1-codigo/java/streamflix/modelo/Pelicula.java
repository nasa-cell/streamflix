package streamflix.modelo;

import java.util.List;
import java.util.Map;
import streamflix.util.ConvertibleAJson;
import streamflix.util.Json;

public record Pelicula(int id, String titulo, int anio, String tipo, int duracionMin,
                       String clasificacion, String descripcion, String portada, String trailer, String trailerIdioma,
                       List<String> generos, List<Plataforma> plataformas,
                       double promedio, int votos) implements ConvertibleAJson {

    public String generoPrincipal() {
        return generos.isEmpty() ? "Otro" : generos.get(0);
    }

    public boolean aptaParaNinos() {
        return "ATP".equals(clasificacion);
    }

    @Override
    public Map<String, Object> aMapa() {
        return Json.objeto("id", id, "titulo", titulo, "anio", anio, "tipo", tipo,
                "duracionMin", duracionMin, "clasificacion", clasificacion,
                "descripcion", descripcion, "portada", portada, "trailer", trailer, "trailerIdioma", trailerIdioma, "generos", generos,
                "plataformas", plataformas, "promedio", Math.round(promedio * 10) / 10.0, "votos", votos);
    }
}
