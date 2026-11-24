package ni.uam.edu.tiendazapatos.reportes;

/**
 * DTO de fila para el reporte JasperReports de ventas y facturación.
 */
public class VentaReporteItem {

    private String numeroVenta;
    private String fechaVenta;
    private String cliente;
    private String metodoPago;
    private String estado;
    private Integer articulos;
    private Double subtotal;
    private Double impuesto;
    private Double total;

    public VentaReporteItem() {
    }

    public VentaReporteItem(
            String numeroVenta,
            String fechaVenta,
            String cliente,
            String metodoPago,
            String estado,
            Integer articulos,
            Double subtotal,
            Double impuesto,
            Double total
    ) {
        this.numeroVenta = numeroVenta;
        this.fechaVenta = fechaVenta;
        this.cliente = cliente;
        this.metodoPago = metodoPago;
        this.estado = estado;
        this.articulos = articulos;
        this.subtotal = subtotal;
        this.impuesto = impuesto;
        this.total = total;
    }

    public String getNumeroVenta() { return numeroVenta; }
    public void setNumeroVenta(String numeroVenta) { this.numeroVenta = numeroVenta; }

    public String getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(String fechaVenta) { this.fechaVenta = fechaVenta; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getArticulos() { return articulos; }
    public void setArticulos(Integer articulos) { this.articulos = articulos; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getImpuesto() { return impuesto; }
    public void setImpuesto(Double impuesto) { this.impuesto = impuesto; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
}
