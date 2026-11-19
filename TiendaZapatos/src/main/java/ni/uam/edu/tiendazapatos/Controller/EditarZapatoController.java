package ni.uam.edu.tiendazapatos.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.dao.InventarioDAO;
import ni.uam.edu.tiendazapatos.dao.MarcaDAO;
import ni.uam.edu.tiendazapatos.dao.ZapatoDAO;
import ni.uam.edu.tiendazapatos.modelo.InventarioFila;
import ni.uam.edu.tiendazapatos.modelo.Marca;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

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

    @FXML private ComboBox<String> cmbGenero;
    @FXML private ComboBox<String> cmbSistemaTalla;
    @FXML private TextField txtTalla;
    @FXML private TextField txtColor;
    @FXML private TextField txtStock;

    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final ZapatoDAO zapatoDAO = new ZapatoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();

    private Zapato zapatoEditando = null;
    private InventarioFila filaEditando = null;
    private String rutaImagenSeleccionada = null;

    @FXML
    public void initialize() {
        cmbMarca.getItems().setAll(marcaDAO.obtenerTodas());
        cmbGenero.getItems().setAll("HOMBRE", "MUJER", "UNISEX");
        cmbSistemaTalla.getItems().setAll("US", "EU", "UK");
    }

    public void setDatos(Zapato zapato, InventarioFila fila) {
        this.zapatoEditando = zapato;
        this.filaEditando = fila;
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

        if (filaEditando != null && filaEditando.genero() != null) {
            cmbGenero.setValue(filaEditando.genero());
            cmbSistemaTalla.setValue(filaEditando.sistemaTalla());
            txtTalla.setText(filaEditando.talla());
            txtColor.setText(filaEditando.color());
            txtStock.setText(filaEditando.stock() != null ? filaEditando.stock().toString() : "0");
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
                    lblMensaje.setText("Marca '" + nombre + "' agregada.");
                } catch (Exception e) {
                    lblMensaje.setText("Error al guardar la marca.");
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
        if (cmbGenero.getValue() == null || txtTalla.getText().trim().isEmpty() || txtColor.getText().trim().isEmpty()) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Los datos de la variante son obligatorios.");
            return;
        }

        int stock;
        try {
            stock = Integer.parseInt(txtStock.getText().trim());
            if (stock < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("El stock debe ser un número entero ≥ 0.");
            return;
        }

        try {
            BigDecimal precioBase = new BigDecimal(txtPrecioBase.getText().trim());
            BigDecimal descuento = txtDescuento.getText().isEmpty() ? BigDecimal.ZERO : new BigDecimal(txtDescuento.getText().trim());
            BigDecimal precioFinal = precioBase.subtract(precioBase.multiply(descuento).divide(new BigDecimal("100")));

            // 1. Actualizar MODELO
            zapatoEditando.setNombre(txtNombre.getText().trim());
            zapatoEditando.setMarca(cmbMarca.getValue());
            zapatoEditando.setPrecioBase(precioBase);
            zapatoEditando.setPorcentajeDescuento(descuento);
            zapatoEditando.setPrecioFinal(precioFinal);
            zapatoEditando.setDescripcion(txtDescripcion.getText().trim());
            if (rutaImagenSeleccionada != null) zapatoEditando.setImagenUrl(rutaImagenSeleccionada);

            zapatoDAO.actualizar(zapatoEditando);

            // 2. Actualizar o CREAR VARIANTE (lógica robusta)
            UUID inventarioId = null;

            // Intentar buscar la variante existente usando los valores actuales del formulario
            inventarioId = inventarioDAO.obtenerIdVariante(
                    zapatoEditando.getZapatoId(),
                    cmbGenero.getValue(),
                    cmbSistemaTalla.getValue(),
                    txtTalla.getText().trim(),
                    txtColor.getText().trim()
            );

            if (inventarioId != null) {
                // La variante ya existe con estos valores, solo actualizar stock
                inventarioDAO.actualizarVariante(
                        inventarioId,
                        cmbGenero.getValue(),
                        cmbSistemaTalla.getValue(),
                        txtTalla.getText().trim(),
                        txtColor.getText().trim(),
                        stock
                );
                lblMensaje.setStyle("-fx-text-fill: green;");
                lblMensaje.setText("¡Zapato y variante actualizados exitosamente!");
            } else {
                // La variante no existe, crearla nueva
                inventarioDAO.guardarVariante(
                        zapatoEditando.getZapatoId(),
                        cmbGenero.getValue(),
                        cmbSistemaTalla.getValue(),
                        txtTalla.getText().trim(),
                        txtColor.getText().trim(),
                        stock
                );
                lblMensaje.setStyle("-fx-text-fill: green;");
                lblMensaje.setText("¡Zapato actualizado y variante creada exitosamente!");
            }

            // Cerrar la ventana tras 1.5 segundos
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(this::handleCancelar);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Error al actualizar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCrearVariante() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ni/uam/edu/tiendazapatos/RegistroZapato.fxml"));
            Parent root = loader.load();
            RegistroZapatoController controller = loader.getController();

            controller.prellenarDatos(zapatoEditando);

            Stage stage = new Stage();
            stage.setTitle("Crear Nueva Variante (Clonar)");
            stage.setScene(new Scene(root, 500, 650));
            stage.show();

            handleCancelar();
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al abrir el formulario de clonación.");
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
        } catch (IOException e) { e.printStackTrace(); }
    }
}