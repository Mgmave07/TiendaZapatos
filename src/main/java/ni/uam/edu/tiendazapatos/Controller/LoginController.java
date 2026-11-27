package ni.uam.edu.tiendazapatos.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;
import ni.uam.edu.tiendazapatos.dao.UsuarioDAO;
import ni.uam.edu.tiendazapatos.modelo.Usuario;

import java.io.IOException;

public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensaje;
    @FXML private Button btnIngresar;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        // Enviar con tecla Enter en ambos campos
        if (txtUsuario != null) {
            txtUsuario.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    if (txtPassword.getText().isEmpty()) {
                        txtPassword.requestFocus();
                    } else {
                        handleIngresar();
                    }
                }
            });
        }
        if (txtPassword != null) {
            txtPassword.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    handleIngresar();
                }
            });
        }
    }

    @FXML
    private void handleIngresar() {
        String nombreUsuario = txtUsuario.getText() != null ? txtUsuario.getText().trim() : "";
        String password = txtPassword.getText() != null ? txtPassword.getText() : "";

        if (nombreUsuario.isEmpty() || password.isEmpty()) {
            mostrarError("Por favor, completa todos los campos.");
            return;
        }

        try {
            Usuario usuario = usuarioDAO.buscarPorNombreUsuario(nombreUsuario);

            if (usuario == null) {
                mostrarError("Usuario no encontrado.");
                return;
            }

            if (!usuario.getPasswordHash().equals(password)) {
                mostrarError("Contraseña incorrecta.");
                return;
            }

            if (usuario.getActivo() != null && !usuario.getActivo()) {
                mostrarError("Esta cuenta está desactivada. Contacta al administrador.");
                return;
            }

            // Iniciar sesión en el singleton
            SesionUsuario.getInstancia().iniciar(usuario);

            String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
            System.out.println("Login exitoso. Rol: " + rolNombre);

            // Redirigir según rol
            String fxmlDestino;
            String tituloDestino;
            int ancho;
            int alto;

            if ("Administrador".equalsIgnoreCase(rolNombre)) {
                fxmlDestino = "/ni/uam/edu/tiendazapatos/AdminView.fxml";
                tituloDestino = "Sistema Tienda de Zapatos - Administrador";
                ancho = 1200;
                alto = 750;
            } else if ("Vendedor".equalsIgnoreCase(rolNombre)) {
                fxmlDestino = "/ni/uam/edu/tiendazapatos/VendedorView.fxml";
                tituloDestino = "Sistema Tienda de Zapatos - Vendedor";
                ancho = 1200;
                alto = 750;
            } else {
                SesionUsuario.getInstancia().cerrar();
                mostrarError("Rol desconocido. No tiene permisos de acceso.");
                return;
            }

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlDestino));
                Scene scene = new Scene(loader.load(), ancho, alto);
                Stage stage = (Stage) btnIngresar.getScene().getWindow();
                stage.setTitle(tituloDestino);
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                mostrarError("Error al cargar la vista: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error de conexión: " + e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        if (lblMensaje != null) {
            lblMensaje.setText(mensaje);
            lblMensaje.setVisible(true);
            lblMensaje.setManaged(true);
        }
    }
}
