package ni.uam.edu.tiendazapatos.Controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.uam.edu.tiendazapatos.dao.InventarioDAO;
import ni.uam.edu.tiendazapatos.dao.VentaDAO;
import ni.uam.edu.tiendazapatos.dao.ZapatoDAO;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.Venta;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.util.*;

public class VentasViewController {

    public record ProductoOption(
            UUID inventarioId,
            String descripcion,
            double precioUnitario,
            int stockDisponible
    ) {
        @Override
        public String toString() {
            return String.format(Locale.US, "%s | Stock: %d | $%.2f", descripcion, stockDisponible, precioUnitario);
        }
    }

    public record FilaCarrito(
            String descripcion,
            DetalleVenta detalle
    ) {}

    @FXML private ComboBox<ProductoOption> cmbProductos;
    @FXML private ComboBox<String> cmbMetodoPago;
    @FXML private TextField txtCantidad;
    @FXML private TableView<FilaCarrito> tablaCarrito;

    @FXML private TableColumn<FilaCarrito, String> colProducto;
    @FXML private TableColumn<FilaCarrito, Number> colPrecio;
    @FXML private TableColumn<FilaCarrito, Number> colCantidad;
    @FXML private TableColumn<FilaCarrito, Number> colSubtotal;

    @FXML private Label lblSubtotal;
    @FXML private Label lblImpuesto;
    @FXML private Label lblTotal;

    private final ObservableList<FilaCarrito> listaCarrito = FXCollections.observableArrayList();
    private final VentaDAO ventaDAO = new VentaDAO();
    private final ZapatoDAO zapatoDAO = new ZapatoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();

    private double subtotalActual = 0.0;
    private double impuestoActual = 0.0;
    private double totalActual = 0.0;

    @FXML
    public void initialize() {
        colProducto.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().descripcion()));
        colPrecio.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().detalle().getPrecioUnitario()));
        colCantidad.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().detalle().getCantidad()));
        colSubtotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().detalle().getSubtotal()));

        tablaCarrito.setItems(listaCarrito);

        cmbMetodoPago.setItems(FXCollections.observableArrayList("Efectivo", "Tarjeta", "Transferencia"));
        cmbMetodoPago.getSelectionModel().selectFirst();

        cargarProductosDisponibles();
    }

    private void cargarProductosDisponibles() {
        List<Zapato> zapatos = zapatoDAO.obtenerTodos();
        List<InventarioDAO.Variante> variantes = inventarioDAO.obtenerVariantes();

        Map<UUID, Zapato> mapaZapatos = new HashMap<>();
        if (zapatos != null) {
            for (Zapato z : zapatos) {
                mapaZapatos.put(z.getZapatoId(), z);
            }
        }

        ObservableList<ProductoOption> opciones = FXCollections.observableArrayList();
        for (InventarioDAO.Variante v : variantes) {
            if (v.cantidadStock() <= 0) continue;

            Zapato z = mapaZapatos.get(v.zapatoId());
            if (z == null || (z.getActivo() != null && !z.getActivo())) continue;

            UUID inventarioId = inventarioDAO.obtenerIdVariante(
                    v.zapatoId(), v.genero(), v.sistemaTalla(), v.talla(), v.color()
            );
            if (inventarioId == null) continue;

            String desc = z.getNombre() + " (" + v.genero() + ", " + v.sistemaTalla() + " " + v.talla() + ", " + v.color() + ")";
            double precio = z.getPrecioFinal() != null ? z.getPrecioFinal().doubleValue() : 0.0;

            opciones.add(new ProductoOption(inventarioId, desc, precio, v.cantidadStock()));
        }

        cmbProductos.setItems(opciones);
    }

    @FXML
    protected void agregarAlCarrito() {
        ProductoOption seleccionado = cmbProductos.getValue();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "Seleccione un producto del inventario.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "La cantidad debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ingrese una cantidad numérica válida.");
            return;
        }

        int yaEnCarrito = listaCarrito.stream()
                .filter(f -> f.detalle().getInventarioId().equals(seleccionado.inventarioId()))
                .mapToInt(f -> f.detalle().getCantidad())
                .sum();

        if (cantidad + yaEnCarrito > seleccionado.stockDisponible()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Stock insuficiente",
                    "Solo hay " + seleccionado.stockDisponible() + " unidades disponibles.");
            return;
        }

        for (int i = 0; i < listaCarrito.size(); i++) {
            FilaCarrito existente = listaCarrito.get(i);
            if (existente.detalle().getInventarioId().equals(seleccionado.inventarioId())) {
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

        DetalleVenta detalle = new DetalleVenta();
        detalle.setInventarioId(seleccionado.inventarioId());
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(seleccionado.precioUnitario());
        detalle.setSubtotal(seleccionado.precioUnitario() * cantidad);

        listaCarrito.add(new FilaCarrito(seleccionado.descripcion(), detalle));
        actualizarTotales();
        txtCantidad.setText("1");
    }

    @FXML
    protected void quitarDelCarrito() {
        FilaCarrito seleccionada = tablaCarrito.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            listaCarrito.remove(seleccionada);
            actualizarTotales();
        }
    }

    @FXML
    protected void facturarVenta() {
        if (listaCarrito.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Advertencia", "El carrito está vacío.");
            return;
        }

        Venta nuevaVenta = new Venta();
        nuevaVenta.setNumeroVenta("FAC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        nuevaVenta.setFechaVenta(new Date());
        nuevaVenta.setSubtotal(subtotalActual);
        nuevaVenta.setImpuesto(impuestoActual);
        nuevaVenta.setTotal(totalActual);
        nuevaVenta.setEstado("PAGADA");
        nuevaVenta.setMetodoPago(cmbMetodoPago.getValue());

        List<DetalleVenta> detalles = new ArrayList<>();
        for (FilaCarrito fila : listaCarrito) {
            DetalleVenta d = fila.detalle();
            d.setVenta(nuevaVenta);
            detalles.add(d);
        }
        nuevaVenta.setDetalles(detalles);

        boolean exito = ventaDAO.guardarVenta(nuevaVenta);

        if (exito) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito",
                    "Factura " + nuevaVenta.getNumeroVenta() + " guardada correctamente.");
            listaCarrito.clear();
            actualizarTotales();
            cargarProductosDisponibles();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Hubo un problema al guardar la factura en PostgreSQL.");
        }
    }

    private void actualizarTotales() {
        subtotalActual = 0.0;
        for (FilaCarrito fila : listaCarrito) {
            subtotalActual += fila.detalle().getSubtotal();
        }
        impuestoActual = subtotalActual * 0.15;
        totalActual = subtotalActual + impuestoActual;

        lblSubtotal.setText(String.format(Locale.US, "%.2f", subtotalActual));
        lblImpuesto.setText(String.format(Locale.US, "%.2f", impuestoActual));
        lblTotal.setText(String.format(Locale.US, "%.2f", totalActual));
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}