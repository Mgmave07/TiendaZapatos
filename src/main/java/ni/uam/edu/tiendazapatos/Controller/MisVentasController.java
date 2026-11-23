package ni.uam.edu.tiendazapatos.Controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.uam.edu.tiendazapatos.dao.VentaDAO;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.Venta;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MisVentasController {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;

    // KPIs
    @FXML private Label lblTotalDia;
    @FXML private Label lblVentasDia;
    @FXML private Label lblTicketPromedio;

    // Tabla ventas
    @FXML private TableView<Venta>            tablaVentas;
    @FXML private TableColumn<Venta, String>  colNumero;
    @FXML private TableColumn<Venta, String>  colFecha;
    @FXML private TableColumn<Venta, String>  colCliente;
    @FXML private TableColumn<Venta, String>  colMetodo;
    @FXML private TableColumn<Venta, String>  colTotal;
    @FXML private TableColumn<Venta, String>  colEstado;

    // Tabla detalles
    @FXML private TableView<DetalleVenta>            tablaDetalles;
    @FXML private TableColumn<DetalleVenta, String>  colDetProducto;
    @FXML private TableColumn<DetalleVenta, String>  colDetCantidad;
    @FXML private TableColumn<DetalleVenta, String>  colDetPrecio;
    @FXML private TableColumn<DetalleVenta, String>  colDetSubtotal;

    private final VentaDAO ventaDAO = new VentaDAO();
    private Map<UUID, String> descripcionesInventario;

    private final ObservableList<Venta>       listaVentas   = FXCollections.observableArrayList();
    private final ObservableList<DetalleVenta> listaDetalles = FXCollections.observableArrayList();

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        // Fechas por defecto: hoy
        dpDesde.setValue(LocalDate.now());
        dpHasta.setValue(LocalDate.now());

        configurarColumnas();
        descripcionesInventario = ventaDAO.obtenerDescripcionesInventario();
        cargarVentas();
    }

    private void configurarColumnas() {
        colNumero.setCellValueFactory(  c -> new ReadOnlyStringWrapper(c.getValue().getNumeroVenta()));
        colFecha.setCellValueFactory(   c -> new ReadOnlyStringWrapper(
                c.getValue().getFechaVenta() != null ? sdf.format(c.getValue().getFechaVenta()) : "—"));
        colCliente.setCellValueFactory( c -> new ReadOnlyStringWrapper(
                c.getValue().getClienteNombre() != null ? c.getValue().getClienteNombre() : "—"));
        colMetodo.setCellValueFactory(  c -> new ReadOnlyStringWrapper(
                c.getValue().getMetodoPago() != null ? c.getValue().getMetodoPago() : "—"));
        colTotal.setCellValueFactory(   c -> new ReadOnlyStringWrapper(
                String.format(Locale.US, "C$ %.2f", c.getValue().getTotal())));
        colEstado.setCellValueFactory(  c -> new ReadOnlyStringWrapper(c.getValue().getEstado()));

        tablaVentas.setItems(listaVentas);
        tablaVentas.getSelectionModel().selectedItemProperty().addListener((obs, old, venta) -> {
            if (venta != null) mostrarDetalles(venta);
        });

        // Columnas detalle
        colDetProducto.setCellValueFactory(c -> {
            UUID invId = c.getValue().getInventarioId();
            String desc = descripcionesInventario.getOrDefault(invId, invId != null ? invId.toString() : "—");
            return new ReadOnlyStringWrapper(desc);
        });
        colDetCantidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().getCantidad())));
        colDetPrecio.setCellValueFactory(  c -> new ReadOnlyStringWrapper(
                String.format(Locale.US, "C$ %.2f", c.getValue().getPrecioUnitario())));
        colDetSubtotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                String.format(Locale.US, "C$ %.2f", c.getValue().getSubtotal())));

        tablaDetalles.setItems(listaDetalles);
    }

    private void cargarVentas() {
        try {
            UUID vendedorId = SesionUsuario.getInstancia().getUsuarioActual().getUsuarioId();
            LocalDate desde = dpDesde.getValue() != null ? dpDesde.getValue() : LocalDate.now();
            LocalDate hasta = dpHasta.getValue() != null ? dpHasta.getValue() : LocalDate.now();

            List<Venta> ventas = ventaDAO.obtenerPorVendedor(vendedorId, desde, hasta);
            listaVentas.setAll(ventas);
            listaDetalles.clear();
            actualizarKPIs(ventas);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void actualizarKPIs(List<Venta> ventas) {
        double total   = ventas.stream().mapToDouble(Venta::getTotal).sum();
        int    count   = ventas.size();
        double promedio = count > 0 ? total / count : 0;

        lblTotalDia.setText(      String.format(Locale.US, "C$ %.2f", total));
        lblVentasDia.setText(     String.valueOf(count));
        lblTicketPromedio.setText(String.format(Locale.US, "C$ %.2f", promedio));
    }

    private void mostrarDetalles(Venta venta) {
        listaDetalles.clear();
        if (venta.getDetalles() != null) {
            listaDetalles.setAll(venta.getDetalles());
        }
    }

    @FXML
    private void handleActualizar() {
        descripcionesInventario = ventaDAO.obtenerDescripcionesInventario();
        cargarVentas();
    }

    @FXML
    private void handleHoy() {
        dpDesde.setValue(LocalDate.now());
        dpHasta.setValue(LocalDate.now());
        cargarVentas();
    }
}
