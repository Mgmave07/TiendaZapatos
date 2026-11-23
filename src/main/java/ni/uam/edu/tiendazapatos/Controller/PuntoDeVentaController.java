package ni.uam.edu.tiendazapatos.Controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.uam.edu.tiendazapatos.dao.InventarioDAO;
import ni.uam.edu.tiendazapatos.dao.MarcaDAO;
import ni.uam.edu.tiendazapatos.dao.VentaDAO;
import ni.uam.edu.tiendazapatos.dao.ZapatoDAO;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.InventarioFila;
import ni.uam.edu.tiendazapatos.modelo.Marca;
import ni.uam.edu.tiendazapatos.modelo.Venta;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.util.*;

public class PuntoDeVentaController {

    public record FilaCarrito(String descripcion, DetalleVenta detalle) {}

    // ── FXML filtros ──────────────────────────────────────────────────────────
    @FXML private TextField txtModelo;
    @FXML private ComboBox<String> cmbMarca;
    @FXML private TextField txtTalla;
    @FXML private ComboBox<String> cmbStock;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private TextField txtPrecioMin;
    @FXML private TextField txtPrecioMax;

    // ── FXML tabla productos ──────────────────────────────────────────────────
    @FXML private TableView<InventarioFila> tablaProductos;
    @FXML private TableColumn<InventarioFila, String> colNombre;
    @FXML private TableColumn<InventarioFila, String> colMarca;
    @FXML private TableColumn<InventarioFila, String> colGenero;
    @FXML private TableColumn<InventarioFila, String> colSistemaTalla;
    @FXML private TableColumn<InventarioFila, String> colTalla;
    @FXML private TableColumn<InventarioFila, String> colColor;
    @FXML private TableColumn<InventarioFila, String> colStock;
    @FXML private TableColumn<InventarioFila, String> colPrecio;
    @FXML private TableColumn<InventarioFila, String> colEstado;

    // ── FXML carrito ──────────────────────────────────────────────────────────
    @FXML private Label lblProductoSeleccionado;
    @FXML private TextField txtCantidad;
    @FXML private Label lblItemsCount;
    @FXML private TableView<FilaCarrito> tablaCarrito;
    @FXML private TableColumn<FilaCarrito, String> colProductoCarrito;
    @FXML private TableColumn<FilaCarrito, Number> colCantidadCarrito;
    @FXML private TableColumn<FilaCarrito, String> colPrecioCarrito;
    @FXML private TableColumn<FilaCarrito, String> colSubtotalCarrito;
    @FXML private Label lblSubtotal;
    @FXML private Label lblImpuesto;
    @FXML private Label lblTotal;

    // ── Estado interno ────────────────────────────────────────────────────────
    private final ZapatoDAO zapatoDAO = new ZapatoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    private final ObservableList<InventarioFila> filasProductos = FXCollections.observableArrayList();
    private final FilteredList<InventarioFila> filtradas = new FilteredList<>(filasProductos, p -> true);
    private final ObservableList<FilaCarrito> listaCarrito = FXCollections.observableArrayList();

    private double subtotalActual = 0.0;
    private double impuestoActual = 0.0;
    private double totalActual = 0.0;

    @FXML
    public void initialize() {
        configurarTablaProductos();
        configurarTablaCarrito();
        configurarFiltros();
        cargarProductos();
    }

