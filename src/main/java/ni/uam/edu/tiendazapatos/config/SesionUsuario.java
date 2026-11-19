package ni.uam.edu.tiendazapatos.config;

import ni.uam.edu.tiendazapatos.modelo.Rol;
import ni.uam.edu.tiendazapatos.modelo.Usuario;

/**
 * Singleton que guarda el usuario autenticado durante toda la sesión.
 * Se inicializa tras el login y se limpia al cerrar sesión.
 */
public class SesionUsuario {

    private static SesionUsuario instancia;
    private Usuario usuarioActual;

    private SesionUsuario() {}

    public static SesionUsuario getInstancia() {
        if (instancia == null) {
            instancia = new SesionUsuario();
        }
        return instancia;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public void iniciar(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    public void cerrar() {
        this.usuarioActual = null;
    }

    public boolean estaAutenticado() {
        return usuarioActual != null;
    }

    public Rol getRol() {
        return (usuarioActual != null) ? usuarioActual.getRol() : null;
    }

    /** Devuelve el nombre del rol tal como está registrado, o cadena vacía si no hay sesión. */
    public String getRolNombre() {
        if (usuarioActual == null || usuarioActual.getRol() == null) return "";
        return usuarioActual.getRol().getNombre();
    }

    public boolean esAdministrador() {
        return "Administrador".equalsIgnoreCase(getRolNombre());
    }

    public boolean esVendedor() {
        return "Vendedor".equalsIgnoreCase(getRolNombre());
    }
}
