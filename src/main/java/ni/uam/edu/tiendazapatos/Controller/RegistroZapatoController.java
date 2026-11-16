package ni.uam.edu.tiendazapatos.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.dao.MarcaDAO;
import ni.uam.edu.tiendazapatos.dao.ZapatoDAO;
import ni.uam.edu.tiendazapatos.modelo.Marca;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public class RegistroZapatoController {
    @FXML private TextField txtNombre;
    @FXML private ComboBox<Marca> cmbMarca;
    @FXML private Button btnNuevaMarca;
    @FXML private TextField txtPrecioBase;
    @FXML private TextField txtDescuento;
    @FXML private TextArea txtDescripcion;
    @FXML private Button btnSeleccionarImagen;
    @FXML private Label lblNombreImagen;
    @FXML private Label lblMensaje;

    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final ZapatoDAO zapatoDAO = new ZapatoDAO();
    private String rutaImagenSeleccionada = null;

    @FXML
    public void initialize() {
        cmbMarca.getItems().setAll(marcaDAO.obtenerTodas());
    }

    @FXML
    private void handleNuevaMarca() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva Marca");
        dialog.setHeaderText("Crear nueva marca");
        dialog.setContentText("Nombre de la marca:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) {
                try {
                    Marca nuevaMarca = new Marca();
                    nuevaMarca.setNombre(nombre.trim());
                    marcaDAO.guardar(nuevaMarca);

                    cmbMarca.getItems().add(nuevaMarca);
                    cmbMarca.setValue(nuevaMarca);
                    lblMensaje.setStyle("-fx-text-fill: green;");
                    lblMensaje.setText("Marca '" + nombre + "' agregada y seleccionada.");
                } catch (Exception e) {
                    lblMensaje.setText("Error al guardar la marca (¿ya existe?).");
                }
            }
        });
    }

    @FXML
    private void handleSeleccionarImagen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar imagen del zapato");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) btnSeleccionarImagen.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            rutaImagenSeleccionada = file.getAbsolutePath();
            lblNombreImagen.setText(file.getName());
            lblNombreImagen.setStyle("-fx-text-fill: green;");
        }
    }

    @FXML
    private void handleGuardar() {
        if (txtNombre.getText().isEmpty() || cmbMarca.getValue() == null || txtPrecioBase.getText().isEmpty()) {
            lblMensaje.setText("Nombre, Marca y Precio Base son obligatorios.");
            return;
        }

        try {
            BigDecimal precioBase = new BigDecimal(txtPrecioBase.getText().trim());
            BigDecimal descuento = txtDescuento.getText().isEmpty() ? BigDecimal.ZERO : new BigDecimal(txtDescuento.getText().trim());
            BigDecimal montoDescuento = precioBase.multiply(descuento).divide(new BigDecimal("100"));
            BigDecimal precioFinal = precioBase.subtract(montoDescuento);

            Zapato zapato = new Zapato();
            zapato.setNombre(txtNombre.getText().trim());
            zapato.setMarca(cmbMarca.getValue());
            zapato.setPrecioBase(precioBase);
            zapato.setPorcentajeDescuento(descuento);
            zapato.setPrecioFinal(precioFinal);
            zapato.setDescripcion(txtDescripcion.getText().trim());
            zapato.setImagenUrl(rutaImagenSeleccionada); // Guardamos la ruta
            zapato.setActivo(true);
            zapato.setCreadoEn(LocalDateTime.now());

            zapatoDAO.guardar(zapato);

            lblMensaje.setStyle("-fx-text-fill: green;");
            lblMensaje.setText("¡Zapato registrado exitosamente!");

            txtNombre.clear(); cmbMarca.getSelectionModel().clearSelection();
            txtPrecioBase.clear(); txtDescuento.clear(); txtDescripcion.clear();
            lblNombreImagen.setText("Ninguna seleccionada");
            rutaImagenSeleccionada = null;

        } catch (NumberFormatException e) {
            lblMensaje.setText("Error: Precio y descuento deben ser números.");
        } catch (Exception e) {
            lblMensaje.setText("Error al guardar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVolver() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ni/uam/edu/tiendazapatos/ZapatosView.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 650, 500);
            Stage stage = (Stage) lblMensaje.getScene().getWindow();
            stage.setTitle("Sistema Tienda de Zapatos - Inventario");
            stage.setScene(scene);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
