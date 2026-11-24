package ni.uam.edu.tiendazapatos.reportes;

/**
 * DTO de fila para el reporte JasperReports de inventario de calzado.
 */
public class InventarioReporteItem {

    private String modelo;
    private String marca;
    private String genero;
    private String talla;
    private String color;
    private Integer stock;
    private Double precioFinal;
    private Double valorInventario;
    private String estado;

    public InventarioReporteItem() {
    }

    public InventarioReporteItem(
            String modelo,
            String marca,
            String genero,
            String talla,
            String color,
            Integer stock,
            Double precioFinal,
            Double valorInventario,
            String estado
    ) {
        this.modelo = modelo;
        this.marca = marca;
        this.genero = genero;
        this.talla = talla;
        this.color = color;
        this.stock = stock;
        this.precioFinal = precioFinal;
        this.valorInventario = valorInventario;
        this.estado = estado;
    }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Double getPrecioFinal() { return precioFinal; }
    public void setPrecioFinal(Double precioFinal) { this.precioFinal = precioFinal; }

    public Double getValorInventario() { return valorInventario; }
    public void setValorInventario(Double valorInventario) { this.valorInventario = valorInventario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
