package ni.uam.edu.tiendazapatos.dao;

import jakarta.persistence.EntityManager;
import ni.uam.edu.tiendazapatos.config.HibernateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Lee y escribe las variantes registradas en la tabla public.inventario. */
public class InventarioDAO {

    public record Variante(
            UUID zapatoId,
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
                    SELECT zapato_id, genero, sistema_talla, talla, color, cantidad_stock
                    FROM public.inventario
                    ORDER BY genero, sistema_talla, talla, color
                    """).getResultList();

            List<Variante> variantes = new ArrayList<>();
            for (Object resultado : resultados) {
                Object[] fila = (Object[]) resultado;
                UUID id = (UUID) fila[0];
                String genero = (String) fila[1];
                String sistema = (String) fila[2];
                String talla = (String) fila[3];
                String color = (String) fila[4];
                int stock = ((Number) fila[5]).intValue();

                variantes.add(new Variante(id, genero, sistema, talla, color, stock));
            }
            return variantes;
        } finally {
            em.close();
        }
    }

    public void guardarVariante(UUID zapatoId, String genero, String sistemaTalla,
                                String talla, String color, int cantidadStock) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(
                            "INSERT INTO public.inventario " +
                                    "(zapato_id, genero, sistema_talla, talla, color, cantidad_stock) " +
                                    "VALUES (CAST(?1 AS uuid), ?2, ?3, ?4, ?5, ?6)")
                    .setParameter(1, zapatoId.toString())
                    .setParameter(2, genero)
                    .setParameter(3, sistemaTalla)
                    .setParameter(4, talla)
                    .setParameter(5, color)
                    .setParameter(6, cantidadStock)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public UUID obtenerIdVariante(UUID zapatoId, String genero, String sistemaTalla, String talla, String color) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            Object result = em.createNativeQuery(
                            "SELECT inventario_id FROM public.inventario " +
                                    "WHERE zapato_id = CAST(?1 AS uuid) AND genero = ?2 AND sistema_talla = ?3 AND talla = ?4 AND color = ?5"
                    ).setParameter(1, zapatoId.toString())
                    .setParameter(2, genero)
                    .setParameter(3, sistemaTalla)
                    .setParameter(4, talla)
                    .setParameter(5, color)
                    .getSingleResult();
            return (UUID) result;
        } catch (Exception e) {
            return null; // Si no existe la variante
        } finally {
            em.close();
        }
    }

    public void actualizarVariante(UUID inventarioId, String genero, String sistemaTalla, String talla, String color, int cantidadStock) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(
                            "UPDATE public.inventario SET " +
                                    "genero = ?1, sistema_talla = ?2, talla = ?3, color = ?4, cantidad_stock = ?5 " +
                                    "WHERE inventario_id = CAST(?6 AS uuid)"
                    ).setParameter(1, genero)
                    .setParameter(2, sistemaTalla)
                    .setParameter(3, talla)
                    .setParameter(4, color)
                    .setParameter(5, cantidadStock)
                    .setParameter(6, inventarioId.toString())
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}