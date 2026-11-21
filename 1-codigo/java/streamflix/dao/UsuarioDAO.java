package streamflix.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import streamflix.modelo.Usuario;

/** Consultas SQL de la tabla usuarios. */
public class UsuarioDAO {

    /** Solo estas columnas se pueden revisar como "ya registrado". */
    private static final Map<String, String> CAMPOS_UNICOS = Map.of(
            "usuario", "usuario", "correo", "correo", "dni", "dni", "celular", "celular");

    public boolean existe(String campo, String valor) throws SQLException {
        String columna = CAMPOS_UNICOS.get(campo);
        if (columna == null) throw new IllegalArgumentException("Campo no permitido: " + campo);
        String sql = "SELECT 1 FROM usuarios WHERE LOWER(" + columna + ") = LOWER(?) LIMIT 1";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertar(Usuario u, String claveHash) throws SQLException {
        String sql = "INSERT INTO usuarios (nombre_completo, usuario, correo, dni, celular, edad, clave_hash) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.nombreCompleto());
            ps.setString(2, u.usuario());
            ps.setString(3, u.correo().toLowerCase());
            ps.setString(4, u.dni());
            ps.setString(5, u.celular());
            ps.setInt(6, u.edad());
            ps.setString(7, claveHash);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Devuelve el usuario y su clave cifrada, buscando por usuario o por gmail. */
    public Map.Entry<Usuario, String> buscarParaIngresar(String usuarioOCorreo) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE LOWER(usuario) = LOWER(?) OR correo = LOWER(?) LIMIT 1";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuarioOCorreo);
            ps.setString(2, usuarioOCorreo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return Map.entry(leer(rs), rs.getString("clave_hash"));
            }
        }
    }

    /** Datos de seguridad de una cuenta: intentos, bloqueo y código de recuperación. */
    public record EstadoCuenta(Usuario usuario, String claveHash, int intentos, int bloqueos, long segundosBloqueo,
                               String codigoHash, long segundosCodigo, int codigoIntentos, long segundosDesdePedido) {}

    /** Busca por usuario o por gmail; el tiempo se calcula en la base para no depender del reloj de Java. */
    public EstadoCuenta buscarEstado(String usuarioOCorreo) throws SQLException {
        String sql = Conexion.sql(
                "SELECT *, GREATEST(0, IFNULL(TIMESTAMPDIFF(SECOND, NOW(), bloqueado_hasta), 0)) AS seg_bloqueo, "
                + "IFNULL(TIMESTAMPDIFF(SECOND, NOW(), codigo_vence), 0) AS seg_codigo, "
                + "IFNULL(TIMESTAMPDIFF(SECOND, codigo_pedido, NOW()), 99999) AS desde_pedido ",
                "SELECT *, GREATEST(0, COALESCE(" + segundos("bloqueado_hasta - LOCALTIMESTAMP") + ", 0)) AS seg_bloqueo, "
                + "COALESCE(" + segundos("codigo_vence - LOCALTIMESTAMP") + ", 0) AS seg_codigo, "
                + "COALESCE(" + segundos("LOCALTIMESTAMP - codigo_pedido") + ", 99999) AS desde_pedido ")
                + "FROM usuarios WHERE LOWER(usuario) = LOWER(?) OR correo = LOWER(?) LIMIT 1";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuarioOCorreo);
            ps.setString(2, usuarioOCorreo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new EstadoCuenta(leer(rs), rs.getString("clave_hash"), rs.getInt("intentos_fallidos"),
                        rs.getInt("bloqueos"), rs.getLong("seg_bloqueo"), rs.getString("codigo_hash"),
                        rs.getLong("seg_codigo"), rs.getInt("codigo_intentos"), rs.getLong("desde_pedido"));
            }
        }
    }

    /** Guarda un intento fallido; si segundosEspera es mayor que 0, bloquea la cuenta ese tiempo. */
    public void registrarFallo(int id, int intentos, int bloqueos, int segundosEspera) throws SQLException {
        actualizar("UPDATE usuarios SET intentos_fallidos = ?, bloqueos = ?, "
                + Conexion.sql("bloqueado_hasta = IF(? > 0, NOW() + INTERVAL ? SECOND, bloqueado_hasta) WHERE id = ?",
                        "bloqueado_hasta = CASE WHEN ? > 0 THEN LOCALTIMESTAMP + ? * INTERVAL '1 second' ELSE bloqueado_hasta END WHERE id = ?"),
                intentos, bloqueos, segundosEspera, segundosEspera, id);
    }

    public void limpiarIntentos(int id) throws SQLException {
        actualizar("UPDATE usuarios SET intentos_fallidos = 0, bloqueos = 0, bloqueado_hasta = NULL WHERE id = ?", id);
    }

    public void guardarCodigo(int id, String codigoHash, int minutos) throws SQLException {
        actualizar(Conexion.sql("UPDATE usuarios SET codigo_hash = ?, codigo_vence = NOW() + INTERVAL ? MINUTE, "
                + "codigo_intentos = 0, codigo_pedido = NOW() WHERE id = ?",
                "UPDATE usuarios SET codigo_hash = ?, codigo_vence = LOCALTIMESTAMP + ? * INTERVAL '1 minute', "
                + "codigo_intentos = 0, codigo_pedido = LOCALTIMESTAMP WHERE id = ?"), codigoHash, minutos, id);
    }

    public void fallarCodigo(int id, int intentos) throws SQLException {
        actualizar("UPDATE usuarios SET codigo_intentos = ? WHERE id = ?", intentos, id);
    }

    public void borrarCodigo(int id) throws SQLException {
        actualizar("UPDATE usuarios SET codigo_hash = NULL, codigo_vence = NULL, codigo_intentos = 0 WHERE id = ?", id);
    }

    /** Nueva contraseña: también quita el bloqueo y el código usado. */
    public void cambiarClave(int id, String claveHash) throws SQLException {
        actualizar("UPDATE usuarios SET clave_hash = ?, intentos_fallidos = 0, bloqueos = 0, bloqueado_hasta = NULL, "
                + "codigo_hash = NULL, codigo_vence = NULL, codigo_intentos = 0 WHERE id = ?", claveHash, id);
    }

    /** Segundos de una resta de fechas en PostgreSQL. */
    private static String segundos(String resta) {
        return "CAST(EXTRACT(EPOCH FROM (" + resta + ")) AS BIGINT)";
    }

    private void actualizar(String sql, Object... valores) throws SQLException {
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < valores.length; i++) ps.setObject(i + 1, valores[i]);
            ps.executeUpdate();
        }
    }

    public Usuario obtener(int id) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM usuarios WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    private Usuario leer(ResultSet rs) throws SQLException {
        return new Usuario(rs.getInt("id"), rs.getString("nombre_completo"), rs.getString("usuario"),
                rs.getString("correo"), rs.getString("dni"), rs.getString("celular"), rs.getInt("edad"));
    }
}
