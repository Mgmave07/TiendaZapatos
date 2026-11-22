package ni.uam.edu.tiendazapatos.Controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;
import ni.uam.edu.tiendazapatos.dao.RolDAO;
import ni.uam.edu.tiendazapatos.dao.UsuarioDAO;
import ni.uam.edu.tiendazapatos.modelo.Rol;
import ni.uam.edu.tiendazapatos.modelo.Usuario;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

public class AdminViewController {

    @FXML private Label lblAdminInfo;
    @FXML private Label lblBienvenida;
    @FXML private Button btnCerrarSesion;
    @FXML private Button btnNavInicio;
    @FXML private Button btnNavInventario;
    @FXML private Button btnNavVentas;
    @FXML private Button btnNavUsuarios;
    @FXML private Button btnVolver;
    @FXML private Label lblRuta;
    @FXML private Label lblFechaActual;
    @FXML private StackPane panelContenido;
    @FXML private ScrollPane vistaInicio;
    @FXML private ScrollPane vistaRegistro;

    // Formulario existente de registro de usuarios
    @FXML private TextField txtNuevoUsuario;
    @FXML private TextField txtNuevoNombreCompleto;
    @FXML private TextField txtNuevoCorreo;
    @FXML private PasswordField txtNuevoPassword;
    @FXML private ComboBox<Rol> cmbNuevoRol;
    @FXML private Label lblMensajeRegistro;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RolDAO rolDAO = new RolDAO();

    // Se cargan cuando se seleccionan; no al iniciar sesion.
    private Parent vistaInventario;
    private Parent vistaVentas;

    private enum Seccion { INICIO, INVENTARIO, VENTAS, USUARIOS }
    private Seccion seccionActual = Seccion.INICIO;
    private final Deque<Seccion> historial = new ArrayDeque<>();

    private static final String ESTILO_NAV_BASE =
            "-fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 7; " +
            "-fx-padding: 14 16; -fx-font-size: 14px; ";

