package ni.uam.edu.tiendazapatos.Controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ni.uam.edu.tiendazapatos.reportes.ReporteService;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ReportesController {

    @FXML private Label lblConteoInventario;
    @FXML private Label lblConteoUsuarios;
    @FXML private Label lblConteoVentas;
    @FXML private Label lblConteoDetalleVentas;
    @FXML private Label lblEstadoReporte;

    @FXML private DatePicker dpVentasDesde;
    @FXML private DatePicker dpVentasHasta;
    @FXML private DatePicker dpDetalleDesde;
    @FXML private DatePicker dpDetalleHasta;
    @FXML private CheckBox chkAbrirAlGenerar;

    @FXML private Button btnGenerarInventario;
    @FXML private Button btnGenerarUsuarios;
    @FXML private Button btnGenerarVentas;
    @FXML private Button btnGenerarVentasDetalle;
    @FXML private Button btnActualizarResumen;

    private final ReporteService reporteService = new ReporteService();
    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        if (chkAbrirAlGenerar != null) {
            chkAbrirAlGenerar.setSelected(true);
        }
        if (dpVentasDesde != null) {
            dpVentasDesde.valueProperty().addListener((obs, ant, nuevo) -> actualizarConteoVentas());
        }
        if (dpVentasHasta != null) {
            dpVentasHasta.valueProperty().addListener((obs, ant, nuevo) -> actualizarConteoVentas());
        }
        if (dpDetalleDesde != null) {
            dpDetalleDesde.valueProperty().addListener((obs, ant, nuevo) -> actualizarConteoVentasDetalle());
        }
        if (dpDetalleHasta != null) {
            dpDetalleHasta.valueProperty().addListener((obs, ant, nuevo) -> actualizarConteoVentasDetalle());
        }
        actualizarResumen();
    }

    @FXML
    private void handleActualizarResumen() {
        actualizarResumen();
        lblEstadoReporte.setText("Conteos actualizados correctamente.");
    }

    @FXML
    private void handleLimpiarFechasVentas() {
        dpVentasDesde.setValue(null);
        dpVentasHasta.setValue(null);
        actualizarConteoVentas();
    }

    @FXML
    private void handleLimpiarFechasDetalle() {
        if (dpDetalleDesde != null) dpDetalleDesde.setValue(null);
        if (dpDetalleHasta != null) dpDetalleHasta.setValue(null);
        actualizarConteoVentasDetalle();
    }

    @FXML
    private void handleGenerarInventario() {
        try {
            var datos = reporteService.obtenerDatosInventario();
            if (datos.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Sin datos",
                        "No hay productos registrados en el inventario para generar el reporte.");
                return;
            }
            File destino = pedirArchivoDestino(
                    "Guardar Reporte de Inventario",
                    "Reporte_Inventario_" + LocalDate.now().format(FORMATO_ARCHIVO) + ".pdf"
            );
            if (destino == null) return;

            ejecutarExportacion(
                    "Inventario",
                    destino,
                    () -> reporteService.generarReporteInventario(destino)
            );
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error al preparar reporte",
                    "No se pudo consultar el inventario: " + e.getMessage());
        }
    }

    @FXML
    private void handleGenerarUsuarios() {
        try {
            var datos = reporteService.obtenerDatosUsuarios();
            if (datos.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Sin datos",
                        "No hay usuarios registrados en el sistema para generar el reporte.");
                return;
            }
            File destino = pedirArchivoDestino(
                    "Guardar Reporte de Usuarios",
                    "Reporte_Usuarios_" + LocalDate.now().format(FORMATO_ARCHIVO) + ".pdf"
            );
            if (destino == null) return;

            ejecutarExportacion(
                    "Usuarios",
                    destino,
                    () -> reporteService.generarReporteUsuarios(destino)
            );
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error al preparar reporte",
                    "No se pudieron consultar los usuarios: " + e.getMessage());
        }
    }

    @FXML
    private void handleGenerarVentas() {
        LocalDate desde = dpVentasDesde.getValue();
        LocalDate hasta = dpVentasHasta.getValue();

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            mostrarAlerta(Alert.AlertType.WARNING, "Rango de fechas inválido",
                    "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }

        try {
            var datos = reporteService.obtenerDatosVentas(desde, hasta);
            if (datos.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Sin datos",
                        (desde != null || hasta != null)
                                ? "No hay ventas registradas en el rango de fechas seleccionado."
                                : "No hay ventas registradas en la base de datos todavía.");
                return;
            }
            File destino = pedirArchivoDestino(
                    "Guardar Reporte de Ventas",
                    "Reporte_Ventas_" + LocalDate.now().format(FORMATO_ARCHIVO) + ".pdf"
            );
            if (destino == null) return;

            ejecutarExportacion(
                    "Ventas",
                    destino,
                    () -> reporteService.generarReporteVentas(destino, desde, hasta)
            );
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error al preparar reporte",
                    "No se pudieron consultar las ventas: " + e.getMessage());
        }
    }

    @FXML
    private void handleGenerarVentasDetalle() {
        LocalDate desde = dpDetalleDesde != null ? dpDetalleDesde.getValue() : null;
        LocalDate hasta = dpDetalleHasta != null ? dpDetalleHasta.getValue() : null;

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            mostrarAlerta(Alert.AlertType.WARNING, "Rango de fechas inválido",
                    "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }

        try {
            var datos = reporteService.obtenerDatosVentasDetalle(desde, hasta);
            if (datos.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Sin datos",
                        (desde != null || hasta != null)
                                ? "No hay líneas de ventas registradas en el rango de fechas seleccionado."
                                : "No hay ventas registradas en la base de datos todavía.");
                return;
            }
            File destino = pedirArchivoDestino(
                    "Guardar Reporte de Ventas por Producto",
                    "Reporte_Ventas_Por_Producto_" + LocalDate.now().format(FORMATO_ARCHIVO) + ".pdf"
            );
            if (destino == null) return;

            ejecutarExportacion(
                    "Ventas por Producto",
                    destino,
                    () -> reporteService.generarReporteVentasDetalle(destino, desde, hasta)
            );
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error al preparar reporte",
                    "No se pudieron consultar las ventas por producto: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ExportacionCallable {
        int ejecutar() throws Exception;
    }

    private void ejecutarExportacion(String nombreReporte, File destino, ExportacionCallable accion) {
        establecerOcupado(true, "Generando reporte PDF de " + nombreReporte + "...");

        Task<Integer> tarea = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return accion.ejecutar();
            }
        };

        tarea.setOnSucceeded(ev -> {
            int registros = tarea.getValue() != null ? tarea.getValue() : 0;
            establecerOcupado(false, "PDF generado: " + destino.getName() + " (" + registros + " registros)");
            actualizarResumen();

            boolean abrir = chkAbrirAlGenerar != null && chkAbrirAlGenerar.isSelected();
            if (abrir) {
                String errorApertura = intentarAbrirPdf(destino);
                if (errorApertura == null) {
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Reporte generado",
                            "El reporte de " + nombreReporte + " se guardó con " + registros +
                                    " registros en:\n" + destino.getAbsolutePath());
                } else {
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Reporte generado",
                            "El reporte se guardó correctamente en:\n" + destino.getAbsolutePath() +
                                    "\n\nNo se pudo abrir automáticamente: " + errorApertura);
                }
            } else {
                Alert confirm = new Alert(Alert.AlertType.INFORMATION);
                confirm.setTitle("Reporte generado");
                confirm.setHeaderText(null);
                confirm.setContentText("El reporte de " + nombreReporte + " (" + registros +
                        " registros) se guardó en:\n" + destino.getAbsolutePath());
                ButtonType btnAbrir = new ButtonType("Abrir PDF");
                ButtonType btnCerrar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
                confirm.getButtonTypes().setAll(btnAbrir, btnCerrar);
                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isPresent() && res.get() == btnAbrir) {
                    String err = intentarAbrirPdf(destino);
                    if (err != null) {
                        mostrarAlerta(Alert.AlertType.WARNING, "No se pudo abrir el PDF", err);
                    }
                }
            }
        });

        tarea.setOnFailed(ev -> {
            Throwable ex = tarea.getException();
            establecerOcupado(false, "No se pudo generar el reporte de " + nombreReporte + ".");
            if (ex instanceof ReporteService.SinDatosReporteException) {
                mostrarAlerta(Alert.AlertType.WARNING, "Sin datos", ex.getMessage());
            } else {
                if (ex != null) ex.printStackTrace();
                String detalle = ex != null && ex.getMessage() != null ? ex.getMessage() : "Error desconocido";
                mostrarAlerta(Alert.AlertType.ERROR, "Error al generar PDF",
                        "Ocurrió un error al generar el reporte de " + nombreReporte + ":\n" + detalle);
            }
        });

        Thread hilo = new Thread(tarea, "jasper-export-" + nombreReporte.toLowerCase());
        hilo.setDaemon(true);
        hilo.start();
    }

    private File pedirArchivoDestino(String titulo, String nombreSugerido) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(titulo);
        chooser.setInitialFileName(nombreSugerido);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos PDF (*.pdf)", "*.pdf"));

        Window ventana = btnGenerarInventario != null && btnGenerarInventario.getScene() != null
                ? btnGenerarInventario.getScene().getWindow()
                : null;
        File seleccionado = chooser.showSaveDialog(ventana);
        if (seleccionado == null) {
            return null;
        }
        if (!seleccionado.getName().toLowerCase().endsWith(".pdf")) {
            return new File(seleccionado.getParentFile(), seleccionado.getName() + ".pdf");
        }
        return seleccionado;
    }

    private String intentarAbrirPdf(File archivo) {
        if (archivo == null || !archivo.exists()) {
            return "El archivo no existe.";
        }
        if (!Desktop.isDesktopSupported()) {
            return "El entorno gráfico no soporta la apertura automática de archivos.";
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            return "El sistema no permite abrir archivos con Desktop.Action.OPEN.";
        }
        try {
            desktop.open(archivo);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private void actualizarResumen() {
        try {
            int totalInv = reporteService.obtenerDatosInventario().size();
            lblConteoInventario.setText(totalInv + (totalInv == 1 ? " registro disponible" : " registros disponibles"));
        } catch (Exception e) {
            lblConteoInventario.setText("Error al consultar");
        }

        try {
            int totalUsu = reporteService.obtenerDatosUsuarios().size();
            lblConteoUsuarios.setText(totalUsu + (totalUsu == 1 ? " usuario registrado" : " usuarios registrados"));
        } catch (Exception e) {
            lblConteoUsuarios.setText("Error al consultar");
        }

        actualizarConteoVentas();
        actualizarConteoVentasDetalle();
    }

    private void actualizarConteoVentas() {
        try {
            LocalDate desde = dpVentasDesde != null ? dpVentasDesde.getValue() : null;
            LocalDate hasta = dpVentasHasta != null ? dpVentasHasta.getValue() : null;
            if (desde != null && hasta != null && desde.isAfter(hasta)) {
                lblConteoVentas.setText("Rango inválido (Desde > Hasta)");
                return;
            }
            int totalVen = reporteService.obtenerDatosVentas(desde, hasta).size();
            if (totalVen == 0) {
                lblConteoVentas.setText("0 ventas disponibles para este criterio");
            } else {
                lblConteoVentas.setText(totalVen + (totalVen == 1 ? " venta disponible" : " ventas disponibles"));
            }
        } catch (Exception e) {
            lblConteoVentas.setText("Error al consultar");
        }
    }

    private void actualizarConteoVentasDetalle() {
        try {
            if (lblConteoDetalleVentas == null) return;
            LocalDate desde = dpDetalleDesde != null ? dpDetalleDesde.getValue() : null;
            LocalDate hasta = dpDetalleHasta != null ? dpDetalleHasta.getValue() : null;
            if (desde != null && hasta != null && desde.isAfter(hasta)) {
                lblConteoDetalleVentas.setText("Rango inválido (Desde > Hasta)");
                return;
            }
            int totalDet = reporteService.obtenerDatosVentasDetalle(desde, hasta).size();
            if (totalDet == 0) {
                lblConteoDetalleVentas.setText("0 líneas disponibles para este criterio");
            } else {
                lblConteoDetalleVentas.setText(totalDet + (totalDet == 1 ? " línea de venta disponible" : " líneas de venta disponibles"));
            }
        } catch (Exception e) {
            if (lblConteoDetalleVentas != null) {
                lblConteoDetalleVentas.setText("Error al consultar");
            }
        }
    }

    private void establecerOcupado(boolean ocupado, String estadoTexto) {
        btnGenerarInventario.setDisable(ocupado);
        btnGenerarUsuarios.setDisable(ocupado);
        btnGenerarVentas.setDisable(ocupado);
        if (btnGenerarVentasDetalle != null) {
            btnGenerarVentasDetalle.setDisable(ocupado);
        }
        btnActualizarResumen.setDisable(ocupado);
        if (lblEstadoReporte != null) {
            lblEstadoReporte.setText(estadoTexto);
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
