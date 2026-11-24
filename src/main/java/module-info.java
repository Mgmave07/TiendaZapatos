module ni.uam.edu.tiendazapatos {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires java.sql;
    requires java.desktop;
    requires org.hibernate.orm.core;
    requires net.sf.jasperreports.core;
    requires net.sf.jasperreports.pdf;
    requires net.sf.jasperreports.jdt;

    // Abrir paquetes para que JavaFX, Hibernate y JasperReports puedan acceder a ellos
    opens ni.uam.edu.tiendazapatos to javafx.fxml;
    opens ni.uam.edu.tiendazapatos.Controller to javafx.fxml;
    opens ni.uam.edu.tiendazapatos.modelo to org.hibernate.orm.core;
    opens ni.uam.edu.tiendazapatos.reportes;

    // Exportar paquetes
    exports ni.uam.edu.tiendazapatos;
    exports ni.uam.edu.tiendazapatos.modelo;
    exports ni.uam.edu.tiendazapatos.Controller;
    exports ni.uam.edu.tiendazapatos.config;
    exports ni.uam.edu.tiendazapatos.dao;
    exports ni.uam.edu.tiendazapatos.reportes;
}