package streamflix.servicio;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import streamflix.util.Carpeta;
import streamflix.util.Json;

/**
 * Envía correos con un servicio web de Google Apps Script (gratis, hasta 100 al día).
 * La dirección y la clave del servicio están en 1-codigo/configuracion/correo.properties.
 * En la nube se usan las variables CORREO_URL y CORREO_CLAVE en lugar del archivo.
 * Si no hay datos, el código se muestra en la ventana del servidor.
 */
public final class Correo {

    private static final Path ARCHIVO = Carpeta.ruta("1-codigo", "configuracion", "correo.properties");
    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private Correo() {}

    /** true si hay un servicio de correo configurado. */
    public static boolean configurado() {
        Properties p = leer();
        return !p.getProperty("url", "").isBlank() && !p.getProperty("clave", "").isBlank();
    }

    /** Envía el correo. Devuelve true si el servicio confirmó el envío. */
    public static boolean enviar(String para, String asunto, String texto, String html) {
        Properties p = leer();
        String url = p.getProperty("url", "").trim();
        String clave = p.getProperty("clave", "").trim();
        if (url.isEmpty() || clave.isEmpty()) return false;
        String cuerpo = Json.escribir(Json.objeto("clave", clave, "para", para, "asunto", asunto,
                "texto", texto, "cuerpo", texto, "html", html, "htmlBody", html));
        try {
            HttpResponse<String> r = CLIENTE.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                    .build(), HttpResponse.BodyHandlers.ofString());
            // Apps Script responde con una redirección a la página que trae el resultado.
            for (int saltos = 0; r.statusCode() / 100 == 3 && saltos < 3; saltos++) {
                String destino = r.headers().firstValue("Location").orElse("");
                if (destino.isEmpty()) break;
                r = CLIENTE.send(HttpRequest.newBuilder(URI.create(destino)).timeout(Duration.ofSeconds(25)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
            }
            Map<String, Object> respuesta = Json.leerObjeto(r.body());
            if (Boolean.TRUE.equals(respuesta.get("ok"))) return true;
            System.out.println("El servicio de correo no envió el mensaje: " + respuesta.get("error"));
        } catch (IOException | RuntimeException e) {
            System.out.println("No se pudo conectar con el servicio de correo: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private static Properties leer() {
        Properties p = new Properties();
        String url = System.getenv("CORREO_URL"), clave = System.getenv("CORREO_CLAVE");
        if (url != null && !url.isBlank() && clave != null && !clave.isBlank()) {
            p.setProperty("url", url.trim());
            p.setProperty("clave", clave.trim());
            return p;
        }
        if (!Files.exists(ARCHIVO)) return p;
        try (Reader in = new InputStreamReader(Files.newInputStream(ARCHIVO), StandardCharsets.UTF_8)) {
            p.load(in);
        } catch (IOException e) {
            System.out.println("No se pudo leer " + ARCHIVO + ": " + e.getMessage());
        }
        return p;
    }
}
