package ni.uam.edu.tiendazapatos.reportes;

/**
 * DTO de fila para el reporte JasperReports de usuarios del sistema.
 */
public class UsuarioReporteItem {

    private String nombreUsuario;
    private String nombreCompleto;
    private String correo;
    private String telefono;
    private String rol;
    private String estado;
    private String fechaRegistro;

    public UsuarioReporteItem() {
    }

    public UsuarioReporteItem(
            String nombreUsuario,
            String nombreCompleto,
            String correo,
            String telefono,
            String rol,
            String estado,
            String fechaRegistro
    ) {
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.telefono = telefono;
        this.rol = rol;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
    }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
