package maineta.eta.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/* import lombok.ToString; */

/* @ToString(exclude = {"roles", "colaborador"}) */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = {"email"}))
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 15, nullable = true)
    private String telefono;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "email_verificado")
    private Boolean emailVerificado = Boolean.FALSE;

    @Column(name = "token_verificacion", length = 120)
    private String tokenVerificacion;

    @Column(name = "token_verificacion_expira_en")
    private LocalDateTime tokenVerificacionExpiraEn;
    
   /*
     *  activo BOOLEAN DEFAULT TRUE,
    

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    */

   /* @ToString.Exclude */
    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

}
