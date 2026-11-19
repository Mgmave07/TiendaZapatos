module ni.uam.edu.tiendazapatos {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires java.sql;
    requires org.hibernate.orm.core;

    // Abrir paquetes para que JavaFX y Hibernate puedan acceder a ellos
    opens ni.uam.edu.tiendazapatos to javafx.fxml;
    opens ni.uam.edu.tiendazapatos.Controller to javafx.fxml;
    opens ni.uam.edu.tiendazapatos.modelo to org.hibernate.orm.core;

    // Exportar paquetes
    exports ni.uam.edu.tiendazapatos;
    exports ni.uam.edu.tiendazapatos.modelo;
    exports ni.uam.edu.tiendazapatos.Controller;
    exports ni.uam.edu.tiendazapatos.config;
    exports ni.uam.edu.tiendazapatos.dao;
}