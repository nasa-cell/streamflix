package streamflix.ia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import streamflix.modelo.Pelicula;
import streamflix.modelo.Recomendacion;

/**
 * Junta las técnicas de IA en un solo puntaje: filtrado colaborativo por
 * usuarios y por películas, análisis de contenido (géneros), árbol de decisión
 * y factorización de matrices. Cuánto pesa cada una lo aprende PesosAprendidos;
 * si todavía no hay pesos aprendidos se usan los pesos fijos de la primera versión.
 */
public class RecomendadorHibrido {

    private static final int VECINOS = 12;
    /** Pesos elegidos con la validación cruzada (ver informe). */
    private static final double PESO_COLABORATIVO = 0.25;
    private static final double PESO_CONTENIDO = 0.55;
    private static final double PESO_ARBOL = 0.20;
    private final DatosEntrenamiento datos;
    private final FiltradoColaborativo colaborativo;
    private final FiltradoContenido contenido;
    private final SistemaPopular popular;
    private final Map<Integer, ArbolDecision> arboles = new java.util.concurrent.ConcurrentHashMap<>();
    private final FiltradoPorItems porItems;
    private final long semilla;
    private volatile FactorizacionMatrices factorizacion;
    private volatile PesosAprendidos pesos;

    public RecomendadorHibrido(DatosEntrenamiento datos) {
        this(datos, 7);
    }

    public RecomendadorHibrido(DatosEntrenamiento datos, long semilla) {
        this.datos = datos;
        this.semilla = semilla;
        this.colaborativo = new FiltradoColaborativo(datos, VECINOS);
        this.contenido = new FiltradoContenido(datos);
        this.popular = new SistemaPopular(datos);
        this.porItems = new FiltradoPorItems(datos);
    }

    public FiltradoPorItems porItems() {
        return porItems;
    }

    /** Se entrena la primera vez que se necesita, con ajuste automático de sus parámetros. */
    public FactorizacionMatrices factorizacion() {
        FactorizacionMatrices f = factorizacion;
        if (f == null) {
            synchronized (this) {
                if (factorizacion == null) factorizacion = FactorizacionMatrices.entrenarAjustada(datos, semilla);
                f = factorizacion;
            }
        }
        return f;
    }

    public PesosAprendidos pesos() {
        return pesos;
    }

    /**
     * Aprende los pesos sin hacer trampa: aparta el 20 % de las calificaciones,
     * entrena las técnicas con el resto, mira qué tan bien adivinan lo apartado
     * y con eso ajusta la regresión.
     */
    public RecomendadorHibrido aprenderPesos() {
        List<Senal> entreno = new ArrayList<>();
        List<Senal> aparte = new ArrayList<>();
        java.util.Random azar = new java.util.Random(semilla + 1);
        for (int perfil : datos.perfiles()) {
            for (Map.Entry<Integer, Double> e : datos.valoresDe(perfil).entrySet()) {
                Senal s = new Senal(perfil, e.getKey(), e.getValue(), Senal.ESTRELLAS);
                (azar.nextDouble() < 0.2 ? aparte : entreno).add(s);
            }
        }
        if (aparte.size() < 30) return this;
        Map<Integer, Set<String>> gustos = new HashMap<>();
        for (int perfil : datos.perfiles()) gustos.put(perfil, datos.gustosDe(perfil));
        RecomendadorHibrido interno = new RecomendadorHibrido(new DatosEntrenamiento(datos.peliculas(), entreno, gustos), semilla + 2);
        List<double[]> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        for (Senal s : aparte) {
            Pelicula p = datos.peliculas().get(s.peliculaId());
            if (p == null) continue;
            x.add(PesosAprendidos.entradas(interno.partesCompletas(s.perfilId(), p), interno.experiencia(s.perfilId())));
            y.add(s.valor());
        }
        pesos = PesosAprendidos.entrenar(x, y);
        return this;
    }

    private double experiencia(int perfilId) {
        return Math.min(datos.valoresDe(perfilId).size(), 50) / 50.0;
    }

    public FiltradoColaborativo colaborativo() {
        return colaborativo;
    }

