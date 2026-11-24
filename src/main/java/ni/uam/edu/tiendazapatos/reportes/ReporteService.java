package ni.uam.edu.tiendazapatos.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import ni.uam.edu.tiendazapatos.config.SesionUsuario;
import ni.uam.edu.tiendazapatos.dao.InventarioDAO;
import ni.uam.edu.tiendazapatos.dao.UsuarioDAO;
import ni.uam.edu.tiendazapatos.dao.VentaDAO;
import ni.uam.edu.tiendazapatos.dao.ZapatoDAO;
import ni.uam.edu.tiendazapatos.modelo.DetalleVenta;
import ni.uam.edu.tiendazapatos.modelo.Usuario;
import ni.uam.edu.tiendazapatos.modelo.Venta;
import ni.uam.edu.tiendazapatos.modelo.Zapato;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compila las plantillas JRXML del proyecto, las llena con datos de los DAO
 * y exporta los reportes resultantes a archivos PDF.
 */
public class ReporteService {

    public static class SinDatosReporteException extends RuntimeException {
        public SinDatosReporteException(String message) {
            super(message);
        }
    }

    private static final String RUTA_PLANTILLAS = "/ni/uam/edu/tiendazapatos/reportes/";
    private static final String NOMBRE_TIENDA = "TIENDA DE ZAPATOS";
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ZapatoDAO zapatoDAO;
    private final InventarioDAO inventarioDAO;
    private final UsuarioDAO usuarioDAO;
    private final VentaDAO ventaDAO;

    private final Map<String, JasperReport> cacheReportes = new ConcurrentHashMap<>();

    public ReporteService() {
        this(new ZapatoDAO(), new InventarioDAO(), new UsuarioDAO(), new VentaDAO());
    }

    public ReporteService(
            ZapatoDAO zapatoDAO,
            InventarioDAO inventarioDAO,
            UsuarioDAO usuarioDAO,
            VentaDAO ventaDAO
    ) {
        this.zapatoDAO = zapatoDAO;
        this.inventarioDAO = inventarioDAO;
        this.usuarioDAO = usuarioDAO;
        this.ventaDAO = ventaDAO;
    }

    public List<InventarioReporteItem> obtenerDatosInventario() {
        List<Zapato> zapatos = zapatoDAO.obtenerTodos();
        if (zapatos == null || zapatos.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<InventarioDAO.Variante>> variantesPorZapato = new HashMap<>();
        List<InventarioDAO.Variante> todasVariantes = inventarioDAO.obtenerVariantes();
        if (todasVariantes != null) {
            for (InventarioDAO.Variante variante : todasVariantes) {
                variantesPorZapato
                        .computeIfAbsent(variante.zapatoId(), id -> new ArrayList<>())
                        .add(variante);
            }
        }

        List<InventarioReporteItem> items = new ArrayList<>();
        for (Zapato zapato : zapatos) {
            String modelo = textoSeguro(zapato.getNombre(), "Sin nombre");
            String marca = (zapato.getMarca() != null && zapato.getMarca().getNombre() != null
                    && !zapato.getMarca().getNombre().isBlank())
                    ? zapato.getMarca().getNombre()
                    : "Sin marca";
            double precioFinal = zapato.getPrecioFinal() != null
                    ? zapato.getPrecioFinal().doubleValue()
                    : 0.0;
            boolean activo = Boolean.TRUE.equals(zapato.getActivo());

            List<InventarioDAO.Variante> variantes = variantesPorZapato.get(zapato.getZapatoId());
            if (variantes == null || variantes.isEmpty()) {
                items.add(new InventarioReporteItem(
                        modelo,
                        marca,
                        "—",
                        "—",
                        "—",
                        0,
                        precioFinal,
                        0.0,
                        activo ? "Sin registro" : "Inactivo"
                ));
            } else {
                for (InventarioDAO.Variante v : variantes) {
                    int stock = Math.max(0, v.cantidadStock());
                    String tallaCompuesta = (textoSeguro(v.sistemaTalla(), "") + " " + textoSeguro(v.talla(), "")).trim();
                    if (tallaCompuesta.isEmpty()) {
                        tallaCompuesta = "—";
                    }
                    String estado = !activo
                            ? "Inactivo"
                            : (stock > 0 ? "Disponible" : "Agotado");

                    items.add(new InventarioReporteItem(
                            modelo,
                            marca,
                            textoSeguro(v.genero(), "—"),
                            tallaCompuesta,
                            textoSeguro(v.color(), "—"),
                            stock,
                            precioFinal,
                            stock * precioFinal,
                            estado
                    ));
                }
            }
        }
        return items;
    }

    public List<UsuarioReporteItem> obtenerDatosUsuarios() {
        List<Usuario> usuarios = usuarioDAO.obtenerTodos();
        if (usuarios == null || usuarios.isEmpty()) {
            return List.of();
        }

        List<UsuarioReporteItem> items = new ArrayList<>();
        for (Usuario u : usuarios) {
            String rol = (u.getRol() != null && u.getRol().getNombre() != null && !u.getRol().getNombre().isBlank())
                    ? u.getRol().getNombre()
                    : "Sin rol";
            String estado = Boolean.TRUE.equals(u.getActivo()) ? "Activo" : "Inactivo";
            String fechaReg = u.getCreadoEn() != null ? u.getCreadoEn().format(FORMATO_FECHA_HORA) : "—";

            items.add(new UsuarioReporteItem(
                    textoSeguro(u.getNombreUsuario(), "—"),
                    textoSeguro(u.getNombreCompleto(), "—"),
                    textoSeguro(u.getCorreo(), "—"),
                    textoSeguro(u.getTelefono(), "—"),
                    rol,
                    estado,
                    fechaReg
            ));
        }
        return items;
    }

    public List<VentaReporteItem> obtenerDatosVentas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }

