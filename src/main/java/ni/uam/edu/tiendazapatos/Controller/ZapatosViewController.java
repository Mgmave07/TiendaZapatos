package ni.uam.edu.tiendazapatos.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.dao.ZapatoDAO;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.io.IOException;
import java.util.List;

public class ZapatosViewController {
    @FXML private TableView<Zapato> tablaZapatos;
    @FXML private TableColumn<Zapato, String> colNombre;
    @FXML private TableColumn<Zapato, String> colMarca;
    @FXML private TableColumn<Zapato, String> colPrecio;
    @FXML private TableColumn<Zapato, String> colEstado;
    @FXML private Button btnVerDetalles;
    @FXML private Button btnEditar;

    private final ZapatoDAO zapatoDAO = new ZapatoDAO();

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colMarca.setCellValueFactory(cellData -> cellData.getValue().getMarca() != null ?
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMarca().getNombre()) :
                new javafx.beans.property.SimpleStringProperty("Sin marca"));
        colPrecio.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty("C$" + cellData.getValue().getPrecioFinal().toString()));
        colEstado.setCellValueFactory(cellData -> {
            Boolean activo = cellData.getValue().getActivo();
            String textoEstado = (activo != null && activo) ? "Disponible" : "No disponible";
            return new javafx.beans.property.SimpleStringProperty(textoEstado);
        });

        cargarZapatos();
    }

    private void cargarZapatos() {
        List<Zapato> zapatos = zapatoDAO.obtenerTodos();
        tablaZapatos.getItems().setAll(zapatos);
    }

    @FXML
    private void handleAgregarZapato() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ni/uam/edu/tiendazapatos/RegistroZapato.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 550);
            Stage stage = (Stage) tablaZapatos.getScene().getWindow();
            stage.setTitle("Registrar Nuevo Zapato");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCerrarSesion() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 500);
            Stage stage = (Stage) tablaZapatos.getScene().getWindow();
            stage.setTitle("Sistema Tienda de Zapatos - Login");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleVerDetalles() {
        Zapato seleccionado = tablaZapatos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Advertencia");
            alert.setHeaderText(null);
            alert.setContentText("Por favor, selecciona un zapato de la tabla primero.");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ni/uam/edu/tiendazapatos/DetalleZapato.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 500, 600);

            // Pasar el zapato seleccionado al controlador de detalles
            DetalleZapatoController controller = fxmlLoader.getController();
            controller.setZapato(seleccionado);

            Stage stage = new Stage();
            stage.setTitle("Detalles del Zapato");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleEditarZapato() {
        Zapato seleccionado = tablaZapatos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Advertencia");
            alert.setHeaderText(null);
            alert.setContentText("Por favor, selecciona un zapato de la tabla primero.");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ni/uam/edu/tiendazapatos/EditarZapato.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 550);

            EditarZapatoController controller = fxmlLoader.getController();
            controller.setZapato(seleccionado);

            Stage stage = (Stage) tablaZapatos.getScene().getWindow();
            stage.setTitle("Editar Zapato");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}