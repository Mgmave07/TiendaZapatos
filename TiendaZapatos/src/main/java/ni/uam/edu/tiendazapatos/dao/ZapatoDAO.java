package ni.uam.edu.tiendazapatos.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import ni.uam.edu.tiendazapatos.config.HibernateUtil;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.util.List;

public class ZapatoDAO {
    public void guardar(Zapato zapato) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(zapato);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al guardar el zapato: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
    public List<Zapato> obtenerTodos() {
        EntityManager em = HibernateUtil.getEntityManager();
        List<Zapato> zapatos = null;
        try {
            TypedQuery<Zapato> query = em.createQuery("SELECT z FROM Zapato z ORDER BY z.creadoEn DESC", Zapato.class);
            zapatos = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
        return zapatos;
    }
    public void actualizar(Zapato zapato) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(zapato); // merge actualiza el registro existente
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al actualizar el zapato: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}
