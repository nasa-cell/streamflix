package streamflix.servicio;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import streamflix.dao.CalificacionDAO;
import streamflix.dao.ComentarioDAO;
import streamflix.dao.GustoDAO;
import streamflix.dao.ListaDAO;
import streamflix.dao.VistaDAO;
import streamflix.ia.AnalizadorSentimiento;
import streamflix.ia.ArbolDecision;
import streamflix.ia.CombinadorSenales;
import streamflix.ia.DatosEntrenamiento;
import streamflix.ia.FiltradoContenido;
import streamflix.ia.OrdenRecursivo;
import streamflix.ia.RecomendadorHibrido;
import streamflix.ia.ResultadoMetodo;
import streamflix.ia.Senal;
import streamflix.ia.Similitud;
import streamflix.ia.ValidacionCruzada;
import streamflix.ia.Vecino;
import streamflix.modelo.Calificacion;
import streamflix.modelo.Pelicula;
import streamflix.modelo.Perfil;
import streamflix.modelo.Recomendacion;
import streamflix.util.Json;

/** Arma las filas del inicio, la pantalla "Mis gustos" y las métricas de la IA. */
public class RecomendacionServicio {

    private final CalificacionDAO calificacionDAO = new CalificacionDAO();
    private final GustoDAO gustoDAO = new GustoDAO();
    private final VistaDAO vistaDAO = new VistaDAO();
    private final ListaDAO listaDAO = new ListaDAO();
    private final ComentarioDAO comentarioDAO = new ComentarioDAO();

    private static final int REPETICIONES = 5;
    private static volatile Map<String, Object> metricasGuardadas;

    private record Contexto(Map<Integer, Pelicula> peliculas, DatosEntrenamiento datos, List<Senal> senales,
                            Map<Long, String> comentarios, RecomendadorHibrido hibrido, Map<Integer, String> nombres) {}

    /** La IA aprende de estrellas, de lo que se vio (minutos) y del sentimiento de los comentarios. */
    private static volatile Contexto guardado;
    private static volatile long versionGuardada = -1;

    /**
     * El modelo se entrena una vez y se comparte entre todas las personas.
     * Solo se vuelve a entrenar cuando alguien califica, comenta, ve algo o cambia un perfil.
     */
    private Contexto contexto() throws SQLException {
        long v = CacheDatos.version();
        if (versionGuardada != v) {
            synchronized (RecomendacionServicio.class) {
                if (versionGuardada != v) {
                    guardado = entrenar();
                    versionGuardada = v;
                }
            }
        }
        return guardado;
    }

    private Contexto entrenar() throws SQLException {
        Map<Integer, Pelicula> peliculas = CacheDatos.peliculas();
        Map<Long, String> comentarios = comentarioDAO.textosPorPar();
        List<Calificacion> calificaciones = calificacionDAO.todas();
        List<Senal> senales = CombinadorSenales.combinar(peliculas, calificaciones, vistaDAO.minutos(), comentarios, Set.of());
        AprendizajeContinuo.revisar(calificaciones.size());
        DatosEntrenamiento datos = new DatosEntrenamiento(peliculas, senales, gustoDAO.declarados());
        return new Contexto(peliculas, datos, senales, comentarios, new RecomendadorHibrido(datos).aprenderPesos(), gustoDAO.nombresDePerfiles());
    }

