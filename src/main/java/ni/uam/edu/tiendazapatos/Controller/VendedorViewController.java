package ni.uam.edu.tiendazapatos.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VendedorViewController {

    public enum Seccion { INICIO, PUNTO_VENTA, BUSCADOR, MIS_VENTAS }

    // ── Header y Breadcrumbs ──────────────────────────────────────────────────
    @FXML private Label lblUsuarioHeader;
    @FXML private Label lblRuta;
    @FXML private Button btnVolver;

    // ── Sidebar Botones ───────────────────────────────────────────────────────
    @FXML private Button btnNavInicio;
    @FXML private Button btnNavVenta;
    @FXML private Button btnNavBuscador;
    @FXML private Button btnNavMisVentas;

    // ── Contenedor dinámico e Inicio ──────────────────────────────────────────
    @FXML private StackPane panelContenido;
    @FXML private ScrollPane vistaInicio;
    @FXML private Label lblBienvenida;
    @FXML private Label lblFechaActual;

    // ── Estado de navegación ──────────────────────────────────────────────────
    private final Map<Seccion, Node> vistasCache = new HashMap<>();
    private final Deque<Seccion> historial = new ArrayDeque<>();
    private Seccion seccionActual = Seccion.INICIO;

    // ── Estilos botones Sidebar ───────────────────────────────────────────────
    private static final String ESTILO_NAV_BASE =
            "-fx-alignment: CENTER_LEFT; -fx-padding: 0 16; -fx-cursor: hand; -fx-font-size: 13px; -fx-background-radius: 7;";
    private static final String ESTILO_ACTIVO = ESTILO_NAV_BASE +
            "-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #38bdf8; -fx-border-width: 0 0 0 4; -fx-border-radius: 7;";
    private static final String ESTILO_INACTIVO = ESTILO_NAV_BASE +
            "-fx-background-color: transparent; -fx-text-fill: #cbd5e1;";

    @FXML
    public void initialize() {
        if (!SesionUsuario.getInstancia().estaAutenticado()) {
            return;
        }

        String nombre = SesionUsuario.getInstancia().getUsuarioActual().getNombreCompleto();
        lblUsuarioHeader.setText(nombre);
        lblBienvenida.setText("¡Bienvenido, " + nombre + "!");

        lblFechaActual.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "NI"))));

        // Registrar pantalla de inicio en caché
        vistasCache.put(Seccion.INICIO, vistaInicio);

        // Iniciar en INICIO
        seccionActual = Seccion.INICIO;
        actualizarNavegacion();
    }

    // ── Handlers de Navegación ────────────────────────────────────────────────
    @FXML
    private void handleNavInicio() {
        if (cambiarSeccion(Seccion.INICIO, false)) {
            historial.clear();
            actualizarNavegacion();
        }
    }

    @FXML
    private void handleNavVenta() {
        cambiarSeccion(Seccion.PUNTO_VENTA, true);
    }

    @FXML
    private void handleNavBuscador() {
        cambiarSeccion(Seccion.BUSCADOR, true);
    }

    @FXML
    private void handleNavMisVentas() {
        cambiarSeccion(Seccion.MIS_VENTAS, true);
    }

    @FXML
    private void handleVolverAtras() {
        if (historial.isEmpty()) {
            cambiarSeccion(Seccion.INICIO, false);
            return;
        }
        Seccion anterior = historial.pop();
        if (!cambiarSeccion(anterior, false)) {
            historial.push(anterior);
        }
        actualizarNavegacion();
    }

    private boolean cambiarSeccion(Seccion destino, boolean guardarHistorial) {
        if (destino == seccionActual) return true;
        try {
            Node vista = obtenerVista(destino);
            if (vista == null) return false;

            if (guardarHistorial) {
                historial.push(seccionActual);
            }

            vista.setVisible(true);
            vista.setManaged(true);
            panelContenido.getChildren().setAll(vista);
            seccionActual = destino;
            actualizarNavegacion();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("No se pudo abrir la sección: " + nombreSeccion(destino) + "\n" + e.getMessage());
            return false;
        }
    }

    private Node obtenerVista(Seccion seccion) {
        return vistasCache.computeIfAbsent(seccion, s -> cargarVista(s));
    }

    private Node cargarVista(Seccion seccion) {
        String fxml = switch (seccion) {
            case INICIO -> null;
            case PUNTO_VENTA -> "PuntoDeVentaView.fxml";
            case BUSCADOR -> "BuscadorCalzadoView.fxml";
            case MIS_VENTAS -> "MisVentasView.fxml";
        };

        if (fxml == null) return vistaInicio;

        try {
            java.net.URL recurso = getClass().getResource("/ni/uam/edu/tiendazapatos/" + fxml);
            if (recurso == null) {
                mostrarAlerta("No se encontró el archivo FXML: " + fxml);
                return null;
            }
            FXMLLoader loader = new FXMLLoader(recurso);
            return loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error al cargar la sección " + fxml + ":\n" + e.getMessage());
            return null;
        }
    }

    private void actualizarNavegacion() {
        if (btnNavInicio != null) btnNavInicio.setStyle(seccionActual == Seccion.INICIO ? ESTILO_ACTIVO : ESTILO_INACTIVO);
        if (btnNavVenta != null) btnNavVenta.setStyle(seccionActual == Seccion.PUNTO_VENTA ? ESTILO_ACTIVO : ESTILO_INACTIVO);
        if (btnNavBuscador != null) btnNavBuscador.setStyle(seccionActual == Seccion.BUSCADOR ? ESTILO_ACTIVO : ESTILO_INACTIVO);
        if (btnNavMisVentas != null) btnNavMisVentas.setStyle(seccionActual == Seccion.MIS_VENTAS ? ESTILO_ACTIVO : ESTILO_INACTIVO);

        if (lblRuta != null) lblRuta.setText(nombreSeccion(seccionActual));

        boolean hayAnterior = seccionActual != Seccion.INICIO && !historial.isEmpty();
        if (btnVolver != null) {
            btnVolver.setVisible(hayAnterior);
            btnVolver.setManaged(hayAnterior);
        }
    }

    private String nombreSeccion(Seccion seccion) {
        return switch (seccion) {
            case INICIO -> "Inicio";
            case PUNTO_VENTA -> "Punto de Venta";
            case BUSCADOR -> "Buscador de Calzado";
            case MIS_VENTAS -> "Mis Ventas del Día";
        };
    }

    // ── Cerrar Sesión ─────────────────────────────────────────────────────────
    @FXML
    private void handleCerrarSesion() {
        SesionUsuario.getInstancia().cerrar();
        try {
            java.net.URL recurso = getClass().getResource("/ni/uam/edu/tiendazapatos/Login.fxml");
            FXMLLoader loader = new FXMLLoader(recurso);
            Stage stage = (Stage) panelContenido.getScene().getWindow();
            stage.getScene().setRoot(loader.load());
            stage.setTitle("Iniciar Sesión");
            stage.sizeToScene();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Navegación");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