    private void configurarTablaProductos() {
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
        colEstado.setCellValueFactory(c -> texto(estado(c.getValue())));

        SortedList<InventarioFila> ordenadas = new SortedList<>(filtradas);
        ordenadas.comparatorProperty().bind(tablaProductos.comparatorProperty());
        tablaProductos.setItems(ordenadas);

        tablaProductos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccion) -> {
            if (seleccion == null) {
                lblProductoSeleccionado.setText("Selecciona un producto de la tabla");
            } else {
                String nombre = seleccion.zapato().getNombre();
                String marca = seleccion.zapato().getMarca() != null ? seleccion.zapato().getMarca().getNombre() : "Sin marca";
                String talla = seleccion.talla() != null ? "Talla " + seleccion.talla() : "Sin talla";
                String color = seleccion.color() != null ? seleccion.color() : "Sin color";
                String stockStr = seleccion.stock() != null ? "Stock: " + seleccion.stock() : "Agotado";
                String precioStr = seleccion.zapato().getPrecioFinal() != null ? "C$ " + seleccion.zapato().getPrecioFinal() : "—";

                lblProductoSeleccionado.setText(String.format("%s (%s) | %s, %s | %s | %s",
                        nombre, marca, talla, color, stockStr, precioStr));
            }
        });

        tablaProductos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tablaProductos.getSelectionModel().getSelectedItem() != null) {
                handleAgregarCarrito();
            }
        });
    }

    private void configurarTablaCarrito() {
        colProductoCarrito.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().descripcion()));
        colPrecioCarrito.setCellValueFactory(data -> new SimpleStringProperty(
                String.format(Locale.US, "C$ %.2f", data.getValue().detalle().getPrecioUnitario())));
        colCantidadCarrito.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().detalle().getCantidad()));
        colSubtotalCarrito.setCellValueFactory(data -> new SimpleStringProperty(
                String.format(Locale.US, "C$ %.2f", data.getValue().detalle().getSubtotal())));
        tablaCarrito.setItems(listaCarrito);
    }

    private void configurarFiltros() {
        cargarMarcas();

        cmbStock.getItems().setAll("Todos", "Disponibles (>0)", "Agotados (=0)", "Bajo stock (≤5)");
        cmbStock.setValue("Disponibles (>0)");

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

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private void cargarProductos() {
        try {
            List<Zapato> zapatos = zapatoDAO.obtenerTodos();
            Map<UUID, List<InventarioDAO.Variante>> variantesPorZapato = new HashMap<>();
            for (InventarioDAO.Variante variante : inventarioDAO.obtenerVariantes()) {
                variantesPorZapato.computeIfAbsent(variante.zapatoId(), id -> new ArrayList<>())
                        .add(variante);
            }

            filasProductos.clear();
            for (Zapato zapato : zapatos) {
                List<InventarioDAO.Variante> variantes = variantesPorZapato.get(zapato.getZapatoId());
                if (variantes == null || variantes.isEmpty()) {
                    filasProductos.add(new InventarioFila(zapato, null, null, null, null, null));
                } else {
                    for (InventarioDAO.Variante variante : variantes) {
                        filasProductos.add(new InventarioFila(
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
            mostrarAlerta("Error al cargar productos: " + e.getMessage(), Alert.AlertType.ERROR);
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
            if (!modelo.isEmpty()) {
                String nombreZapato = fila.zapato() != null && fila.zapato().getNombre() != null
                        ? fila.zapato().getNombre().toLowerCase(Locale.ROOT) : "";
                if (!nombreZapato.contains(modelo)) return false;
            }

            if (marcaSel != null && !"Todas".equalsIgnoreCase(marcaSel) && !marcaSel.isBlank()) {
                String marcaZapato = fila.zapato() != null && fila.zapato().getMarca() != null
                        ? fila.zapato().getMarca().getNombre() : "";
                if (!marcaSel.equalsIgnoreCase(marcaZapato)) return false;
            }

            if (!tallaFiltro.isEmpty()) {
                String tallaZapato = fila.talla() == null ? "" : fila.talla().toLowerCase(Locale.ROOT);
                if (!tallaZapato.contains(tallaFiltro)) return false;
            }

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

            Double precioFinal = fila.zapato() != null && fila.zapato().getPrecioFinal() != null
                    ? fila.zapato().getPrecioFinal().doubleValue() : null;

            if (finalMin != null) {
                if (precioFinal == null || precioFinal < finalMin) return false;
            }
            if (finalMax != null) {
                if (precioFinal == null || precioFinal > finalMax) return false;
            }

            if (generoSel != null && !"Todos".equalsIgnoreCase(generoSel) && !generoSel.isBlank()) {
                if (!generoSel.equalsIgnoreCase(fila.genero())) return false;
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
        if (cmbStock.getItems().contains("Disponibles (>0)")) {
            cmbStock.setValue("Disponibles (>0)");
        } else {
            cmbStock.getSelectionModel().selectFirst();
        }
        cmbGenero.getSelectionModel().selectFirst();
        txtPrecioMin.clear();
        txtPrecioMax.clear();
        aplicarFiltros();
    }

    @FXML
    private void handleActualizar() {
        cargarProductos();
    }

    @FXML
    protected void handleAgregarCarrito() {
        InventarioFila seleccion = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            mostrarAlerta("Por favor, selecciona un producto de la tabla.", Alert.AlertType.WARNING);
            return;
        }

        if (!Boolean.TRUE.equals(seleccion.zapato().getActivo())) {
            mostrarAlerta("Este producto está marcado como inactivo.", Alert.AlertType.WARNING);
            return;
        }

        if (seleccion.stock() == null || seleccion.stock() <= 0) {
            mostrarAlerta("Este producto no tiene existencias disponibles.", Alert.AlertType.WARNING);
            return;
        }

        UUID inventarioId = seleccion.inventarioId();
        if (inventarioId == null) {
            inventarioId = inventarioDAO.obtenerIdVariante(
                    seleccion.zapato().getZapatoId(),
                    seleccion.genero(),
                    seleccion.sistemaTalla(),
                    seleccion.talla(),
                    seleccion.color()
            );
        }

        if (inventarioId == null) {
            mostrarAlerta("El producto no tiene una variante válida en el inventario.", Alert.AlertType.WARNING);
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) {
                mostrarAlerta("La cantidad debe ser mayor a 0.", Alert.AlertType.WARNING);
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Ingresa una cantidad numérica entera válida.", Alert.AlertType.ERROR);
            return;
        }

        final UUID idInvFinal = inventarioId;
        int yaEnCarrito = listaCarrito.stream()
                .filter(f -> f.detalle().getInventarioId().equals(idInvFinal))
                .mapToInt(f -> f.detalle().getCantidad())
                .sum();

        if (cantidad + yaEnCarrito > seleccion.stock()) {
            mostrarAlerta("Solo hay " + seleccion.stock() + " unidades disponibles (ya hay " + yaEnCarrito + " en el carrito).", Alert.AlertType.WARNING);
            return;
        }

        double precioUnitario = seleccion.zapato().getPrecioFinal() != null
                ? seleccion.zapato().getPrecioFinal().doubleValue()
                : 0.0;

        for (int i = 0; i < listaCarrito.size(); i++) {
            FilaCarrito existente = listaCarrito.get(i);
            if (existente.detalle().getInventarioId().equals(idInvFinal)) {
                DetalleVenta d = existente.detalle();
                int nuevaCant = d.getCantidad() + cantidad;
                d.setCantidad(nuevaCant);
                d.setSubtotal(nuevaCant * d.getPrecioUnitario());
                listaCarrito.set(i, new FilaCarrito(existente.descripcion(), d));
                actualizarTotales();
                txtCantidad.setText("1");
                return;
            }
        }

        String desc = seleccion.zapato().getNombre() + " (" +
                valor(seleccion.genero()) + ", " +
                valor(seleccion.sistemaTalla()) + " " +
                valor(seleccion.talla()) + ", " +
                valor(seleccion.color()) + ")";

        DetalleVenta nuevoDetalle = new DetalleVenta();
        nuevoDetalle.setInventarioId(inventarioId);
        nuevoDetalle.setCantidad(cantidad);
        nuevoDetalle.setPrecioUnitario(precioUnitario);
        nuevoDetalle.setSubtotal(cantidad * precioUnitario);

        listaCarrito.add(new FilaCarrito(desc, nuevoDetalle));
        actualizarTotales();
        txtCantidad.setText("1");
    }

    @FXML
    protected void handleLimpiarCarrito() {
        if (!listaCarrito.isEmpty()) {
            listaCarrito.clear();
            actualizarTotales();
            lblProductoSeleccionado.setText("Selecciona un producto de la tabla");
        }
    }

    private void actualizarTotales() {
        subtotalActual = 0.0;
        int totalItems = 0;
        for (FilaCarrito fila : listaCarrito) {
            subtotalActual += fila.detalle().getSubtotal();
            totalItems += fila.detalle().getCantidad();
        }
        impuestoActual = subtotalActual * 0.15;
        totalActual = subtotalActual + impuestoActual;

        lblSubtotal.setText(String.format(Locale.US, "C$ %.2f", subtotalActual));
        lblImpuesto.setText(String.format(Locale.US, "C$ %.2f", impuestoActual));
        lblTotal.setText(String.format(Locale.US, "C$ %.2f", totalActual));

        lblItemsCount.setText(String.format("%d %s", totalItems, totalItems == 1 ? "ítem" : "ítems"));
    }

    @FXML
    protected void handleProcederCobro() {
        if (listaCarrito.isEmpty()) {
            mostrarAlerta("El carrito está vacío. Agrega productos antes de cobrar.", Alert.AlertType.WARNING);
            return;
        }

        List<DetalleVenta> detalles = new ArrayList<>();
        for (FilaCarrito fila : listaCarrito) {
            detalles.add(fila.detalle());
        }

        Optional<Venta> resultado = DialogoFacturacion.mostrar(detalles, subtotalActual, impuestoActual, totalActual);
        resultado.ifPresent(venta -> {
            boolean ok = ventaDAO.guardarVenta(venta);
            if (ok) {
                mostrarAlerta("Factura " + venta.getNumeroVenta() + " registrada y procesada exitosamente.\nTotal: "
                        + String.format(Locale.US, "C$ %.2f", totalActual), Alert.AlertType.INFORMATION);
                handleLimpiarCarrito();
                cargarProductos();
            } else {
                mostrarAlerta("Hubo un error al guardar la venta en la base de datos.", Alert.AlertType.ERROR);
            }
        });
    }

    private void mostrarAlerta(String mensaje, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
