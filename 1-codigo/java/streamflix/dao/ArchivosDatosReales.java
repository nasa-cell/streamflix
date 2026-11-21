package streamflix.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import streamflix.modelo.Calificacion;
import streamflix.modelo.Pelicula;

/**
 * Lee los conjuntos de datos reales que se usan solo para probar el algoritmo:
 *  - MovieLens ml-latest-small (GroupLens, Universidad de Minnesota): películas.
 *  - Anime de MyAnimeList (copia en Hugging Face, licencia Apache 2.0): series y películas de anime.
 * Se descargan con 3-pruebas/datos-reales/descargar.bat; no se incluyen en el proyecto.
 */
public final class ArchivosDatosReales {

    /** Un conjunto de datos listo para la validación cruzada. */
    public record Conjunto(String nombre, String fuente, Map<Integer, Pelicula> peliculas,
                           List<Calificacion> calificaciones, int usuarios, String nota) {}

    private static final Path CARPETA = streamflix.util.Carpeta.ruta("3-pruebas", "datos-reales");
    private static final Map<String, String> GENEROS = new HashMap<>();

    static {
        String[][] pares = {
                {"Action", "Acción"}, {"Adventure", "Aventura"}, {"Animation", "Animación"}, {"Children", "Familiar"},
                {"Comedy", "Comedia"}, {"Crime", "Crimen"}, {"Documentary", "Documental"}, {"Drama", "Drama"},
                {"Fantasy", "Fantasía"}, {"Film-Noir", "Cine negro"}, {"Horror", "Terror"}, {"Musical", "Musical"},
                {"Mystery", "Misterio"}, {"Romance", "Romance"}, {"Sci-Fi", "Ciencia ficción"}, {"Thriller", "Suspenso"},
                {"War", "Bélica"}, {"Western", "Western"}, {"IMAX", "IMAX"}, {"Sci-fi", "Ciencia ficción"},
                {"Supernatural", "Sobrenatural"}, {"Slice of Life", "Vida cotidiana"}, {"Sports", "Deportes"},
                {"School", "Escolar"}, {"Shounen", "Shōnen"}, {"Shoujo", "Shōjo"}, {"Seinen", "Seinen"},
                {"Mecha", "Mecha"}, {"Music", "Musical"}, {"Psychological", "Psicológico"}, {"Historical", "Histórico"},
                {"Military", "Militar"}, {"Magic", "Magia"}, {"Super Power", "Superpoderes"}, {"Kids", "Familiar"},
                {"Space", "Espacio"}, {"Martial Arts", "Artes marciales"}, {"Police", "Policial"}, {"Game", "Juegos"},
                {"Parody", "Parodia"}, {"Samurai", "Samurái"}, {"Demons", "Demonios"}, {"Vampire", "Vampiros"},
                {"Josei", "Josei"}, {"Harem", "Harem"}, {"Cars", "Autos"}, {"Dementia", "Surrealista"}};
        for (String[] p : pares) GENEROS.put(p[0], p[1]);
    }

    private ArchivosDatosReales() {}

    public static boolean hayMovieLens() {
        return Files.exists(CARPETA.resolve("ml-latest-small").resolve("ratings.csv"));
    }

    public static boolean hayAnime() {
        return Files.exists(CARPETA.resolve("anime").resolve("UserRatings.csv"));
    }

    /**
     * MovieLens: 100 836 calificaciones de 610 personas. Las notas van de 0,5 a 5
     * y se redondean a estrellas enteras (0,5 cuenta como 1). El año se agrupa por década.
     */
    public static Conjunto movieLens() throws IOException {
        Path base = CARPETA.resolve("ml-latest-small");
        Map<Integer, Pelicula> peliculas = new LinkedHashMap<>();
        List<List<String>> filas = leerCsv(base.resolve("movies.csv"));
        for (List<String> f : filas.subList(1, filas.size())) {
            int id = Integer.parseInt(f.get(0));
            String titulo = f.get(1).trim();
            int anio = 0;
            int abre = titulo.lastIndexOf('(');
            if (abre >= 0 && titulo.endsWith(")")) {
                try {
                    anio = Integer.parseInt(titulo.substring(abre + 1, titulo.length() - 1).split("[–-]")[0]);
                } catch (NumberFormatException ignorado) {
                    anio = 0;
                }
            }
            List<String> generos = new ArrayList<>();
            for (String g : f.get(2).split("\\|")) if (!g.startsWith("(")) generos.add(GENEROS.getOrDefault(g, g));
            peliculas.put(id, new Pelicula(id, titulo, anio / 10 * 10, "Película", 100, "", "", null, null, null,
                    generos, List.of(), 0, 0));
        }
        List<Calificacion> calificaciones = new ArrayList<>();
        java.util.Set<Integer> usuarios = new java.util.HashSet<>();
        try (BufferedReader in = Files.newBufferedReader(base.resolve("ratings.csv"), StandardCharsets.UTF_8)) {
            in.readLine();
            String linea;
            while ((linea = in.readLine()) != null) {
                String[] p = linea.split(",");
                int u = Integer.parseInt(p[0]);
                int estrellas = (int) Math.max(1, Math.min(5, Math.round(Double.parseDouble(p[2]))));
                calificaciones.add(new Calificacion(u, Integer.parseInt(p[1]), estrellas));
                usuarios.add(u);
            }
        }
        return new Conjunto("MovieLens (películas reales)", "GroupLens · ml-latest-small", peliculas, calificaciones,
                usuarios.size(), "Notas de 0,5 a 5 redondeadas a estrellas enteras.");
    }

