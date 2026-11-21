package streamflix.ia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import streamflix.modelo.Calificacion;
import streamflix.modelo.Pelicula;

/**
 * Junta todo lo que dice cuánto le gustó algo a un perfil:
 *  1. Estrellas (lo más claro), ajustadas un poco por su comentario.
 *  2. Vistas sin estrellas: si la vio casi completa, le gustó; si la dejó al inicio, no.
 *  3. Comentarios sin estrellas ni vista: el sentimiento del texto.
 */
public final class CombinadorSenales {

    private static final double AJUSTE_COMENTARIO = 0.35;

    private CombinadorSenales() {}

    public static List<Senal> combinar(Map<Integer, Pelicula> peliculas, List<Calificacion> calificaciones,
                                       Map<Integer, Map<Integer, Integer>> minutos,
                                       Map<Long, String> comentarios, Set<Long> excluir) {
        List<Senal> senales = new ArrayList<>();
        Set<Long> usados = new HashSet<>();
        for (Calificacion c : calificaciones) {
            long par = Senal.par(c.perfilId(), c.peliculaId());
            double valor = c.estrellas();
            String texto = comentarios.get(par);
            if (texto != null) valor += AJUSTE_COMENTARIO * AnalizadorSentimiento.analizar(texto);
            senales.add(new Senal(c.perfilId(), c.peliculaId(), limitar(valor), Senal.ESTRELLAS));
            usados.add(par);
        }
        for (Map.Entry<Integer, Map<Integer, Integer>> e : minutos.entrySet()) {
            for (Map.Entry<Integer, Integer> v : e.getValue().entrySet()) {
                long par = Senal.par(e.getKey(), v.getKey());
                Pelicula p = peliculas.get(v.getKey());
                if (p == null || usados.contains(par) || excluir.contains(par)) continue;
                double total = "Serie".equals(p.tipo()) ? p.duracionMin() * 8.0 : p.duracionMin();
                double avance = Math.min(1, v.getValue() / total);
                double valor = 1.2 + 3.6 * avance;
                String texto = comentarios.get(par);
                if (texto != null) valor += 0.8 * AnalizadorSentimiento.analizar(texto);
                senales.add(new Senal(e.getKey(), v.getKey(), limitar(valor), Senal.VISTA));
                usados.add(par);
            }
        }
        for (Map.Entry<Long, String> c : comentarios.entrySet()) {
            long par = c.getKey();
            if (usados.contains(par) || excluir.contains(par)) continue;
            int perfil = (int) (par / 1_000_000L), pelicula = (int) (par % 1_000_000L);
            if (!peliculas.containsKey(pelicula)) continue;
            senales.add(new Senal(perfil, pelicula, limitar(3 + 1.6 * AnalizadorSentimiento.analizar(c.getValue())), Senal.COMENTARIO));
        }
        return senales;
    }

    /** Solo las estrellas, sin vistas ni comentarios (para comparar). */
    public static List<Senal> soloEstrellas(List<Calificacion> calificaciones) {
        List<Senal> s = new ArrayList<>();
        for (Calificacion c : calificaciones) s.add(new Senal(c.perfilId(), c.peliculaId(), c.estrellas(), Senal.ESTRELLAS));
        return s;
    }

    private static double limitar(double v) {
        return Math.max(1, Math.min(5, v));
    }
}