    public FiltradoContenido contenido() {
        return contenido;
    }

    public SistemaPopular popular() {
        return popular;
    }

    /**
     * Árbol propio de cada perfil. Aprende de sus calificaciones (peso 1),
     * de las de sus vecinos (peso según el parecido) y, si tiene muy pocas,
     * de perfiles que eligieron los mismos gustos.
     */
    public ArbolDecision arbolDe(int perfilId) {
        return arboles.computeIfAbsent(perfilId, id -> {
            List<Ejemplo> ejemplos = new ArrayList<>();
            agregarEjemplos(ejemplos, datos.valoresDe(id), 1.0);
            for (Vecino v : colaborativo.vecinos(id)) agregarEjemplos(ejemplos, datos.valoresDe(v.perfilId()), 0.5 * v.similitud());
            if (datos.valoresDe(id).size() < 6) {
                Set<String> mios = datos.gustosDe(id);
                for (int otro : datos.perfiles()) {
                    if (otro == id) continue;
                    long comunes = datos.gustosDe(otro).stream().filter(mios::contains).count();
                    if (comunes >= 2) agregarEjemplos(ejemplos, datos.valoresDe(otro), 0.15 * comunes);
                }
            }
            return new ArbolDecision(4, 1.5).entrenar(ejemplos);
        });
    }

    private void agregarEjemplos(List<Ejemplo> ejemplos, Map<Integer, Double> estrellas, double peso) {
        for (Map.Entry<Integer, Double> e : estrellas.entrySet()) {
            Pelicula p = datos.peliculas().get(e.getKey());
            if (p != null) ejemplos.add(new Ejemplo(Caracteristicas.de(p), e.getValue() >= 4, peso));
        }
    }

    /** Estrellas estimadas (1 a 5) que el perfil le daría: se usa en la validación cruzada. */
    public double predecirEstrellas(int perfilId, Pelicula p) {
        double[] partes = partes(perfilId, p);
        return 1 + 4 * combinar(perfilId, partes);
    }

    /** Predicción con los pesos fijos de la primera versión (para comparar). */
    public double predecirEstrellasPesosFijos(int perfilId, Pelicula p) {
        return 1 + 4 * combinarFijo(perfilId, partes(perfilId, p));
    }

    /** Lista de recomendaciones ordenada de mayor a menor con el motivo de cada una. */
    public List<Recomendacion> recomendar(int perfilId, Collection<Pelicula> candidatas, Set<Integer> excluir,
                                          boolean infantil, int cuantas, Map<Integer, String> nombres) {
        List<Recomendacion> lista = new ArrayList<>();
        for (Pelicula p : candidatas) {
            if (excluir.contains(p.id())) continue;
            if (infantil && !p.aptaParaNinos()) continue;
            double[] c = partes(perfilId, p);
            lista.add(new Recomendacion(p, combinar(perfilId, c), c[0], c[1], c[2], ""));
        }
        List<Recomendacion> ordenadas = OrdenRecursivo.ordenar(lista, (a, b) -> Double.compare(b.puntaje(), a.puntaje()));
        List<Recomendacion> resultado = new ArrayList<>();
        Map<String, Integer> usados = new HashMap<>();
        for (Recomendacion r : ordenadas.subList(0, Math.min(cuantas, ordenadas.size()))) {
            String motivo = motivo(perfilId, r.pelicula(), r.arbol(), nombres, usados);
            resultado.add(new Recomendacion(r.pelicula(), r.puntaje(), r.colaborativo(), r.contenido(), r.arbol(), motivo));
        }
        return resultado;
    }

    /** [colaborativo, contenido, árbol], cada uno de 0 a 1 (más factorización y por películas si hay pesos). */
    private double[] partes(int perfilId, Pelicula p) {
        return pesos == null ? partesBasicas(perfilId, p) : partesCompletas(perfilId, p);
    }

