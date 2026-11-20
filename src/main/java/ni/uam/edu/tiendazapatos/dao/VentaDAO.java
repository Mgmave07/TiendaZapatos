package ni.uam.edu.tiendazapatos.dao;

import jakarta.persistence.EntityManager;
import ni.uam.edu.tiendazapatos.config.HibernateUtil;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.Venta;

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
}