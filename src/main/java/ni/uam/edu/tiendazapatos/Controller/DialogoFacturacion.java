package ni.uam.edu.tiendazapatos.Controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.Venta;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;

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
        root.setPrefWidth(480);
        root.setStyle("-fx-background-color: #f8fafc;");

        // ── Título ────────────────────────────────────────────────────────────
        Label lblTitulo = new Label("NUEVA VENTA");
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
        lblTotalVal.setFont(Font.font("System", FontWeight.BOLD, 15));
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

        CheckBox chkConsumidorFinal = new CheckBox("Consumidor Final");
        chkConsumidorFinal.setStyle("-fx-font-size: 12px;");

        TextField txtNombre         = txtField("Nombre completo");
        TextField txtIdentificacion = txtField("Cédula / RUC");
        TextField txtTelefono       = txtField("Teléfono");

        chkConsumidorFinal.selectedProperty().addListener((obs, old, now) -> {
            txtNombre.setDisable(now);
            txtIdentificacion.setDisable(now);
            txtTelefono.setDisable(now);
            if (now) {
                txtNombre.setText("Consumidor Final");
                txtIdentificacion.clear();
                txtTelefono.clear();
            } else {
                txtNombre.clear();
            }
        });

        cardCliente.getChildren().addAll(
            lblClienteTitle,
            chkConsumidorFinal,
            etiquetaCampo("Nombre *", txtNombre),
            etiquetaCampo("Identificación", txtIdentificacion),
            etiquetaCampo("Teléfono", txtTelefono)
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

        // Sección efectivo (vuelto)
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
        dialog.getDialogPane().setPrefHeight(680);

        // ── Estilo botón confirmar ────────────────────────────────────────────
        Button btnConf = (Button) dialog.getDialogPane().lookupButton(btnConfirmar);
        btnConf.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");

        // ── Validación ────────────────────────────────────────────────────────
        btnConf.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (!chkConsumidorFinal.isSelected() && txtNombre.getText().trim().isEmpty()) {
                mostrarAlerta("El nombre del cliente es obligatorio (o marca 'Consumidor Final').");
                e.consume();
                return;
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
                Venta venta = new Venta();
                venta.setClienteNombre(chkConsumidorFinal.isSelected() ? "Consumidor Final" : txtNombre.getText().trim());
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
