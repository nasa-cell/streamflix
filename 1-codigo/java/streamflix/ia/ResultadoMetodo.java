package streamflix.ia;

import java.util.List;
import java.util.Map;
import streamflix.util.ConvertibleAJson;
import streamflix.util.Json;

/**
 * Resultado de un método en la validación cruzada.
 * mae / rmse: error al adivinar las estrellas (menos es mejor).
 * precision: de las 3 primeras que recomienda, cuántas le gustaron de verdad.
 * satisfaccion: estrellas reales promedio de lo recomendado.
 * minutos: minutos que de verdad vio de lo recomendado (tiempo de visualización).
 */
public record ResultadoMetodo(String nombre, double mae, double rmse, double precision,
                              double satisfaccion, double minutos, double milisegundos,
                              List<Double> precisionPorParte) implements ConvertibleAJson {
    @Override
    public Map<String, Object> aMapa() {
        return Json.objeto("nombre", nombre, "mae", r(mae, 3), "rmse", r(rmse, 3),
                "precision", r(precision * 100, 1), "satisfaccion", r(satisfaccion, 2),
                "minutos", r(minutos, 1), "milisegundos", r(milisegundos, 1),
                "precisionPorParte", precisionPorParte.stream().map(p -> r(p * 100, 1)).toList());
    }

    private static double r(double v, int decimales) {
        double f = Math.pow(10, decimales);
        return Math.round(v * f) / f;
    }
}
