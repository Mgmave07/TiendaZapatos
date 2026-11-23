package ni.uam.edu.tiendazapatos.modelo;

import jakarta.persistence.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ventas")
public class Venta {
    @Id
    @GeneratedValue
    private UUID venta_id;

    @Column(name = "numero_venta", unique = true)
    private String numeroVenta;

    @Column(name = "fecha_venta")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaVenta;

    private double subtotal;
    private double impuesto;
    private double total;

    private String estado = "PAGADA";

    @Column(name = "metodo_pago")
    private String metodoPago;

    @Column(name = "vendedor_id")
    private UUID vendedorId;

    @Column(name = "cliente_nombre", length = 150)
    private String clienteNombre;

    @Column(name = "cliente_identificacion", length = 50)
    private String clienteIdentificacion;

    @Column(name = "cliente_telefono", length = 20)
    private String clienteTelefono;

    @Column(name = "notas")
    private String notas;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<DetalleVenta> detalles;

    public Venta() {
    }

    public UUID getVenta_id() {
        return venta_id;
    }

    public void setVenta_id(UUID venta_id) {
        this.venta_id = venta_id;
    }

    public String getNumeroVenta() {
        return numeroVenta;
    }

    public void setNumeroVenta(String numeroVenta) {
        this.numeroVenta = numeroVenta;
    }

    public Date getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(Date fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getImpuesto() {
        return impuesto;
    }

    public void setImpuesto(double impuesto) {
        this.impuesto = impuesto;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }

    public UUID getVendedorId() {
        return vendedorId;
    }

    public void setVendedorId(UUID vendedorId) {
        this.vendedorId = vendedorId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteIdentificacion() {
        return clienteIdentificacion;
    }

    public void setClienteIdentificacion(String clienteIdentificacion) {
        this.clienteIdentificacion = clienteIdentificacion;
    }

    public String getClienteTelefono() {
        return clienteTelefono;
    }

    public void setClienteTelefono(String clienteTelefono) {
        this.clienteTelefono = clienteTelefono;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}