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

    public enum TipoDevolucion {
        RECAMBIO,          // Cambio de producto: suma stock al devuelto, descuenta al nuevo, actualiza volumen_ventas en ambos y ajusta total si difiere precio
        PRODUCTO_DANADO,   // Daño en el producto: NO regresa al stock, reduce volumen_ventas y descuenta dinero del total de la venta
        DEVOLUCION_NORMAL  // Devolución normal: SÍ regresa al stock, reduce volumen_ventas y descuenta dinero del total de la venta
    }

    public boolean procesarDevolucionOCambio(UUID ventaId, UUID detalleId, int cantidadDevolver,
                                            TipoDevolucion tipo, UUID nuevoInventarioId, double nuevoPrecioUnitario,
                                            String motivoTexto) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Venta venta = em.find(Venta.class, ventaId);
            if (venta == null) {
                em.getTransaction().rollback();
                return false;
            }

            DetalleVenta detalle = null;
            if (venta.getDetalles() != null) {
                for (DetalleVenta d : venta.getDetalles()) {
                    if (d.getDetalle_id() != null && d.getDetalle_id().equals(detalleId)) {
                        detalle = d;
                        break;
                    }
                }
            }

            if (detalle == null || cantidadDevolver <= 0 || cantidadDevolver > detalle.getCantidad()) {
                em.getTransaction().rollback();
                return false;
            }

            UUID inventarioOriginalId = detalle.getInventarioId();
            double precioOriginal = detalle.getPrecioUnitario();

            if (tipo == TipoDevolucion.PRODUCTO_DANADO) {
                // 1. NO se añade al stock
                // 2. Reduce volumen de ventas
                em.createNativeQuery(
                        "UPDATE public.zapatos SET volumen_ventas = GREATEST(0, COALESCE(volumen_ventas, 0) - ?1) " +
                        "WHERE zapato_id = (SELECT zapato_id FROM public.inventario WHERE inventario_id = CAST(?2 AS uuid) LIMIT 1)")
                        .setParameter(1, cantidadDevolver)
                        .setParameter(2, inventarioOriginalId.toString())
                        .executeUpdate();

                // 3. Ajuste de ítems en la factura
                int nuevaCant = detalle.getCantidad() - cantidadDevolver;
                if (nuevaCant <= 0) {
                    em.createNativeQuery("DELETE FROM public.detalles_venta WHERE detalle_id = CAST(?1 AS uuid)")
                            .setParameter(1, detalle.getDetalle_id().toString())
                            .executeUpdate();
                    venta.getDetalles().remove(detalle);
                } else {
                    double nuevoSub = nuevaCant * precioOriginal;
                    em.createNativeQuery("UPDATE public.detalles_venta SET cantidad = ?1, subtotal = ?2 WHERE detalle_id = CAST(?3 AS uuid)")
                            .setParameter(1, nuevaCant)
                            .setParameter(2, nuevoSub)
                            .setParameter(3, detalle.getDetalle_id().toString())
                            .executeUpdate();
                    detalle.setCantidad(nuevaCant);
                    detalle.setSubtotal(nuevoSub);
                }

                // 4. Recalcular totales exactos de la factura
                double subtotalFinal = 0.0;
                for (DetalleVenta d : venta.getDetalles()) {
                    subtotalFinal += d.getSubtotal();
                }
                double impuestoFinal = Math.round(subtotalFinal * 0.15 * 100.0) / 100.0;
                double totalFinal = subtotalFinal + impuestoFinal;

                String nuevoEstado = venta.getDetalles().isEmpty() ? "DEVUELTA" : "PAGADA";
                String notaActual = venta.getNotas() != null ? venta.getNotas() : "";
                String nuevaNota = String.format(Locale.US, " [Devolución por DAÑO: %d unidad(es). Reembolso total: C$ %.2f. Motivo: %s]",
                        cantidadDevolver, (cantidadDevolver * precioOriginal * 1.15), motivoTexto);

                em.createNativeQuery(
                        "UPDATE public.ventas SET subtotal = ?1, impuesto = ?2, total = ?3, estado = ?4, notas = ?5 WHERE venta_id = CAST(?6 AS uuid)")
                        .setParameter(1, subtotalFinal)
                        .setParameter(2, impuestoFinal)
                        .setParameter(3, totalFinal)
                        .setParameter(4, nuevoEstado)
                        .setParameter(5, notaActual + nuevaNota)
                        .setParameter(6, venta.getVenta_id().toString())
                        .executeUpdate();

                venta.setSubtotal(subtotalFinal);
                venta.setImpuesto(impuestoFinal);
                venta.setTotal(totalFinal);
                venta.setEstado(nuevoEstado);
                venta.setNotas(notaActual + nuevaNota);

            } else if (tipo == TipoDevolucion.DEVOLUCION_NORMAL) {
                // 1. SÍ se añade al stock
                em.createNativeQuery(
                        "UPDATE public.inventario SET cantidad_stock = cantidad_stock + ?1 WHERE inventario_id = CAST(?2 AS uuid)")
                        .setParameter(1, cantidadDevolver)
                        .setParameter(2, inventarioOriginalId.toString())
                        .executeUpdate();

                // 2. Reduce volumen de ventas
                em.createNativeQuery(
                        "UPDATE public.zapatos SET volumen_ventas = GREATEST(0, COALESCE(volumen_ventas, 0) - ?1) " +
                        "WHERE zapato_id = (SELECT zapato_id FROM public.inventario WHERE inventario_id = CAST(?2 AS uuid) LIMIT 1)")
                        .setParameter(1, cantidadDevolver)
                        .setParameter(2, inventarioOriginalId.toString())
                        .executeUpdate();

                // 3. Ajuste de ítems en la factura
                int nuevaCant = detalle.getCantidad() - cantidadDevolver;
                if (nuevaCant <= 0) {
                    em.createNativeQuery("DELETE FROM public.detalles_venta WHERE detalle_id = CAST(?1 AS uuid)")
                            .setParameter(1, detalle.getDetalle_id().toString())
                            .executeUpdate();
                    venta.getDetalles().remove(detalle);
                } else {
                    double nuevoSub = nuevaCant * precioOriginal;
                    em.createNativeQuery("UPDATE public.detalles_venta SET cantidad = ?1, subtotal = ?2 WHERE detalle_id = CAST(?3 AS uuid)")
                            .setParameter(1, nuevaCant)
                            .setParameter(2, nuevoSub)
                            .setParameter(3, detalle.getDetalle_id().toString())
                            .executeUpdate();
                    detalle.setCantidad(nuevaCant);
                    detalle.setSubtotal(nuevoSub);
                }

                // 4. Recalcular totales exactos de la factura
                double subtotalFinal = 0.0;
                for (DetalleVenta d : venta.getDetalles()) {
                    subtotalFinal += d.getSubtotal();
                }
                double impuestoFinal = Math.round(subtotalFinal * 0.15 * 100.0) / 100.0;
                double totalFinal = subtotalFinal + impuestoFinal;

                String nuevoEstado = venta.getDetalles().isEmpty() ? "DEVUELTA" : "PAGADA";
                String notaActual = venta.getNotas() != null ? venta.getNotas() : "";
                String nuevaNota = String.format(Locale.US, " [Devolución NORMAL: %d unidad(es) reincorporadas al stock. Reembolso total: C$ %.2f. Motivo: %s]",
                        cantidadDevolver, (cantidadDevolver * precioOriginal * 1.15), motivoTexto);

                em.createNativeQuery(
                        "UPDATE public.ventas SET subtotal = ?1, impuesto = ?2, total = ?3, estado = ?4, notas = ?5 WHERE venta_id = CAST(?6 AS uuid)")
                        .setParameter(1, subtotalFinal)
                        .setParameter(2, impuestoFinal)
                        .setParameter(3, totalFinal)
                        .setParameter(4, nuevoEstado)
                        .setParameter(5, notaActual + nuevaNota)
                        .setParameter(6, venta.getVenta_id().toString())
                        .executeUpdate();

                venta.setSubtotal(subtotalFinal);
                venta.setImpuesto(impuestoFinal);
                venta.setTotal(totalFinal);
                venta.setEstado(nuevoEstado);
                venta.setNotas(notaActual + nuevaNota);

            } else if (tipo == TipoDevolucion.RECAMBIO) {
                if (nuevoInventarioId == null) {
                    em.getTransaction().rollback();
                    return false;
                }

                // 1. Devuelto ingresa al stock
                em.createNativeQuery(
                        "UPDATE public.inventario SET cantidad_stock = cantidad_stock + ?1 WHERE inventario_id = CAST(?2 AS uuid)")
                        .setParameter(1, cantidadDevolver)
                        .setParameter(2, inventarioOriginalId.toString())
                        .executeUpdate();

                // 2. Nuevo sale del stock
                em.createNativeQuery(
                        "UPDATE public.inventario SET cantidad_stock = cantidad_stock - ?1 WHERE inventario_id = CAST(?2 AS uuid)")
                        .setParameter(1, cantidadDevolver)
                        .setParameter(2, nuevoInventarioId.toString())
                        .executeUpdate();

                // 3. Ajuste de volumen de ventas (viejo -, nuevo +)
                em.createNativeQuery(
                        "UPDATE public.zapatos SET volumen_ventas = GREATEST(0, COALESCE(volumen_ventas, 0) - ?1) " +
                        "WHERE zapato_id = (SELECT zapato_id FROM public.inventario WHERE inventario_id = CAST(?2 AS uuid) LIMIT 1)")
                        .setParameter(1, cantidadDevolver)
                        .setParameter(2, inventarioOriginalId.toString())
                        .executeUpdate();

                em.createNativeQuery(
                        "UPDATE public.zapatos SET volumen_ventas = COALESCE(volumen_ventas, 0) + ?1 " +
                        "WHERE zapato_id = (SELECT zapato_id FROM public.inventario WHERE inventario_id = CAST(?2 AS uuid) LIMIT 1)")
                        .setParameter(1, cantidadDevolver)
                        .setParameter(2, nuevoInventarioId.toString())
                        .executeUpdate();

                // 4. Modificar o añadir ítem en la factura
                if (cantidadDevolver == detalle.getCantidad()) {
                    double nuevoSubtotal = cantidadDevolver * nuevoPrecioUnitario;
                    em.createNativeQuery(
                            "UPDATE public.detalles_venta SET inventario_id = CAST(?1 AS uuid), precio_unitario = ?2, subtotal = ?3 WHERE detalle_id = CAST(?4 AS uuid)")
                            .setParameter(1, nuevoInventarioId.toString())
                            .setParameter(2, nuevoPrecioUnitario)
                            .setParameter(3, nuevoSubtotal)
                            .setParameter(4, detalle.getDetalle_id().toString())
                            .executeUpdate();

                    detalle.setInventarioId(nuevoInventarioId);
                    detalle.setPrecioUnitario(nuevoPrecioUnitario);
                    detalle.setSubtotal(nuevoSubtotal);
                } else {
                    int restante = detalle.getCantidad() - cantidadDevolver;
                    double subtotalRestante = restante * precioOriginal;
                    em.createNativeQuery(
                            "UPDATE public.detalles_venta SET cantidad = ?1, subtotal = ?2 WHERE detalle_id = CAST(?3 AS uuid)")
                            .setParameter(1, restante)
                            .setParameter(2, subtotalRestante)
                            .setParameter(3, detalle.getDetalle_id().toString())
                            .executeUpdate();

                    detalle.setCantidad(restante);
                    detalle.setSubtotal(subtotalRestante);

                    UUID nuevoDetalleId = UUID.randomUUID();
                    double subtotalNuevoItem = cantidadDevolver * nuevoPrecioUnitario;
                    em.createNativeQuery(
                            "INSERT INTO public.detalles_venta (detalle_id, venta_id, inventario_id, cantidad, precio_unitario, subtotal) " +
                            "VALUES (CAST(?1 AS uuid), CAST(?2 AS uuid), CAST(?3 AS uuid), ?4, ?5, ?6)")
                            .setParameter(1, nuevoDetalleId.toString())
                            .setParameter(2, venta.getVenta_id().toString())
                            .setParameter(3, nuevoInventarioId.toString())
                            .setParameter(4, cantidadDevolver)
                            .setParameter(5, nuevoPrecioUnitario)
                            .setParameter(6, subtotalNuevoItem)
                            .executeUpdate();

                    DetalleVenta nuevoDet = new DetalleVenta();
                    nuevoDet.setDetalle_id(nuevoDetalleId);
                    nuevoDet.setVenta(venta);
                    nuevoDet.setInventarioId(nuevoInventarioId);
                    nuevoDet.setCantidad(cantidadDevolver);
                    nuevoDet.setPrecioUnitario(nuevoPrecioUnitario);
                    nuevoDet.setSubtotal(subtotalNuevoItem);
                    venta.getDetalles().add(nuevoDet);
                }

                // 5. Recalcular totales de la factura
                double subtotalFinal = 0.0;
                for (DetalleVenta d : venta.getDetalles()) {
                    subtotalFinal += d.getSubtotal();
                }
                double impuestoFinal = Math.round(subtotalFinal * 0.15 * 100.0) / 100.0;
                double totalFinal = subtotalFinal + impuestoFinal;

                double diffTotal = (cantidadDevolver * (nuevoPrecioUnitario - precioOriginal)) * 1.15;
                String notaActual = venta.getNotas() != null ? venta.getNotas() : "";
                String nuevaNota = String.format(Locale.US, " [Recambio de producto: %d unidad(es) cambiadas (Diferencia: C$ %.2f). Motivo: %s]",
                        cantidadDevolver, diffTotal, motivoTexto);

                em.createNativeQuery(
                        "UPDATE public.ventas SET subtotal = ?1, impuesto = ?2, total = ?3, notas = ?4 WHERE venta_id = CAST(?5 AS uuid)")
                        .setParameter(1, subtotalFinal)
                        .setParameter(2, impuestoFinal)
                        .setParameter(3, totalFinal)
                        .setParameter(4, notaActual + nuevaNota)
                        .setParameter(5, venta.getVenta_id().toString())
                        .executeUpdate();

                venta.setSubtotal(subtotalFinal);
                venta.setImpuesto(impuestoFinal);
                venta.setTotal(totalFinal);
                venta.setNotas(notaActual + nuevaNota);
            }

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