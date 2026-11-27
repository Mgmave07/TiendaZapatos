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
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;
import ni.uam.edu.tiendazapatos.dao.InventarioDAO;
import ni.uam.edu.tiendazapatos.dao.MarcaDAO;
import ni.uam.edu.tiendazapatos.dao.ZapatoDAO;
import ni.uam.edu.tiendazapatos.modelo.InventarioFila;
import ni.uam.edu.tiendazapatos.modelo.Marca;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

    // Filtros separados
    @FXML private TextField txtModelo;
    @FXML private ComboBox<String> cmbMarca;
    @FXML private TextField txtTalla;
    @FXML private ComboBox<String> cmbStock;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private TextField txtPrecioMin;
    @FXML private TextField txtPrecioMax;

    @FXML private Button btnVerDetalles;
    @FXML private Button btnEditar;
    @FXML private Button btnToggleActivo;
    @FXML private Button btnAgregarZapato;
    @FXML private Button btnCerrarSesion;

    private final ZapatoDAO zapatoDAO = new ZapatoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final MarcaDAO marcaDAO = new MarcaDAO();
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

        configurarFiltros();

        // Control de permisos según rol de usuario
        boolean esAdmin = SesionUsuario.getInstancia().estaAutenticado()
                && SesionUsuario.getInstancia().esAdministrador();
        if (!esAdmin) {
            if (btnEditar != null) { btnEditar.setVisible(false); btnEditar.setManaged(false); }
            if (btnToggleActivo != null) { btnToggleActivo.setVisible(false); btnToggleActivo.setManaged(false); }
            if (btnAgregarZapato != null) { btnAgregarZapato.setVisible(false); btnAgregarZapato.setManaged(false); }
        }
        if (btnCerrarSesion != null) {
            // El menú principal de la aplicación ya dispone de cierre de sesión
            btnCerrarSesion.setVisible(false);
            btnCerrarSesion.setManaged(false);
        }

        // Permitir ver detalles con doble clic en la tabla
        tablaZapatos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tablaZapatos.getSelectionModel().getSelectedItem() != null) {
                handleVerDetalles();
            }
        });

        SortedList<InventarioFila> ordenadas = new SortedList<>(filtradas);
        ordenadas.comparatorProperty().bind(tablaZapatos.comparatorProperty());
        tablaZapatos.setItems(ordenadas);

        cargarZapatos();
    }

    private void configurarFiltros() {
        cargarMarcas();

        cmbStock.getItems().setAll("Todos", "Disponibles (>0)", "Agotados (=0)", "Bajo stock (≤5)");
        cmbStock.getSelectionModel().selectFirst();

        cmbGenero.getItems().setAll("Todos", "HOMBRE", "MUJER", "UNISEX");
        cmbGenero.getSelectionModel().selectFirst();

        txtModelo.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        cmbMarca.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        txtTalla.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        cmbStock.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        cmbGenero.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());

        txtPrecioMin.textProperty().addListener((obs, anterior, nuevo) -> {
            if (!nuevo.matches("\\d*(\\.\\d*)?")) {
                txtPrecioMin.setText(anterior);
            } else {
                aplicarFiltros();
            }
        });

        txtPrecioMax.textProperty().addListener((obs, anterior, nuevo) -> {
            if (!nuevo.matches("\\d*(\\.\\d*)?")) {
                txtPrecioMax.setText(anterior);
            } else {
                aplicarFiltros();
            }
        });
    }

    private void cargarMarcas() {
        List<Marca> marcas = marcaDAO.obtenerTodas();
        List<String> nombresMarcas = new ArrayList<>();
        nombresMarcas.add("Todas");
        if (marcas != null) {
            for (Marca m : marcas) {
                if (m.getNombre() != null && !m.getNombre().isBlank()) {
                    nombresMarcas.add(m.getNombre());
                }
            }
        }
        cmbMarca.getItems().setAll(nombresMarcas);
        cmbMarca.getSelectionModel().selectFirst();
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
                                variante.inventarioId(),
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
        String modelo = txtModelo.getText() == null ? "" : txtModelo.getText().trim().toLowerCase(Locale.ROOT);
        String marcaSel = cmbMarca.getValue();
        String tallaFiltro = txtTalla.getText() == null ? "" : txtTalla.getText().trim().toLowerCase(Locale.ROOT);
        String stockSel = cmbStock.getValue();
        String generoSel = cmbGenero.getValue();
        String minStr = txtPrecioMin.getText() == null ? "" : txtPrecioMin.getText().trim();
        String maxStr = txtPrecioMax.getText() == null ? "" : txtPrecioMax.getText().trim();

        Double minPrecio = null;
        Double maxPrecio = null;
        try {
            if (!minStr.isEmpty()) minPrecio = Double.parseDouble(minStr);
        } catch (NumberFormatException ignored) {}
        try {
            if (!maxStr.isEmpty()) maxPrecio = Double.parseDouble(maxStr);
        } catch (NumberFormatException ignored) {}

        final Double finalMin = minPrecio;
        final Double finalMax = maxPrecio;

        filtradas.setPredicate(fila -> {
            // 1. Filtro por nombre de modelo
            if (!modelo.isEmpty()) {
                String nombreZapato = fila.zapato() != null && fila.zapato().getNombre() != null
                        ? fila.zapato().getNombre().toLowerCase(Locale.ROOT) : "";
                if (!nombreZapato.contains(modelo)) {
                    return false;
                }
            }

            // 2. Filtro por marca
            if (marcaSel != null && !"Todas".equalsIgnoreCase(marcaSel) && !marcaSel.isBlank()) {
                String marcaZapato = fila.zapato() != null && fila.zapato().getMarca() != null
                        ? fila.zapato().getMarca().getNombre() : "";
                if (!marcaSel.equalsIgnoreCase(marcaZapato)) {
                    return false;
                }
            }

            // 3. Filtro por talla
            if (!tallaFiltro.isEmpty()) {
                String tallaZapato = fila.talla() == null ? "" : fila.talla().toLowerCase(Locale.ROOT);
                if (!tallaZapato.contains(tallaFiltro)) {
                    return false;
                }
            }

            // 4. Filtro por stock
            if (stockSel != null && !"Todos".equalsIgnoreCase(stockSel)) {
                int stockVal = fila.stock() == null ? 0 : fila.stock();
                boolean activo = fila.zapato() != null && Boolean.TRUE.equals(fila.zapato().getActivo());

                if (stockSel.contains("Disponibles")) {
                    if (stockVal <= 0 || !activo) return false;
                } else if (stockSel.contains("Agotados")) {
                    if (stockVal > 0) return false;
                } else if (stockSel.contains("bajo") || stockSel.contains("Bajo")) {
                    if (stockVal <= 0 || stockVal > 5) return false;
                }
            }

            // 5. Filtro por rango de precio
            Double precioFinal = fila.zapato() != null && fila.zapato().getPrecioFinal() != null
                    ? fila.zapato().getPrecioFinal().doubleValue() : null;

            if (finalMin != null) {
                if (precioFinal == null || precioFinal < finalMin) {
                    return false;
                }
            }
            if (finalMax != null) {
                if (precioFinal == null || precioFinal > finalMax) {
                    return false;
                }
            }

            // 6. Filtro por género
            if (generoSel != null && !"Todos".equalsIgnoreCase(generoSel) && !generoSel.isBlank()) {
                if (!generoSel.equalsIgnoreCase(fila.genero())) {
                    return false;
                }
            }

            return true;
        });
    }

    @FXML
    private void handleLimpiarFiltros() {
        txtModelo.clear();
        if (cmbMarca.getItems().contains("Todas")) {
            cmbMarca.setValue("Todas");
        } else {
            cmbMarca.getSelectionModel().selectFirst();
        }
        txtTalla.clear();
        cmbStock.getSelectionModel().selectFirst();
        cmbGenero.getSelectionModel().selectFirst();
        txtPrecioMin.clear();
        txtPrecioMax.clear();
        aplicarFiltros();
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
            SesionUsuario.getInstancia().cerrar();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/ni/uam/edu/tiendazapatos/Login.fxml"));
            Scene scene = new Scene(loader.load(), 450, 500);
            Stage stage = (Stage) tablaZapatos.getScene().getWindow();
            stage.setTitle("Sistema Tienda de Zapatos - Login");
            stage.setScene(scene);
            stage.setMaximized(false);
            stage.centerOnScreen();
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

    @FXML
    private void handleToggleActivoZapato() {
        InventarioFila seleccion = tablaZapatos.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            new Alert(Alert.AlertType.WARNING, "Por favor, selecciona un zapato de la tabla primero.").showAndWait();
            return;
        }

        Zapato zapato = seleccion.zapato();
        boolean estadoActual = Boolean.TRUE.equals(zapato.getActivo());
        String accion = estadoActual ? "desactivar (marcar como no disponible)" : "activar (marcar como disponible)";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Acción");
        confirm.setHeaderText("¿Estás seguro de que deseas " + accion + " el modelo '" + zapato.getNombre() + "'?");
        confirm.setContentText(estadoActual
                ? "Al desactivarlo, no estará disponible para venta ni aparecerá en el punto de venta para los vendedores, pero se conservará su historial de ventas pasadas."
                : "Al activarlo, volverá a estar disponible en el inventario para la venta.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                zapato.setActivo(!estadoActual);
                zapatoDAO.actualizar(zapato);
                new Alert(Alert.AlertType.INFORMATION, "El calzado ha sido " + (!estadoActual ? "activado" : "desactivado") + " correctamente.").showAndWait();
                cargarZapatos();
            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("Error al cambiar estado", e.getMessage());
            }
        }
    }
}