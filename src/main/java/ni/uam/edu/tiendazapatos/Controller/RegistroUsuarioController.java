package ni.uam.edu.tiendazapatos.Controller;


import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ni.uam.edu.tiendazapatos.dao.UsuarioDAO;
import ni.uam.edu.tiendazapatos.dao.RolDAO;
import ni.uam.edu.tiendazapatos.modelo.Rol;
import ni.uam.edu.tiendazapatos.modelo.Usuario;

import java.time.LocalDateTime;

public class RegistroUsuarioController {
    @FXML private TextField txtUsuario;
    @FXML private TextField txtNombreCompleto;
    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtTelefono;
    @FXML private Label lblMensaje;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RolDAO rolDAO = new RolDAO();

    @FXML
    private void handleGuardar() {
        if (txtUsuario.getText().isEmpty() || txtPassword.getText().isEmpty()) {
            lblMensaje.setText("El usuario y la contraseña son obligatorios.");
            return;
        }

        try {
            Usuario usuario = new Usuario();
            usuario.setNombreUsuario(txtUsuario.getText());
            usuario.setNombreCompleto(txtNombreCompleto.getText());
            usuario.setCorreo(txtCorreo.getText());
            usuario.setTelefono(txtTelefono.getText());
            usuario.setActivo(true);
            usuario.setCreadoEn(LocalDateTime.now());

            Rol rol = rolDAO.buscarPorNombre("Administrador");
            if (rol == null) {
                lblMensaje.setText("Error: No existe el rol 'Administrador' en la base de datos.");
                return;
            }


            usuario.setRol(rol);

            // Guardar contraseña directamente (sin encriptación)
            usuario.setPasswordHash(txtPassword.getText());

            usuarioDAO.guardar(usuario);

            lblMensaje.setStyle("-fx-text-fill: green;");
            lblMensaje.setText("¡Usuario registrado exitosamente en PostgreSQL!");

            txtUsuario.clear();
            txtNombreCompleto.clear();
            txtCorreo.clear();
            txtPassword.clear();
            txtTelefono.clear();

        } catch (Exception e) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
