package ni.uam.edu.tiendazapatos.Controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.dao.InventarioDAO;
import ni.uam.edu.tiendazapatos.dao.ZapatoDAO;
import ni.uam.edu.tiendazapatos.modelo.InventarioFila;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ZapatosViewController {

    @FXML private TableView<InventarioFila> tablaZapatos;
    @FXML private TableColumn<InventarioFila, String> colNombre;
    @FXML private TableColumn<InventarioFila, String> colMarca;
    @FXML private TableColumn<InventarioFila, String> colGenero;
    @FXML private TableColumn<InventarioFila, String> colSistemaTalla;
    @FXML private TableColumn<InventarioFila, String> colTalla;
    @FXML private TableColumn<InventarioFila, String> colColor;
    @FXML private TableColumn<InventarioFila, String> colStock;
    @FXML private TableColumn<InventarioFila, String> colPrecio;
    @FXML private TableColumn<InventarioFila, String> colVentas;
    @FXML private TableColumn<InventarioFila, String> colEstado;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private CheckBox chkSoloDisponibles;
    @FXML private Button btnVerDetalles;
    @FXML private Button btnEditar;

    private final ZapatoDAO zapatoDAO = new ZapatoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final ObservableList<InventarioFila> filas = FXCollections.observableArrayList();
    private final FilteredList<InventarioFila> filtradas = new FilteredList<>(filas, fila -> true);

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(c -> texto(c.getValue().zapato().getNombre()));
        colMarca.setCellValueFactory(c -> texto(
                c.getValue().zapato().getMarca() == null ? "Sin marca" :
                        c.getValue().zapato().getMarca().getNombre()));
        colGenero.setCellValueFactory(c -> texto(c.getValue().genero()));
        colSistemaTalla.setCellValueFactory(c -> texto(c.getValue().sistemaTalla()));
        colTalla.setCellValueFactory(c -> texto(c.getValue().talla()));
        colColor.setCellValueFactory(c -> texto(c.getValue().color()));
        colStock.setCellValueFactory(c -> texto(c.getValue().stock() == null
                ? "Sin registrar" : c.getValue().stock().toString()));
        colPrecio.setCellValueFactory(c -> texto(c.getValue().zapato().getPrecioFinal() == null
                ? "—" : "C$ " + c.getValue().zapato().getPrecioFinal()));
        colVentas.setCellValueFactory(c -> texto(c.getValue().zapato().getVolumenVentas() == null
                ? "0" : c.getValue().zapato().getVolumenVentas().toString()));
        colEstado.setCellValueFactory(c -> texto(estado(c.getValue())));

        cmbGenero.getItems().setAll("Todos", "HOMBRE", "MUJER", "UNISEX");
        cmbGenero.getSelectionModel().selectFirst();
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        cmbGenero.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        chkSoloDisponibles.selectedProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());

        SortedList<InventarioFila> ordenadas = new SortedList<>(filtradas);
        ordenadas.comparatorProperty().bind(tablaZapatos.comparatorProperty());
        tablaZapatos.setItems(ordenadas);

        cargarZapatos();
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor == null || valor.isBlank() ? "—" : valor);
    }

    private static String estado(InventarioFila fila) {
        if (!Boolean.TRUE.equals(fila.zapato().getActivo())) return "Inactivo";
        if (fila.stock() == null) return "Sin registro";
        return fila.stock() > 0 ? "Disponible" : "Agotado";
    }

    private void cargarZapatos() {
        try {
            List<Zapato> zapatos = zapatoDAO.obtenerTodos();
            Map<UUID, List<InventarioDAO.Variante>> variantesPorZapato = new HashMap<>();
            for (InventarioDAO.Variante variante : inventarioDAO.obtenerVariantes()) {
                variantesPorZapato.computeIfAbsent(variante.zapatoId(), id -> new ArrayList<>())
                        .add(variante);
            }

            filas.clear();
            for (Zapato zapato : zapatos) {
                List<InventarioDAO.Variante> variantes = variantesPorZapato.get(zapato.getZapatoId());
                if (variantes == null || variantes.isEmpty()) {
                    filas.add(new InventarioFila(zapato, null, null, null, null, null));
                } else {
                    for (InventarioDAO.Variante variante : variantes) {
                        filas.add(new InventarioFila(
                                zapato,
                                variante.genero(),
                                variante.sistemaTalla(),
                                variante.talla(),
                                variante.color(),
                                variante.cantidadStock()
                        ));
                    }
                }
            }
            aplicarFiltros();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No se pudo consultar el inventario", e.getMessage());
        }
    }

    private void aplicarFiltros() {
        String buscar = txtBuscar.getText() == null ? "" :
                txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        String genero = cmbGenero.getValue();
        boolean soloDisponibles = chkSoloDisponibles.isSelected();

        filtradas.setPredicate(fila -> {
            if (genero != null && !"Todos".equals(genero)
                    && !genero.equalsIgnoreCase(fila.genero())) return false;
            if (soloDisponibles && (fila.stock() == null || fila.stock() <= 0
                    || !Boolean.TRUE.equals(fila.zapato().getActivo()))) return false;

            String contenido = String.join(" ",
                    valor(fila.zapato().getNombre()),
                    fila.zapato().getMarca() == null ? "" : valor(fila.zapato().getMarca().getNombre()),
                    valor(fila.talla()), valor(fila.color()),
                    valor(fila.genero()), valor(fila.sistemaTalla()),
                    fila.zapato().getZapatoId().toString()
            ).toLowerCase(Locale.ROOT);
            return contenido.contains(buscar);
        });
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private Zapato zapatoSeleccionado() {
        InventarioFila seleccion = tablaZapatos.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Por favor, selecciona un zapato de la tabla primero.").showAndWait();
            return null;
        }
        return seleccion.zapato();
    }

    private void mostrarError(String titulo, String detalle) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error de inventario");
        alerta.setHeaderText(titulo);
        alerta.setContentText(detalle == null ? "Revisa la consola de IntelliJ." : detalle);
        alerta.showAndWait();
    }

    @FXML
    private void handleActualizar() {
        cargarZapatos();
    }

    @FXML
    private void handleAgregarZapato() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/ni/uam/edu/tiendazapatos/RegistroZapato.fxml"));
            Scene scene = new Scene(loader.load(), 450, 550);
            Stage stage = (Stage) tablaZapatos.getScene().getWindow();
            stage.setTitle("Registrar Nuevo Zapato");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("No se pudo abrir el registro", e.getMessage());
        }
    }

    @FXML
    private void handleCerrarSesion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/ni/uam/edu/tiendazapatos/Login.fxml"));
            Scene scene = new Scene(loader.load(), 450, 500);
            Stage stage = (Stage) tablaZapatos.getScene().getWindow();
            stage.setTitle("Sistema Tienda de Zapatos - Login");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("No se pudo regresar al login", e.getMessage());
        }
    }

    @FXML
    private void handleVerDetalles() {
        Zapato seleccionado = zapatoSeleccionado();
        if (seleccionado == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/ni/uam/edu/tiendazapatos/DetalleZapato.fxml"));
            Scene scene = new Scene(loader.load(), 500, 600);
            DetalleZapatoController controller = loader.getController();
            InventarioFila filaSeleccionada = tablaZapatos.getSelectionModel().getSelectedItem();
            controller.setZapato(seleccionado, filaSeleccionada.genero(), filaSeleccionada.color());
            Stage stage = new Stage();
            stage.setTitle("Detalles del Zapato");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("No se pudieron abrir los detalles", e.getMessage());
        }
    }

    @FXML
    private void handleEditarZapato() {
        InventarioFila seleccion = tablaZapatos.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            new Alert(Alert.AlertType.WARNING, "Por favor, selecciona un zapato de la tabla primero.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ni/uam/edu/tiendazapatos/EditarZapato.fxml"));
            Scene scene = new Scene(loader.load(), 500, 650);
            EditarZapatoController controller = loader.getController();

            // CAMBIO CLAVE: Pasamos el zapato Y la fila completa
            controller.setDatos(seleccion.zapato(), seleccion);

            Stage stage = (Stage) tablaZapatos.getScene().getWindow();
            stage.setTitle("Editar Zapato");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("No se pudo abrir la edición", e.getMessage());
        }
    }
}