package ni.uam.edu.tiendazapatos.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.dao.InventarioDAO;
import ni.uam.edu.tiendazapatos.dao.MarcaDAO;
import ni.uam.edu.tiendazapatos.dao.ZapatoDAO;
import ni.uam.edu.tiendazapatos.modelo.Marca;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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

    @FXML private ComboBox<String> cmbGenero;
    @FXML private ComboBox<String> cmbSistemaTalla;
    @FXML private TextField txtTalla;
    @FXML private TextField txtColor;
    @FXML private TextField txtStockInicial;

    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final ZapatoDAO zapatoDAO = new ZapatoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private String rutaImagenSeleccionada = null;

    @FXML
    public void initialize() {
        cmbMarca.getItems().setAll(marcaDAO.obtenerTodas());
        cmbGenero.getItems().setAll("HOMBRE", "MUJER", "UNISEX");
        cmbSistemaTalla.getItems().setAll("US", "EU", "UK");
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
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Nombre, Marca y Precio Base son obligatorios.");
            return;
        }
        if (cmbGenero.getValue() == null || cmbSistemaTalla.getValue() == null
                || txtTalla.getText().trim().isEmpty() || txtColor.getText().trim().isEmpty()
                || txtStockInicial.getText().trim().isEmpty()) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Completa la variante: Género, Sistema, Talla, Color y Stock.");
            return;
        }

        int stockInicial;
        try {
            stockInicial = Integer.parseInt(txtStockInicial.getText().trim());
            if (stockInicial < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("El stock inicial debe ser un número entero ≥ 0.");
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
            zapato.setImagenUrl(rutaImagenSeleccionada);
            zapato.setActivo(true);
            zapato.setCreadoEn(LocalDateTime.now());

            zapatoDAO.guardar(zapato);
            UUID zapatoId = zapato.getZapatoId();
            if (zapatoId == null) {
                throw new IllegalStateException("No se obtuvo el ID del zapato tras guardarlo.");
            }

            inventarioDAO.guardarVariante(
                    zapatoId,
                    cmbGenero.getValue(),
                    cmbSistemaTalla.getValue(),
                    txtTalla.getText().trim(),
                    txtColor.getText().trim(),
                    stockInicial
            );

            lblMensaje.setStyle("-fx-text-fill: green;");
            lblMensaje.setText("¡Zapato y variante registrados exitosamente!");

            // ✨ NUEVO: Cerrar la ventana automáticamente tras 1 segundo
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> {
                        Stage stage = (Stage) lblMensaje.getScene().getWindow();
                        stage.close();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (NumberFormatException e) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Error: Precio y descuento deben ser números.");
        } catch (Exception e) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Error al guardar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVolver() {
        // ✨ CAMBIO: En lugar de abrir una nueva pestaña de inventario, solo cierra esta ventana
        Stage stage = (Stage) lblMensaje.getScene().getWindow();
        stage.close();
    }

    public void prellenarDatos(Zapato zapato) {
        txtNombre.setText(zapato.getNombre());
        cmbMarca.setValue(zapato.getMarca());
        txtPrecioBase.setText(zapato.getPrecioBase().toString());
        txtDescuento.setText(zapato.getPorcentajeDescuento().toString());
        txtDescripcion.setText(zapato.getDescripcion());

        if (zapato.getImagenUrl() != null && !zapato.getImagenUrl().isEmpty()) {
            File file = new File(zapato.getImagenUrl());
            if (file.exists()) {
                lblNombreImagen.setText(file.getName());
                lblNombreImagen.setStyle("-fx-text-fill: green;");
                rutaImagenSeleccionada = zapato.getImagenUrl();
            }
        }
        lblMensaje.setStyle("-fx-text-fill: blue;");
        lblMensaje.setText("MODELO CLONADO. Define los datos de la NUEVA variante abajo.");
    }
}