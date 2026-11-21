package streamflix.controlador;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Entrega las páginas HTML, CSS, JS, sonidos y portadas de la carpeta web.
 * En la nube, si existe la variable IMAGENES_CLOUDINARY (solo en Render), las portadas y el video
 * se piden a Cloudinary (carpeta streamflix) para no cargar el servidor.
 */
public class ArchivoControlador implements HttpHandler {

    private static final Path RAIZ = streamflix.util.Carpeta.ruta("1-codigo", "web");
    private static final String NUBE = System.getenv("IMAGENES_CLOUDINARY");
    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8", "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8", "jpg", "image/jpeg", "png", "image/png",
            "svg", "image/svg+xml", "ico", "image/x-icon", "mp3", "audio/mpeg", "json", "application/json");
    private static final Map<String, String> VIDEOS = Map.of("mp4", "video/mp4", "webm", "video/webm");

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String ruta = ex.getRequestURI().getPath();
            if (ruta.equals("/")) ruta = "/entrada.html";
            String enNube = enCloudinary(ruta);
            if (enNube != null) {
                ex.getResponseHeaders().set("Location", enNube);
                ex.getResponseHeaders().set("Cache-Control", "max-age=86400");
                ex.sendResponseHeaders(302, -1);
                return;
            }
            Path archivo = RAIZ.resolve(ruta.substring(1)).normalize();
            if (!archivo.startsWith(RAIZ) || !Files.isRegularFile(archivo)) {
                byte[] b = paginaNoEncontrada(ruta).getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                ex.sendResponseHeaders(404, b.length);
                try (OutputStream o = ex.getResponseBody()) { o.write(b); }
                return;
            }
            String nombre = archivo.getFileName().toString();
            String ext = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase();
            byte[] b = Files.readAllBytes(archivo);
            ex.getResponseHeaders().set("Content-Type", TIPOS.getOrDefault(ext, VIDEOS.getOrDefault(ext, "application/octet-stream")));
            ex.getResponseHeaders().set("Cache-Control", ext.equals("jpg") || ext.equals("png") ? "max-age=86400" : "no-cache");
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream o = ex.getResponseBody()) { o.write(b); }
        } finally {
            ex.close();
        }
    }

    /** Dirección en Cloudinary de una portada o del video; null si se entrega desde el servidor. */
    private String enCloudinary(String ruta) {
        if (NUBE == null || NUBE.isBlank() || !ruta.matches("/(portadas|video)/[A-Za-z0-9/_.-]+\\.(jpg|png|mp4|webm)")) return null;
        if (ruta.startsWith("/video/")) {
            // entrada-h.mp4 y entrada-h.webm se guardan como entrada-h-mp4 y entrada-h-webm para no pisarse
            String ext = ruta.substring(ruta.lastIndexOf('.') + 1);
            String sinExt = ruta.substring(0, ruta.lastIndexOf('.'));
            return "https://res.cloudinary.com/" + NUBE.trim() + "/video/upload/streamflix" + sinExt + "-" + ext + "." + ext;
        }
        return "https://res.cloudinary.com/" + NUBE.trim() + "/image/upload/streamflix" + ruta;
    }

    private String paginaNoEncontrada(String ruta) {
        return "<!doctype html><meta charset=utf-8><title>No encontrada</title>"
                + "<body style='font-family:sans-serif;background:#141433;color:#fff;text-align:center;padding:60px'>"
                + "<h1>Esta página no existe</h1><p>Se pidió: " + ruta.replace("<", "&lt;") + "</p>"
                + "<p><a style='color:#ffe066' href='/inicio.html'>Volver al inicio</a></p></body>";
    }
}
