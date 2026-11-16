package ni.uam.edu.tiendazapatos.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.io.File;

public class DetalleZapatoController {
    @FXML private Label lblTitulo;
    @FXML private ImageView imgZapato;
    @FXML
    private Label lblNombre;
    @FXML private Label lblMarca;
    @FXML private Label lblPrecio;
    @FXML private Label lblDescripcion;

    private Zapato zapato;

    public void setZapato(Zapato zapato) {
        this.zapato = zapato;
        cargarDatos();
    }

    private void cargarDatos() {
        if (zapato == null) return;

        lblNombre.setText(zapato.getNombre());
        lblMarca.setText("Marca: " + (zapato.getMarca() != null ? zapato.getMarca().getNombre() : "Sin marca"));
        lblPrecio.setText("Precio Final: $" + zapato.getPrecioFinal().toString());
        lblDescripcion.setText(zapato.getDescripcion() != null ? zapato.getDescripcion() : "Sin descripción");

        // Cargar imagen si existe la ruta
        if (zapato.getImagenUrl() != null && !zapato.getImagenUrl().isEmpty()) {
            File file = new File(zapato.getImagenUrl());
            if (file.exists()) {
                Image image = new Image(file.toURI().toString());
                imgZapato.setImage(image);
            }
        }
    }

    @FXML
    private void handleCerrar() {
        Stage stage = (Stage) lblNombre.getScene().getWindow();
        stage.close();
    }
}