    @FXML
    public void initialize() {
        if (!SesionUsuario.getInstancia().estaAutenticado()
                || !SesionUsuario.getInstancia().esAdministrador()) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Acceso denegado");
                alert.setHeaderText("Acceso no autorizado");
                alert.setContentText("No tienes permisos de administrador para acceder a este panel.");
                alert.showAndWait();
                redirigirALogin();
            });
            return;
        }

        Usuario usuarioActual = SesionUsuario.getInstancia().getUsuarioActual();
        if (usuarioActual != null) {
            String nombre = usuarioActual.getNombreCompleto();
            lblAdminInfo.setText("Administrador: " + nombre);
            lblBienvenida.setText("¡Bienvenido, " + nombre + "!");
        }

        lblFechaActual.setText("Hoy es " + LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "NI"))));

        // El formulario permanece en memoria, pero se abre solo al seleccionarlo.
        panelContenido.getChildren().remove(vistaRegistro);
        configurarComboRoles();
        cargarRoles();
        actualizarNavegacion();
    }

    @FXML
    private void mostrarInicio() {
        if (cambiarSeccion(Seccion.INICIO, false)) {
            historial.clear();
            actualizarNavegacion();
        }
    }

    @FXML
    private void mostrarInventario() {
        cambiarSeccion(Seccion.INVENTARIO, true);
    }

    @FXML
    private void mostrarVentas() {
        cambiarSeccion(Seccion.VENTAS, true);
    }

    @FXML
    private void mostrarRegistro() {
        cambiarSeccion(Seccion.USUARIOS, true);
    }

    // Regresa a la sección anterior (no simplemente a Inicio).
    @FXML
    private void volverAtras() {
        if (historial.isEmpty()) {
            cambiarSeccion(Seccion.INICIO, false);
            return;
        }
        Seccion anterior = historial.pop();
        if (!cambiarSeccion(anterior, false)) {
            historial.push(anterior);
        }
        actualizarNavegacion();
    }

    private boolean cambiarSeccion(Seccion destino, boolean guardarHistorial) {
        if (destino == seccionActual) return true;
        try {
            Node vista = obtenerVista(destino);
            if (guardarHistorial) {
                historial.push(seccionActual);
            }
            vista.setVisible(true);
            vista.setManaged(true);
            panelContenido.getChildren().setAll(vista);
            seccionActual = destino;
            actualizarNavegacion();
            return true;
        } catch (Exception e) {
            mostrarError("No se pudo abrir la sección " + nombreSeccion(destino), e);
            return false;
        }
    }

    private Node obtenerVista(Seccion seccion) throws IOException {
        return switch (seccion) {
            case INICIO -> vistaInicio;
            case USUARIOS -> vistaRegistro;
            case INVENTARIO -> {
                if (vistaInventario == null) vistaInventario = cargarVista("ZapatosView.fxml");
                yield vistaInventario;
            }
            case VENTAS -> {
                if (vistaVentas == null) vistaVentas = cargarVista("VentasView.fxml");
                yield vistaVentas;
            }
        };
    }

    private String nombreSeccion(Seccion seccion) {
        return switch (seccion) {
            case INICIO -> "Inicio";
            case INVENTARIO -> "Inventario";
            case VENTAS -> "Ventas y facturación";
            case USUARIOS -> "Registrar usuario";
        };
    }

    private void actualizarNavegacion() {
        marcarActivo(btnNavInicio, seccionActual == Seccion.INICIO);
        marcarActivo(btnNavInventario, seccionActual == Seccion.INVENTARIO);
        marcarActivo(btnNavVentas, seccionActual == Seccion.VENTAS);
        marcarActivo(btnNavUsuarios, seccionActual == Seccion.USUARIOS);

        lblRuta.setText(nombreSeccion(seccionActual));
        boolean hayAnterior = seccionActual != Seccion.INICIO && !historial.isEmpty();
        btnVolver.setVisible(hayAnterior);
        btnVolver.setManaged(hayAnterior);
    }

    private void marcarActivo(Button boton, boolean activo) {
        boton.setStyle(ESTILO_NAV_BASE + (activo
                ? "-fx-background-color: #45678b; -fx-font-weight: bold; " +
                  "-fx-border-color: #8fc0ff; -fx-border-width: 0 0 0 4; -fx-border-radius: 7;"
                : "-fx-background-color: transparent;"));
    }

    private Parent cargarVista(String archivo) throws IOException {
        String ruta = "/ni/uam/edu/tiendazapatos/" + archivo;
        URL recurso = getClass().getResource(ruta);
        if (recurso == null) {
            throw new IOException("No se encontró la vista: " + ruta);
        }
        return FXMLLoader.load(recurso);
    }

    private void mostrarError(String mensaje, Exception e) {
        e.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de navegación");
        alert.setHeaderText(mensaje);
        alert.setContentText(e.getMessage() != null ? e.getMessage() : e.toString());
        alert.showAndWait();
    }

    private void configurarComboRoles() {
        cmbNuevoRol.setConverter(new StringConverter<>() {
            @Override
            public String toString(Rol rol) {
                return rol != null ? rol.getNombre() : "";
            }

            @Override
            public Rol fromString(String string) {
                return null;
            }
        });
    }

    private void cargarRoles() {
        try {
            List<Rol> roles = rolDAO.obtenerTodos();
            cmbNuevoRol.setItems(FXCollections.observableArrayList(roles));
        } catch (Exception e) {
            e.printStackTrace();
            lblMensajeRegistro.setStyle("-fx-text-fill: red;");
            lblMensajeRegistro.setText("Error al cargar roles de la base de datos.");
        }
    }

    @FXML
    private void handleRegistrarUsuario() {
        String usuario = txtNuevoUsuario.getText() != null ? txtNuevoUsuario.getText().trim() : "";
        String nombreCompleto = txtNuevoNombreCompleto.getText() != null ? txtNuevoNombreCompleto.getText().trim() : "";
        String correo = txtNuevoCorreo.getText() != null ? txtNuevoCorreo.getText().trim() : "";
        String password = txtNuevoPassword.getText() != null ? txtNuevoPassword.getText() : "";
        Rol rolSeleccionado = cmbNuevoRol.getValue();

        if (usuario.isEmpty() || nombreCompleto.isEmpty() || correo.isEmpty()
                || password.isEmpty() || rolSeleccionado == null) {
            lblMensajeRegistro.setStyle("-fx-text-fill: red;");
            lblMensajeRegistro.setText("Por favor, completa todos los campos requeridos.");
            return;
        }

        try {
            if (usuarioDAO.buscarPorNombreUsuario(usuario) != null) {
                lblMensajeRegistro.setStyle("-fx-text-fill: red;");
                lblMensajeRegistro.setText("El nombre de usuario '" + usuario + "' ya está registrado.");
                return;
            }

            if (usuarioDAO.buscarPorCorreo(correo) != null) {
                lblMensajeRegistro.setStyle("-fx-text-fill: red;");
                lblMensajeRegistro.setText("El correo electrónico '" + correo + "' ya está registrado.");
                return;
            }

            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombreUsuario(usuario);
            nuevoUsuario.setNombreCompleto(nombreCompleto);
            nuevoUsuario.setCorreo(correo);
            // Se conserva el comportamiento actual del proyecto. Pendiente: hash seguro.
            nuevoUsuario.setPasswordHash(password);
            nuevoUsuario.setRol(rolSeleccionado);
            nuevoUsuario.setActivo(true);
            nuevoUsuario.setCreadoEn(LocalDateTime.now());
            usuarioDAO.guardar(nuevoUsuario);

            lblMensajeRegistro.setStyle("-fx-text-fill: green;");
            lblMensajeRegistro.setText("¡Usuario '" + usuario + "' registrado exitosamente con rol "
                    + rolSeleccionado.getNombre() + "!");

            txtNuevoUsuario.clear();
            txtNuevoNombreCompleto.clear();
            txtNuevoCorreo.clear();
            txtNuevoPassword.clear();
            cmbNuevoRol.getSelectionModel().clearSelection();
        } catch (Exception e) {
            lblMensajeRegistro.setStyle("-fx-text-fill: red;");
            lblMensajeRegistro.setText("Error al guardar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCerrarSesion(ActionEvent event) {
        SesionUsuario.getInstancia().cerrar();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        navegarALogin(stage);
    }

    private void redirigirALogin() {
        if (btnCerrarSesion != null && btnCerrarSesion.getScene() != null) {
            Stage stage = (Stage) btnCerrarSesion.getScene().getWindow();
            navegarALogin(stage);
        }
    }

    private void navegarALogin(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/uam/edu/tiendazapatos/Login.fxml"));
            Scene scene = new Scene(loader.load(), 450, 500);
            stage.setTitle("Sistema Tienda de Zapatos - Login");
            stage.setScene(scene);
            stage.setMaximized(false);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            mostrarError("Error al cerrar sesión", e);
        }
    }
}
