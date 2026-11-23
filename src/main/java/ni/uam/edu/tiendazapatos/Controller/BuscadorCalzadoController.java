package ni.uam.edu.tiendazapatos.Controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.uam.edu.tiendazapatos.dao.InventarioDAO;
import ni.uam.edu.tiendazapatos.dao.MarcaDAO;
import ni.uam.edu.tiendazapatos.dao.ZapatoDAO;
import ni.uam.edu.tiendazapatos.modelo.InventarioFila;
import ni.uam.edu.tiendazapatos.modelo.Marca;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.util.*;

public class BuscadorCalzadoController {

    @FXML private TextField txtModelo;
    @FXML private ComboBox<String> cmbMarca;
    @FXML private TextField txtTalla;
    @FXML private ComboBox<String> cmbStock;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private TextField txtPrecioMin;
    @FXML private TextField txtPrecioMax;
    @FXML private Label lblResultados;

    @FXML private TableView<InventarioFila> tablaProductos;
    @FXML private TableColumn<InventarioFila, String> colNombre;
    @FXML private TableColumn<InventarioFila, String> colMarca;
    @FXML private TableColumn<InventarioFila, String> colGenero;
    @FXML private TableColumn<InventarioFila, String> colSistemaTalla;
    @FXML private TableColumn<InventarioFila, String> colTalla;
    @FXML private TableColumn<InventarioFila, String> colColor;
    @FXML private TableColumn<InventarioFila, String> colStock;
    @FXML private TableColumn<InventarioFila, String> colPrecio;
    @FXML private TableColumn<InventarioFila, String> colEstado;

    private final ZapatoDAO zapatoDAO = new ZapatoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final MarcaDAO marcaDAO = new MarcaDAO();

    private final ObservableList<InventarioFila> filasProductos = FXCollections.observableArrayList();
    private final FilteredList<InventarioFila> filtradas = new FilteredList<>(filasProductos, p -> true);

    @FXML
    public void initialize() {
        configurarColumnas();
        configurarFiltros();
        cargarProductos();
    }

    private void configurarColumnas() {
        colNombre.setCellValueFactory(c -> texto(c.getValue().zapato().getNombre()));
        colMarca.setCellValueFactory(c -> texto(
                c.getValue().zapato().getMarca() == null ? "Sin marca" :
                        c.getValue().zapato().getMarca().getNombre()));
        colGenero.setCellValueFactory(c -> texto(c.getValue().genero()));
        colSistemaTalla.setCellValueFactory(c -> texto(c.getValue().sistemaTalla()));
        colTalla.setCellValueFactory(c -> texto(c.getValue().talla()));
        colColor.setCellValueFactory(c -> texto(c.getValue().color()));
        colStock.setCellValueFactory(c -> texto(c.getValue().stock() == null
                ? "Sin registrar" : c.getValue().stock().toString()));
        colPrecio.setCellValueFactory(c -> texto(c.getValue().zapato().getPrecioFinal() == null
                ? "—" : "C$ " + c.getValue().zapato().getPrecioFinal()));
        colEstado.setCellValueFactory(c -> texto(estado(c.getValue())));

        SortedList<InventarioFila> ordenadas = new SortedList<>(filtradas);
        ordenadas.comparatorProperty().bind(tablaProductos.comparatorProperty());
        tablaProductos.setItems(ordenadas);
    }

    private void configurarFiltros() {
        cargarMarcas();

        cmbStock.getItems().setAll("Todos", "Disponibles (>0)", "Agotados (=0)", "Bajo stock (≤5)");
        cmbStock.setValue("Disponibles (>0)");

        cmbGenero.getItems().setAll("Todos", "HOMBRE", "MUJER", "UNISEX");
        cmbGenero.getSelectionModel().selectFirst();

        txtModelo.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        cmbMarca.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        txtTalla.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        cmbStock.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        cmbGenero.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());

        txtPrecioMin.textProperty().addListener((obs, anterior, nuevo) -> {
            if (!nuevo.matches("\\d*(\\.\\d*)?")) {
                txtPrecioMin.setText(anterior);
            } else {
                aplicarFiltros();
            }
        });

