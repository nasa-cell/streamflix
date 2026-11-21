-- StreamFlix: sistema de recomendación de películas
-- Ejecutar en MySQL Workbench (MySQL 8.4)

DROP DATABASE IF EXISTS streamflix;
CREATE DATABASE streamflix CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci;
USE streamflix;

CREATE TABLE usuarios (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  nombre_completo VARCHAR(40)  NOT NULL,
  usuario         VARCHAR(20)  NOT NULL,
  correo          VARCHAR(40)  NOT NULL,
  dni             CHAR(8)      NOT NULL,
  celular         CHAR(9)      NOT NULL,
  edad            TINYINT UNSIGNED NOT NULL,
  clave_hash      VARCHAR(200) NOT NULL,
  intentos_fallidos TINYINT UNSIGNED NOT NULL DEFAULT 0,
  bloqueos        TINYINT UNSIGNED NOT NULL DEFAULT 0,
  bloqueado_hasta DATETIME     NULL,
  codigo_hash     VARCHAR(200) NULL,
  codigo_vence    DATETIME     NULL,
  codigo_intentos TINYINT UNSIGNED NOT NULL DEFAULT 0,
  codigo_pedido   DATETIME     NULL,
  creado_en       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_usuario UNIQUE (usuario),
  CONSTRAINT uq_correo  UNIQUE (correo),
  CONSTRAINT uq_dni     UNIQUE (dni),
  CONSTRAINT uq_celular UNIQUE (celular),
  CONSTRAINT ck_usuario CHECK (REGEXP_LIKE(usuario, '^[A-Za-z0-9]{4,20}$', 'c')),
  CONSTRAINT ck_correo  CHECK (REGEXP_LIKE(correo, '^[A-Za-z0-9.]{6,30}@gmail\\.com$')),
  CONSTRAINT ck_dni     CHECK (REGEXP_LIKE(dni, '^[0-9]{8}$')),
  CONSTRAINT ck_celular CHECK (REGEXP_LIKE(celular, '^9[0-9]{8}$')),
  CONSTRAINT ck_edad    CHECK (edad BETWEEN 13 AND 99)
);

