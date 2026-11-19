package ni.uam.edu.tiendazapatos.Controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;
import ni.uam.edu.tiendazapatos.modelo.Usuario;

import java.io.IOException;

public class VendedorViewController {

    @FXML private Label lblBienvenida;
    @FXML private Button btnCerrarSesion;

    @FXML
    public void initialize() {
        if (!SesionUsuario.getInstancia().estaAutenticado()) {
            Platform.runLater(this::redirigirALogin);
            return;
        }

        Usuario usuario = SesionUsuario.getInstancia().getUsuarioActual();
        if (usuario != null) {
            lblBienvenida.setText("¡Bienvenido, " + usuario.getNombreCompleto() + "!");
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
            mostrarError("Error al regresar al login", e.getMessage());
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
