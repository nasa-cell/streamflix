package streamflix;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import streamflix.controlador.ArchivoControlador;
import streamflix.controlador.CuentaControlador;
import streamflix.controlador.InteraccionControlador;
import streamflix.controlador.PeliculaControlador;
import streamflix.controlador.PerfilControlador;
import streamflix.controlador.RecomendacionControlador;
import streamflix.dao.Conexion;
import streamflix.dao.PrepararBase;

/**
 * Arranca el servidor web de StreamFlix y conecta cada ruta con su controlador.
 * Modos:
 *   (sin nada)   como siempre, desde iniciar.bat.
 *   --preparar   lo usa el instalador: crea la base y los datos si faltan y termina.
 *   --app        versión instalada: abre el navegador y se apaga sola tras 1 hora sin uso.
 */
public class Principal {

    private static final long MINUTOS_SIN_USO = 60;
    private static volatile long ultimoUso = System.currentTimeMillis();

    public static void main(String[] args) throws Exception {
        String modo = args.length > 0 ? args[0] : "";
        if (modo.equals("--preparar")) {
            PrepararBase.Resultado r = PrepararBase.preparar();
            System.out.println("Resultado: " + r);
            System.exit(switch (r) {
                case CREADA, YA_EXISTIA -> 0;
                case CLAVE_INCORRECTA -> 2;
                case SIN_CONEXION -> 3;
            });
        }
        boolean app = modo.equals("--app");
        boolean nube = System.getenv("PORT") != null;
        int puerto = Integer.parseInt(Conexion.propiedad("puerto", "8095"));
        String direccion = "http://localhost:" + puerto;

        if (nube) PrepararBase.preparar();
        if (app) encenderMysqlSiHaceFalta();

        HttpServer servidor;
        try {
            servidor = HttpServer.create(new InetSocketAddress(nube ? "0.0.0.0" : "127.0.0.1", puerto), 200);
        } catch (BindException e) {
            // Ya hay un StreamFlix abierto: la versión instalada solo abre la página.
            if (app) {
                abrirNavegador(direccion);
                return;
            }
            throw e;
        }
        Filter uso = new Filter() {
            @Override
            public void doFilter(HttpExchange ex, Chain cadena) throws IOException {
                ultimoUso = System.currentTimeMillis();
                cadena.doFilter(ex);
            }

            @Override
            public String description() {
                return "Anota la hora del último pedido";
            }
        };
        ruta(servidor, "/api/cuenta", new CuentaControlador(), uso);
        ruta(servidor, "/api/perfiles", new PerfilControlador(), uso);
        ruta(servidor, "/api/peliculas", new PeliculaControlador(), uso);
        ruta(servidor, "/api/interaccion", new InteraccionControlador(), uso);
        ruta(servidor, "/api/recomendaciones", new RecomendacionControlador(), uso);
        ruta(servidor, "/", new ArchivoControlador(), uso);
        servidor.setExecutor(Executors.newFixedThreadPool(16));
        servidor.start();

        String base = Conexion.POSTGRES ? "PostgreSQL" : "MySQL";
        try (var c = Conexion.abrir()) {
            System.out.println("Conectado a " + base + " correctamente.");
        } catch (Exception e) {
            System.out.println("AVISO: no se pudo conectar a " + base + " (" + e.getMessage() + ").");
            if (!Conexion.POSTGRES)
                System.out.println("Enciende el servicio MySQL84 y revisa 1-codigo/configuracion/conexion.properties.");
        }
        System.out.println("StreamFlix listo en " + direccion);

        if (nube) {
            // En el plan gratis el procesador es lento: las métricas de la IA se calculan apenas
            // arranca, para que quien abra «Cómo funciona la IA» no espere más de un minuto.
            Thread previo = new Thread(() -> {
                try {
                    new streamflix.servicio.RecomendacionServicio().metricas(false);
                    System.out.println("Métricas de la IA listas.");
                } catch (Exception e) {
                    System.out.println("No se pudieron calcular las métricas al arrancar: " + e.getMessage());
                }
            });
            previo.setDaemon(true);
            previo.start();
        }

        if (app) {
            abrirNavegador(direccion);
            var reloj = Executors.newSingleThreadScheduledExecutor();
            reloj.scheduleAtFixedRate(() -> {
                if (System.currentTimeMillis() - ultimoUso > TimeUnit.MINUTES.toMillis(MINUTOS_SIN_USO)) {
                    servidor.stop(0);
                    System.exit(0);
                }
            }, 1, 1, TimeUnit.MINUTES);
        } else if (!nube) {
            System.out.println("Para cerrar, cierra esta ventana o presiona Ctrl + C.");
        }
    }

    private static void ruta(HttpServer servidor, String camino, HttpHandler controlador, Filter uso) {
        servidor.createContext(camino, controlador).getFilters().add(uso);
    }

    /** La versión instalada enciende el servicio MySQL84 si está apagado (Windows pide permiso). */
    private static void encenderMysqlSiHaceFalta() {
        try (var c = Conexion.abrir()) {
            return;
        } catch (Exception apagado) {
            try {
                new ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command",
                        "Start-Process net -ArgumentList 'start','MySQL84' -Verb RunAs -WindowStyle Hidden -Wait")
                        .start().waitFor(60, TimeUnit.SECONDS);
            } catch (IOException | InterruptedException e) {
                System.out.println("No se pudo encender MySQL84: " + e.getMessage());
            }
        }
    }

    private static void abrirNavegador(String direccion) {
        try {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", direccion).start();
        } catch (IOException e) {
            System.out.println("Abre tú el navegador en " + direccion);
        }
    }
}