    public Map<String, Object> inicio(Perfil perfil) throws SQLException {
        Contexto cx = contexto();
        int id = perfil.id();
        Set<Integer> vistas = vistaDAO.dePerfil(id);
        Set<Integer> lista = listaDAO.dePerfil(id);
        List<Recomendacion> paraTi = cx.hibrido().recomendar(id, cx.peliculas().values(), vistas, perfil.infantil(), 14, cx.nombres());

        List<Map<String, Object>> filas = new ArrayList<>();
        filas.add(fila("para-ti", "Recomendado para ti", "según tus gustos y perfiles parecidos", paraTi));

        Integer base = ultimaQueLeGusto(cx.datos(), vistas, id);
        if (base != null) {
            Pelicula p = cx.peliculas().get(base);
            filas.add(fila("porque", "Porque viste «" + p.titulo() + "»", "mismos géneros",
                    parecidas(p, cx.peliculas().values(), vistas, perfil.infantil())));
        }
        List<Recomendacion> series = new ArrayList<>();
        for (Recomendacion r : cx.hibrido().recomendar(id, cx.peliculas().values(), vistas, perfil.infantil(), 60, cx.nombres())) {
            if ("Serie".equals(r.pelicula().tipo()) && series.size() < 10) series.add(r);
        }
        filas.add(fila("series", "Series para ti", "", series));
        filas.add(fila("estrenos", "Estrenos 2026", "lo nuevo del año", filtrar(cx, perfil, p -> p.anio() == 2026)));
        filas.add(fila("populares", "Lo más popular", "lo que el sistema anterior mostraba a todos", populares(cx, perfil)));
        if (!lista.isEmpty()) filas.add(fila("mi-lista", "Mi lista", "lo que guardaste para después", deIds(cx, lista, perfil)));
        if (!vistas.isEmpty()) filas.add(fila("vistas", "Ya las viste", "califícalas para mejorar tus recomendaciones", deIds(cx, vistas, perfil)));

        return Json.objeto("destacada", paraTi.isEmpty() ? null : paraTi.get(0), "filas", filas,
                "vistas", vistas, "lista", lista);
    }

    public Map<String, Object> misGustos(Perfil perfil) throws SQLException {
        Contexto cx = contexto();
        int id = perfil.id();
        RecomendadorHibrido h = cx.hibrido();
        Set<Integer> vistas = vistaDAO.dePerfil(id);

        List<Map<String, Object>> vecinos = new ArrayList<>();
        for (Vecino v : h.colaborativo().vecinos(id)) {
            if (vecinos.size() >= 8) break;
            vecinos.add(Json.objeto("nombre", cx.nombres().getOrDefault(v.perfilId(), "Perfil " + v.perfilId()),
                    "parecido", Math.round(v.similitud() * 100), "enComun", v.enComun()));
        }
        ArbolDecision arbol = h.arbolDe(id);
        Map<Integer, Double> mias = cx.datos().valoresDe(id);
        double promedio = mias.isEmpty() ? 0 : mias.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        Map<String, Integer> fuentes = new LinkedHashMap<>();
        fuentes.put(Senal.ESTRELLAS, 0);
        fuentes.put(Senal.VISTA, 0);
        fuentes.put(Senal.COMENTARIO, 0);
        for (Senal s : cx.senales()) {
            if (s.perfilId() == id && !s.fuente().equals(Senal.COMENTARIO)) fuentes.merge(s.fuente(), 1, Integer::sum);
        }
        for (Long par : cx.comentarios().keySet()) if (par / 1_000_000L == id) fuentes.merge(Senal.COMENTARIO, 1, Integer::sum);
        List<Map<String, Object>> sentimientos = new ArrayList<>();
        for (Map.Entry<Long, String> c : cx.comentarios().entrySet()) {
            if (c.getKey() / 1_000_000L != id) continue;
            Pelicula p = cx.peliculas().get((int) (c.getKey() % 1_000_000L));
            double s = AnalizadorSentimiento.analizar(c.getValue());
            sentimientos.add(Json.objeto("pelicula", p == null ? "" : p.titulo(), "texto", c.getValue(),
                    "sentimiento", Math.round(s * 100), "etiqueta", AnalizadorSentimiento.etiqueta(s)));
        }
        int minutos = 0;
        for (int v : vistas) {
            Pelicula p = cx.peliculas().get(v);
            if (p != null) minutos += "Serie".equals(p.tipo()) ? p.duracionMin() * 8 : p.duracionMin();
        }
        return Json.objeto(
                "generos", h.contenido().generosFavoritos(id, 6),
                "vecinos", vecinos,
                "porque", h.recomendar(id, cx.peliculas().values(), vistas, perfil.infantil(), 6, cx.nombres()),
                "arbolTexto", arbol.dibujar(),
                "arbol", arbol.aMapa(),
                "importancia", arbol.importancia(),
                "nodos", arbol.contarNodos(),
                "profundidad", arbol.profundidad(),
                "fuentes", fuentes,
                "sentimientos", sentimientos,
                "resumen", Json.objeto("vistas", vistas.size(), "calificadas", fuentes.get(Senal.ESTRELLAS),
                        "promedio", Math.round(promedio * 10) / 10.0, "horas", Math.round(minutos / 60.0)));
    }

