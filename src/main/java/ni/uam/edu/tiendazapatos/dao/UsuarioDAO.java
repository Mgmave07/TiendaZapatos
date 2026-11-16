package ni.uam.edu.tiendazapatos.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import ni.uam.edu.tiendazapatos.config.HibernateUtil;
import ni.uam.edu.tiendazapatos.modelo.Usuario;

public class UsuarioDAO {

    public Usuario buscarPorNombreUsuario(String nombreUsuario) {
        EntityManager em = HibernateUtil.getEntityManager();
        Usuario usuario = null;
        try {
            // JPQL: Usamos los nombres de nuestras clases Java, no los de la base de datos
            String jpql = "SELECT u FROM Usuario u WHERE u.nombreUsuario = :nombreUsuario";
            TypedQuery<Usuario> query = em.createQuery(jpql, Usuario.class);
            query.setParameter("nombreUsuario", nombreUsuario);

            usuario = query.getSingleResult();
        } catch (NoResultException e) {
            // Si no encuentra el usuario, no es un error fatal, simplemente no existe
            System.out.println("Usuario no encontrado en la base de datos.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close(); // Siempre cerramos la conexión para no saturar la BD
        }
        return usuario;
    }


    public void guardar(Usuario usuario) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin(); // 1. Iniciamos la transacción
            em.persist(usuario);         // 2. Preparamos el INSERT
            em.getTransaction().commit(); // 3. Ejecutamos el INSERT en PostgreSQL
            System.out.println("Usuario guardado exitosamente.");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback(); // Si algo falla, deshacemos los cambios
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}
