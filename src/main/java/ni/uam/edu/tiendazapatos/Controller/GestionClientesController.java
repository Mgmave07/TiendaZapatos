package ni.uam.edu.tiendazapatos.Controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import ni.uam.edu.tiendazapatos.dao.ClienteDAO;
import ni.uam.edu.tiendazapatos.modelo.Cliente;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.Venta;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GestionClientesController {

    // ── FXML KPIs y Filtros ───────────────────────────────────────────────────
    @FXML private Label lblKpiTotalClientes;
    @FXML private Label lblKpiClientesActivos;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbEstado;

    // ── FXML Tabla Clientes ───────────────────────────────────────────────────
    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colIdentificacion;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colCorreo;
    @FXML private TableColumn<Cliente, String> colDireccion;
    @FXML private TableColumn<Cliente, String> colEstado;
    @FXML private TableColumn<Cliente, String> colFechaRegistro;

    // ── FXML Panel Inferior (Solo Lectura) ────────────────────────────────────
    @FXML private Label lblClienteSeleccionado;
    @FXML private Label lblResumenFacturas;
    @FXML private Label lblResumenArticulos;
    @FXML private Label lblResumenTotal;

    @FXML private TableView<Venta> tablaFacturasCliente;
    @FXML private TableColumn<Venta, String> colFacturaNumero;
    @FXML private TableColumn<Venta, String> colFacturaFecha;
    @FXML private TableColumn<Venta, String> colFacturaMetodo;
    @FXML private TableColumn<Venta, String> colFacturaTotal;
    @FXML private TableColumn<Venta, String> colFacturaEstado;
    @FXML private TableColumn<Venta, String> colFacturaArticulos;

    // ── Estado ────────────────────────────────────────────────────────────────
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Cliente> masterClientes = FXCollections.observableArrayList();
    private FilteredList<Cliente> clientesFiltrados;
    private final ObservableList<Venta> facturasCliente = FXCollections.observableArrayList();

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private final SimpleDateFormat sdfFecha = new SimpleDateFormat("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarTablaClientes();
        configurarTablaFacturas();
        configurarFiltros();
        cargarDatos();
    }

    private void configurarTablaClientes() {
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getNombre()));
        colIdentificacion.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getIdentificacion() != null && !c.getValue().getIdentificacion().isBlank()
                        ? c.getValue().getIdentificacion() : "—"));
        colTelefono.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getTelefono() != null && !c.getValue().getTelefono().isBlank()
                        ? c.getValue().getTelefono() : "—"));
        colCorreo.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getCorreo() != null && !c.getValue().getCorreo().isBlank()
                        ? c.getValue().getCorreo() : "—"));
        colDireccion.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getDireccion() != null && !c.getValue().getDireccion().isBlank()
                        ? c.getValue().getDireccion() : "—"));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Boolean.TRUE.equals(c.getValue().getActivo()) ? "✅ Activo" : "❌ Inactivo"));
        colFechaRegistro.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getCreadoEn() != null ? sdfFecha.format(c.getValue().getCreadoEn()) : "—"));

        clientesFiltrados = new FilteredList<>(masterClientes, p -> true);
        SortedList<Cliente> sorted = new SortedList<>(clientesFiltrados);
        sorted.comparatorProperty().bind(tablaClientes.comparatorProperty());
        tablaClientes.setItems(sorted);

        tablaClientes.getSelectionModel().selectedItemProperty().addListener((obs, old, cliente) -> {
            if (cliente != null) {
                mostrarHistorialCliente(cliente);
            } else {
                limpiarHistorialCliente();
            }
        });
    }

    private void configurarTablaFacturas() {
        colFacturaNumero.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getNumeroVenta() != null ? c.getValue().getNumeroVenta() : "—"));
        colFacturaFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getFechaVenta() != null ? sdf.format(c.getValue().getFechaVenta()) : "—"));
        colFacturaMetodo.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getMetodoPago() != null ? c.getValue().getMetodoPago() : "—"));
        colFacturaTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                String.format(Locale.US, "C$ %.2f", c.getValue().getTotal())));
        colFacturaEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getEstado() != null ? c.getValue().getEstado() : "—"));
        colFacturaArticulos.setCellValueFactory(c -> {
            int arts = 0;
            if (c.getValue().getDetalles() != null) {
                for (DetalleVenta d : c.getValue().getDetalles()) arts += d.getCantidad();
            }
            return new ReadOnlyStringWrapper(String.valueOf(arts));
        });

        tablaFacturasCliente.setItems(facturasCliente);
    }

    private void configurarFiltros() {
        cmbEstado.getItems().setAll("Todos", "Activos", "Inactivos");
        cmbEstado.setValue("Todos");

        txtBuscar.textProperty().addListener((o, v, n) -> aplicarFiltros());
        cmbEstado.valueProperty().addListener((o, v, n) -> aplicarFiltros());
    }

    private void cargarDatos() {
        List<Cliente> lista = clienteDAO.obtenerTodos();
        masterClientes.setAll(lista);
        actualizarKpis();
        aplicarFiltros();
        if (!masterClientes.isEmpty() && tablaClientes.getSelectionModel().getSelectedItem() == null) {
            tablaClientes.getSelectionModel().selectFirst();
        }
    }

    private void actualizarKpis() {
        int total = masterClientes.size();
        long activos = masterClientes.stream().filter(c -> Boolean.TRUE.equals(c.getActivo())).count();
        lblKpiTotalClientes.setText(total + " registrados");
        lblKpiClientesActivos.setText(activos + " activos");
    }

    private void aplicarFiltros() {
        String busqueda = txtBuscar.getText() != null ? txtBuscar.getText().trim().toLowerCase() : "";
        String estadoSel = cmbEstado.getValue();

        clientesFiltrados.setPredicate(c -> {
            if (c == null) return false;

            if (!busqueda.isEmpty()) {
                boolean matchNom = c.getNombre() != null && c.getNombre().toLowerCase().contains(busqueda);
                boolean matchId  = c.getIdentificacion() != null && c.getIdentificacion().toLowerCase().contains(busqueda);
                boolean matchTel = c.getTelefono() != null && c.getTelefono().toLowerCase().contains(busqueda);
                boolean matchCor = c.getCorreo() != null && c.getCorreo().toLowerCase().contains(busqueda);
                if (!matchNom && !matchId && !matchTel && !matchCor) return false;
            }

            if ("Activos".equals(estadoSel) && !Boolean.TRUE.equals(c.getActivo())) return false;
            if ("Inactivos".equals(estadoSel) && Boolean.TRUE.equals(c.getActivo())) return false;

            return true;
        });
    }

    private void mostrarHistorialCliente(Cliente cliente) {
        lblClienteSeleccionado.setText(cliente.getNombre() +
                (cliente.getIdentificacion() != null && !cliente.getIdentificacion().isBlank()
                        ? " (Cédula: " + cliente.getIdentificacion() + ")" : ""));

        List<Venta> ventas = clienteDAO.obtenerVentasPorCliente(
                cliente.getClienteId(), cliente.getIdentificacion(), cliente.getNombre());
        facturasCliente.setAll(ventas);

        ClienteDAO.ResumenComprasCliente resumen = clienteDAO.obtenerResumenCompras(
                cliente.getClienteId(), cliente.getIdentificacion(), cliente.getNombre());

        lblResumenFacturas.setText(String.valueOf(resumen.totalFacturas()));
        lblResumenArticulos.setText(String.valueOf(resumen.totalArticulos()));
        lblResumenTotal.setText(String.format(Locale.US, "C$ %.2f", resumen.totalInvertido()));
    }

    private void limpiarHistorialCliente() {
        lblClienteSeleccionado.setText("(Selecciona un cliente arriba para ver sus compras)");
        facturasCliente.clear();
        lblResumenFacturas.setText("0");
        lblResumenArticulos.setText("0");
        lblResumenTotal.setText("C$ 0.00");
    }

    @FXML
    private void handleLimpiarFiltro() {
        txtBuscar.clear();
        cmbEstado.setValue("Todos");
    }

    @FXML
    private void handleActualizar() {
        cargarDatos();
        Cliente sel = tablaClientes.getSelectionModel().getSelectedItem();
        if (sel != null) mostrarHistorialCliente(sel);
    }

    @FXML
    private void handleNuevoCliente() {
        Dialog<Cliente> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Cliente");
        dialog.setHeaderText("Registrar nuevo cliente en el sistema");

        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtNom = new TextField();
        txtNom.setPromptText("Nombre completo");
        TextField txtId = new TextField();
        txtId.setPromptText("Cédula / Identificación");
        TextField txtTel = new TextField();
        txtTel.setPromptText("Teléfono");
        TextField txtCor = new TextField();
        txtCor.setPromptText("Correo electrónico");
        TextField txtDir = new TextField();
        txtDir.setPromptText("Dirección de residencia");

        grid.add(new Label("Nombre *:"), 0, 0);
        grid.add(txtNom, 1, 0);
        grid.add(new Label("Identificación:"), 0, 1);
        grid.add(txtId, 1, 1);
        grid.add(new Label("Teléfono:"), 0, 2);
        grid.add(txtTel, 1, 2);
        grid.add(new Label("Correo:"), 0, 3);
        grid.add(txtCor, 1, 3);
        grid.add(new Label("Dirección:"), 0, 4);
        grid.add(txtDir, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                if (txtNom.getText().trim().isEmpty()) {
                    mostrarAlerta(Alert.AlertType.WARNING, "El nombre del cliente es obligatorio.");
                    return null;
                }
                return new Cliente(txtNom.getText().trim(), txtId.getText().trim(),
                        txtTel.getText().trim(), txtCor.getText().trim(), txtDir.getText().trim());
            }
            return null;
        });

        Optional<Cliente> resultado = dialog.showAndWait();
        resultado.ifPresent(c -> {
            Cliente guardado = clienteDAO.guardar(c);
            if (guardado != null) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Cliente registrado exitosamente.");
                cargarDatos();
                tablaClientes.getSelectionModel().select(guardado);
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "No se pudo registrar el cliente.");
            }
        });
    }

    @FXML
    private void handleEditarCliente() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selecciona un cliente de la tabla para editar su información.");
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Modificar Información de Cliente");
        dialog.setHeaderText("Editar datos personales y de contacto de: " + seleccionado.getNombre() +
                "\n(Nota: Las ventas e historial de compras son inmutables)");

        ButtonType btnGuardar = new ButtonType("Guardar Cambios", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 120, 10, 10));

        TextField txtNom = new TextField(seleccionado.getNombre());
        TextField txtId = new TextField(seleccionado.getIdentificacion() != null ? seleccionado.getIdentificacion() : "");
        TextField txtTel = new TextField(seleccionado.getTelefono() != null ? seleccionado.getTelefono() : "");
        TextField txtCor = new TextField(seleccionado.getCorreo() != null ? seleccionado.getCorreo() : "");
        TextField txtDir = new TextField(seleccionado.getDireccion() != null ? seleccionado.getDireccion() : "");
        CheckBox chkActivo = new CheckBox("Cliente Activo");
        chkActivo.setSelected(Boolean.TRUE.equals(seleccionado.getActivo()));

        grid.add(new Label("Nombre Completo *:"), 0, 0);
        grid.add(txtNom, 1, 0);
        grid.add(new Label("Identificación / Cédula:"), 0, 1);
        grid.add(txtId, 1, 1);
        grid.add(new Label("Teléfono:"), 0, 2);
        grid.add(txtTel, 1, 2);
        grid.add(new Label("Correo:"), 0, 3);
        grid.add(txtCor, 1, 3);
        grid.add(new Label("Dirección:"), 0, 4);
        grid.add(txtDir, 1, 4);
        grid.add(new Label("Estado:"), 0, 5);
        grid.add(chkActivo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                if (txtNom.getText().trim().isEmpty()) {
                    mostrarAlerta(Alert.AlertType.WARNING, "El nombre del cliente no puede quedar vacío.");
                    return false;
                }
                seleccionado.setNombre(txtNom.getText().trim());
                seleccionado.setIdentificacion(txtId.getText().trim());
                seleccionado.setTelefono(txtTel.getText().trim());
                seleccionado.setCorreo(txtCor.getText().trim());
                seleccionado.setDireccion(txtDir.getText().trim());
                seleccionado.setActivo(chkActivo.isSelected());
                return true;
            }
            return false;
        });

        Optional<Boolean> resultado = dialog.showAndWait();
        if (resultado.orElse(false)) {
            boolean exito = clienteDAO.actualizar(seleccionado);
            if (exito) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Información del cliente modificada correctamente.");
                tablaClientes.refresh();
                actualizarKpis();
                mostrarHistorialCliente(seleccionado);
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Hubo un error al actualizar la información del cliente.");
            }
        }
    }

    @FXML
    private void handleToggleActivo() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selecciona un cliente de la tabla.");
            return;
        }

        boolean nuevoEstado = !Boolean.TRUE.equals(seleccionado.getActivo());
        seleccionado.setActivo(nuevoEstado);
        boolean ok = clienteDAO.actualizar(seleccionado);
        if (ok) {
            mostrarAlerta(Alert.AlertType.INFORMATION,
                    "Cliente " + (nuevoEstado ? "activado" : "desactivado") + " exitosamente.");
            tablaClientes.refresh();
            actualizarKpis();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo cambiar el estado del cliente.");
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
