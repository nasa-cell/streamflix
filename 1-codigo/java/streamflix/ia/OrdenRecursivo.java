package streamflix.ia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ordenamiento por mezcla (merge sort) escrito de forma recursiva:
 * divide la lista en dos mitades, ordena cada mitad llamándose a sí mismo
 * y luego une las dos mitades ya ordenadas. Tiempo O(n log n).
 * Se usa para ordenar las recomendaciones de mayor a menor puntaje.
 */
public final class OrdenRecursivo {

    private OrdenRecursivo() {}

    public static <T> List<T> ordenar(List<T> lista, Comparator<? super T> criterio) {
        if (lista.size() <= 1) return new ArrayList<>(lista);
        int mitad = lista.size() / 2;
        List<T> izquierda = ordenar(lista.subList(0, mitad), criterio);
        List<T> derecha = ordenar(lista.subList(mitad, lista.size()), criterio);
        return mezclar(izquierda, derecha, criterio);
    }

    private static <T> List<T> mezclar(List<T> a, List<T> b, Comparator<? super T> criterio) {
        List<T> resultado = new ArrayList<>(a.size() + b.size());
        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) {
            if (criterio.compare(a.get(i), b.get(j)) <= 0) resultado.add(a.get(i++));
            else resultado.add(b.get(j++));
        }
        while (i < a.size()) resultado.add(a.get(i++));
        while (j < b.size()) resultado.add(b.get(j++));
        return resultado;
    }

    /** Suma recursiva de una lista de números (usada para promedios de las métricas). */
    public static double sumar(List<Double> valores, int desde) {
        if (desde >= valores.size()) return 0;
        return valores.get(desde) + sumar(valores, desde + 1);
    }
}
