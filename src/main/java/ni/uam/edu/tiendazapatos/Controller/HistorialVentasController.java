package ni.uam.edu.tiendazapatos.Controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.uam.edu.tiendazapatos.dao.VentaDAO;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.Venta;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class HistorialVentasController {

    public record FilaDetalle(
            String producto,
            int cantidad,
            double precioUnitario,
            double subtotal
    ) {}

    @FXML private Label lblKpiTotalFacturado;
    @FXML private Label lblKpiCantidadVentas;

    // Filtros separados
    @FXML private TextField txtBuscarFactura;
    @FXML private ComboBox<String> cmbMetodoPago;
    @FXML private ComboBox<String> cmbEstadoVenta;
    @FXML private DatePicker dpFechaDesde;
    @FXML private DatePicker dpFechaHasta;

    // Tabla de Ventas (Facturas)
    @FXML private TableView<Venta> tablaVentas;
    @FXML private TableColumn<Venta, String> colNumeroVenta;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, String> colMetodoPago;
    @FXML private TableColumn<Venta, String> colSubtotal;
    @FXML private TableColumn<Venta, String> colImpuesto;
    @FXML private TableColumn<Venta, String> colTotal;
    @FXML private TableColumn<Venta, String> colEstado;
    @FXML private TableColumn<Venta, Number> colCantArticulos;

    // Tabla de Desglose de Productos
    @FXML private Label lblDetalleFactura;
    @FXML private TableView<FilaDetalle> tablaDetalles;
    @FXML private TableColumn<FilaDetalle, String> colDetalleProducto;
    @FXML private TableColumn<FilaDetalle, Number> colDetalleCantidad;
    @FXML private TableColumn<FilaDetalle, String> colDetallePrecioUnit;
    @FXML private TableColumn<FilaDetalle, String> colDetalleSubtotal;

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ObservableList<Venta> listaVentas = FXCollections.observableArrayList();
    private final FilteredList<Venta> ventasFiltradas = new FilteredList<>(listaVentas, v -> true);
    private final ObservableList<FilaDetalle> listaDetalles = FXCollections.observableArrayList();

    private Map<UUID, String> mapaInventario = new HashMap<>();
    private static final SimpleDateFormat FORMATO_FECHA_HORA = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        configurarTablaVentas();
        configurarTablaDetalles();
        configurarFiltros();
        cargarDatos();
    }

    private void configurarTablaVentas() {
        colNumeroVenta.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroVenta()));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaVenta() != null ? FORMATO_FECHA_HORA.format(c.getValue().getFechaVenta()) : "—"));
        colMetodoPago.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getMetodoPago() != null ? c.getValue().getMetodoPago() : "—"));
        colSubtotal.setCellValueFactory(c -> new SimpleStringProperty(
                String.format(Locale.US, "C$ %.2f", c.getValue().getSubtotal())));
        colImpuesto.setCellValueFactory(c -> new SimpleStringProperty(
                String.format(Locale.US, "C$ %.2f", c.getValue().getImpuesto())));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
                String.format(Locale.US, "C$ %.2f", c.getValue().getTotal())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado() != null ? c.getValue().getEstado() : "PAGADA"));

        colCantArticulos.setCellValueFactory(c -> {
            int suma = 0;
            if (c.getValue().getDetalles() != null) {
                for (DetalleVenta d : c.getValue().getDetalles()) {
                    suma += d.getCantidad();
                }
            }
            return new SimpleIntegerProperty(suma);
        });

        // Estilo para la columna de estado
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("PAGADA".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    } else if ("CANCELADA".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #eab308; -fx-font-weight: bold;");
                    }
                }
            }
        });

        SortedList<Venta> ordenadas = new SortedList<>(ventasFiltradas);
        ordenadas.comparatorProperty().bind(tablaVentas.comparatorProperty());
        tablaVentas.setItems(ordenadas);

        // Al seleccionar una venta de la tabla, mostrar su desglose de productos abajo
        tablaVentas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccion) -> {
            mostrarDetallesVenta(seleccion);
        });
    }

    private void configurarTablaDetalles() {
        colDetalleProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().producto()));
        colDetalleCantidad.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().cantidad()));
        colDetallePrecioUnit.setCellValueFactory(c -> new SimpleStringProperty(
                String.format(Locale.US, "C$ %.2f", c.getValue().precioUnitario())));
        colDetalleSubtotal.setCellValueFactory(c -> new SimpleStringProperty(
                String.format(Locale.US, "C$ %.2f", c.getValue().subtotal())));

        tablaDetalles.setItems(listaDetalles);
    }

    private void configurarFiltros() {
        cmbMetodoPago.getItems().setAll("Todos los métodos", "Efectivo", "Tarjeta", "Transferencia");
        cmbMetodoPago.getSelectionModel().selectFirst();

        cmbEstadoVenta.getItems().setAll("Todos los estados", "PAGADA", "PENDIENTE", "CANCELADA");
        cmbEstadoVenta.getSelectionModel().selectFirst();

        txtBuscarFactura.textProperty().addListener((obs, ant, nuevo) -> aplicarFiltros());
        cmbMetodoPago.valueProperty().addListener((obs, ant, nuevo) -> aplicarFiltros());
        cmbEstadoVenta.valueProperty().addListener((obs, ant, nuevo) -> aplicarFiltros());
        dpFechaDesde.valueProperty().addListener((obs, ant, nuevo) -> aplicarFiltros());
        dpFechaHasta.valueProperty().addListener((obs, ant, nuevo) -> aplicarFiltros());
    }

    public void cargarDatos() {
        try {
            mapaInventario = ventaDAO.obtenerDescripcionesInventario();
            List<Venta> ventas = ventaDAO.obtenerTodas();
            listaVentas.setAll(ventas);
            aplicarFiltros();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo consultar el historial de ventas: " + e.getMessage());
        }
    }

    private void aplicarFiltros() {
        String numFactura = txtBuscarFactura.getText() == null ? "" : txtBuscarFactura.getText().trim().toLowerCase(Locale.ROOT);
        String metodoSel = cmbMetodoPago.getValue();
        String estadoSel = cmbEstadoVenta.getValue();
        LocalDate desde = dpFechaDesde.getValue();
        LocalDate hasta = dpFechaHasta.getValue();

        ventasFiltradas.setPredicate(v -> {
            // 1. Filtro por número de factura
            if (!numFactura.isEmpty()) {
                String num = v.getNumeroVenta() != null ? v.getNumeroVenta().toLowerCase(Locale.ROOT) : "";
                if (!num.contains(numFactura)) {
                    return false;
                }
            }

            // 2. Filtro por método de pago
            if (metodoSel != null && !"Todos los métodos".equalsIgnoreCase(metodoSel)) {
                if (!metodoSel.equalsIgnoreCase(v.getMetodoPago())) {
                    return false;
                }
            }

            // 3. Filtro por estado
            if (estadoSel != null && !"Todos los estados".equalsIgnoreCase(estadoSel)) {
                if (!estadoSel.equalsIgnoreCase(v.getEstado())) {
                    return false;
                }
            }

            // 4. Filtro por fecha desde / hasta
            if (v.getFechaVenta() != null) {
                LocalDate fechaVentaLocal = v.getFechaVenta().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (desde != null && fechaVentaLocal.isBefore(desde)) {
                    return false;
                }
                if (hasta != null && fechaVentaLocal.isAfter(hasta)) {
                    return false;
                }
            }

            return true;
        });

        actualizarKpis();
    }

    private void actualizarKpis() {
        double totalFacturado = 0.0;
        int cantidad = ventasFiltradas.size();
        for (Venta v : ventasFiltradas) {
            totalFacturado += v.getTotal();
        }

        lblKpiTotalFacturado.setText(String.format(Locale.US, "C$ %.2f", totalFacturado));
        lblKpiCantidadVentas.setText(String.format("%d %s", cantidad, cantidad == 1 ? "venta" : "ventas"));
    }

    private void mostrarDetallesVenta(Venta venta) {
        listaDetalles.clear();
        if (venta == null) {
            lblDetalleFactura.setText("(Seleccione una factura de arriba para ver sus productos)");
            return;
        }

        String fechaStr = venta.getFechaVenta() != null ? FORMATO_FECHA_HORA.format(venta.getFechaVenta()) : "";
        lblDetalleFactura.setText(String.format("Factura: %s | Fecha: %s | Total: C$ %.2f | Método: %s",
                venta.getNumeroVenta(), fechaStr, venta.getTotal(), venta.getMetodoPago()));

        if (venta.getDetalles() != null) {
            for (DetalleVenta d : venta.getDetalles()) {
                String desc = mapaInventario.get(d.getInventarioId());
                if (desc == null || desc.isBlank()) {
                    desc = "Producto ID: " + d.getInventarioId();
                }
                listaDetalles.add(new FilaDetalle(desc, d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal()));
            }
        }
    }

    @FXML
    private void handleActualizar() {
        cargarDatos();
    }

    @FXML
    private void handleLimpiarFiltros() {
        txtBuscarFactura.clear();
        cmbMetodoPago.getSelectionModel().selectFirst();
        cmbEstadoVenta.getSelectionModel().selectFirst();
        dpFechaDesde.setValue(null);
        dpFechaHasta.setValue(null);
        aplicarFiltros();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
