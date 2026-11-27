package ni.uam.edu.tiendazapatos.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;
import ni.uam.edu.tiendazapatos.dao.ClienteDAO;
import ni.uam.edu.tiendazapatos.modelo.Cliente;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.Venta;

import java.util.*;

public class DialogoFacturacion {

    public static Optional<Venta> mostrar(List<DetalleVenta> detalles, double subtotal, double impuesto, double total) {

        Dialog<Venta> dialog = new Dialog<>();
        dialog.setTitle("Datos de Facturación");
        dialog.setHeaderText(null);

        // ── Botones ───────────────────────────────────────────────────────────
        ButtonType btnConfirmar = new ButtonType("✔ Confirmar Venta", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar  = new ButtonType("Cancelar",           ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, btnCancelar);

        // ── Layout raíz ───────────────────────────────────────────────────────
        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setPrefWidth(520);
        root.setStyle("-fx-background-color: #f8fafc;");

        // ── Título ────────────────────────────────────────────────────────────
        Label lblTitulo = new Label("NUEVA FACTURA DE VENTA");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 18));
        lblTitulo.setStyle("-fx-text-fill: #0f172a;");

        // ── Resumen financiero ────────────────────────────────────────────────
        VBox cardResumen = crearCard();
        Label lblResumenTitle = new Label("Resumen de la Venta");
        lblResumenTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblResumenTitle.setStyle("-fx-text-fill: #334155;");

        GridPane gridResumen = new GridPane();
        gridResumen.setHgap(12);
        gridResumen.setVgap(4);
        gridResumen.add(new Label("Subtotal:"),                    0, 0);
        gridResumen.add(labelMonto(String.format(Locale.US, "C$ %.2f", subtotal)), 1, 0);
        gridResumen.add(new Label("IVA (15%):"),                   0, 1);
        gridResumen.add(labelMonto(String.format(Locale.US, "C$ %.2f", impuesto)), 1, 1);
        Label lblTotalVal = new Label(String.format(Locale.US, "C$ %.2f", total));
        lblTotalVal.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblTotalVal.setStyle("-fx-text-fill: #0284c7;");
        Label lblTotalKey = new Label("TOTAL:");
        lblTotalKey.setFont(Font.font("System", FontWeight.BOLD, 14));
        gridResumen.add(lblTotalKey, 0, 2);
        gridResumen.add(lblTotalVal, 1, 2);

        cardResumen.getChildren().addAll(lblResumenTitle, gridResumen);

        // ── Datos del cliente ─────────────────────────────────────────────────
        VBox cardCliente = crearCard();
        Label lblClienteTitle = new Label("Datos del Cliente");
        lblClienteTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblClienteTitle.setStyle("-fx-text-fill: #334155;");

        ClienteDAO clienteDAO = new ClienteDAO();
        List<Cliente> clientesBD = clienteDAO.obtenerActivos();
        ObservableList<Cliente> listaClientes = FXCollections.observableArrayList(clientesBD);

        // Opciones de selección de cliente
        ToggleGroup grupoTipoCliente = new ToggleGroup();
        RadioButton rbRegistrado = new RadioButton("Cliente Registrado");
        RadioButton rbNuevo      = new RadioButton("Nuevo Cliente");
        RadioButton rbConsumidor = new RadioButton("Consumidor Final");
        rbRegistrado.setToggleGroup(grupoTipoCliente);
        rbNuevo.setToggleGroup(grupoTipoCliente);
        rbConsumidor.setToggleGroup(grupoTipoCliente);
        rbRegistrado.setSelected(true);

        HBox boxRadios = new HBox(12, rbRegistrado, rbNuevo, rbConsumidor);
        boxRadios.setStyle("-fx-padding: 4 0 6 0;");

