package ni.uam.edu.tiendazapatos.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.dao.UsuarioDAO;
import ni.uam.edu.tiendazapatos.modelo.Usuario;

import java.io.IOException;

public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensaje;
    @FXML private javafx.scene.control.Button btnRegistro;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private void handleIngresar() {
        if (txtUsuario.getText().isEmpty() || txtPassword.getText().isEmpty()) {
            lblMensaje.setText("Por favor, completa todos los campos.");
            return;
        }

        try {
            Usuario usuario = usuarioDAO.buscarPorNombreUsuario(txtUsuario.getText());

            if (usuario == null) {
                lblMensaje.setText("Usuario no encontrado.");
                return;
            }


            if (usuario.getPasswordHash().equals(txtPassword.getText())) {
                lblMensaje.setStyle("-fx-text-fill: green;");
                lblMensaje.setText("¡Bienvenido, " + usuario.getNombreCompleto() + "!");
                System.out.println("Login exitoso. Rol: " + usuario.getRol().getNombre());

                // --- AQUÍ EMPIEZA EL CAMBIO PARA REDIRIGIR AL MENÚ ---
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(
                            getClass().getResource("/ni/uam/edu/tiendazapatos/ZapatosView.fxml")
                    );

                    Scene scene = new Scene(fxmlLoader.load(), 1200, 690);

                    Stage stage = (Stage) lblMensaje.getScene().getWindow();

                    stage.setTitle("Sistema Tienda de Zapatos - Inventario");
                    stage.setScene(scene);

// Abrir el inventario maximizado
                    stage.setMaximized(true);
                    stage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                    lblMensaje.setText("Error al cargar el menú.");
                }


            } else {
                lblMensaje.setText("Contraseña incorrecta.");
            }

        } catch (Exception e) {
            lblMensaje.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegistro() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ni/uam/edu/tiendazapatos/RegistroUsuario.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 550);
            Stage stage = (Stage) btnRegistro.getScene().getWindow();
            stage.setTitle("Registro de Usuario");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error: No se encontró el archivo FXML.");
        }
    }
}