    /** Validación cruzada de 5 partes. Se calcula una vez y se guarda; "recalcular" la vuelve a correr. */
    public Map<String, Object> metricas(boolean recalcular) throws SQLException {
        if (metricasGuardadas != null && !recalcular) return metricasGuardadas;
        Map<Integer, Pelicula> peliculas = CacheDatos.peliculas();
        List<Calificacion> calificaciones = calificacionDAO.todas();
        long inicio = System.nanoTime();
        List<ResultadoMetodo> r = new ValidacionCruzada().ejecutarRepetida(peliculas, calificaciones,
                gustoDAO.declarados(), vistaDAO.minutos(), comentarioDAO.textosPorPar(), 5, REPETICIONES);
        double segundos = (System.nanoTime() - inicio) / 1e9;
        metricasGuardadas = Json.objeto("metodos", r, "partes", 5, "repeticiones", REPETICIONES, "calificaciones", calificaciones.size(),
                "perfiles", new HashSet<>(calificaciones.stream().map(Calificacion::perfilId).toList()).size(),
                "peliculas", peliculas.size(), "top", ValidacionCruzada.TOP,
                "segundos", Math.round(segundos * 100) / 100.0);
        return metricasGuardadas;
    }

    /** Tipo y género que eligió la persona en los botones del inicio ("Todo" o vacío = sin filtro). */
    private static boolean pasa(Pelicula p, String tipo, String genero) {
        boolean okTipo = switch (tipo == null ? "" : tipo) {
            case "Película", "Serie" -> tipo.equals(p.tipo()) && !p.generos().contains("Anime");
            case "Anime" -> p.generos().contains("Anime");
            default -> true;
        };
        return okTipo && (genero == null || genero.isBlank() || p.generos().contains(genero));
    }

    private static String nombreTipo(String tipo) {
        return switch (tipo == null ? "" : tipo) {
            case "Película" -> "Películas";
            case "Serie" -> "Series";
            case "Anime" -> "Anime";
            default -> "";
        };
    }

    /** Inicio filtrado por los botones: la IA sigue ordenando y explicando, pero solo dentro de lo elegido. */
    public Map<String, Object> explorar(Perfil perfil, String tipo, String genero) throws SQLException {
        Contexto cx = contexto();
        int id = perfil.id();
        Set<Integer> vistas = vistaDAO.dePerfil(id);
        List<Pelicula> elegidas = new ArrayList<>();
        for (Pelicula p : cx.peliculas().values()) {
            if (pasa(p, tipo, genero) && (!perfil.infantil() || p.aptaParaNinos())) elegidas.add(p);
        }
        String nombre = String.join(" · ", java.util.stream.Stream.of(nombreTipo(tipo), genero == null ? "" : genero)
                .filter(x -> !x.isBlank()).toList());
        List<Recomendacion> paraTi = cx.hibrido().recomendar(id, elegidas, vistas, perfil.infantil(), elegidas.size(), cx.nombres());
        List<Map<String, Object>> filas = new ArrayList<>();
        filas.add(fila("para-ti", "Recomendado para ti · " + nombre, "la IA lo ordena según tus gustos", paraTi));
        boolean sinTipo = tipo == null || tipo.isBlank() || "Todo".equals(tipo);
        if (sinTipo) {
            for (String t : List.of("Película", "Serie", "Anime")) {
                List<Recomendacion> parte = new ArrayList<>();
                for (Recomendacion r : paraTi) if (pasa(r.pelicula(), t, null)) parte.add(r);
                if (!parte.isEmpty() && parte.size() < paraTi.size()) filas.add(fila("tipo-" + t, nombreTipo(t) + " · " + genero, "", parte));
            }
        }
        List<Pelicula> yaVistas = new ArrayList<>();
        for (Pelicula p : elegidas) if (vistas.contains(p.id())) yaVistas.add(p);
        if (!yaVistas.isEmpty()) filas.add(fila("vistas", "Ya las viste · " + nombre, "califícalas para mejorar tus recomendaciones", yaVistas));
        return Json.objeto("filas", filas, "total", elegidas.size(), "nombre", nombre, "vistas", vistas, "lista", listaDAO.dePerfil(id));
    }

