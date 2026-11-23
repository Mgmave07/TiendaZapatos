package ni.uam.edu.tiendazapatos.modelo;

import java.util.UUID;

/**
 * Fila de presentación del inventario. NO es una entidad JPA.
 * Cada registro representa una variante por género, sistema de talla, talla y color.
 * stock == null significa que esa variante todavía no está registrada.
 */
public record InventarioFila(
        UUID inventarioId,
        Zapato zapato,
        String genero,
        String sistemaTalla,
        String talla,
        String color,
        Integer stock
) {
    public InventarioFila(Zapato zapato, String genero, String sistemaTalla, String talla, String color, Integer stock) {
        this(null, zapato, genero, sistemaTalla, talla, color, stock);
    }
}
