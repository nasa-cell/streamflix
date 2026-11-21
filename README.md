**⬇️ Descargar el instalador para Windows: [StreamFlix-instalador.exe](https://github.com/nasa-cell/streamflix/releases/latest/download/StreamFlix-instalador.exe)** · **🌐 Usar en internet: [streamflix-6q1n.onrender.com](https://streamflix-6q1n.onrender.com)**

# StreamFlix

Recomendador de películas y series con inteligencia artificial. Tiene 146 títulos de 2025 y 2026, con tráiler y portada, y aprende de las estrellas, los comentarios y lo que ve cada perfil.

## Cómo usarlo

El instalador pregunta primero dónde guardar los datos:

| | ☁️ Nube | 💻 Esta PC |
|---|---|---|
| Qué instala | Nada, solo un ícono que abre la página | StreamFlix con su propio Java (unos 90 MB) |
| Internet | Sí | No hace falta |
| Necesita | — | MySQL 8.4 (gratis) |
| Datos | En internet, se entra desde cualquier equipo | Solo en este equipo |

La versión en internet está en un plan gratis: la primera vez del día puede tardar unos 50 segundos en abrir.

Cuenta de ejemplo: **demo2025** / **Demo2025x**

## Qué hace la IA

- **Filtrado colaborativo:** busca perfiles con gustos parecidos.
- **Filtrado por contenido:** compara géneros, tipo y clasificación.
- **Factorización de matrices y árbol de decisión:** predicen cuántas estrellas le darías a cada título.
- **Análisis de comentarios:** lee si un comentario es positivo o negativo.
- **Aprendizaje continuo:** con cada calificación nueva se vuelve a medir y mejora.

## Carpetas

- `1-codigo/java`: servidor en Java 17, sin frameworks.
- `1-codigo/web`: la página (HTML, CSS, JS, portadas y video de entrada).
- `2-base-de-datos`: tablas y datos de ejemplo para MySQL; `nube-postgres` tiene lo mismo para PostgreSQL.
- `3-pruebas/resultados`: resultados de las pruebas de la IA.

## Abrir desde el código

1. Instala Java 17 y MySQL 8.4.
2. Copia `1-codigo/configuracion/conexion.ejemplo.properties` como `conexion.properties` y pon tu contraseña de MySQL.
3. Doble clic en `iniciar.bat`. La base se crea con `2-base-de-datos/1-crear-tablas.sql` y `2-datos-ejemplo.sql` en MySQL Workbench.

## Créditos

Animación de la entrada: pixiv VRoid Project. Música: CHILL FRAME TOKYO. Portadas y tráileres pertenecen a sus estudios; se usan solo como muestra en un trabajo educativo.
