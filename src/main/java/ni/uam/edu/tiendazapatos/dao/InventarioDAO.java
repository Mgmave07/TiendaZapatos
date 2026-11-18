package ni.uam.edu.tiendazapatos.dao;

import jakarta.persistence.EntityManager;
import ni.uam.edu.tiendazapatos.config.HibernateUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Lee las variantes registradas en la tabla public.inventario. */
public class InventarioDAO {

    public record Variante(
            UUID zapatoId,
            String sku,
            String genero,
            String sistemaTalla,
            String talla,
            String color,
            int cantidadStock
    ) {}

    public List<Variante> obtenerVariantes() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            List<?> resultados = em.createNativeQuery("""
                    SELECT zapato_id, sku, genero, sistema_talla, talla, color, cantidad_stock
                    FROM public.inventario
                    ORDER BY genero, sistema_talla, talla, color
                    """).getResultList();

            List<Variante> variantes = new ArrayList<>();
            for (Object resultado : resultados) {
                Object[] fila = (Object[]) resultado;
                UUID id = (UUID) fila[0];
                String sku = (String) fila[1];
                String genero = (String) fila[2];
                String sistema = (String) fila[3];
                BigDecimal talla = (BigDecimal) fila[4];
                String color = (String) fila[5];
                int stock = ((Number) fila[6]).intValue();

                variantes.add(new Variante(
                        id,
                        sku,
                        genero,
                        sistema,
                        talla.stripTrailingZeros().toPlainString(),
                        color,
                        stock
                ));
            }
            return variantes;
        } finally {
            em.close();
        }
    }
}
