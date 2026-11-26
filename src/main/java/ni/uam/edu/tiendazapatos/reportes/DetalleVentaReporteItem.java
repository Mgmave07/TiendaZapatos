package ni.uam.edu.tiendazapatos.reportes;

/**
 * DTO (Data Transfer Object) que representa UNA LÍNEA DE PRODUCTO dentro de una venta.
 *
 * <p>El reporte "REPORTE DE VENTAS POR PRODUCTO" muestra una fila por cada artículo
 * vendido, agrupada por número de factura, a diferencia del reporte de ventas
 * resumido (ventas_reporte.jrxml) que muestra una sola fila por factura.</p>
 *
 * <p>JasperReports llena este DTO a través de JRBeanCollectionDataSource: llama
 * automáticamente a los getters de esta clase usando reflexión, igualando el nombre
 * del getter con el atributo {@code <field name="...">} declarado en el .jrxml.</p>
 */
public class DetalleVentaReporteItem {

    // ── Datos de la factura (cabecera, iguales para todas las líneas de la misma venta) ──
    private String numeroVenta;   // Ej. "FAC-A1B2C3D4"
    private String fechaVenta;    // Formateada "dd/MM/yyyy HH:mm"
    private String vendedor;      // Nombre completo del usuario que realizó la venta
    private String metodoPago;    // "Efectivo", "Tarjeta", etc.
    private Double ivaVenta;      // IVA total de la factura (para el pie de grupo)
    private Double totalVenta;    // Total con IVA de la factura (para el pie de grupo)

    // ── Datos de la línea de producto ──
    private String producto;      // "Nike Air Max (Nike)"
    private String variante;      // "HOMBRE | US 42 | Negro"
    private Integer cantidad;     // Unidades vendidas en esta línea
    private Double precioUnitario; // Precio por unidad (C$)
    private Double subtotalLinea; // cantidad × precioUnitario

    public DetalleVentaReporteItem() {}

    public DetalleVentaReporteItem(
            String numeroVenta,
            String fechaVenta,
            String vendedor,
            String metodoPago,
            Double ivaVenta,
            Double totalVenta,
            String producto,
            String variante,
            Integer cantidad,
            Double precioUnitario,
            Double subtotalLinea
    ) {
        this.numeroVenta    = numeroVenta;
        this.fechaVenta     = fechaVenta;
        this.vendedor       = vendedor;
        this.metodoPago     = metodoPago;
        this.ivaVenta       = ivaVenta;
        this.totalVenta     = totalVenta;
        this.producto       = producto;
        this.variante       = variante;
        this.cantidad       = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotalLinea  = subtotalLinea;
    }

    // ── Getters y setters (JasperReports usa los getters por reflexión) ──────────

    public String getNumeroVenta()    { return numeroVenta; }
    public void setNumeroVenta(String v) { this.numeroVenta = v; }

    public String getFechaVenta()     { return fechaVenta; }
    public void setFechaVenta(String v)  { this.fechaVenta = v; }

    public String getVendedor()       { return vendedor; }
    public void setVendedor(String v)    { this.vendedor = v; }

    public String getMetodoPago()     { return metodoPago; }
    public void setMetodoPago(String v)  { this.metodoPago = v; }

    public Double getIvaVenta()       { return ivaVenta; }
    public void setIvaVenta(Double v)    { this.ivaVenta = v; }

    public Double getTotalVenta()     { return totalVenta; }
    public void setTotalVenta(Double v)  { this.totalVenta = v; }

    public String getProducto()       { return producto; }
    public void setProducto(String v)    { this.producto = v; }

    public String getVariante()       { return variante; }
    public void setVariante(String v)    { this.variante = v; }

    public Integer getCantidad()      { return cantidad; }
    public void setCantidad(Integer v)   { this.cantidad = v; }

    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double v) { this.precioUnitario = v; }

    public Double getSubtotalLinea()  { return subtotalLinea; }
    public void setSubtotalLinea(Double v)  { this.subtotalLinea = v; }
}
