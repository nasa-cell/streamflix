package streamflix.ia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import streamflix.util.Json;

/**
 * Árbol de decisión binario entrenado con ganancia de información (entropía).
 * En cada nodo elige la pregunta "¿atributo = valor?" que mejor separa las
 * películas que le gustaron al perfil de las que no. Construir, predecir,
 * dibujar y medir el árbol se hace con funciones recursivas.
 */
public class ArbolDecision {

    public static final String SI = "Sí";
    public static final String NO = "No";

    private final int profundidadMaxima;
    private final double muestrasMinimas;
    private NodoArbol raiz;
    private double probabilidadBase = 0.5;

    public ArbolDecision(int profundidadMaxima, double muestrasMinimas) {
        this.profundidadMaxima = profundidadMaxima;
        this.muestrasMinimas = muestrasMinimas;
    }

    public ArbolDecision entrenar(List<Ejemplo> ejemplos) {
        probabilidadBase = probabilidad(ejemplos, 0.5);
        raiz = construir(ejemplos, 0);
        return this;
    }

    /** Paso recursivo: crea un nodo y, si conviene, sus dos ramas. */
    private NodoArbol construir(List<Ejemplo> ejemplos, int profundidad) {
        double total = pesoTotal(ejemplos);
        double prob = probabilidad(ejemplos, probabilidadBase);
        if (profundidad >= profundidadMaxima || total < muestrasMinimas * 2 || prob <= 0.02 || prob >= 0.98) {
            return NodoArbol.hoja(prob, total);
        }
        String mejorAtributo = null, mejorValor = null;
        double mejorGanancia = 0.01;
        double entropiaActual = entropia(ejemplos);
        for (String atributo : Caracteristicas.NOMBRES) {
            for (String valor : valores(ejemplos, atributo)) {
                List<Ejemplo> si = new ArrayList<>(), no = new ArrayList<>();
                dividir(ejemplos, atributo, valor, si, no);
                double ps = pesoTotal(si), pn = pesoTotal(no);
                if (ps < muestrasMinimas || pn < muestrasMinimas) continue;
                double ganancia = entropiaActual - (ps / total) * entropia(si) - (pn / total) * entropia(no);
                if (ganancia > mejorGanancia) {
                    mejorGanancia = ganancia;
                    mejorAtributo = atributo;
                    mejorValor = valor;
                }
            }
        }
        if (mejorAtributo == null) return NodoArbol.hoja(prob, total);
        List<Ejemplo> si = new ArrayList<>(), no = new ArrayList<>();
        dividir(ejemplos, mejorAtributo, mejorValor, si, no);
        NodoArbol nodo = new NodoArbol(mejorAtributo + "=" + mejorValor, prob, total, mejorGanancia);
        nodo.getRamas().put(SI, construir(si, profundidad + 1));
        nodo.getRamas().put(NO, construir(no, profundidad + 1));
        return nodo;
    }

    /** Probabilidad de que la película guste, bajando por el árbol de forma recursiva. */
    public double predecir(Map<String, String> atributos) {
        return raiz == null ? probabilidadBase : predecir(raiz, atributos);
    }

    private double predecir(NodoArbol nodo, Map<String, String> atributos) {
        if (nodo.esHoja()) return nodo.getProbabilidad();
        return predecir(nodo.getRamas().get(cumple(nodo, atributos) ? SI : NO), atributos);
    }

    /** Las preguntas que se respondieron para llegar a la decisión (recursivo). */
    public List<String> camino(Map<String, String> atributos) {
        List<String> pasos = new ArrayList<>();
        if (raiz != null) camino(raiz, atributos, pasos);
        return pasos;
    }

    private void camino(NodoArbol nodo, Map<String, String> atributos, List<String> pasos) {
        if (nodo.esHoja()) {
            pasos.add("Probabilidad de que te guste: " + Math.round(nodo.getProbabilidad() * 100) + "%");
            return;
        }
        boolean si = cumple(nodo, atributos);
        String[] partes = nodo.getAtributo().split("=", 2);
        pasos.add(partes[0] + (si ? " es " : " no es ") + partes[1]);
        camino(nodo.getRamas().get(si ? SI : NO), atributos, pasos);
    }

