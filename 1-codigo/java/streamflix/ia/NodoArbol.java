package streamflix.ia;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nodo del árbol de decisión. Si es una hoja solo guarda la probabilidad de
 * que la película guste; si no, guarda la pregunta (atributo) y una rama por
 * cada respuesta posible.
 */
public class NodoArbol {

    private final String atributo;
    private final double probabilidad;
    private final double muestras;
    private final double ganancia;
    private final Map<String, NodoArbol> ramas = new LinkedHashMap<>();

    public NodoArbol(String atributo, double probabilidad, double muestras, double ganancia) {
        this.atributo = atributo;
        this.probabilidad = probabilidad;
        this.muestras = muestras;
        this.ganancia = ganancia;
    }

    public static NodoArbol hoja(double probabilidad, double muestras) {
        return new NodoArbol(null, probabilidad, muestras, 0);
    }

    public boolean esHoja() {
        return atributo == null;
    }

    public String getAtributo() {
        return atributo;
    }

    public double getProbabilidad() {
        return probabilidad;
    }

    public double getMuestras() {
        return muestras;
    }

    public double getGanancia() {
        return ganancia;
    }

    public Map<String, NodoArbol> getRamas() {
        return ramas;
    }
}
