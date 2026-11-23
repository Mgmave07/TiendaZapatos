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

public class VentasViewController {

    public record FilaCarrito(
            String descripcion,
            DetalleVenta detalle
    ) {}

    // Controles de Búsqueda de Productos por separado
    @FXML private TextField txtModelo;
    @FXML private ComboBox<String> cmbMarca;
    @FXML private TextField txtTalla;
    @FXML private ComboBox<String> cmbStock;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private TextField txtPrecioMin;
    @FXML private TextField txtPrecioMax;

    // Tabla de Productos (Idéntica a Gestión de Inventario)
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

    // Controles de Selección y Carrito
    @FXML private Label lblProductoSeleccionado;
    @FXML private TextField txtCantidad;
    @FXML private Label lblItemsCount;

    @FXML private TableView<FilaCarrito> tablaCarrito;
    @FXML private TableColumn<FilaCarrito, String> colProductoCarrito;
    @FXML private TableColumn<FilaCarrito, String> colPrecioCarrito;
    @FXML private TableColumn<FilaCarrito, Number> colCantidadCarrito;
    @FXML private TableColumn<FilaCarrito, String> colSubtotalCarrito;

    // Totales y Pago
    @FXML private ComboBox<String> cmbMetodoPago;
    @FXML private Label lblSubtotal;
    @FXML private Label lblImpuesto;
    @FXML private Label lblTotal;

    // DAOs y Listas
    private final ZapatoDAO zapatoDAO = new ZapatoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    private final ObservableList<InventarioFila> filasProductos = FXCollections.observableArrayList();
    private final FilteredList<InventarioFila> filtradas = new FilteredList<>(filasProductos, fila -> true);
    private final ObservableList<FilaCarrito> listaCarrito = FXCollections.observableArrayList();

    private double subtotalActual = 0.0;
    private double impuestoActual = 0.0;
    private double totalActual = 0.0;

    @FXML
    public void initialize() {
        configurarTablaProductos();
        configurarTablaCarrito();
        configurarFiltros();

        cmbMetodoPago.setItems(FXCollections.observableArrayList("Efectivo", "Tarjeta", "Transferencia"));
        cmbMetodoPago.getSelectionModel().selectFirst();

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

        // Actualizar información del producto seleccionado
        tablaProductos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccion) -> {
            if (seleccion == null) {
                lblProductoSeleccionado.setText("Seleccione un producto de la tabla para agregarlo al carrito");
            } else {
                String nombre = seleccion.zapato().getNombre();
                String marca = seleccion.zapato().getMarca() != null ? seleccion.zapato().getMarca().getNombre() : "Sin marca";
                String talla = seleccion.talla() != null ? "Talla " + seleccion.talla() : "Sin talla";
                String color = seleccion.color() != null ? seleccion.color() : "Sin color";
                String stockStr = seleccion.stock() != null ? "Stock: " + seleccion.stock() : "Agotado";
                String precioStr = seleccion.zapato().getPrecioFinal() != null ? "C$ " + seleccion.zapato().getPrecioFinal() : "—";

                lblProductoSeleccionado.setText(String.format("Seleccionado: %s (%s) | %s, %s | %s | %s",
                        nombre, marca, talla, color, stockStr, precioStr));
            }
        });

        // Doble clic en la tabla de productos para agregar automáticamente al carrito
        tablaProductos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tablaProductos.getSelectionModel().getSelectedItem() != null) {
                agregarAlCarrito();
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
            mostrarAlerta(Alert.AlertType.ERROR, "Error de inventario", "No se pudo consultar el inventario: " + e.getMessage());
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
    protected void agregarAlCarrito() {
        InventarioFila seleccion = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "Por favor, seleccione un producto de la tabla.");
            return;
        }

        if (!Boolean.TRUE.equals(seleccion.zapato().getActivo())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Producto inactivo", "Este producto está marcado como inactivo.");
            return;
        }

        if (seleccion.stock() == null || seleccion.stock() <= 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "Producto agotado", "Este producto no tiene existencias disponibles.");
            return;
        }

        UUID inventarioId = seleccion.inventarioId();
        if (inventarioId == null) {
            // Intentar recuperar ID por si la variante se registró externamente
            inventarioId = inventarioDAO.obtenerIdVariante(
                    seleccion.zapato().getZapatoId(),
                    seleccion.genero(),
                    seleccion.sistemaTalla(),
                    seleccion.talla(),
                    seleccion.color()
            );
        }

        if (inventarioId == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Variante no registrada", "El producto no tiene una variante válida en el inventario.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Cantidad inválida", "La cantidad debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Cantidad inválida", "Ingrese una cantidad numérica entera válida.");
            return;
        }

        final UUID idInvFinal = inventarioId;
        int yaEnCarrito = listaCarrito.stream()
                .filter(f -> f.detalle().getInventarioId().equals(idInvFinal))
                .mapToInt(f -> f.detalle().getCantidad())
                .sum();

        if (cantidad + yaEnCarrito > seleccion.stock()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Stock insuficiente",
                    "Solo hay " + seleccion.stock() + " unidades disponibles (ya hay " + yaEnCarrito + " en el carrito).");
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

        DetalleVenta detalle = new DetalleVenta();
        detalle.setInventarioId(idInvFinal);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(precioUnitario);
        detalle.setSubtotal(precioUnitario * cantidad);

        listaCarrito.add(new FilaCarrito(desc, detalle));
        actualizarTotales();
        txtCantidad.setText("1");
    }

    @FXML
    protected void quitarDelCarrito() {
        FilaCarrito seleccionada = tablaCarrito.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            listaCarrito.remove(seleccionada);
            actualizarTotales();
        } else {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "Seleccione un elemento del carrito para quitarlo.");
        }
    }

    @FXML
    protected void limpiarCarrito() {
        if (!listaCarrito.isEmpty()) {
            listaCarrito.clear();
            actualizarTotales();
        }
    }

    @FXML
    protected void facturarVenta() {
        if (listaCarrito.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Advertencia", "El carrito está vacío.");
            return;
        }

        List<DetalleVenta> detalles = new ArrayList<>();
        for (FilaCarrito fila : listaCarrito) {
            detalles.add(fila.detalle());
        }

        Optional<Venta> ventaOpt = DialogoFacturacion.mostrar(detalles, subtotalActual, impuestoActual, totalActual);
        if (ventaOpt.isPresent()) {
            Venta nuevaVenta = ventaOpt.get();
            boolean exito = ventaDAO.guardarVenta(nuevaVenta);
            if (exito) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Venta Exitosa",
                        "Factura " + nuevaVenta.getNumeroVenta() + " guardada y procesada correctamente.");
                listaCarrito.clear();
                actualizarTotales();
                cargarProductos();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "Hubo un problema al guardar la factura en la base de datos.");
            }
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

        lblItemsCount.setText(String.format("(%d %s)", totalItems, totalItems == 1 ? "artículo" : "artículos"));
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
