# StreamFlix en Render: compila el servidor Java y lo arranca con PostgreSQL (Neon).
FROM eclipse-temurin:17-jdk AS compilar
WORKDIR /app
COPY 1-codigo/java 1-codigo/java
COPY 1-codigo/lib 1-codigo/lib
RUN javac -encoding UTF-8 -d clases -cp "1-codigo/lib/*" -sourcepath 1-codigo/java 1-codigo/java/streamflix/Principal.java

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=compilar /app/clases clases
COPY 1-codigo/lib 1-codigo/lib
COPY 1-codigo/web 1-codigo/web
COPY 2-base-de-datos/nube-postgres 2-base-de-datos/nube-postgres
COPY 3-pruebas/resultados 3-pruebas/resultados
ENV JAVA_TOOL_OPTIONS="-Xmx300m -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"
CMD ["java", "-cp", "clases:1-codigo/lib/*", "streamflix.Principal"]
