package streamflix.util;

import java.nio.file.Path;

/**
 * Carpeta principal de StreamFlix (la que tiene 1-codigo, 2-base-de-datos...).
 * Con iniciar.bat es la carpeta actual; la versión instalada la indica con
 * -Dstreamflix.carpeta para que funcione aunque se abra desde otro lugar.
 */
public final class Carpeta {

    private static final Path BASE = Path.of(System.getProperty("streamflix.carpeta", ".")).toAbsolutePath().normalize();

    private Carpeta() {}

    public static Path ruta(String... partes) {
        Path p = BASE;
        for (String parte : partes) p = p.resolve(parte);
        return p;
    }
}
