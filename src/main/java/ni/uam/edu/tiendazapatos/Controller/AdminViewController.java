package ni.uam.edu.tiendazapatos.Controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;
import ni.uam.edu.tiendazapatos.modelo.Usuario;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public class AdminViewController {

    @FXML private Label lblAdminInfo;
    @FXML private Label lblBienvenida;
    @FXML private Button btnCerrarSesion;
    @FXML private Button btnNavInicio;
    @FXML private Button btnNavInventario;
    @FXML private Button btnNavVentas;
    @FXML private Button btnNavHistorialVentas;
    @FXML private Button btnNavUsuarios;
    @FXML private Button btnNavClientes;
    @FXML private Button btnNavReportes;
    @FXML private Button btnVolver;
    @FXML private Label lblRuta;
    @FXML private Label lblFechaActual;
    @FXML private StackPane panelContenido;
    @FXML private ScrollPane vistaInicio;

    // Vistas cargadas dinámicamente bajo demanda
    private Parent vistaInventario;
    private Parent vistaVentas;
    private Parent vistaHistorialVentas;
    private Parent vistaUsuarios;
    private Parent vistaClientes;
    private Parent vistaReportes;

    private enum Seccion { INICIO, INVENTARIO, VENTAS, HISTORIAL_VENTAS, USUARIOS, CLIENTES, REPORTES }
    private Seccion seccionActual = Seccion.INICIO;
    private final Deque<Seccion> historial = new ArrayDeque<>();

    private static final String ESTILO_NAV_BASE =
            "-fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 7; " +
            "-fx-padding: 12 16; -fx-font-size: 14px; ";

    @FXML
    public void initialize() {
        if (!SesionUsuario.getInstancia().estaAutenticado()
                || !SesionUsuario.getInstancia().esAdministrador()) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Acceso denegado");
                alert.setHeaderText("Acceso no autorizado");
                alert.setContentText("No tienes permisos de administrador para acceder a este panel.");
                alert.showAndWait();
                redirigirALogin();
            });
            return;
        }

        Usuario usuarioActual = SesionUsuario.getInstancia().getUsuarioActual();
        if (usuarioActual != null) {
            String nombre = usuarioActual.getNombreCompleto();
            lblAdminInfo.setText("Administrador: " + nombre);
            lblBienvenida.setText("¡Bienvenido, " + nombre + "!");
        }

        lblFechaActual.setText("Hoy es " + LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "NI"))));

        actualizarNavegacion();
    }

    @FXML
    private void mostrarInicio() {
        if (cambiarSeccion(Seccion.INICIO, false)) {
            historial.clear();
            actualizarNavegacion();
        }
    }

    @FXML
    private void mostrarInventario() {
        cambiarSeccion(Seccion.INVENTARIO, true);
    }

    @FXML
    private void mostrarVentas() {
        cambiarSeccion(Seccion.VENTAS, true);
    }

    @FXML
    private void mostrarHistorialVentas() {
        cambiarSeccion(Seccion.HISTORIAL_VENTAS, true);
    }

    @FXML
    private void mostrarUsuarios() {
        cambiarSeccion(Seccion.USUARIOS, true);
    }

    @FXML
    private void mostrarClientes() {
        cambiarSeccion(Seccion.CLIENTES, true);
    }

    @FXML
    private void mostrarReportes() {
        cambiarSeccion(Seccion.REPORTES, true);
    }

    @FXML
    private void mostrarRegistro() {
        mostrarUsuarios();
    }

    @FXML
    private void volverAtras() {
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
            mostrarError("No se pudo abrir la sección " + nombreSeccion(destino), e);
            return false;
        }
    }

    private Node obtenerVista(Seccion seccion) throws IOException {
        return switch (seccion) {
            case INICIO -> vistaInicio;
            case INVENTARIO -> {
                if (vistaInventario == null) vistaInventario = cargarVista("ZapatosView.fxml");
                yield vistaInventario;
            }
            case VENTAS -> {
                if (vistaVentas == null) vistaVentas = cargarVista("VentasView.fxml");
                yield vistaVentas;
            }
            case HISTORIAL_VENTAS -> {
                if (vistaHistorialVentas == null) vistaHistorialVentas = cargarVista("HistorialVentasView.fxml");
                yield vistaHistorialVentas;
            }
            case USUARIOS -> {
                if (vistaUsuarios == null) vistaUsuarios = cargarVista("GestionUsuariosView.fxml");
                yield vistaUsuarios;
            }
            case CLIENTES -> {
                if (vistaClientes == null) vistaClientes = cargarVista("GestionClientesView.fxml");
                yield vistaClientes;
            }
            case REPORTES -> {
                if (vistaReportes == null) vistaReportes = cargarVista("ReportesView.fxml");
                yield vistaReportes;
            }
        };
    }

    private String nombreSeccion(Seccion seccion) {
        return switch (seccion) {
            case INICIO -> "Inicio";
            case INVENTARIO -> "Inventario";
            case VENTAS -> "Facturación";
            case HISTORIAL_VENTAS -> "Historial de ventas";
            case USUARIOS -> "Gestión de usuarios";
            case CLIENTES -> "Gestión de clientes";
            case REPORTES -> "Reportes";
        };
    }

    private void actualizarNavegacion() {
        marcarActivo(btnNavInicio, seccionActual == Seccion.INICIO);
        marcarActivo(btnNavInventario, seccionActual == Seccion.INVENTARIO);
        marcarActivo(btnNavVentas, seccionActual == Seccion.VENTAS);
        marcarActivo(btnNavHistorialVentas, seccionActual == Seccion.HISTORIAL_VENTAS);
        marcarActivo(btnNavUsuarios, seccionActual == Seccion.USUARIOS);
        marcarActivo(btnNavClientes, seccionActual == Seccion.CLIENTES);
        marcarActivo(btnNavReportes, seccionActual == Seccion.REPORTES);

        lblRuta.setText(nombreSeccion(seccionActual));
        boolean hayAnterior = seccionActual != Seccion.INICIO && !historial.isEmpty();
        btnVolver.setVisible(hayAnterior);
        btnVolver.setManaged(hayAnterior);
    }

    private void marcarActivo(Button boton, boolean activo) {
        if (boton == null) return;
        boton.setStyle(ESTILO_NAV_BASE + (activo
                ? "-fx-background-color: #45678b; -fx-font-weight: bold; " +
                  "-fx-border-color: #8fc0ff; -fx-border-width: 0 0 0 4; -fx-border-radius: 7;"
                : "-fx-background-color: transparent;"));
    }

    private Parent cargarVista(String archivo) throws IOException {
        String ruta = "/ni/uam/edu/tiendazapatos/" + archivo;
        URL recurso = getClass().getResource(ruta);
        if (recurso == null) {
            throw new IOException("No se encontró la vista: " + ruta);
        }
        return FXMLLoader.load(recurso);
    }

    private void mostrarError(String mensaje, Exception e) {
        e.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de navegación");
        alert.setHeaderText(mensaje);
        alert.setContentText(e != null ? e.getMessage() : "Error desconocido");
        alert.showAndWait();
    }

    @FXML
    private void handleCerrarSesion(ActionEvent event) {
        SesionUsuario.getInstancia().cerrar();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        navegarALogin(stage);
    }

    private void redirigirALogin() {
        if (btnCerrarSesion != null && btnCerrarSesion.getScene() != null) {
            Stage stage = (Stage) btnCerrarSesion.getScene().getWindow();
            navegarALogin(stage);
        }
    }

    private void navegarALogin(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/uam/edu/tiendazapatos/Login.fxml"));
            Scene scene = new Scene(loader.load(), 450, 500);
            stage.setTitle("Sistema Tienda de Zapatos - Login");
            stage.setScene(scene);
            stage.setMaximized(false);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            mostrarError("Error al cerrar sesión", e);
        }
    }
}
