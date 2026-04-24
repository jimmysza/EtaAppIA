package maineta.eta.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "colaborador")
public class Colaborador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idColaborador;

    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(length = 50)
    private String nit;

    @Column(length = 150)
    private String correoSeguridad;

    @Column(name = "foto_perfil", length = 255)
    private String fotoPerfil;

    // Campos para pagos a colaborador
    @Column(length = 100)
    private String banco;

    @Column(length = 50)
    private String numeroCuenta;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoCuenta tipoCuenta;

    @Column(nullable = false)
    private Integer penalizaciones = 0;

    // getters y setters
}
