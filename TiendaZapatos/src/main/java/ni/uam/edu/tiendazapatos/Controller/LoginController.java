package ni.uam.edu.tiendazapatos.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;
import ni.uam.edu.tiendazapatos.dao.UsuarioDAO;
import ni.uam.edu.tiendazapatos.modelo.Usuario;

import java.io.IOException;

public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensaje;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private void handleIngresar() {
        String nombreUsuario = txtUsuario.getText() != null ? txtUsuario.getText().trim() : "";
        String password = txtPassword.getText() != null ? txtPassword.getText() : "";

        if (nombreUsuario.isEmpty() || password.isEmpty()) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Por favor, completa todos los campos.");
            return;
        }

        try {
            Usuario usuario = usuarioDAO.buscarPorNombreUsuario(nombreUsuario);

            if (usuario == null) {
                lblMensaje.setStyle("-fx-text-fill: red;");
                lblMensaje.setText("Usuario no encontrado.");
                return;
            }

            if (!usuario.getPasswordHash().equals(password)) {
                lblMensaje.setStyle("-fx-text-fill: red;");
                lblMensaje.setText("Contraseña incorrecta.");
                return;
            }

            // Iniciar sesión en el singleton
            SesionUsuario.getInstancia().iniciar(usuario);

            String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
            System.out.println("Login exitoso. Rol: " + rolNombre);

            // Redirigir según rol, comparando por nombre
            String fxmlDestino;
            String tituloDestino;
            int ancho;
            int alto;

            if ("Administrador".equals(rolNombre)) {
                fxmlDestino = "/ni/uam/edu/tiendazapatos/AdminView.fxml";
                tituloDestino = "Sistema Tienda de Zapatos - Administrador";
                ancho = 1200;
                alto = 700;
            } else if ("Vendedor".equals(rolNombre)) {
                fxmlDestino = "/ni/uam/edu/tiendazapatos/VendedorView.fxml";
                tituloDestino = "Sistema Tienda de Zapatos - Vendedor";
                ancho = 900;
                alto = 600;
            } else {
                // Rol desconocido -> mensaje de error
                SesionUsuario.getInstancia().cerrar();
                lblMensaje.setStyle("-fx-text-fill: red;");
                lblMensaje.setText("Rol desconocido. No tiene permisos de acceso.");
                return;
            }

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlDestino));
                Scene scene = new Scene(loader.load(), ancho, alto);
                Stage stage = (Stage) lblMensaje.getScene().getWindow();
                stage.setTitle(tituloDestino);
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                lblMensaje.setStyle("-fx-text-fill: red;");
                lblMensaje.setText("Error al cargar la vista: " + e.getMessage());
            }

        } catch (Exception e) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
