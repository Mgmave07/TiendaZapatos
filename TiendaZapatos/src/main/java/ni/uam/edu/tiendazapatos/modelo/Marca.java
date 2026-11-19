package ni.uam.edu.tiendazapatos.modelo;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Table(name = "marcas")
public class Marca {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "marca_id", columnDefinition = "uuid")
    private UUID marcaId;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    public UUID getMarcaId() { return marcaId; }
    public void setMarcaId(UUID marcaId) { this.marcaId = marcaId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override
    public String toString() { return nombre; }
}
