package streamflix.servicio;

/** Persona que inició sesión y el perfil que está usando en este momento. */
public class Sesion {

    private final int usuarioId;
    private volatile int perfilId;

    public Sesion(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public int getPerfilId() {
        return perfilId;
    }

    public void setPerfilId(int perfilId) {
        this.perfilId = perfilId;
    }

    public boolean tienePerfil() {
        return perfilId > 0;
    }
}
