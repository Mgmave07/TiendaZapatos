package ni.uam.edu.tiendazapatos.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import ni.uam.edu.tiendazapatos.config.HibernateUtil;
import ni.uam.edu.tiendazapatos.modelo.Cliente;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.Venta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClienteDAO {

    public void sincronizarClientesDesdeVentas() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            // 1. Si existen ventas con datos de clientes que aún no estén en la tabla clientes, importarlos
            em.createNativeQuery("""
                INSERT INTO public.clientes (nombre, identificacion, telefono, direccion, activo, creado_en)
                SELECT 
                    TRIM(v.cliente_nombre) as nombre,
                    NULLIF(TRIM(v.cliente_identificacion), '') as identificacion,
                    MAX(NULLIF(TRIM(v.cliente_telefono), '')) as telefono,
                    'Registrado vía venta previa' as direccion,
                    true as activo,
                    COALESCE(MIN(v.fecha_venta), CURRENT_TIMESTAMP) as creado_en
                FROM public.ventas v
                WHERE v.cliente_nombre IS NOT NULL 
                  AND TRIM(v.cliente_nombre) <> ''
                  AND NOT EXISTS (
                      SELECT 1 FROM public.clientes c 
                      WHERE (c.identificacion IS NOT NULL AND c.identificacion <> '' 
                             AND v.cliente_identificacion IS NOT NULL AND v.cliente_identificacion <> ''
                             AND LOWER(TRIM(c.identificacion)) = LOWER(TRIM(v.cliente_identificacion)))
                         OR (LOWER(TRIM(c.nombre)) = LOWER(TRIM(v.cliente_nombre)))
                  )
                GROUP BY TRIM(v.cliente_nombre), NULLIF(TRIM(v.cliente_identificacion), '')
            """).executeUpdate();

            // 2. Asociar el cliente_id a las ventas existentes
            em.createNativeQuery("""
                UPDATE public.ventas v
                SET cliente_id = c.cliente_id
                FROM public.clientes c
                WHERE v.cliente_id IS NULL
                  AND (
                      (v.cliente_identificacion IS NOT NULL AND v.cliente_identificacion <> '' 
                       AND c.identificacion IS NOT NULL AND c.identificacion <> ''
                       AND LOWER(TRIM(v.cliente_identificacion)) = LOWER(TRIM(c.identificacion)))
                   OR (v.cliente_nombre IS NOT NULL AND v.cliente_nombre <> ''
                       AND LOWER(TRIM(v.cliente_nombre)) = LOWER(TRIM(c.nombre)))
                  )
            """).executeUpdate();

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Aviso sincronización clientes: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public List<Cliente> obtenerTodos() {
        sincronizarClientesDesdeVentas();
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            TypedQuery<Cliente> query = em.createQuery(
                    "SELECT c FROM Cliente c ORDER BY c.nombre ASC", Cliente.class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }

    public List<Cliente> obtenerActivos() {
        sincronizarClientesDesdeVentas();
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            TypedQuery<Cliente> query = em.createQuery(
                    "SELECT c FROM Cliente c WHERE c.activo = true ORDER BY c.nombre ASC", Cliente.class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }

    public Cliente buscarPorId(UUID clienteId) {
        if (clienteId == null) return null;
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.find(Cliente.class, clienteId);
        } finally {
            em.close();
        }
    }

    public Cliente buscarPorIdentificacion(String identificacion) {
        if (identificacion == null || identificacion.trim().isEmpty()) return null;
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            TypedQuery<Cliente> query = em.createQuery(
                    "SELECT c FROM Cliente c WHERE LOWER(TRIM(c.identificacion)) = LOWER(TRIM(:id))", Cliente.class);
            query.setParameter("id", identificacion.trim());
            List<Cliente> lista = query.getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    public Cliente guardar(Cliente cliente) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(cliente);
            em.getTransaction().commit();
            return cliente;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    public boolean actualizar(Cliente cliente) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(cliente);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            return false;
        } finally {
            em.close();
        }
    }

    public List<Venta> obtenerVentasPorCliente(UUID clienteId, String identificacion, String nombre) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            String nom = nombre != null ? nombre.trim().toLowerCase() : "";
            String id  = identificacion != null ? identificacion.trim().toLowerCase() : "";

            TypedQuery<Venta> query = em.createQuery(
                    "SELECT DISTINCT v FROM Venta v " +
                    "LEFT JOIN FETCH v.detalles " +
                    "LEFT JOIN v.cliente c " +
                    "WHERE (c.clienteId = :cId) " +
                    "   OR (:id <> '' AND v.clienteIdentificacion IS NOT NULL AND LOWER(TRIM(v.clienteIdentificacion)) = :id) " +
                    "   OR (:nom <> '' AND v.clienteNombre IS NOT NULL AND LOWER(TRIM(v.clienteNombre)) = :nom) " +
                    "ORDER BY v.fechaVenta DESC", Venta.class);

            query.setParameter("cId", clienteId);
            query.setParameter("id", id);
            query.setParameter("nom", nom);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }

    public record ResumenComprasCliente(int totalFacturas, int totalArticulos, double totalInvertido) {}

    public ResumenComprasCliente obtenerResumenCompras(UUID clienteId, String identificacion, String nombre) {
        List<Venta> ventas = obtenerVentasPorCliente(clienteId, identificacion, nombre);
        int totalFacturas = ventas.size();
        int totalArticulos = 0;
        double totalInvertido = 0.0;

        for (Venta v : ventas) {
            totalInvertido += v.getTotal();
            if (v.getDetalles() != null) {
                for (DetalleVenta d : v.getDetalles()) {
                    totalArticulos += d.getCantidad();
                }
            }
        }
        return new ResumenComprasCliente(totalFacturas, totalArticulos, totalInvertido);
    }
}