    /** Géneros para los botones y los mosaicos: cuántos títulos tiene cada uno y tres portadas de muestra. */
    public List<Map<String, Object>> categorias(Perfil perfil) throws SQLException {
        Contexto cx = contexto();
        Map<String, List<Pelicula>> porGenero = new java.util.TreeMap<>();
        for (Pelicula p : cx.peliculas().values()) {
            if (perfil.infantil() && !p.aptaParaNinos()) continue;
            for (String g : p.generos()) porGenero.computeIfAbsent(g, k -> new ArrayList<>()).add(p);
        }
        List<Map<String, Object>> r = new ArrayList<>();
        for (Map.Entry<String, List<Pelicula>> e : porGenero.entrySet()) {
            List<Pelicula> orden = OrdenRecursivo.ordenar(e.getValue(), (a, b) -> Double.compare(
                    cx.hibrido().popular().popularidad(b.id()), cx.hibrido().popular().popularidad(a.id())));
            List<String> portadas = new ArrayList<>();
            for (Pelicula p : orden) if (p.portada() != null && portadas.size() < 6) portadas.add(p.portada());
            r.add(Json.objeto("nombre", e.getKey(), "cantidad", e.getValue().size(), "portadas", portadas));
        }
        return r;
    }

    private Integer ultimaQueLeGusto(DatosEntrenamiento datos, Set<Integer> vistas, int perfilId) {
        Map<Integer, Double> mias = datos.valoresDe(perfilId);
        for (int v : vistas) if (mias.getOrDefault(v, 0.0) >= 4) return v;
        return vistas.isEmpty() ? null : vistas.iterator().next();
    }

    private List<Pelicula> parecidas(Pelicula base, Iterable<Pelicula> todas, Set<Integer> vistas, boolean infantil) {
        Map<String, Double> vb = FiltradoContenido.vectorDePelicula(base);
        List<Map.Entry<Pelicula, Double>> lista = new ArrayList<>();
        for (Pelicula p : todas) {
            if (p.id() == base.id() || vistas.contains(p.id()) || (infantil && !p.aptaParaNinos())) continue;
            double s = Similitud.coseno(vb, FiltradoContenido.vectorDePelicula(p)) + p.promedio() / 50;
            if (s > 0.3) lista.add(Map.entry(p, s));
        }
        List<Pelicula> r = new ArrayList<>();
        for (Map.Entry<Pelicula, Double> e : OrdenRecursivo.ordenar(lista, (a, b) -> Double.compare(b.getValue(), a.getValue()))) {
            if (r.size() >= 10) break;
            r.add(e.getKey());
        }
        return r;
    }

    private List<Pelicula> populares(Contexto cx, Perfil perfil) {
        List<Pelicula> lista = filtrar(cx, perfil, p -> true);
        return OrdenRecursivo.ordenar(lista, (a, b) -> Double.compare(
                cx.hibrido().popular().popularidad(b.id()), cx.hibrido().popular().popularidad(a.id()))).subList(0, Math.min(12, lista.size()));
    }

    private List<Pelicula> filtrar(Contexto cx, Perfil perfil, java.util.function.Predicate<Pelicula> condicion) {
        List<Pelicula> r = new ArrayList<>();
        for (Pelicula p : cx.peliculas().values()) {
            if (perfil.infantil() && !p.aptaParaNinos()) continue;
            if (condicion.test(p)) r.add(p);
        }
        return OrdenRecursivo.ordenar(r, (a, b) -> Double.compare(b.promedio(), a.promedio()));
    }

    private List<Pelicula> deIds(Contexto cx, Set<Integer> ids, Perfil perfil) {
        List<Pelicula> r = new ArrayList<>();
        for (int i : ids) {
            Pelicula p = cx.peliculas().get(i);
            if (p != null && (!perfil.infantil() || p.aptaParaNinos())) r.add(p);
        }
        return r;
    }

    private Map<String, Object> fila(String id, String titulo, String detalle, List<?> elementos) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("titulo", titulo);
        m.put("detalle", detalle);
        m.put("peliculas", elementos);
        return m;
    }
}