    /** Cuánto aportó cada característica a las decisiones, en porcentaje. */
    public Map<String, Integer> importancia() {
        Map<String, Double> acumulado = new LinkedHashMap<>();
        for (String n : Caracteristicas.NOMBRES) acumulado.put(n, 0.0);
        if (raiz != null) acumular(raiz, acumulado);
        double total = 0;
        for (double v : acumulado.values()) total += v;
        List<Map.Entry<String, Double>> orden = OrdenRecursivo.ordenar(
                List.copyOf(acumulado.entrySet()), (a, b) -> Double.compare(b.getValue(), a.getValue()));
        Map<String, Integer> r = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : orden) {
            r.put(e.getKey(), total == 0 ? 0 : (int) Math.round(100 * e.getValue() / total));
        }
        return r;
    }

    private void acumular(NodoArbol nodo, Map<String, Double> acumulado) {
        if (nodo.esHoja()) return;
        String atributo = nodo.getAtributo().split("=", 2)[0];
        acumulado.merge(atributo, nodo.getGanancia() * nodo.getMuestras(), Double::sum);
        for (NodoArbol hijo : nodo.getRamas().values()) acumular(hijo, acumulado);
    }

    /** Dibujo del árbol en texto, con líneas, hecho de forma recursiva. */
    public String dibujar() {
        StringBuilder sb = new StringBuilder();
        if (raiz == null) return "Aún no hay datos para armar el árbol.";
        dibujar(raiz, "", "", "", sb);
        return sb.toString();
    }

    private void dibujar(NodoArbol nodo, String prefijo, String etiqueta, String prefijoHijos, StringBuilder sb) {
        sb.append(prefijo).append(etiqueta);
        if (nodo.esHoja()) {
            int p = (int) Math.round(nodo.getProbabilidad() * 100);
            sb.append(p >= 50 ? "RECOMENDAR (" : "NO RECOMENDAR (").append(p).append("% le gusta)\n");
            return;
        }
        String[] partes = nodo.getAtributo().split("=", 2);
        sb.append("¿").append(partes[0]).append(" es ").append(partes[1]).append("?\n");
        dibujar(nodo.getRamas().get(SI), prefijoHijos, "├─ Sí → ", prefijoHijos + "│   ", sb);
        dibujar(nodo.getRamas().get(NO), prefijoHijos, "└─ No → ", prefijoHijos + "    ", sb);
    }

    /** El árbol como objeto JSON para dibujarlo en la página. */
    public Map<String, Object> aMapa() {
        return raiz == null ? null : aMapa(raiz);
    }

    private Map<String, Object> aMapa(NodoArbol nodo) {
        if (nodo.esHoja()) {
            return Json.objeto("hoja", true, "probabilidad", Math.round(nodo.getProbabilidad() * 100),
                    "muestras", Math.round(nodo.getMuestras()));
        }
        String[] partes = nodo.getAtributo().split("=", 2);
        return Json.objeto("hoja", false, "atributo", partes[0], "valor", partes[1],
                "muestras", Math.round(nodo.getMuestras()),
                "si", aMapa(nodo.getRamas().get(SI)), "no", aMapa(nodo.getRamas().get(NO)));
    }

    public int contarNodos() {
        return raiz == null ? 0 : contarNodos(raiz);
    }

    private int contarNodos(NodoArbol nodo) {
        int n = 1;
        for (NodoArbol hijo : nodo.getRamas().values()) n += contarNodos(hijo);
        return n;
    }

    public int profundidad() {
        return raiz == null ? 0 : profundidad(raiz);
    }

    private int profundidad(NodoArbol nodo) {
        int max = 0;
        for (NodoArbol hijo : nodo.getRamas().values()) max = Math.max(max, profundidad(hijo));
        return nodo.esHoja() ? 0 : 1 + max;
    }

    private static boolean cumple(NodoArbol nodo, Map<String, String> atributos) {
        String[] partes = nodo.getAtributo().split("=", 2);
        return partes[1].equals(atributos.get(partes[0]));
    }

    private static void dividir(List<Ejemplo> ejemplos, String atributo, String valor, List<Ejemplo> si, List<Ejemplo> no) {
        for (Ejemplo e : ejemplos) (valor.equals(e.atributos().get(atributo)) ? si : no).add(e);
    }

    private static Set<String> valores(List<Ejemplo> ejemplos, String atributo) {
        Set<String> s = new LinkedHashSet<>();
        for (Ejemplo e : ejemplos) s.add(e.atributos().get(atributo));
        return s;
    }

    private static double pesoTotal(List<Ejemplo> ejemplos) {
        double t = 0;
        for (Ejemplo e : ejemplos) t += e.peso();
        return t;
    }

    /** Probabilidad suavizada (estimador m con m = 2) para no dar 0% o 100% con pocos datos. */
    private static double probabilidad(List<Ejemplo> ejemplos, double previa) {
        double positivos = 0, total = 0;
        for (Ejemplo e : ejemplos) {
            total += e.peso();
            if (e.gusta()) positivos += e.peso();
        }
        return (positivos + 2 * previa) / (total + 2);
    }

    private static double entropia(List<Ejemplo> ejemplos) {
        double total = pesoTotal(ejemplos);
        if (total == 0) return 0;
        double p = 0;
        for (Ejemplo e : ejemplos) if (e.gusta()) p += e.peso();
        p /= total;
        if (p == 0 || p == 1) return 0;
        return -p * log2(p) - (1 - p) * log2(1 - p);
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
