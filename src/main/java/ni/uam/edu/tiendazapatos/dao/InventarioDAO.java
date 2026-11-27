package ni.uam.edu.tiendazapatos.dao;

import jakarta.persistence.EntityManager;
import ni.uam.edu.tiendazapatos.config.HibernateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Lee y escribe las variantes registradas en la tabla public.inventario. */
public class InventarioDAO {

    public record Variante(
            UUID inventarioId,
            UUID zapatoId,
            String genero,
            String sistemaTalla,
            String talla,
            String color,
            int cantidadStock
    ) {
        public Variante(UUID zapatoId, String genero, String sistemaTalla, String talla, String color, int cantidadStock) {
            this(null, zapatoId, genero, sistemaTalla, talla, color, cantidadStock);
        }
    }

    public record OpcionRecambio(
            UUID inventarioId,
            String nombre,
            String marca,
            String talla,
            String color,
            int stock,
            double precio
    ) {
        @Override
        public String toString() {
            return String.format("%s (%s) | Talla %s | %s | Stock: %d | C$ %.2f",
                    nombre, marca, talla, color, stock, precio);
        }
    }

    public List<Variante> obtenerVariantes() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            List<?> resultados = em.createNativeQuery("""
                    SELECT inventario_id, zapato_id, genero, sistema_talla, talla, color, cantidad_stock
                    FROM public.inventario
                    ORDER BY genero, sistema_talla, talla, color
                    """).getResultList();

            List<Variante> variantes = new ArrayList<>();
            for (Object resultado : resultados) {
                Object[] fila = (Object[]) resultado;
                UUID inventarioId = (UUID) fila[0];
                UUID id = (UUID) fila[1];
                String genero = (String) fila[2];
                String sistema = (String) fila[3];
                String talla = (String) fila[4];
                String color = (String) fila[5];
                int stock = ((Number) fila[6]).intValue();

                variantes.add(new Variante(inventarioId, id, genero, sistema, talla, color, stock));
            }
            return variantes;
        } finally {
            em.close();
        }
    }

    public List<Variante> obtenerVariantesPorZapato(UUID zapatoId) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            List<?> resultados = em.createNativeQuery("""
                    SELECT inventario_id, zapato_id, genero, sistema_talla, talla, color, cantidad_stock
                    FROM public.inventario
                    WHERE zapato_id = CAST(?1 AS uuid)
                    ORDER BY genero, sistema_talla, talla, color
                    """)
                    .setParameter(1, zapatoId.toString())
                    .getResultList();

            List<Variante> variantes = new ArrayList<>();
            for (Object resultado : resultados) {
                Object[] fila = (Object[]) resultado;
                UUID inventarioId = (UUID) fila[0];
                UUID id = (UUID) fila[1];
                String genero = (String) fila[2];
                String sistema = (String) fila[3];
                String talla = (String) fila[4];
                String color = (String) fila[5];
                int stock = ((Number) fila[6]).intValue();

                variantes.add(new Variante(inventarioId, id, genero, sistema, talla, color, stock));
            }
            return variantes;
        } finally {
            em.close();
        }
    }

    public UUID obtenerIdVariante(UUID zapatoId, String genero, String sistemaTalla, String talla, String color) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            List<?> resultados = em.createNativeQuery("""
                    SELECT inventario_id
                    FROM public.inventario
                    WHERE zapato_id = CAST(?1 AS uuid)
                      AND (genero = ?2 OR (?2 IS NULL AND genero IS NULL))
                      AND (sistema_talla = ?3 OR (?3 IS NULL AND sistema_talla IS NULL))
                      AND (talla = ?4 OR (?4 IS NULL AND talla IS NULL))
                      AND (color = ?5 OR (?5 IS NULL AND color IS NULL))
                    LIMIT 1
                    """)
                    .setParameter(1, zapatoId.toString())
                    .setParameter(2, genero)
                    .setParameter(3, sistemaTalla)
                    .setParameter(4, talla)
                    .setParameter(5, color)
                    .getResultList();

            if (resultados.isEmpty()) return null;
            return (UUID) resultados.get(0);
        } finally {
            em.close();
        }
    }

    public void guardarVariante(UUID zapatoId, String genero, String sistemaTalla, String talla, String color, int stock) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("""
                    INSERT INTO public.inventario (inventario_id, zapato_id, genero, sistema_talla, talla, color, cantidad_stock)
                    VALUES (gen_random_uuid(), CAST(?1 AS uuid), ?2, ?3, ?4, ?5, ?6)
                    """)
                    .setParameter(1, zapatoId.toString())
                    .setParameter(2, genero)
                    .setParameter(3, sistemaTalla)
                    .setParameter(4, talla)
                    .setParameter(5, color)
                    .setParameter(6, stock)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void actualizarVariante(UUID inventarioId, String genero, String sistemaTalla, String talla, String color, int stock) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("""
                    UPDATE public.inventario
                    SET genero = ?2, sistema_talla = ?3, talla = ?4, color = ?5, cantidad_stock = ?6
                    WHERE inventario_id = CAST(?1 AS uuid)
                    """)
                    .setParameter(1, inventarioId.toString())
                    .setParameter(2, genero)
                    .setParameter(3, sistemaTalla)
                    .setParameter(4, talla)
                    .setParameter(5, color)
                    .setParameter(6, stock)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<OpcionRecambio> obtenerOpcionesRecambio() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            List<?> rows = em.createNativeQuery("""
                SELECT i.inventario_id, z.nombre, COALESCE(m.nombre, 'Sin marca'), i.talla, i.color, i.cantidad_stock, z.precio_final
                FROM public.inventario i
                JOIN public.zapatos z ON i.zapato_id = z.zapato_id
                LEFT JOIN public.marcas m ON z.marca_id = m.marca_id
                WHERE i.cantidad_stock > 0 AND z.activo = true
                ORDER BY z.nombre, i.talla
            """).getResultList();

            List<OpcionRecambio> lista = new ArrayList<>();
            for (Object r : rows) {
                Object[] f = (Object[]) r;
                UUID invId = (UUID) f[0];
                String nom = (String) f[1];
                String mar = (String) f[2];
                String tal = f[3] != null ? f[3].toString() : "—";
                String col = f[4] != null ? f[4].toString() : "—";
                int stk = ((Number) f[5]).intValue();
                double pre = f[6] != null ? ((Number) f[6]).doubleValue() : 0.0;
                lista.add(new OpcionRecambio(invId, nom, mar, tal, col, stk, pre));
            }
            return lista;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }
}
