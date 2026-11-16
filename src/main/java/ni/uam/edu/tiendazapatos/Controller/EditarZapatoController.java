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
import java.util.Optional;

public class EditarZapatoController {
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
    private Zapato zapatoEditando = null;

    @FXML
    public void initialize() {
        cmbMarca.getItems().setAll(marcaDAO.obtenerTodas());
    }

    public void setZapato(Zapato zapato) {
        this.zapatoEditando = zapato;
        cargarDatos();
    }

    private void cargarDatos() {
        if (zapatoEditando == null) return;

        txtNombre.setText(zapatoEditando.getNombre());
        cmbMarca.setValue(zapatoEditando.getMarca());
        txtPrecioBase.setText(zapatoEditando.getPrecioBase().toString());
        txtDescuento.setText(zapatoEditando.getPorcentajeDescuento().toString());
        txtDescripcion.setText(zapatoEditando.getDescripcion());

        if (zapatoEditando.getImagenUrl() != null && !zapatoEditando.getImagenUrl().isEmpty()) {
            File file = new File(zapatoEditando.getImagenUrl());
            if (file.exists()) {
                lblNombreImagen.setText(file.getName());
                lblNombreImagen.setStyle("-fx-text-fill: green;");
                rutaImagenSeleccionada = zapatoEditando.getImagenUrl();
            }
        }
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

            zapatoEditando.setNombre(txtNombre.getText().trim());
            zapatoEditando.setMarca(cmbMarca.getValue());
            zapatoEditando.setPrecioBase(precioBase);
            zapatoEditando.setPorcentajeDescuento(descuento);
            zapatoEditando.setPrecioFinal(precioFinal);
            zapatoEditando.setDescripcion(txtDescripcion.getText().trim());

            if (rutaImagenSeleccionada != null) {
                zapatoEditando.setImagenUrl(rutaImagenSeleccionada);
            }

            zapatoDAO.actualizar(zapatoEditando);

            lblMensaje.setStyle("-fx-text-fill: green;");
            lblMensaje.setText("¡Zapato actualizado exitosamente!");

        } catch (NumberFormatException e) {
            lblMensaje.setText("Error: Precio y descuento deben ser números.");
        } catch (Exception e) {
            lblMensaje.setText("Error al actualizar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelar() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ni/uam/edu/tiendazapatos/ZapatosView.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 650, 500);
            Stage stage = (Stage) lblMensaje.getScene().getWindow();
            stage.setTitle("Sistema Tienda de Zapatos - Inventario");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
