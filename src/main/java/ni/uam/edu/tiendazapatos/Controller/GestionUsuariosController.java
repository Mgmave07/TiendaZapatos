package ni.uam.edu.tiendazapatos.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;
import ni.uam.edu.tiendazapatos.dao.RolDAO;
import ni.uam.edu.tiendazapatos.dao.UsuarioDAO;
import ni.uam.edu.tiendazapatos.modelo.Rol;
import ni.uam.edu.tiendazapatos.modelo.Usuario;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GestionUsuariosController {

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbFiltroRol;
    @FXML private ComboBox<String> cmbFiltroEstado;

    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colNombreCompleto;
    @FXML private TableColumn<Usuario, String> colCorreo;
    @FXML private TableColumn<Usuario, String> colTelefono;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colEstado;
    @FXML private TableColumn<Usuario, String> colCreado;

    @FXML private Label lblSeleccion;
    @FXML private Button btnModificar;
    @FXML private Button btnCambiarRol;
    @FXML private Button btnToggleEstado;
    @FXML private Button btnEliminar;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RolDAO rolDAO = new RolDAO();

    private final ObservableList<Usuario> listaUsuarios = FXCollections.observableArrayList();
    private final FilteredList<Usuario> usuariosFiltrados = new FilteredList<>(listaUsuarios, u -> true);

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        actualizarEstadoBotones(null);
        cargarUsuarios();
    }

    private void configurarTabla() {
        colUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreUsuario()));
        colNombreCompleto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreCompleto()));
        colCorreo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCorreo() != null && !c.getValue().getCorreo().isBlank() ? c.getValue().getCorreo() : "—"));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTelefono() != null && !c.getValue().getTelefono().isBlank() ? c.getValue().getTelefono() : "—"));
        colRol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRol() != null ? c.getValue().getRol().getNombre() : "Sin rol"));

        colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getActivo()) ? "Activo" : "Inactivo"));

        // Estilo en celda de estado para diferenciar visualmente activos de inactivos
        colEstado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Activo".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }
                }
            }
        });

        colCreado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreadoEn() != null ? c.getValue().getCreadoEn().format(FORMATO_FECHA) : "—"));

        SortedList<Usuario> ordenados = new SortedList<>(usuariosFiltrados);
        ordenados.comparatorProperty().bind(tablaUsuarios.comparatorProperty());
        tablaUsuarios.setItems(ordenados);

        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, ant, nuevo) -> {
            actualizarEstadoBotones(nuevo);
        });
    }

    private void actualizarEstadoBotones(Usuario usuario) {
        boolean seleccionado = usuario != null;
        btnModificar.setDisable(!seleccionado);
        btnCambiarRol.setDisable(!seleccionado);
        btnToggleEstado.setDisable(!seleccionado);
        btnEliminar.setDisable(!seleccionado);

        if (seleccionado) {
            String estadoTexto = Boolean.TRUE.equals(usuario.getActivo()) ? "Desactivar" : "Activar";
            btnToggleEstado.setText(Boolean.TRUE.equals(usuario.getActivo()) ? "🔒 Desactivar Cuenta" : "🔓 Activar Cuenta");
            btnToggleEstado.setStyle(Boolean.TRUE.equals(usuario.getActivo())
                    ? "-fx-background-color: #eab308; -fx-text-fill: #713f12; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;"
                    : "-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");

            lblSeleccion.setText(String.format("Usuario: %s (%s) | Rol: %s | Estado: %s",
                    usuario.getNombreUsuario(),
                    usuario.getNombreCompleto(),
                    usuario.getRol() != null ? usuario.getRol().getNombre() : "Sin rol",
                    Boolean.TRUE.equals(usuario.getActivo()) ? "Activo" : "Inactivo"));
        } else {
            btnToggleEstado.setText("🔒 Desactivar / Activar");
            lblSeleccion.setText("Seleccione un usuario de la tabla para realizar operaciones");
        }
    }

    private void configurarFiltros() {
        cmbFiltroRol.getItems().setAll("Todos los roles");
        List<Rol> roles = rolDAO.obtenerTodos();
        if (roles != null) {
            for (Rol r : roles) {
                cmbFiltroRol.getItems().add(r.getNombre());
            }
        }
        cmbFiltroRol.getSelectionModel().selectFirst();

        cmbFiltroEstado.getItems().setAll("Todos los estados", "Solo Activos", "Solo Inactivos");
        cmbFiltroEstado.getSelectionModel().selectFirst();

        txtBuscar.textProperty().addListener((obs, ant, nuevo) -> aplicarFiltros());
        cmbFiltroRol.valueProperty().addListener((obs, ant, nuevo) -> aplicarFiltros());
        cmbFiltroEstado.valueProperty().addListener((obs, ant, nuevo) -> aplicarFiltros());
    }

    private void aplicarFiltros() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        String rolSel = cmbFiltroRol.getValue();
        String estadoSel = cmbFiltroEstado.getValue();

        usuariosFiltrados.setPredicate(u -> {
            // Filtro por texto libre (usuario, nombre, correo, teléfono)
            if (!texto.isEmpty()) {
                String uNombre = u.getNombreUsuario() != null ? u.getNombreUsuario().toLowerCase(Locale.ROOT) : "";
                String uCompleto = u.getNombreCompleto() != null ? u.getNombreCompleto().toLowerCase(Locale.ROOT) : "";
                String uCorreo = u.getCorreo() != null ? u.getCorreo().toLowerCase(Locale.ROOT) : "";
                String uTel = u.getTelefono() != null ? u.getTelefono().toLowerCase(Locale.ROOT) : "";

                if (!uNombre.contains(texto) && !uCompleto.contains(texto) && !uCorreo.contains(texto) && !uTel.contains(texto)) {
                    return false;
                }
            }

            // Filtro por rol
            if (rolSel != null && !"Todos los roles".equalsIgnoreCase(rolSel)) {
                String rolNombre = u.getRol() != null ? u.getRol().getNombre() : "";
                if (!rolSel.equalsIgnoreCase(rolNombre)) {
                    return false;
                }
            }

            // Filtro por estado activo/inactivo
            if (estadoSel != null && !"Todos los estados".equalsIgnoreCase(estadoSel)) {
                if ("Solo Activos".equalsIgnoreCase(estadoSel) && !Boolean.TRUE.equals(u.getActivo())) {
                    return false;
                }
                if ("Solo Inactivos".equalsIgnoreCase(estadoSel) && Boolean.TRUE.equals(u.getActivo())) {
                    return false;
                }
            }

            return true;
        });
    }

    public void cargarUsuarios() {
        try {
            List<Usuario> lista = usuarioDAO.obtenerTodos();
            listaUsuarios.setAll(lista);
            aplicarFiltros();
            actualizarEstadoBotones(tablaUsuarios.getSelectionModel().getSelectedItem());
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron consultar los usuarios: " + e.getMessage());
        }
    }

    @FXML
    private void handleActualizar() {
        cargarUsuarios();
    }

    @FXML
    private void handleLimpiarFiltros() {
        txtBuscar.clear();
        cmbFiltroRol.getSelectionModel().selectFirst();
        cmbFiltroEstado.getSelectionModel().selectFirst();
        aplicarFiltros();
    }

    @FXML
    private void handleNuevoUsuario() {
        Dialog<Usuario> dialog = new Dialog<>();
        dialog.setTitle("Registrar Nuevo Usuario");
        dialog.setHeaderText("Complete los datos del nuevo usuario a registrar en el sistema.");

        ButtonType btnGuardarType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardarType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField txtUser = new TextField();
        txtUser.setPromptText("Ej. jgonzalez");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre completo");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("correo@ejemplo.com");
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Contraseña");
        TextField txtTel = new TextField();
        txtTel.setPromptText("Número de teléfono");

        ComboBox<Rol> cmbRol = new ComboBox<>();
        cmbRol.getItems().setAll(rolDAO.obtenerTodos());
        if (!cmbRol.getItems().isEmpty()) cmbRol.getSelectionModel().selectFirst();

        grid.add(new Label("Nombre de Usuario: *"), 0, 0);
        grid.add(txtUser, 1, 0);
        grid.add(new Label("Nombre Completo: *"), 0, 1);
        grid.add(txtNombre, 1, 1);
        grid.add(new Label("Correo Electrónico: *"), 0, 2);
        grid.add(txtEmail, 1, 2);
        grid.add(new Label("Contraseña: *"), 0, 3);
        grid.add(txtPass, 1, 3);
        grid.add(new Label("Rol: *"), 0, 4);
        grid.add(cmbRol, 1, 4);
        grid.add(new Label("Teléfono:"), 0, 5);
        grid.add(txtTel, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardarType) {
                String uName = txtUser.getText().trim();
                String nCompleto = txtNombre.getText().trim();
                String email = txtEmail.getText().trim();
                String pass = txtPass.getText();
                Rol rol = cmbRol.getValue();

                if (uName.isBlank() || nCompleto.isBlank() || email.isBlank() || pass.isBlank() || rol == null) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Campos obligatorios", "Por favor completa todos los campos marcados con *.");
                    return null;
                }

                if (usuarioDAO.buscarPorNombreUsuario(uName) != null) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Usuario duplicado", "El nombre de usuario '" + uName + "' ya está registrado.");
                    return null;
                }

                if (usuarioDAO.buscarPorCorreo(email) != null) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Correo duplicado", "El correo '" + email + "' ya está registrado.");
                    return null;
                }

                Usuario nuevo = new Usuario();
                nuevo.setNombreUsuario(uName);
                nuevo.setNombreCompleto(nCompleto);
                nuevo.setCorreo(email);
                nuevo.setPasswordHash(pass);
                nuevo.setRol(rol);
                nuevo.setTelefono(txtTel.getText().trim());
                nuevo.setActivo(true);
                nuevo.setCreadoEn(LocalDateTime.now());
                return nuevo;
            }
            return null;
        });

        Optional<Usuario> resultado = dialog.showAndWait();
        resultado.ifPresent(u -> {
            try {
                usuarioDAO.guardar(u);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Usuario '" + u.getNombreUsuario() + "' guardado correctamente.");
                cargarUsuarios();
            } catch (Exception e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo guardar el usuario: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleModificarUsuario() {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        Dialog<Usuario> dialog = new Dialog<>();
        dialog.setTitle("Modificar Información de Usuario");
        dialog.setHeaderText("Editar los datos del usuario: " + seleccionado.getNombreUsuario());

        ButtonType btnActualizarType = new ButtonType("Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnActualizarType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField txtNombre = new TextField(seleccionado.getNombreCompleto());
        TextField txtEmail = new TextField(seleccionado.getCorreo() != null ? seleccionado.getCorreo() : "");
        TextField txtTel = new TextField(seleccionado.getTelefono() != null ? seleccionado.getTelefono() : "");

        ComboBox<Rol> cmbRol = new ComboBox<>();
        cmbRol.getItems().setAll(rolDAO.obtenerTodos());
        if (seleccionado.getRol() != null) {
            for (Rol r : cmbRol.getItems()) {
                if (r.getRolId().equals(seleccionado.getRol().getRolId())) {
                    cmbRol.setValue(r);
                    break;
                }
            }
        }

        PasswordField txtNuevaPass = new PasswordField();
        txtNuevaPass.setPromptText("Dejar en blanco para mantener la actual");

        grid.add(new Label("Usuario:"), 0, 0);
        Label lblUser = new Label(seleccionado.getNombreUsuario());
        lblUser.setStyle("-fx-font-weight: bold;");
        grid.add(lblUser, 1, 0);

        grid.add(new Label("Nombre Completo: *"), 0, 1);
        grid.add(txtNombre, 1, 1);
        grid.add(new Label("Correo Electrónico: *"), 0, 2);
        grid.add(txtEmail, 1, 2);
        grid.add(new Label("Rol: *"), 0, 3);
        grid.add(cmbRol, 1, 3);
        grid.add(new Label("Teléfono:"), 0, 4);
        grid.add(txtTel, 1, 4);
        grid.add(new Label("Nueva Contraseña:"), 0, 5);
        grid.add(txtNuevaPass, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnActualizarType) {
                String nCompleto = txtNombre.getText().trim();
                String email = txtEmail.getText().trim();
                Rol rol = cmbRol.getValue();

                if (nCompleto.isBlank() || email.isBlank() || rol == null) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Campos obligatorios", "Nombre completo, correo y rol son requeridos.");
                    return null;
                }

                seleccionado.setNombreCompleto(nCompleto);
                seleccionado.setCorreo(email);
                seleccionado.setRol(rol);
                seleccionado.setTelefono(txtTel.getText().trim());

                if (!txtNuevaPass.getText().isBlank()) {
                    seleccionado.setPasswordHash(txtNuevaPass.getText().trim());
                }

                return seleccionado;
            }
            return null;
        });

        Optional<Usuario> resultado = dialog.showAndWait();
        resultado.ifPresent(u -> {
            try {
                usuarioDAO.actualizar(u);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Usuario '" + u.getNombreUsuario() + "' actualizado correctamente.");
                cargarUsuarios();
            } catch (Exception e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo actualizar el usuario: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleCambiarRol() {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        List<Rol> roles = rolDAO.obtenerTodos();
        ChoiceDialog<Rol> dialog = new ChoiceDialog<>(seleccionado.getRol(), roles);
        dialog.setTitle("Cambiar Rol");
        dialog.setHeaderText("Asignar nuevo rol a " + seleccionado.getNombreUsuario());
        dialog.setContentText("Seleccione el nuevo rol:");

        Optional<Rol> res = dialog.showAndWait();
        res.ifPresent(nuevoRol -> {
            try {
                seleccionado.setRol(nuevoRol);
                usuarioDAO.actualizar(seleccionado);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Rol Actualizado",
                        "El rol de " + seleccionado.getNombreUsuario() + " ahora es: " + nuevoRol.getNombre());
                cargarUsuarios();
            } catch (Exception e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo cambiar el rol: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleToggleEstado() {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        Usuario actual = SesionUsuario.getInstancia().getUsuarioActual();
        if (actual != null && actual.getUsuarioId().equals(seleccionado.getUsuarioId())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Acción no permitida",
                    "No puedes desactivar tu propia cuenta mientras estás en sesión.");
            return;
        }

        boolean nuevoEstado = !Boolean.TRUE.equals(seleccionado.getActivo());
        String accion = nuevoEstado ? "activar" : "desactivar";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar cambio de estado");
        confirm.setHeaderText("¿Desea " + accion + " la cuenta de " + seleccionado.getNombreUsuario() + "?");
        confirm.setContentText(nuevoEstado
                ? "El usuario podrá iniciar sesión normalmente en el sistema."
                : "El usuario NO podrá iniciar sesión hasta que sea reactivado.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                seleccionado.setActivo(nuevoEstado);
                usuarioDAO.actualizar(seleccionado);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Estado modificado",
                        "El usuario '" + seleccionado.getNombreUsuario() + "' ahora está " + (nuevoEstado ? "ACTIVO" : "INACTIVO") + ".");
                cargarUsuarios();
            } catch (Exception e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo cambiar el estado: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleEliminarUsuario() {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        Usuario actual = SesionUsuario.getInstancia().getUsuarioActual();
        if (actual != null && actual.getUsuarioId().equals(seleccionado.getUsuarioId())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Acción no permitida",
                    "No puedes eliminar tu propia cuenta.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminación permanente");
        confirm.setHeaderText("¿Está seguro de eliminar permanentemente al usuario '" + seleccionado.getNombreUsuario() + "'?");
        confirm.setContentText("⚠️ Esta acción borrará el usuario de la base de datos de forma irreversible.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                boolean eliminado = usuarioDAO.eliminar(seleccionado.getUsuarioId());
                if (eliminado) {
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Usuario Eliminado",
                            "El usuario '" + seleccionado.getNombreUsuario() + "' ha sido eliminado permanentemente de la base de datos.");
                    cargarUsuarios();
                } else {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se encontró el usuario a eliminar.");
                }
            } catch (Exception e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar",
                        "No se pudo eliminar el usuario de la base de datos.\nSi el usuario tiene registros vinculados, use la opción 'Desactivar Cuenta' en su lugar.\nDetalle: " + e.getMessage());
            }
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