        List<Venta> ventas = ventaDAO.obtenerTodas();
        if (ventas == null || ventas.isEmpty()) {
            return List.of();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        List<VentaReporteItem> items = new ArrayList<>();

        for (Venta v : ventas) {
            if (v.getFechaVenta() != null && (desde != null || hasta != null)) {
                LocalDate fechaLocal = v.getFechaVenta().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                if (desde != null && fechaLocal.isBefore(desde)) {
                    continue;
                }
                if (hasta != null && fechaLocal.isAfter(hasta)) {
                    continue;
                }
            } else if ((desde != null || hasta != null) && v.getFechaVenta() == null) {
                continue;
            }

            int cantArticulos = 0;
            if (v.getDetalles() != null) {
                for (DetalleVenta d : v.getDetalles()) {
                    cantArticulos += d.getCantidad();
                }
            }

            items.add(new VentaReporteItem(
                    textoSeguro(v.getNumeroVenta(), "—"),
                    v.getFechaVenta() != null ? sdf.format(v.getFechaVenta()) : "—",
                    textoSeguro(v.getClienteNombre(), "Consumidor Final"),
                    textoSeguro(v.getMetodoPago(), "—"),
                    textoSeguro(v.getEstado(), "PAGADA"),
                    cantArticulos,
                    v.getSubtotal(),
                    v.getImpuesto(),
                    v.getTotal()
            ));
        }
        return items;
    }

    public int generarReporteInventario(File destinoPdf) throws JRException, IOException {
        List<InventarioReporteItem> datos = obtenerDatosInventario();
        if (datos.isEmpty()) {
            throw new SinDatosReporteException("No hay productos registrados en el inventario para generar el reporte.");
        }

        int totalUnidades = 0;
        double valorTotal = 0.0;
        for (InventarioReporteItem item : datos) {
            totalUnidades += item.getStock() != null ? item.getStock() : 0;
            valorTotal += item.getValorInventario() != null ? item.getValorInventario() : 0.0;
        }

        Map<String, Object> parametros = crearParametrosBase("REPORTE DE INVENTARIO DE CALZADO");
        parametros.put("TOTAL_UNIDADES", totalUnidades);
        parametros.put("VALOR_INVENTARIO", valorTotal);

        exportarPdf("inventario_reporte.jrxml", parametros, datos, destinoPdf);
        return datos.size();
    }