    /**
     * Anime de MyAnimeList. Solo trae notas de 5 a 10, que se pasan a estrellas
     * (5→1, 6→2, 7→3, 8→4, 9 y 10→5). Se usan series (TV y ONA) y películas,
     * los anime con al menos 100 calificaciones y hasta 120 calificaciones al azar
     * por persona, para que entre en la memoria de una PC común.
     */
    public static Conjunto anime() throws IOException {
        Path base = CARPETA.resolve("anime");
        Map<Integer, Pelicula> todos = new HashMap<>();
        List<List<String>> filas = leerCsv(base.resolve("Animes.csv"));
        List<String> cab = filas.get(0);
        int cId = cab.indexOf("anime_id"), cGen = cab.indexOf("genres"), cNom = cab.indexOf("name"),
                cTipo = cab.indexOf("type"), cEdad = cab.indexOf("anime_rating"), cEp = cab.indexOf("episodes");
        for (List<String> f : filas.subList(1, filas.size())) {
            if (f.size() <= Math.max(cEdad, cTipo)) continue;
            String tipo = f.get(cTipo);
            String nuestro = switch (tipo) {
                case "TV", "ONA" -> "Serie";
                case "Movie" -> "Película";
                default -> null;
            };
            if (nuestro == null) continue;
            List<String> generos = new ArrayList<>();
            for (String g : f.get(cGen).split(",\\s*")) if (!g.isBlank() && !g.equals("UNKNOWN")) generos.add(GENEROS.getOrDefault(g.trim(), g.trim()));
            int episodios = 1;
            try {
                episodios = (int) Double.parseDouble(f.get(cEp));
            } catch (NumberFormatException ignorado) {
                episodios = 1;
            }
            int duracion = nuestro.equals("Serie") ? (episodios > 26 ? 40 : 24) : 100;
            int id = Integer.parseInt(f.get(cId));
            todos.put(id, new Pelicula(id, f.get(cNom), 0, nuestro, duracion, clasificacion(f.get(cEdad)), "", null, null, null,
                    generos, List.of(), 0, 0));
        }

        Map<Integer, List<int[]>> porUsuario = new HashMap<>();
        Map<Integer, Integer> conteo = new HashMap<>();
        try (BufferedReader in = Files.newBufferedReader(base.resolve("UserRatings.csv"), StandardCharsets.UTF_8)) {
            in.readLine();
            String linea;
            while ((linea = in.readLine()) != null) {
                String[] p = linea.split(",");
                int anime = Integer.parseInt(p[p.length - 2]);
                if (!todos.containsKey(anime)) continue;
                int u = Integer.parseInt(p[0]);
                int nota = Integer.parseInt(p[p.length - 1]);
                porUsuario.computeIfAbsent(u, k -> new ArrayList<>()).add(new int[]{anime, nota});
                conteo.merge(anime, 1, Integer::sum);
            }
        }
        List<Calificacion> calificaciones = new ArrayList<>();
        Map<Integer, Pelicula> usados = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<int[]>> e : porUsuario.entrySet()) {
            List<int[]> lista = new ArrayList<>();
            for (int[] r : e.getValue()) if (conteo.get(r[0]) >= 100) lista.add(r);
            java.util.Collections.shuffle(lista, new Random(e.getKey()));
            for (int[] r : lista.subList(0, Math.min(120, lista.size()))) {
                calificaciones.add(new Calificacion(e.getKey(), r[0], Math.max(1, Math.min(5, r[1] - 4))));
                usados.put(r[0], todos.get(r[0]));
            }
        }
        return new Conjunto("Anime (MyAnimeList)", "Hugging Face · krishnaveni76", usados, calificaciones,
                porUsuario.size(), "Notas de 5 a 10 pasadas a estrellas; muestra de 120 por persona.");
    }

    private static String clasificacion(String texto) {
        if (texto.startsWith("G") || texto.startsWith("PG -")) return "ATP";
        if (texto.startsWith("PG-13")) return "+13";
        if (texto.startsWith("R+") || texto.startsWith("Rx")) return "+18";
        if (texto.startsWith("R")) return "+16";
        return "+13";
    }

    /** Lector de CSV que respeta comillas y saltos de línea dentro de los textos. */
    private static List<List<String>> leerCsv(Path archivo) throws IOException {
        String todo = Files.readString(archivo, StandardCharsets.UTF_8);
        List<List<String>> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean comillas = false;
        for (int k = 0; k < todo.length(); k++) {
            char c = todo.charAt(k);
            if (comillas) {
                if (c == '"') {
                    if (k + 1 < todo.length() && todo.charAt(k + 1) == '"') {
                        campo.append('"');
                        k++;
                    } else {
                        comillas = false;
                    }
                } else {
                    campo.append(c);
                }
            } else if (c == '"') {
                comillas = true;
            } else if (c == ',') {
                fila.add(campo.toString());
                campo.setLength(0);
            } else if (c == '\n') {
                fila.add(campo.toString());
                campo.setLength(0);
                filas.add(fila);
                fila = new ArrayList<>();
            } else if (c != '\r') {
                campo.append(c);
            }
        }
        if (campo.length() > 0 || !fila.isEmpty()) {
            fila.add(campo.toString());
            filas.add(fila);
        }
        return filas;
    }
}