    private double[] partesBasicas(int perfilId, Pelicula p) {
        double pred = colaborativo.predecir(perfilId, p.id());
        double col = Double.isNaN(pred) ? 0.8 * popular.popularidad(p.id()) : (pred - 1) / 4;
        double con = (contenido.puntaje(perfilId, p) + 1) / 2;
        double arb = arbolDe(perfilId).predecir(Caracteristicas.de(p));
        return new double[]{col, con, arb, 0, 0, (popular.predecir(p.id()) - 1) / 4};
    }

    /** [colaborativo, contenido, árbol, factorización, por películas], cada uno de 0 a 1. */
    private double[] partesCompletas(int perfilId, Pelicula p) {
        double[] c = partesBasicas(perfilId, p);
        c[3] = (factorizacion().predecir(perfilId, p.id()) - 1) / 4;
        double it = porItems.predecir(perfilId, p.id());
        c[4] = Double.isNaN(it) ? c[0] : (it - 1) / 4;
        return c;
    }

    /** Usa los pesos aprendidos; si el perfil es nuevo o aún no hay pesos, los fijos. */
    private double combinar(int perfilId, double[] c) {
        if (pesos != null && datos.valoresDe(perfilId).size() >= 5) {
            return (pesos.predecir(PesosAprendidos.entradas(c, experiencia(perfilId))) - 1) / 4;
        }
        return combinarFijo(perfilId, c);
    }

    /** Si el perfil es nuevo (pocas estrellas) se confía más en sus gustos y en el árbol. */
    private double combinarFijo(int perfilId, double[] c) {
        int calificadas = datos.valoresDe(perfilId).size();
        if (calificadas < 5) return 0.15 * c[0] + 0.55 * c[1] + 0.30 * c[2];
        return PESO_COLABORATIVO * c[0] + PESO_CONTENIDO * c[1] + PESO_ARBOL * c[2];
    }

    /**
     * Explica la recomendación. Hay tres tipos de motivo (perfil parecido,
     * árbol de decisión y géneros) y se reparten para no repetir siempre el mismo.
     */
    private String motivo(int perfilId, Pelicula p, double probArbol, Map<Integer, String> nombres, Map<String, Integer> usados) {
        Map<String, String> opciones = new LinkedHashMap<>();
        Vecino elegido = null;
        Map<Integer, Double> quienes = datos.valoresDePelicula(p.id());
        for (Vecino v : colaborativo.vecinos(perfilId)) {
            Double nota = quienes.get(v.perfilId());
            if (nota == null || nota < 4) continue;
            if (elegido == null || usados.getOrDefault("v" + v.perfilId(), 0) < usados.getOrDefault("v" + elegido.perfilId(), 0)) elegido = v;
        }
        if (elegido != null) {
            opciones.put("vecino", "A " + nombres.getOrDefault(elegido.perfilId(), "un perfil") + ", que se parece a ti en un "
                    + Math.round(elegido.similitud() * 100) + "%, le encantó (" + Math.round(quienes.get(elegido.perfilId())) + " de 5).");
        }
        Map<String, Integer> favoritos = contenido.generosFavoritos(perfilId, 4);
        List<String> coinciden = new ArrayList<>();
        for (String g : p.generos()) if (favoritos.containsKey(g)) coinciden.add(g.toLowerCase());
        if (!coinciden.isEmpty()) opciones.put("contenido", "Te gustan " + String.join(" y ", coinciden) + ", y esta tiene eso.");
        List<String> camino = arbolDe(perfilId).camino(Caracteristicas.de(p));
        if (probArbol >= 0.6 && camino.size() > 1) opciones.put("arbol", "Tu árbol de decisión: " + String.join(" → ", camino) + ".");
        if (opciones.isEmpty()) return "Es de las mejor calificadas de " + p.anio() + " y aún no la ves.";

        String tipo = null;
        for (String t : opciones.keySet()) {
            if (tipo == null || usados.getOrDefault(t, 0) < usados.getOrDefault(tipo, 0)) tipo = t;
        }
        usados.merge(tipo, 1, Integer::sum);
        if (tipo.equals("vecino")) usados.merge("v" + elegido.perfilId(), 1, Integer::sum);
        return opciones.get(tipo);
    }
}
