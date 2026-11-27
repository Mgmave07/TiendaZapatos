package ni.uam.edu.tiendazapatos.Controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import ni.uam.edu.tiendazapatos.dao.InventarioDAO;
import ni.uam.edu.tiendazapatos.dao.VentaDAO;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.InventarioFila;
import ni.uam.edu.tiendazapatos.modelo.Venta;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class HistorialVentasController {

    public record FilaDetalle(
            UUID detalleId,
            UUID inventarioId,
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
    private final InventarioDAO inventarioDAO = new InventarioDAO();

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
                    } else if ("CANCELADA".equalsIgnoreCase(item) || "DEVUELTA".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else if (item.contains("PARCIAL")) {
                        setStyle("-fx-text-fill: #ea580c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ca8a04; -fx-font-weight: bold;");
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

        cmbEstadoVenta.getItems().setAll("Todos los estados", "PAGADA", "DEVUELTA", "CANCELADA");
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
            if (!numFactura.isEmpty()) {
                String num = v.getNumeroVenta() != null ? v.getNumeroVenta().toLowerCase(Locale.ROOT) : "";
                if (!num.contains(numFactura)) return false;
            }

            if (metodoSel != null && !"Todos los métodos".equalsIgnoreCase(metodoSel)) {
                if (!metodoSel.equalsIgnoreCase(v.getMetodoPago())) return false;
            }

            if (estadoSel != null && !"Todos los estados".equalsIgnoreCase(estadoSel)) {
                if (!estadoSel.equalsIgnoreCase(v.getEstado())) return false;
            }

            if (v.getFechaVenta() != null) {
                LocalDate fechaVentaLocal = v.getFechaVenta().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (desde != null && fechaVentaLocal.isBefore(desde)) return false;
                if (hasta != null && fechaVentaLocal.isAfter(hasta)) return false;
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
        lblDetalleFactura.setText(String.format("Factura: %s | Fecha: %s | Total: C$ %.2f | Método: %s | Estado: %s",
                venta.getNumeroVenta(), fechaStr, venta.getTotal(), venta.getMetodoPago(), venta.getEstado()));

        if (venta.getDetalles() != null) {
            for (DetalleVenta d : venta.getDetalles()) {
                String desc = mapaInventario.get(d.getInventarioId());
                if (desc == null || desc.isBlank()) {
                    desc = "Producto ID: " + d.getInventarioId();
                }
                listaDetalles.add(new FilaDetalle(d.getDetalle_id(), d.getInventarioId(), desc, d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal()));
            }
        }
    }

    @FXML
    private void handleProcesarDevolucionOCambio() {
        Venta ventaSeleccionada = tablaVentas.getSelectionModel().getSelectedItem();
        FilaDetalle detalleSeleccionado = tablaDetalles.getSelectionModel().getSelectedItem();

        if (ventaSeleccionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selección requerida", "Por favor, selecciona una factura de la tabla superior.");
            return;
        }

        if (detalleSeleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selección requerida", "Por favor, selecciona el producto específico a devolver o cambiar de la tabla de desglose inferior.");
            return;
        }

        if (detalleSeleccionado.cantidad() <= 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin unidades", "Este producto ya fue devuelto en su totalidad (cantidad: 0).");
            return;
        }

        // Crear Diálogo Modal para Devolución / Cambio
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Procesar Devolución o Cambio");
        dialog.setHeaderText("Factura: " + ventaSeleccionada.getNumeroVenta() +
                "\nProducto: " + detalleSeleccionado.producto() +
                "\nCantidad vendida: " + detalleSeleccionado.cantidad() + " | Precio Unitario: C$ " +
                String.format(Locale.US, "%.2f", detalleSeleccionado.precioUnitario()));

        ButtonType btnConfirmar = new ButtonType("Confirmar Transacción", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 10));

        // Selector de cantidad a devolver
        Spinner<Integer> spnCantidad = new Spinner<>(1, detalleSeleccionado.cantidad(), 1);
        spnCantidad.setEditable(true);
        spnCantidad.setPrefWidth(120);

        // Selector de Motivo / Tipo de acción
        ComboBox<String> cmbTipoAccion = new ComboBox<>();
        cmbTipoAccion.getItems().addAll(
                "Recambio de producto (cambio por otro calzado)",
                "Producto dañado (devolución sin ingresar al stock)",
                "Devolución normal (devolución con reingreso al stock)"
        );
        cmbTipoAccion.setValue("Devolución normal (devolución con reingreso al stock)");
        cmbTipoAccion.setMaxWidth(Double.MAX_VALUE);

        // Controles para Recambio de producto con buscador y TableView
        Label lblNuevoProd = new Label("Buscar y seleccionar calzado de reemplazo del catálogo *:");
        lblNuevoProd.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

        TextField txtBuscarRecambio = new TextField();
        txtBuscarRecambio.setPromptText("🔍 Escriba modelo, marca, talla o color para filtrar en tiempo real...");
        txtBuscarRecambio.setStyle("-fx-padding: 7 10; -fx-background-radius: 6; -fx-border-color: #cbd5e1; -fx-border-radius: 6;");

        TableView<InventarioDAO.OpcionRecambio> tablaRecambio = new TableView<>();
        tablaRecambio.setPrefHeight(180);
        tablaRecambio.setMaxHeight(200);

        TableColumn<InventarioDAO.OpcionRecambio, String> colRecModelo = new TableColumn<>("Modelo");
        colRecModelo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombre()));
        colRecModelo.setPrefWidth(160);

        TableColumn<InventarioDAO.OpcionRecambio, String> colRecMarca = new TableColumn<>("Marca");
        colRecMarca.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().marca()));
        colRecMarca.setPrefWidth(100);

        TableColumn<InventarioDAO.OpcionRecambio, String> colRecTalla = new TableColumn<>("Talla");
        colRecTalla.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().talla()));
        colRecTalla.setPrefWidth(60);

        TableColumn<InventarioDAO.OpcionRecambio, String> colRecColor = new TableColumn<>("Color");
        colRecColor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().color()));
        colRecColor.setPrefWidth(110);

        TableColumn<InventarioDAO.OpcionRecambio, Number> colRecStock = new TableColumn<>("Stock");
        colRecStock.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().stock()));
        colRecStock.setPrefWidth(65);

        TableColumn<InventarioDAO.OpcionRecambio, String> colRecPrecio = new TableColumn<>("Precio");
        colRecPrecio.setCellValueFactory(c -> new SimpleStringProperty(String.format(Locale.US, "C$ %.2f", c.getValue().precio())));
        colRecPrecio.setPrefWidth(95);

        tablaRecambio.getColumns().addAll(colRecModelo, colRecMarca, colRecTalla, colRecColor, colRecStock, colRecPrecio);

        // Cargar inventario disponible para reemplazo y configurar filtro reactivo
        List<InventarioDAO.OpcionRecambio> disponibles = inventarioDAO.obtenerOpcionesRecambio();
        ObservableList<InventarioDAO.OpcionRecambio> listaRecambio = FXCollections.observableArrayList(disponibles);
        FilteredList<InventarioDAO.OpcionRecambio> filtradasRecambio = new FilteredList<>(listaRecambio, p -> true);

        txtBuscarRecambio.textProperty().addListener((obs, ant, nuevo) -> {
            String filtro = nuevo == null ? "" : nuevo.trim().toLowerCase(Locale.ROOT);
            filtradasRecambio.setPredicate(item -> {
                if (filtro.isEmpty()) return true;
                return (item.nombre() != null && item.nombre().toLowerCase(Locale.ROOT).contains(filtro))
                        || (item.marca() != null && item.marca().toLowerCase(Locale.ROOT).contains(filtro))
                        || (item.talla() != null && item.talla().toLowerCase(Locale.ROOT).contains(filtro))
                        || (item.color() != null && item.color().toLowerCase(Locale.ROOT).contains(filtro));
            });
        });

        SortedList<InventarioDAO.OpcionRecambio> ordenadasRecambio = new SortedList<>(filtradasRecambio);
        ordenadasRecambio.comparatorProperty().bind(tablaRecambio.comparatorProperty());
        tablaRecambio.setItems(ordenadasRecambio);

        Label lblSeleccionado = new Label("Ningún calzado seleccionado aún. Haga clic en una fila de la tabla.");
        lblSeleccionado.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-font-style: italic;");

        Label lblDiferencia = new Label("Diferencia financiera: —");
        lblDiferencia.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0284c7;");

        Runnable calcularDiferencia = () -> {
            InventarioDAO.OpcionRecambio nuevo = tablaRecambio.getSelectionModel().getSelectedItem();
            if (nuevo != null) {
                lblSeleccionado.setText(String.format("Seleccionado: %s (%s) | Talla %s, %s | Stock: %d | C$ %.2f",
                        nuevo.nombre(), nuevo.marca(), nuevo.talla(), nuevo.color(), nuevo.stock(), nuevo.precio()));
                lblSeleccionado.setStyle("-fx-font-size: 12px; -fx-text-fill: #0f172a; -fx-font-weight: bold;");

                double nuevoPrecio = nuevo.precio();
                int cant = spnCantidad.getValue();
                double diffSub = cant * (nuevoPrecio - detalleSeleccionado.precioUnitario());
                double diffTot = diffSub * 1.15;
                if (diffTot > 0) {
                    lblDiferencia.setText(String.format(Locale.US, "Cobrar al cliente: C$ %.2f (Total con IVA)", diffTot));
                    lblDiferencia.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #16a34a;");
                } else if (diffTot < 0) {
                    lblDiferencia.setText(String.format(Locale.US, "Devolver al cliente: C$ %.2f (Total con IVA)", Math.abs(diffTot)));
                    lblDiferencia.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #dc2626;");
                } else {
                    lblDiferencia.setText("Mismo valor: C$ 0.00 (sin cobro ni devolución)");
                    lblDiferencia.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0284c7;");
                }
            } else {
                lblSeleccionado.setText("Ningún calzado seleccionado aún. Haga clic en una fila de la tabla.");
                lblSeleccionado.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-font-style: italic;");
                lblDiferencia.setText("Diferencia financiera: —");
            }
        };

        tablaRecambio.getSelectionModel().selectedItemProperty().addListener((o, v, n) -> calcularDiferencia.run());
        spnCantidad.valueProperty().addListener((o, v, n) -> calcularDiferencia.run());

        VBox boxRecambio = new VBox(8, lblNuevoProd, txtBuscarRecambio, tablaRecambio, lblSeleccionado, lblDiferencia);
        boxRecambio.setStyle("-fx-background-color: #f8fafc; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        boxRecambio.setVisible(false);
        boxRecambio.setManaged(false);

        cmbTipoAccion.valueProperty().addListener((obs, old, val) -> {
            boolean esRecambio = val != null && val.startsWith("Recambio");
            boxRecambio.setVisible(esRecambio);
            boxRecambio.setManaged(esRecambio);
        });

        TextField txtMotivo = new TextField();
        txtMotivo.setPromptText("Ej. Defecto de fábrica en costura / Talla incorrecta / Arrepentimiento");
        txtMotivo.setStyle("-fx-padding: 6 10;");

        grid.add(new Label("Cantidad a Procesar *:"), 0, 0);
        grid.add(spnCantidad, 1, 0);
        grid.add(new Label("Motivo / Acción *:"), 0, 1);
        grid.add(cmbTipoAccion, 1, 1);
        grid.add(boxRecambio, 0, 2, 2, 1);
        grid.add(new Label("Notas / Explicación:"), 0, 3);
        grid.add(txtMotivo, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(720);
        dialog.setResizable(true);

        dialog.setResultConverter(button -> {
            if (button == btnConfirmar) {
                String accion = cmbTipoAccion.getValue();
                int cantDev = spnCantidad.getValue();
                String motivo = txtMotivo.getText() != null ? txtMotivo.getText().trim() : "";

                VentaDAO.TipoDevolucion tipoEnum;
                UUID nuevoInvId = null;
                double nuevoPrecio = 0.0;

                if (accion.startsWith("Recambio")) {
                    tipoEnum = VentaDAO.TipoDevolucion.RECAMBIO;
                    InventarioDAO.OpcionRecambio nuevo = tablaRecambio.getSelectionModel().getSelectedItem();
                    if (nuevo == null) {
                        mostrarAlerta(Alert.AlertType.WARNING, "Selección requerida", "Debes seleccionar un calzado de la tabla para el recambio.");
                        return false;
                    }
                    if (nuevo.stock() < cantDev) {
                        mostrarAlerta(Alert.AlertType.WARNING, "Stock insuficiente", "No hay suficiente stock del calzado seleccionado (Disponible: " + nuevo.stock() + ").");
                        return false;
                    }
                    nuevoInvId = nuevo.inventarioId();
                    nuevoPrecio = nuevo.precio();
                } else if (accion.contains("dañado")) {
                    tipoEnum = VentaDAO.TipoDevolucion.PRODUCTO_DANADO;
                } else {
                    tipoEnum = VentaDAO.TipoDevolucion.DEVOLUCION_NORMAL;
                }

                boolean exito = ventaDAO.procesarDevolucionOCambio(
                        ventaSeleccionada.getVenta_id(),
                        detalleSeleccionado.detalleId(),
                        cantDev,
                        tipoEnum,
                        nuevoInvId,
                        nuevoPrecio,
                        motivo
                );

                if (!exito) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo procesar la transacción en la base de datos.");
                }

                return exito;
            }
            return false;
        });

        Optional<Boolean> res = dialog.showAndWait();
        if (res.orElse(false)) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "La devolución o cambio se procesó y guardó exitosamente en la factura.");
            UUID idVenta = ventaSeleccionada.getVenta_id();
            cargarDatos();
            // Reseleccionar la venta actualizada y refrescar tablas
            for (Venta v : listaVentas) {
                if (v.getVenta_id().equals(idVenta)) {
                    tablaVentas.getSelectionModel().select(v);
                    mostrarDetallesVenta(v);
                    tablaVentas.refresh();
                    tablaDetalles.refresh();
                    break;
                }
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