        // Panel para cliente registrado
        ComboBox<Cliente> cmbClientes = new ComboBox<>(listaClientes);
        cmbClientes.setMaxWidth(Double.MAX_VALUE);
        cmbClientes.setPromptText("Selecciona un cliente de la lista...");
        cmbClientes.setStyle("-fx-font-size: 12px;");

        TextField txtFiltroCliente = txtField("Buscar cliente por nombre o cédula...");
        txtFiltroCliente.textProperty().addListener((obs, old, text) -> {
            if (text == null || text.trim().isEmpty()) {
                cmbClientes.setItems(listaClientes);
            } else {
                String filtro = text.trim().toLowerCase();
                ObservableList<Cliente> filtrados = listaClientes.filtered(c ->
                    (c.getNombre() != null && c.getNombre().toLowerCase().contains(filtro)) ||
                    (c.getIdentificacion() != null && c.getIdentificacion().toLowerCase().contains(filtro))
                );
                cmbClientes.setItems(filtrados);
                if (!filtrados.isEmpty()) cmbClientes.show();
            }
        });

        VBox boxClienteRegistrado = new VBox(6,
                etiquetaCampo("Filtrar clientes", txtFiltroCliente),
                etiquetaCampo("Cliente Registrado *", cmbClientes)
        );

        // Campos de datos del cliente
        TextField txtNombre         = txtField("Nombre completo *");
        TextField txtIdentificacion = txtField("Cédula / RUC");
        TextField txtTelefono       = txtField("Teléfono");
        TextField txtCorreo         = txtField("Correo electrónico (opcional)");
        TextField txtDireccion      = txtField("Dirección (opcional)");

        VBox boxCamposCliente = new VBox(8,
                etiquetaCampo("Nombre *", txtNombre),
                etiquetaCampo("Identificación / Cédula", txtIdentificacion),
                etiquetaCampo("Teléfono", txtTelefono),
                etiquetaCampo("Correo", txtCorreo),
                etiquetaCampo("Dirección", txtDireccion)
        );

        // Listener para sincronizar cliente registrado seleccionado
        cmbClientes.valueProperty().addListener((obs, old, cli) -> {
            if (cli != null) {
                txtNombre.setText(cli.getNombre());
                txtIdentificacion.setText(cli.getIdentificacion() != null ? cli.getIdentificacion() : "");
                txtTelefono.setText(cli.getTelefono() != null ? cli.getTelefono() : "");
                txtCorreo.setText(cli.getCorreo() != null ? cli.getCorreo() : "");
                txtDireccion.setText(cli.getDireccion() != null ? cli.getDireccion() : "");
            }
        });

