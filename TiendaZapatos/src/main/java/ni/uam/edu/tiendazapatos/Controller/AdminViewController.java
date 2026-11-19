package ni.uam.edu.tiendazapatos.Controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;
import ni.uam.edu.tiendazapatos.dao.RolDAO;
import ni.uam.edu.tiendazapatos.dao.UsuarioDAO;
import ni.uam.edu.tiendazapatos.modelo.Rol;
import ni.uam.edu.tiendazapatos.modelo.Usuario;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class AdminViewController {

    @FXML private Label lblAdminInfo;
    @FXML private Button btnCerrarSesion;

    // Formulario de registro de usuarios
    @FXML private TextField txtNuevoUsuario;
    @FXML private TextField txtNuevoNombreCompleto;
    @FXML private TextField txtNuevoCorreo;
    @FXML private PasswordField txtNuevoPassword;
    @FXML private ComboBox<Rol> cmbNuevoRol;
    @FXML private Label lblMensajeRegistro;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RolDAO rolDAO = new RolDAO();

    @FXML
    public void initialize() {
        // Control de acceso: un vendedor o usuario no administrador no puede ver esta vista
        if (!SesionUsuario.getInstancia().estaAutenticado() || !SesionUsuario.getInstancia().esAdministrador()) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Acceso Denegado");
                alert.setHeaderText("Acceso no autorizado");
                alert.setContentText("No tienes permisos de Administrador para acceder a este panel.");
                alert.showAndWait();
                redirigirALogin();
            });
            return;
        }

        Usuario usuarioActual = SesionUsuario.getInstancia().getUsuarioActual();
        if (lblAdminInfo != null && usuarioActual != null) {
            lblAdminInfo.setText("Administrador: " + usuarioActual.getNombreCompleto());
        }

        configurarComboRoles();
        cargarRoles();
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
            if (lblMensajeRegistro != null) {
                lblMensajeRegistro.setStyle("-fx-text-fill: red;");
                lblMensajeRegistro.setText("Error al cargar roles de la base de datos.");
            }
        }
    }

    @FXML
    private void handleRegistrarUsuario() {
        String usuario = txtNuevoUsuario.getText() != null ? txtNuevoUsuario.getText().trim() : "";
        String nombreCompleto = txtNuevoNombreCompleto.getText() != null ? txtNuevoNombreCompleto.getText().trim() : "";
        String correo = txtNuevoCorreo.getText() != null ? txtNuevoCorreo.getText().trim() : "";
        String password = txtNuevoPassword.getText() != null ? txtNuevoPassword.getText() : "";
        Rol rolSeleccionado = cmbNuevoRol.getValue();

        // 1. Validación de campos vacíos
        if (usuario.isEmpty() || nombreCompleto.isEmpty() || correo.isEmpty() || password.isEmpty() || rolSeleccionado == null) {
            lblMensajeRegistro.setStyle("-fx-text-fill: red;");
            lblMensajeRegistro.setText("Por favor, completa todos los campos requeridos.");
            return;
        }

        try {
            // 2. Validación de usuario duplicado
            if (usuarioDAO.buscarPorNombreUsuario(usuario) != null) {
                lblMensajeRegistro.setStyle("-fx-text-fill: red;");
                lblMensajeRegistro.setText("El nombre de usuario '" + usuario + "' ya está registrado.");
                return;
            }

            // 3. Validación de correo duplicado
            if (usuarioDAO.buscarPorCorreo(correo) != null) {
                lblMensajeRegistro.setStyle("-fx-text-fill: red;");
                lblMensajeRegistro.setText("El correo electrónico '" + correo + "' ya está registrado.");
                return;
            }

            // 4. Crear entidad Usuario y guardar en la BD
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombreUsuario(usuario);
            nuevoUsuario.setNombreCompleto(nombreCompleto);
            nuevoUsuario.setCorreo(correo);
            nuevoUsuario.setPasswordHash(password); // Mismo método que usa el proyecto
            nuevoUsuario.setRol(rolSeleccionado);
            nuevoUsuario.setActivo(true);
            nuevoUsuario.setCreadoEn(LocalDateTime.now());

            usuarioDAO.guardar(nuevoUsuario);

            lblMensajeRegistro.setStyle("-fx-text-fill: green;");
            lblMensajeRegistro.setText("¡Usuario '" + usuario + "' registrado exitosamente con rol " + rolSeleccionado.getNombre() + "!");

            // Limpiar formulario
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
        Stage stage = null;
        if (btnCerrarSesion != null && btnCerrarSesion.getScene() != null) {
            stage = (Stage) btnCerrarSesion.getScene().getWindow();
        }
        if (stage != null) {
            navegarALogin(stage);
        }
    }

    private void navegarALogin(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ni/uam/edu/tiendazapatos/Login.fxml"));
            Scene scene = new Scene(loader.load(), 450, 500);
            stage.setTitle("Sistema Tienda de Zapatos - Login");
            stage.setScene(scene);
            stage.setMaximized(false);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al cerrar sesión");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
