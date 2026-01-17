package maineta.eta.dto;



import lombok.Data;

@Data
public class CategoriaDTO {

    private Long idCategoria;
    private String nombre;
    private String imagen;
    private int cantidad;

}