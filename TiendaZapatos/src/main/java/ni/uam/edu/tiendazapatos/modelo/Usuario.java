package ni.uam.edu.tiendazapatos.modelo;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")

public class Usuario {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        @Column(name = "usuario_id", columnDefinition = "uuid")
        private UUID usuarioId;

        @Column(name = "nombre_usuario", nullable = false, unique = true, length = 50)
        private String nombreUsuario;

        @Column(name = "nombre_completo", nullable = false, length = 150)
        private String nombreCompleto;

        @Column(name = "correo", unique = true, length = 100)
        private String correo;

        @Column(name = "password_hash", nullable = false, length = 255)
        private String passwordHash;

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "rol_id")
        private Rol rol;

        @Column(name = "telefono", length = 20)
        private String telefono;

        @Column(name = "activo")
        private Boolean activo = true;

        @Column(name = "ultimo_acceso")
        private LocalDateTime ultimoAcceso;

        @Column(name = "creado_en")
        private LocalDateTime creadoEn;

        public UUID getUsuarioId() { return usuarioId; }
        public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
        public String getNombreUsuario() { return nombreUsuario; }
        public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
        public String getNombreCompleto() { return nombreCompleto; }
        public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
        public String getCorreo() { return correo; }
        public void setCorreo(String correo) { this.correo = correo; }
        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
        public Rol getRol() { return rol; }
        public void setRol(Rol rol) { this.rol = rol; }
        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }
        public Boolean getActivo() { return activo; }
        public void setActivo(Boolean activo) { this.activo = activo; }
        public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
        public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
        public LocalDateTime getCreadoEn() { return creadoEn; }
        public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}

