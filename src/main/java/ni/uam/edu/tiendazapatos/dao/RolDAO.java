package ni.uam.edu.tiendazapatos.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import ni.uam.edu.tiendazapatos.config.HibernateUtil;
import ni.uam.edu.tiendazapatos.modelo.Rol;

import java.util.ArrayList;
import java.util.List;

public class RolDAO {
    public Rol buscarPorNombre(String nombre) {
        EntityManager em = HibernateUtil.getEntityManager();
        Rol rol = null;
        try {
            String jpql = "SELECT r FROM Rol r WHERE r.nombre = :nombre";
            TypedQuery<Rol> query = em.createQuery(jpql, Rol.class);
            query.setParameter("nombre", nombre);
            rol = query.getSingleResult();
        } catch (NoResultException e) {
            System.out.println("Rol no encontrado: " + nombre);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
        return rol;
    }

    public List<Rol> obtenerTodos() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            String jpql = "SELECT r FROM Rol r ORDER BY r.nombre";
            TypedQuery<Rol> query = em.createQuery(jpql, Rol.class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }
}