CREATE TABLE perfiles (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  usuario_id INT         NOT NULL,
  nombre     VARCHAR(15) NOT NULL,
  avatar     VARCHAR(10) NOT NULL,
  color      VARCHAR(10) NOT NULL,
  infantil   BOOLEAN     NOT NULL DEFAULT FALSE,
  idioma     VARCHAR(15) NOT NULL DEFAULT 'Español',
  creado_en  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_perfil_nombre UNIQUE (usuario_id, nombre),
  CONSTRAINT fk_perfil_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE generos (
  id     INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE perfil_gustos (
  perfil_id INT NOT NULL,
  genero_id INT NOT NULL,
  PRIMARY KEY (perfil_id, genero_id),
  FOREIGN KEY (perfil_id) REFERENCES perfiles(id) ON DELETE CASCADE,
  FOREIGN KEY (genero_id) REFERENCES generos(id)
);

CREATE TABLE peliculas (
  id            INT PRIMARY KEY,
  titulo        VARCHAR(80)  NOT NULL UNIQUE,
  anio          SMALLINT     NOT NULL,
  tipo          ENUM('Película','Serie') NOT NULL,
  duracion_min  SMALLINT     NOT NULL,
  clasificacion VARCHAR(4)   NOT NULL,
  descripcion   VARCHAR(400) NOT NULL,
  portada       VARCHAR(60),
  trailer       VARCHAR(20),
  trailer_idioma CHAR(2),
  CONSTRAINT ck_anio CHECK (anio BETWEEN 2025 AND 2026)
);

CREATE TABLE pelicula_generos (
  pelicula_id INT NOT NULL,
  genero_id   INT NOT NULL,
  orden       TINYINT NOT NULL,
  PRIMARY KEY (pelicula_id, genero_id),
  FOREIGN KEY (pelicula_id) REFERENCES peliculas(id) ON DELETE CASCADE,
  FOREIGN KEY (genero_id) REFERENCES generos(id)
);

CREATE TABLE plataformas (
  id     INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(30)  NOT NULL UNIQUE,
  enlace VARCHAR(120) NOT NULL,
  color  VARCHAR(10)  NOT NULL
);

CREATE TABLE pelicula_plataformas (
  pelicula_id   INT NOT NULL,
  plataforma_id INT NOT NULL,
  PRIMARY KEY (pelicula_id, plataforma_id),
  FOREIGN KEY (pelicula_id) REFERENCES peliculas(id) ON DELETE CASCADE,
  FOREIGN KEY (plataforma_id) REFERENCES plataformas(id)
);

CREATE TABLE vistas (
  perfil_id   INT NOT NULL,
  pelicula_id INT NOT NULL,
  minutos     SMALLINT NOT NULL DEFAULT 0,
  visto_en    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (perfil_id, pelicula_id),
  FOREIGN KEY (perfil_id) REFERENCES perfiles(id) ON DELETE CASCADE,
  FOREIGN KEY (pelicula_id) REFERENCES peliculas(id) ON DELETE CASCADE
);

CREATE TABLE calificaciones (
  perfil_id   INT NOT NULL,
  pelicula_id INT NOT NULL,
  estrellas   TINYINT NOT NULL,
  fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (perfil_id, pelicula_id),
  CONSTRAINT ck_estrellas CHECK (estrellas BETWEEN 1 AND 5),
  FOREIGN KEY (perfil_id) REFERENCES perfiles(id) ON DELETE CASCADE,
  FOREIGN KEY (pelicula_id) REFERENCES peliculas(id) ON DELETE CASCADE
);

CREATE TABLE comentarios (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  perfil_id   INT NOT NULL,
  pelicula_id INT NOT NULL,
  texto       VARCHAR(300) NOT NULL,
  fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_texto CHECK (CHAR_LENGTH(texto) BETWEEN 10 AND 300),
  FOREIGN KEY (perfil_id) REFERENCES perfiles(id) ON DELETE CASCADE,
  FOREIGN KEY (pelicula_id) REFERENCES peliculas(id) ON DELETE CASCADE
);

CREATE TABLE mi_lista (
  perfil_id   INT NOT NULL,
  pelicula_id INT NOT NULL,
  agregado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (perfil_id, pelicula_id),
  FOREIGN KEY (perfil_id) REFERENCES perfiles(id) ON DELETE CASCADE,
  FOREIGN KEY (pelicula_id) REFERENCES peliculas(id) ON DELETE CASCADE
);

CREATE TABLE encuestas (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  perfil_id     INT NOT NULL,
  satisfaccion  TINYINT NOT NULL,
  acierto       TINYINT NOT NULL,
  facilidad     TINYINT NOT NULL,
  recomendaria  BOOLEAN NOT NULL,
  comentario    VARCHAR(200),
  fecha         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_enc CHECK (satisfaccion BETWEEN 1 AND 5 AND acierto BETWEEN 1 AND 5 AND facilidad BETWEEN 1 AND 5),
  FOREIGN KEY (perfil_id) REFERENCES perfiles(id) ON DELETE CASCADE
);

-- Cómo mejora la IA con el tiempo: cada vez que llegan datos nuevos se mide otra vez
CREATE TABLE historial_ia (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  fecha           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  calificaciones  INT NOT NULL,
  perfiles        INT NOT NULL,
  mae             DECIMAL(5,3) NOT NULL,
  precision_top   DECIMAL(5,1) NOT NULL,
  nota            VARCHAR(80) NOT NULL
);

CREATE INDEX ix_calif_pelicula ON calificaciones (pelicula_id);
CREATE INDEX ix_coment_pelicula ON comentarios (pelicula_id, fecha);
