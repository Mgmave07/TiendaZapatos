package ni.uam.edu.tiendazapatos.modelo;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "detalles_venta")
public class DetalleVenta {
    @Id
    @GeneratedValue
    private UUID detalle_id;

    @ManyToOne
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @Column(name = "inventario_id")
    private UUID inventarioId;

    private int cantidad;

    @Column(name = "precio_unitario")
    private double precioUnitario;

    private double subtotal;

    public DetalleVenta() {
    }

    public UUID getDetalle_id() {
        return detalle_id;
    }

    public void setDetalle_id(UUID detalle_id) {
        this.detalle_id = detalle_id;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public UUID getInventarioId() {
        return inventarioId;
    }

    public void setInventarioId(UUID inventarioId) {
        this.inventarioId = inventarioId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}