    public int generarReporteUsuarios(File destinoPdf) throws JRException, IOException {
        List<UsuarioReporteItem> datos = obtenerDatosUsuarios();
        if (datos.isEmpty()) {
            throw new SinDatosReporteException("No hay usuarios registrados en el sistema para generar el reporte.");
        }

        int activos = 0;
        int inactivos = 0;
        for (UsuarioReporteItem item : datos) {
            if ("Activo".equalsIgnoreCase(item.getEstado())) {
                activos++;
            } else {
                inactivos++;
            }
        }

        Map<String, Object> parametros = crearParametrosBase("REPORTE DE USUARIOS DEL SISTEMA");
        parametros.put("TOTAL_ACTIVOS", activos);
        parametros.put("TOTAL_INACTIVOS", inactivos);

        exportarPdf("usuarios_reporte.jrxml", parametros, datos, destinoPdf);
        return datos.size();
    }

    public int generarReporteVentas(File destinoPdf, LocalDate desde, LocalDate hasta) throws JRException, IOException {
        List<VentaReporteItem> datos = obtenerDatosVentas(desde, hasta);
        if (datos.isEmpty()) {
            throw new SinDatosReporteException(
                    (desde != null || hasta != null)
                            ? "No se encontraron ventas registradas en el rango de fechas seleccionado."
                            : "No hay ventas registradas en la base de datos todavía."
            );
        }

        String rangoTexto = construirTextoRango(desde, hasta);
        Map<String, Object> parametros = crearParametrosBase("REPORTE DE VENTAS Y FACTURACIÓN");
        parametros.put("RANGO_FECHAS", rangoTexto);

        exportarPdf("ventas_reporte.jrxml", parametros, datos, destinoPdf);
        return datos.size();
    }

    private void exportarPdf(
            String nombrePlantilla,
            Map<String, Object> parametros,
            List<?> datos,
            File destinoPdf
    ) throws JRException, IOException {
        if (destinoPdf == null) {
            throw new IllegalArgumentException("Debe seleccionar un archivo PDF de destino.");
        }

        File padre = destinoPdf.getParentFile();
        if (padre != null && !padre.exists() && !padre.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de destino: " + padre.getAbsolutePath());
        }

        JasperReport reporte = obtenerOCompilarPlantilla(nombrePlantilla);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(datos);
        JasperPrint jasperPrint = JasperFillManager.fillReport(reporte, parametros, dataSource);
        JasperExportManager.exportReportToPdfFile(jasperPrint, destinoPdf.getAbsolutePath());
    }

    private JasperReport obtenerOCompilarPlantilla(String nombrePlantilla) throws JRException, IOException {
        JasperReport enCache = cacheReportes.get(nombrePlantilla);
        if (enCache != null) {
            return enCache;
        }

        String rutaCompleta = RUTA_PLANTILLAS + nombrePlantilla;
        try (InputStream is = ReporteService.class.getResourceAsStream(rutaCompleta)) {
            if (is == null) {
                throw new IOException("No se encontró la plantilla de reporte: " + rutaCompleta);
            }
            JasperReport compilado = JasperCompileManager.compileReport(is);
            cacheReportes.put(nombrePlantilla, compilado);
            return compilado;
        }
    }

    private Map<String, Object> crearParametrosBase(String subtituloReporte) {
        Map<String, Object> params = new HashMap<>();
        params.put("NOMBRE_TIENDA", NOMBRE_TIENDA);
        params.put("TITULO_REPORTE", subtituloReporte);
        params.put("FECHA_GENERACION", LocalDateTime.now().format(FORMATO_FECHA_HORA));
        params.put("GENERADO_POR", obtenerUsuarioGenerador());
        return params;
    }

    private String obtenerUsuarioGenerador() {
        Usuario actual = SesionUsuario.getInstancia().getUsuarioActual();
        if (actual == null) {
            return "Administrador";
        }
        if (actual.getNombreCompleto() != null && !actual.getNombreCompleto().isBlank()) {
            return actual.getNombreCompleto() + " (" + actual.getNombreUsuario() + ")";
        }
        return textoSeguro(actual.getNombreUsuario(), "Administrador");
    }

    private static String construirTextoRango(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return "Todas las fechas";
        }
        String d = desde != null ? desde.format(FORMATO_FECHA) : "Inicio";
        String h = hasta != null ? hasta.format(FORMATO_FECHA) : "Actual";
        return "Del " + d + " al " + h;
    }

    private static String textoSeguro(String valor, String respaldo) {
        return (valor == null || valor.isBlank()) ? respaldo : valor.trim();
    }
}
