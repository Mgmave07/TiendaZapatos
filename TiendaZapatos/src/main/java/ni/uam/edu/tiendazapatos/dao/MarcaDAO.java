package ni.uam.edu.tiendazapatos.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import ni.uam.edu.tiendazapatos.config.HibernateUtil;
import ni.uam.edu.tiendazapatos.modelo.Marca;

import java.util.List;

public class MarcaDAO {
    public List<Marca> obtenerTodas() {
        EntityManager em = HibernateUtil.getEntityManager();
        List<Marca> marcas = null;
        try {
            TypedQuery<Marca> query = em.createQuery("SELECT m FROM Marca m ORDER BY m.nombre", Marca.class);
            marcas = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
        return marcas;
    }
    public void guardar(Marca marca) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(marca);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Error al guardar marca: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}
