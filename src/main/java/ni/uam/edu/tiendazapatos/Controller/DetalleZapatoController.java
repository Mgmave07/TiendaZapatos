package ni.uam.edu.tiendazapatos.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Detalles de productos: funciona con URLs web y recursos locales.
 * Las fotos externas son de sitios de terceros y requieren conexion a internet.
 * La foto de Revolution 8 puede depender de la variante seleccionada.
 */
public class DetalleZapatoController {
    @FXML private Label lblTitulo;
    @FXML private ImageView imgZapato;
    @FXML private Label lblNombre;
    @FXML private Label lblMarca;
    @FXML private Label lblPrecio;
    @FXML private Label lblDescripcion;

    private Zapato zapato;
    private String genero;
    private String color;

    public void setZapato(Zapato zapato) {
        setZapato(zapato, null, null);
    }

    public void setZapato(Zapato zapato, String genero, String color) {
        this.zapato = zapato;
        this.genero = genero;
        this.color = color;
        cargarDatos();
    }

    private void cargarDatos() {
        if (zapato == null) return;

        lblNombre.setText(zapato.getNombre());
        lblMarca.setText("Marca: " + (zapato.getMarca() == null ? "Sin marca" : zapato.getMarca().getNombre()));
        lblPrecio.setText("Precio Final: C$" + zapato.getPrecioFinal());
        lblDescripcion.setText(zapato.getDescripcion() == null ? "Sin descripcion" : zapato.getDescripcion());
        cargarImagen();
    }

    private void cargarImagen() {
        imgZapato.setImage(null);
        imgZapato.setPreserveRatio(true);

        LinkedHashSet<String> candidatos = new LinkedHashSet<>();
        String nombre = zapato.getNombre() == null ? "" : zapato.getNombre().toLowerCase(Locale.ROOT);
        String ruta = zapato.getImagenUrl();

        // Una fila de hombre/negro y otra de mujer/blanco del mismo Nike Revolution 8
        // comparten el mismo zapato_id, pero pueden mostrar fotos distintas.
        if (nombre.contains("revolution 8")) {
            if ("MUJER".equalsIgnoreCase(genero) && "BLANCO".equalsIgnoreCase(color)) {
                candidatos.add("https://0990b9.a-cdn.akinoncloud.com/products/2025/09/02/7980553/e7324f1f-9d4a-4af8-9ef2-5c300808b9c4.jpg");
            } else if ("HOMBRE".equalsIgnoreCase(genero) && "NEGRO".equalsIgnoreCase(color)) {
                candidatos.add("https://prochampions.vtexassets.com/arquivos/ids/1208164-800-800?aspect=true&height=800&v=638811165007970000&width=800");
            }
        }

        if (ruta != null && !ruta.isBlank()) {
            String nombreArchivo = ruta.replace('\\', '/');
            nombreArchivo = nombreArchivo.substring(nombreArchivo.lastIndexOf('/') + 1);

            // Si el equipo incorpora recursos autorizados en src/main/resources/images:
            URL recurso = getClass().getResource("/images/" + nombreArchivo);
            if (recurso != null) candidatos.add(recurso.toExternalForm());

            File archivo = new File(ruta);
            if (archivo.isFile()) candidatos.add(archivo.toURI().toString());

            if (ruta.startsWith("https://") || ruta.startsWith("http://")) {
                candidatos.add(ruta);
            } else if (nombreArchivo.toLowerCase(Locale.ROOT).matches("[a-z0-9+_-]+\\.[a-z0-9_.-]+\\.(jpg|jpeg|png)")) {
                // Intento opcional de recuperar la foto original si venia de Amazon.
                candidatos.add("https://m.media-amazon.com/images/I/" + nombreArchivo);
            }
        }

        // Respaldos de fotografias autenticas de estos modelos (no siempre el color exacto).
        if (nombre.contains("blazer mid")) {
            candidatos.add("https://www.nike.ae/dw/image/v2/BDVB_PRD/on/demandware.static/-/Sites-akeneo-master-catalog/default/dw0768d748/nk/389/b/7/0/9/e/389b709e_5102_4e55_aa5d_07099b500831.jpg");
        } else if (nombre.contains("daily 4.0")) {
            candidatos.add("https://assets.adidas.com/images/w_1880%2Cf_auto%2Cq_auto/67b468d7479f47d8bc06caac354167a2_9366/IF6659_01_standard.jpg");
        } else if (nombre.contains("club c 85")) {
            candidatos.add("https://reebok.ca/cdn/shop/files/100233954B3495.jpg?v=1750987507");
        } else if (nombre.contains("energen lux")) {
            candidatos.add("https://i5.walmartimages.com/seo/Reebok-Energen-Lux-Men-s-Running-Shoes_7a6919d5-5bd7-43ff-9fe0-1aaf758e5bc7.a7f6dda5dec9ff2b07097d4f679a79a4.jpeg");
        } else if (nombre.contains("revolution 8")) {
            candidatos.add("https://prochampions.vtexassets.com/arquivos/ids/1208164-800-800?aspect=true&height=800&v=638811165007970000&width=800");
        }

        intentarImagen(new ArrayList<>(candidatos), 0);
    }

    private void intentarImagen(List<String> candidatos, int indice) {
        if (indice >= candidatos.size()) {
            imgZapato.setImage(null);
            System.err.println("No se encontro una foto accesible para: " + zapato.getNombre());
            return;
        }
        try {
            String direccion = candidatos.get(indice);
            Image imagen = new Image(direccion, true); // carga en segundo plano
            imgZapato.setImage(imagen);
            imagen.errorProperty().addListener((obs, anterior, error) -> {
                if (Boolean.TRUE.equals(error)) {
                    System.err.println("No se pudo cargar imagen de: " + direccion);
                    intentarImagen(candidatos, indice + 1);
                }
            });
            if (imagen.isError()) intentarImagen(candidatos, indice + 1);
        } catch (Exception e) {
            intentarImagen(candidatos, indice + 1);
        }
    }

    @FXML
    private void handleCerrar() {
        Stage stage = (Stage) lblNombre.getScene().getWindow();
        stage.close();
    }
}
