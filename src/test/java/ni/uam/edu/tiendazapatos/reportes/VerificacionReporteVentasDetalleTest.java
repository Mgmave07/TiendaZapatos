package ni.uam.edu.tiendazapatos.reportes;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración y verificación para ventas_detalle_reporte.jrxml.
 * Valida que la plantilla compile sin errores en JasperReports 7.0.3,
 * se llene correctamente con DetalleVentaReporteItem y se exporte a PDF.
 */
public class VerificacionReporteVentasDetalleTest {

    @Test
    @DisplayName("Debe compilar la plantilla JRXML y exportar exitosamente un PDF con datos de prueba")
    public void testGeneracionPdfVentasDetalle() throws Exception {
        String rutaPlantilla = "/ni/uam/edu/tiendazapatos/reportes/ventas_detalle_reporte.jrxml";
        InputStream is = getClass().getResourceAsStream(rutaPlantilla);
        assertNotNull(is, "El archivo ventas_detalle_reporte.jrxml debe existir en el classpath");

        // 1. Compilación de la plantilla JRXML
        JasperReport reporte = JasperCompileManager.compileReport(is);
        assertNotNull(reporte, "El reporte debe compilarse correctamente en memoria");

        // 2. Parámetros del reporte
        Map<String, Object> params = new HashMap<>();
        params.put("NOMBRE_TIENDA", "TIENDA DE ZAPATOS - TEST");
        params.put("TITULO_REPORTE", "REPORTE DETALLADO DE VENTAS POR PRODUCTO");
        params.put("FECHA_GENERACION", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        params.put("GENERADO_POR", "Administrador de Pruebas");
        params.put("RANGO_FECHAS", "Todas las fechas (Prueba Unitaria)");
        params.put("TOTAL_FACTURAS", 2);
        params.put("GRAN_TOTAL_IVA", 360.00);
        params.put("GRAN_TOTAL", 2760.00);

        // 3. Datos de prueba con 2 facturas y varias líneas de productos
        List<DetalleVentaReporteItem> items = List.of(
                new DetalleVentaReporteItem(
                        "FAC-TEST-001",
                        "26/11/2026 10:00",
                        "Juan Pérez (vendedor)",
                        "Efectivo",
                        180.00,
                        1380.00,
                        "Nike Air Max SC (Nike)",
                        "HOMBRE | US 42 | Negro/Blanco",
                        1,
                        1200.00,
                        1200.00
                ),
                new DetalleVentaReporteItem(
                        "FAC-TEST-001",
                        "26/11/2026 10:00",
                        "Juan Pérez (vendedor)",
                        "Efectivo",
                        180.00,
                        1380.00,
                        "Adidas Grand Court (Adidas)",
                        "HOMBRE | US 41 | Blanco",
                        1,
                        800.00,
                        800.00
                ),
                new DetalleVentaReporteItem(
                        "FAC-TEST-002",
                        "26/11/2026 11:30",
                        "Ana Martínez (vendedor)",
                        "Tarjeta",
                        180.00,
                        1380.00,
                        "Puma Smash v2 (Puma)",
                        "UNISEX | US 39 | Azul Marino",
                        2,
                        600.00,
                        1200.00
                )
        );

        // 4. Llenado con JRBeanCollectionDataSource
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(items);
        JasperPrint jasperPrint = JasperFillManager.fillReport(reporte, params, dataSource);
        assertNotNull(jasperPrint, "JasperPrint no debe ser nulo");
        assertFalse(jasperPrint.getPages().isEmpty(), "El reporte generado debe contener al menos una página");

        // 5. Exportación a archivo temporal PDF
        File tempPdf = File.createTempFile("Test_Reporte_Ventas_Detalle_", ".pdf");
        tempPdf.deleteOnExit();

        JasperExportManager.exportReportToPdfFile(jasperPrint, tempPdf.getAbsolutePath());

        assertTrue(tempPdf.exists(), "El archivo PDF debe existir en disco");
        assertTrue(tempPdf.length() > 1000, "El PDF generado debe tener contenido válido (tamaño > 1 KB)");

        System.out.println(">>> VERIFICACIÓN EXITOSA: PDF generado en " + tempPdf.getAbsolutePath() +
                " (" + tempPdf.length() + " bytes)");
    }
}