        txtPrecioMax.textProperty().addListener((obs, anterior, nuevo) -> {
            if (!nuevo.matches("\\d*(\\.\\d*)?")) {
                txtPrecioMax.setText(anterior);
            } else {
                aplicarFiltros();
            }
        });
    }

    private void cargarMarcas() {
        List<Marca> marcas = marcaDAO.obtenerTodas();
        List<String> nombresMarcas = new ArrayList<>();
        nombresMarcas.add("Todas");
        if (marcas != null) {
            for (Marca m : marcas) {
                if (m.getNombre() != null && !m.getNombre().isBlank()) {
                    nombresMarcas.add(m.getNombre());
                }
            }
        }
        cmbMarca.getItems().setAll(nombresMarcas);
        cmbMarca.getSelectionModel().selectFirst();
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor == null || valor.isBlank() ? "—" : valor);
    }

    private static String estado(InventarioFila fila) {
        if (!Boolean.TRUE.equals(fila.zapato().getActivo())) return "Inactivo";
        if (fila.stock() == null) return "Sin registro";
        return fila.stock() > 0 ? "Disponible" : "Agotado";
    }

    private void cargarProductos() {
        try {
            List<Zapato> zapatos = zapatoDAO.obtenerTodos();
            Map<UUID, List<InventarioDAO.Variante>> variantesPorZapato = new HashMap<>();
            for (InventarioDAO.Variante variante : inventarioDAO.obtenerVariantes()) {
                variantesPorZapato.computeIfAbsent(variante.zapatoId(), id -> new ArrayList<>())
                        .add(variante);
            }

            filasProductos.clear();
            for (Zapato zapato : zapatos) {
                List<InventarioDAO.Variante> variantes = variantesPorZapato.get(zapato.getZapatoId());
                if (variantes == null || variantes.isEmpty()) {
                    filasProductos.add(new InventarioFila(zapato, null, null, null, null, null));
                } else {
                    for (InventarioDAO.Variante variante : variantes) {
                        filasProductos.add(new InventarioFila(
                                variante.inventarioId(),
                                zapato,
                                variante.genero(),
                                variante.sistemaTalla(),
                                variante.talla(),
                                variante.color(),
                                variante.cantidadStock()
                        ));
                    }
                }
            }
            aplicarFiltros();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void aplicarFiltros() {
        String modelo = txtModelo.getText() == null ? "" : txtModelo.getText().trim().toLowerCase(Locale.ROOT);
        String marcaSel = cmbMarca.getValue();
        String tallaFiltro = txtTalla.getText() == null ? "" : txtTalla.getText().trim().toLowerCase(Locale.ROOT);
        String stockSel = cmbStock.getValue();
        String generoSel = cmbGenero.getValue();
        String minStr = txtPrecioMin.getText() == null ? "" : txtPrecioMin.getText().trim();
        String maxStr = txtPrecioMax.getText() == null ? "" : txtPrecioMax.getText().trim();

        Double minPrecio = null;
        Double maxPrecio = null;
        try {
            if (!minStr.isEmpty()) minPrecio = Double.parseDouble(minStr);
        } catch (NumberFormatException ignored) {}
        try {
            if (!maxStr.isEmpty()) maxPrecio = Double.parseDouble(maxStr);
        } catch (NumberFormatException ignored) {}

        final Double finalMin = minPrecio;
        final Double finalMax = maxPrecio;

        filtradas.setPredicate(fila -> {
            if (!modelo.isEmpty()) {
                String nombreZapato = fila.zapato() != null && fila.zapato().getNombre() != null
                        ? fila.zapato().getNombre().toLowerCase(Locale.ROOT) : "";
                if (!nombreZapato.contains(modelo)) return false;
            }

            if (marcaSel != null && !"Todas".equalsIgnoreCase(marcaSel) && !marcaSel.isBlank()) {
                String marcaZapato = fila.zapato() != null && fila.zapato().getMarca() != null
                        ? fila.zapato().getMarca().getNombre() : "";
                if (!marcaSel.equalsIgnoreCase(marcaZapato)) return false;
            }

            if (!tallaFiltro.isEmpty()) {
                String tallaZapato = fila.talla() == null ? "" : fila.talla().toLowerCase(Locale.ROOT);
                if (!tallaZapato.contains(tallaFiltro)) return false;
            }

            if (stockSel != null && !"Todos".equalsIgnoreCase(stockSel)) {
                int stockVal = fila.stock() == null ? 0 : fila.stock();
                boolean activo = fila.zapato() != null && Boolean.TRUE.equals(fila.zapato().getActivo());

                if (stockSel.contains("Disponibles")) {
                    if (stockVal <= 0 || !activo) return false;
                } else if (stockSel.contains("Agotados")) {
                    if (stockVal > 0) return false;
                } else if (stockSel.contains("bajo") || stockSel.contains("Bajo")) {
                    if (stockVal <= 0 || stockVal > 5) return false;
                }
            }

            Double precioFinal = fila.zapato() != null && fila.zapato().getPrecioFinal() != null
                    ? fila.zapato().getPrecioFinal().doubleValue() : null;

            if (finalMin != null) {
                if (precioFinal == null || precioFinal < finalMin) return false;
            }
            if (finalMax != null) {
                if (precioFinal == null || precioFinal > finalMax) return false;
            }

            if (generoSel != null && !"Todos".equalsIgnoreCase(generoSel) && !generoSel.isBlank()) {
                if (!generoSel.equalsIgnoreCase(fila.genero())) return false;
            }

            return true;
        });

        int total = filtradas.size();
        lblResultados.setText(total + " variante(s) encontrada(s)");
    }

    @FXML
    private void handleLimpiarFiltros() {
        txtModelo.clear();
        if (cmbMarca.getItems().contains("Todas")) {
            cmbMarca.setValue("Todas");
        } else {
            cmbMarca.getSelectionModel().selectFirst();
        }
        txtTalla.clear();
        if (cmbStock.getItems().contains("Disponibles (>0)")) {
            cmbStock.setValue("Disponibles (>0)");
        } else {
            cmbStock.getSelectionModel().selectFirst();
        }
        cmbGenero.getSelectionModel().selectFirst();
        txtPrecioMin.clear();
        txtPrecioMax.clear();
        aplicarFiltros();
    }

    @FXML
    private void handleActualizar() {
        cargarProductos();
    }
}
