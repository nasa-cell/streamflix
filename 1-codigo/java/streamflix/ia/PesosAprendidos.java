package streamflix.ia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Aprende cuánto confiar en cada técnica del modelo híbrido.
 * Es una regresión lineal con regularización (ridge): busca los pesos que mejor
 * convierten las predicciones de cada técnica en las estrellas reales.
 * Los pesos no pueden ser negativos: si una técnica no ayuda, su peso queda en 0.
 * Algunas entradas se multiplican por la "experiencia" del perfil (cuántas
 * calificaciones tiene), así el peso de cada técnica cambia según el perfil.
 */
public class PesosAprendidos {

    public static final List<String> NOMBRES = List.of(
            "Colaborativo (usuarios)", "Contenido", "Árbol de decisión", "Factorización", "Colaborativo (películas)",
            "Popularidad", "Colaborativo × experiencia", "Factorización × experiencia", "Por películas × experiencia");

    private static final double LAMBDA = 0.5;

    private double intercepto;
    private double[] pesos;

    /** Entradas de 0 a 1: colaborativo, contenido, árbol, factorización, por películas, popularidad; experiencia de 0 a 1. */
    public static double[] entradas(double[] partes, double experiencia) {
        return new double[]{partes[0], partes[1], partes[2], partes[3], partes[4], partes[5],
                partes[0] * experiencia, partes[3] * experiencia, partes[4] * experiencia};
    }

    public static PesosAprendidos entrenar(List<double[]> x, List<Double> y) {
        PesosAprendidos p = new PesosAprendidos();
        int d = x.get(0).length;
        boolean[] activo = new boolean[d];
        Arrays.fill(activo, true);
        for (int vuelta = 0; vuelta < d; vuelta++) {
            double[] w = resolver(x, y, activo);
            boolean cambio = false;
            for (int j = 0; j < d; j++) {
                if (activo[j] && w[j + 1] < 0) {
                    activo[j] = false;
                    cambio = true;
                }
            }
            if (!cambio) {
                p.intercepto = w[0];
                p.pesos = Arrays.copyOfRange(w, 1, w.length);
                return p;
            }
        }
        double[] w = resolver(x, y, activo);
        p.intercepto = w[0];
        p.pesos = Arrays.copyOfRange(w, 1, w.length);
        return p;
    }

    public double predecir(double[] entrada) {
        double s = intercepto;
        for (int j = 0; j < pesos.length; j++) s += pesos[j] * entrada[j];
        return Math.max(1, Math.min(5, s));
    }

    public double[] pesos() {
        return pesos.clone();
    }

    public double intercepto() {
        return intercepto;
    }

    /** Ecuaciones normales (XᵀX + λI) w = Xᵀy resueltas con eliminación de Gauss. */
    private static double[] resolver(List<double[]> x, List<Double> y, boolean[] activo) {
        List<Integer> columnas = new ArrayList<>();
        for (int j = 0; j < activo.length; j++) if (activo[j]) columnas.add(j);
        int n = columnas.size() + 1;
        double[][] a = new double[n][n + 1];
        for (int fila = 0; fila < x.size(); fila++) {
            double[] v = new double[n];
            v[0] = 1;
            for (int c = 0; c < columnas.size(); c++) v[c + 1] = x.get(fila)[columnas.get(c)];
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) a[r][c] += v[r] * v[c];
                a[r][n] += v[r] * y.get(fila);
            }
        }
        for (int r = 1; r < n; r++) a[r][r] += LAMBDA;
        for (int col = 0; col < n; col++) {
            int piv = col;
            for (int r = col + 1; r < n; r++) if (Math.abs(a[r][col]) > Math.abs(a[piv][col])) piv = r;
            double[] t = a[col]; a[col] = a[piv]; a[piv] = t;
            if (Math.abs(a[col][col]) < 1e-12) continue;
            for (int r = 0; r < n; r++) {
                if (r == col) continue;
                double f = a[r][col] / a[col][col];
                for (int c = col; c <= n; c++) a[r][c] -= f * a[col][c];
            }
        }
        double[] w = new double[activo.length + 1];
        w[0] = Math.abs(a[0][0]) < 1e-12 ? 0 : a[0][n] / a[0][0];
        for (int c = 0; c < columnas.size(); c++) {
            double piv = a[c + 1][c + 1];
            w[columnas.get(c) + 1] = Math.abs(piv) < 1e-12 ? 0 : a[c + 1][n] / piv;
        }
        return w;
    }
}
