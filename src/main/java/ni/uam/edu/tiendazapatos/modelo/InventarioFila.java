package ni.uam.edu.tiendazapatos.modelo;

/**
 * Fila de presentación del inventario. NO es una entidad JPA.
 * Cada registro representa una variante por género, sistema de talla, talla y color.
 * stock == null significa que esa variante todavía no está registrada.
 */
public record InventarioFila(
        Zapato zapato,
        String genero,
        String sistemaTalla,
        String talla,
        String color,
        Integer stock
) {}
