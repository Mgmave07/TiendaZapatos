package ni.uam.edu.tiendazapatos.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import ni.uam.edu.tiendazapatos.config.HibernateUtil;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.Venta;

import java.util.*;

public class VentaDAO {

    public boolean guardarVenta(Venta venta) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            em.persist(venta);

            for (DetalleVenta detalle : venta.getDetalles()) {
                em.createNativeQuery(
                                "UPDATE public.inventario SET cantidad_stock = cantidad_stock - ?1 " +
                                        "WHERE inventario_id = CAST(?2 AS uuid)")
                        .setParameter(1, detalle.getCantidad())
                        .setParameter(2, detalle.getInventarioId().toString())
                        .executeUpdate();

                em.createNativeQuery(
                                "UPDATE public.zapatos SET volumen_ventas = COALESCE(volumen_ventas, 0) + ?1 " +
                                "WHERE zapato_id = (SELECT z2.zapato_id FROM public.inventario i2 JOIN public.zapatos z2 ON i2.zapato_id = z2.zapato_id WHERE i2.inventario_id = CAST(?2 AS uuid))")
                        .setParameter(1, detalle.getCantidad())
                        .setParameter(2, detalle.getInventarioId().toString())
                        .executeUpdate();
            }

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Error al guardar la venta: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            em.close();
        }
    }

    public List<Venta> obtenerTodas() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            TypedQuery<Venta> query = em.createQuery(
                    "SELECT DISTINCT v FROM Venta v LEFT JOIN FETCH v.detalles ORDER BY v.fechaVenta DESC",
                    Venta.class
            );
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }

    public Map<UUID, String> obtenerDescripcionesInventario() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            List<?> rows = em.createNativeQuery("""
                SELECT i.inventario_id, z.nombre, COALESCE(m.nombre, 'Sin marca'), i.genero, i.sistema_talla, i.talla, i.color
                FROM public.inventario i
                JOIN public.zapatos z ON i.zapato_id = z.zapato_id
                LEFT JOIN public.marcas m ON z.marca_id = m.marca_id
            """).getResultList();

            Map<UUID, String> mapa = new HashMap<>();
            for (Object r : rows) {
                Object[] f = (Object[]) r;
                UUID invId = (UUID) f[0];
                String zNombre = (String) f[1];
                String mNombre = (String) f[2];
                String genero = (String) f[3];
                String sistema = (String) f[4];
                String talla = (String) f[5];
                String color = (String) f[6];
                String desc = String.format("%s (%s) | %s | %s %s | %s", zNombre, mNombre, genero, sistema, talla, color);
                mapa.put(invId, desc);
            }
            return mapa;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        } finally {
            em.close();
        }
    }

    public static class InfoInventario {
        private final String producto;
        private final String variante;

        public InfoInventario(String producto, String variante) {
            this.producto = producto;
            this.variante = variante;
        }

        public String getProducto() { return producto; }
        public String getVariante() { return variante; }
    }

    public Map<UUID, InfoInventario> obtenerInfoCompletaInventario() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            List<?> rows = em.createNativeQuery("""
                SELECT i.inventario_id, z.nombre, COALESCE(m.nombre, 'Sin marca'), i.genero, i.sistema_talla, i.talla, i.color
                FROM public.inventario i
                JOIN public.zapatos z ON i.zapato_id = z.zapato_id
                LEFT JOIN public.marcas m ON z.marca_id = m.marca_id
            """).getResultList();

            Map<UUID, InfoInventario> mapa = new HashMap<>();
            for (Object r : rows) {
                Object[] f = (Object[]) r;
                UUID invId = (UUID) f[0];
                String zNombre = (String) f[1];
                String mNombre = (String) f[2];
                String genero = f[3] != null ? f[3].toString() : "—";
                String sistema = f[4] != null ? f[4].toString() : "";
                String talla = f[5] != null ? f[5].toString() : "";
                String color = f[6] != null ? f[6].toString() : "—";

                String producto = String.format("%s (%s)", zNombre != null ? zNombre : "Calzado", mNombre);
                String tallaCompleta = (sistema + " " + talla).trim();
                String variante = String.format("%s | %s | %s", genero, tallaCompleta.isEmpty() ? "—" : tallaCompleta, color);

                mapa.put(invId, new InfoInventario(producto, variante));
            }
            return mapa;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        } finally {
            em.close();
        }
    }

    public List<Venta> obtenerPorVendedor(UUID vendedorId, java.time.LocalDate desde, java.time.LocalDate hasta) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            java.time.LocalDateTime desdeDateTime = desde.atStartOfDay();
            java.time.LocalDateTime hastaDateTime = hasta.atTime(23, 59, 59);
            TypedQuery<Venta> query = em.createQuery(
                "SELECT DISTINCT v FROM Venta v LEFT JOIN FETCH v.detalles " +
                "WHERE v.vendedorId = :vendedorId " +
                "AND v.fechaVenta >= :desde AND v.fechaVenta <= :hasta " +
                "ORDER BY v.fechaVenta DESC",
                Venta.class
            );
            query.setParameter("vendedorId", vendedorId);
            query.setParameter("desde", java.util.Date.from(desdeDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()));
            query.setParameter("hasta", java.util.Date.from(hastaDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()));
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        } finally {
            em.close();
        }
    }
}