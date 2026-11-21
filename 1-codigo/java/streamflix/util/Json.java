package streamflix.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conversión mínima entre objetos Java y texto JSON, sin librerías externas.
 * Tanto escribir como leer se resuelven de forma recursiva: un objeto puede
 * contener listas que contienen otros objetos, y así sucesivamente.
 */
public final class Json {

    private Json() {}

    public static Map<String, Object> objeto(Object... claveValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < claveValor.length; i += 2) {
            m.put(String.valueOf(claveValor[i]), claveValor[i + 1]);
        }
        return m;
    }

    public static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(valor, sb);
        return sb.toString();
    }

    private static void escribir(Object v, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            texto(s, sb);
        } else if (v instanceof Number || v instanceof Boolean) {
            if (v instanceof Double d && (d.isNaN() || d.isInfinite())) sb.append("0");
            else sb.append(v);
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!primero) sb.append(',');
                primero = false;
                texto(String.valueOf(e.getKey()), sb);
                sb.append(':');
                escribir(e.getValue(), sb);
            }
            sb.append('}');
        } else if (v instanceof Collection<?> c) {
            sb.append('[');
            boolean primero = true;
            for (Object o : c) {
                if (!primero) sb.append(',');
                primero = false;
                escribir(o, sb);
            }
            sb.append(']');
        } else if (v instanceof ConvertibleAJson cj) {
            escribir(cj.aMapa(), sb);
        } else {
            texto(v.toString(), sb);
        }
    }

    private static void texto(String s, StringBuilder sb) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> sb.append("\\u003c");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> leerObjeto(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        Lector l = new Lector(json);
        Object v = l.valor();
        if (v instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new IllegalArgumentException("Se esperaba un objeto JSON");
    }

    private static final class Lector {
        private final String s;
        private int i;

        Lector(String s) { this.s = s; }

        Object valor() {
            espacios();
            if (i >= s.length()) throw new IllegalArgumentException("JSON incompleto");
            char c = s.charAt(i);
            if (c == '{') return objeto();
            if (c == '[') return lista();
            if (c == '"') return cadena();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            return numero();
        }

        Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            espacios();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                espacios();
                String clave = cadena();
                espacios();
                esperar(':');
                m.put(clave, valor());
                espacios();
                if (s.charAt(i) == ',') { i++; continue; }
                esperar('}');
                return m;
            }
        }

        List<Object> lista() {
            List<Object> l = new ArrayList<>();
            i++;
            espacios();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(valor());
                espacios();
                if (s.charAt(i) == ',') { i++; continue; }
                esperar(']');
                return l;
            }
        }

        String cadena() {
            esperar('"');
            StringBuilder sb = new StringBuilder();
            while (s.charAt(i) != '"') {
                char c = s.charAt(i++);
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            i++;
            return sb.toString();
        }

        Number numero() {
            int ini = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String n = s.substring(ini, i);
            if (n.isEmpty()) throw new IllegalArgumentException("Valor JSON no válido en la posición " + ini);
            if (n.contains(".") || n.contains("e") || n.contains("E")) return Double.parseDouble(n);
            return Long.parseLong(n);
        }

        void espacios() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        void esperar(char c) {
            if (i >= s.length() || s.charAt(i) != c) throw new IllegalArgumentException("Se esperaba '" + c + "' en el JSON");
            i++;
        }
    }
}