        // Configurar estado según RadioButton seleccionado
        grupoTipoCliente.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (toggle == rbRegistrado) {
                boxClienteRegistrado.setVisible(true);
                boxClienteRegistrado.setManaged(true);
                boxCamposCliente.setVisible(true);
                boxCamposCliente.setManaged(true);

                txtNombre.setDisable(true);
                txtIdentificacion.setDisable(true);
                txtTelefono.setDisable(true);
                txtCorreo.setDisable(true);
                txtDireccion.setDisable(true);

                Cliente c = cmbClientes.getValue();
                if (c != null) {
                    txtNombre.setText(c.getNombre());
                    txtIdentificacion.setText(c.getIdentificacion() != null ? c.getIdentificacion() : "");
                    txtTelefono.setText(c.getTelefono() != null ? c.getTelefono() : "");
                    txtCorreo.setText(c.getCorreo() != null ? c.getCorreo() : "");
                    txtDireccion.setText(c.getDireccion() != null ? c.getDireccion() : "");
                } else {
                    txtNombre.clear();
                    txtIdentificacion.clear();
                    txtTelefono.clear();
                    txtCorreo.clear();
                    txtDireccion.clear();
                }

            } else if (toggle == rbNuevo) {
                boxClienteRegistrado.setVisible(false);
                boxClienteRegistrado.setManaged(false);
                boxCamposCliente.setVisible(true);
                boxCamposCliente.setManaged(true);

                txtNombre.setDisable(false);
                txtIdentificacion.setDisable(false);
                txtTelefono.setDisable(false);
                txtCorreo.setDisable(false);
                txtDireccion.setDisable(false);

                txtNombre.clear();
                txtIdentificacion.clear();
                txtTelefono.clear();
                txtCorreo.clear();
                txtDireccion.clear();

            } else if (toggle == rbConsumidor) {
                boxClienteRegistrado.setVisible(false);
                boxClienteRegistrado.setManaged(false);
                boxCamposCliente.setVisible(true);
                boxCamposCliente.setManaged(true);

                txtNombre.setDisable(true);
                txtIdentificacion.setDisable(true);
                txtTelefono.setDisable(true);
                txtCorreo.setDisable(true);
                txtDireccion.setDisable(true);

                txtNombre.setText("Consumidor Final");
                txtIdentificacion.clear();
                txtTelefono.clear();
                txtCorreo.clear();
                txtDireccion.clear();
            }
        });

        // Iniciar estado inicial para cliente registrado
        txtNombre.setDisable(true);
        txtIdentificacion.setDisable(true);
        txtTelefono.setDisable(true);
        txtCorreo.setDisable(true);
        txtDireccion.setDisable(true);

        cardCliente.getChildren().addAll(
                lblClienteTitle,
                boxRadios,
                boxClienteRegistrado,
                boxCamposCliente
        );

        // ── Pago ──────────────────────────────────────────────────────────────
        VBox cardPago = crearCard();
        Label lblPagoTitle = new Label("Método de Pago");
        lblPagoTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblPagoTitle.setStyle("-fx-text-fill: #334155;");

        ComboBox<String> cmbMetodo = new ComboBox<>();
        cmbMetodo.getItems().addAll("Efectivo", "Tarjeta", "Transferencia");
        cmbMetodo.setValue("Efectivo");
        cmbMetodo.setMaxWidth(Double.MAX_VALUE);
        cmbMetodo.setStyle("-fx-font-size: 12px;");

        HBox boxEfectivo = new HBox(10);
        boxEfectivo.setAlignment(Pos.CENTER_LEFT);
        TextField txtMontoRecibido = txtField("Monto recibido");
        txtMontoRecibido.setPrefWidth(140);
        Label lblVuelto = new Label("Vuelto: C$ 0.00");
        lblVuelto.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a; -fx-font-size: 12px;");
        boxEfectivo.getChildren().addAll(new Label("Recibido:"), txtMontoRecibido, lblVuelto);

        txtMontoRecibido.textProperty().addListener((obs, old, now) -> {
            try {
                double recibido = Double.parseDouble(now.replace(",", "."));
                double vuelto = recibido - total;
                lblVuelto.setText(String.format(Locale.US, "Vuelto: C$ %.2f", Math.max(vuelto, 0)));
                lblVuelto.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " +
                        (vuelto >= 0 ? "#16a34a" : "#dc2626") + ";");
            } catch (NumberFormatException ignored) {
                lblVuelto.setText("Vuelto: —");
            }
        });

        cmbMetodo.valueProperty().addListener((obs, old, now) ->
                boxEfectivo.setVisible("Efectivo".equals(now))
        );

        TextArea txtNotas = new TextArea();
        txtNotas.setPromptText("Notas adicionales (opcional)");
        txtNotas.setPrefRowCount(2);
        txtNotas.setStyle("-fx-font-size: 12px;");

        cardPago.getChildren().addAll(lblPagoTitle, etiquetaCampo("Método", cmbMetodo), boxEfectivo, etiquetaCampo("Notas", txtNotas));

        root.getChildren().addAll(lblTitulo, cardResumen, cardCliente, cardPago);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().setPrefHeight(720);

        // ── Estilo botón confirmar ────────────────────────────────────────────
        Button btnConf = (Button) dialog.getDialogPane().lookupButton(btnConfirmar);
        btnConf.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");

        // ── Validación ────────────────────────────────────────────────────────
        btnConf.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (rbRegistrado.isSelected()) {
                if (cmbClientes.getValue() == null) {
                    mostrarAlerta("Selecciona un cliente de la lista o elige 'Nuevo Cliente' / 'Consumidor Final'.");
                    e.consume();
                    return;
                }
            } else if (rbNuevo.isSelected()) {
                if (txtNombre.getText().trim().isEmpty()) {
                    mostrarAlerta("El nombre del nuevo cliente es obligatorio.");
                    e.consume();
                    return;
                }
            }

            if ("Efectivo".equals(cmbMetodo.getValue())) {
                try {
                    double recibido = Double.parseDouble(txtMontoRecibido.getText().replace(",", "."));
                    if (recibido < total) {
                        mostrarAlerta("El monto recibido (" + String.format(Locale.US, "C$ %.2f", recibido) +
                                ") es menor que el total a pagar (" + String.format(Locale.US, "C$ %.2f", total) + ").");
                        e.consume();
                    }
                } catch (NumberFormatException ex) {
                    mostrarAlerta("Ingresa un monto recibido válido.");
                    e.consume();
                }
            }
        });

        // ── Resultado ─────────────────────────────────────────────────────────
        dialog.setResultConverter(tipo -> {
            if (tipo == btnConfirmar) {
                Cliente clienteParaVenta = null;

                if (rbRegistrado.isSelected()) {
                    clienteParaVenta = cmbClientes.getValue();
                } else if (rbNuevo.isSelected()) {
                    String nom = txtNombre.getText().trim();
                    String id = txtIdentificacion.getText().trim();
                    String tel = txtTelefono.getText().trim();
                    String cor = txtCorreo.getText().trim();
                    String dir = txtDireccion.getText().trim();

                    // Buscar si ya existe por identificación
                    Cliente existente = !id.isEmpty() ? clienteDAO.buscarPorIdentificacion(id) : null;
                    if (existente != null) {
                        clienteParaVenta = existente;
                    } else {
                        Cliente nuevo = new Cliente(nom, id, tel, cor, dir);
                        clienteParaVenta = clienteDAO.guardar(nuevo);
                    }
                }

                Venta venta = new Venta();
                venta.setCliente(clienteParaVenta);
                venta.setClienteNombre(txtNombre.getText().trim());
                venta.setClienteIdentificacion(txtIdentificacion.getText().trim());
                venta.setClienteTelefono(txtTelefono.getText().trim());
                venta.setMetodoPago(cmbMetodo.getValue());
                venta.setNotas(txtNotas.getText().trim());
                venta.setSubtotal(subtotal);
                venta.setImpuesto(impuesto);
                venta.setTotal(total);
                venta.setEstado("PAGADA");
                venta.setFechaVenta(new Date());
                venta.setNumeroVenta("VNT-" + System.currentTimeMillis());
                try {
                    venta.setVendedorId(SesionUsuario.getInstancia().getUsuarioActual().getUsuarioId());
                } catch (Exception ignored) {}
                venta.setDetalles(detalles);
                for (DetalleVenta d : detalles) {
                    d.setVenta(venta);
                }
                return venta;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static VBox crearCard() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");
        return card;
    }

    private static TextField txtField(String placeholder) {
        TextField tf = new TextField();
        tf.setPromptText(placeholder);
        tf.setStyle("-fx-font-size: 12px;");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private static Label labelMonto(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
        return l;
    }

    private static VBox etiquetaCampo(String etiqueta, javafx.scene.Node campo) {
        Label lbl = new Label(etiqueta);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        VBox box = new VBox(3, lbl, campo);
        VBox.setVgrow(campo, Priority.ALWAYS);
        return box;
    }

    private static void mostrarAlerta(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle("Validación");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
