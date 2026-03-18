package maineta.eta.config;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import maineta.eta.entity.Admin;
import maineta.eta.entity.Categoria;
import maineta.eta.entity.Idioma;
import maineta.eta.entity.Rol;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.AdminRepository;
import maineta.eta.repository.CategoriaRepository;
import maineta.eta.repository.IdiomaRepository;
import maineta.eta.repository.RolRepository;
import maineta.eta.repository.UsuarioRepository;

/**
 * Esta clase inicializa datos en la base de datos al arrancar la aplicación.
 * En este caso, se asegura de que los roles principales estén creados.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final IdiomaRepository idiomaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoriaRepository categoriaRepository;

    /**
     * Inyecta el repositorio de roles para interactuar con la base de datos.
     *
     * @param rolRepository repositorio de la entidad Rol
     */
    public DataInitializer(PasswordEncoder passwordEncoder, AdminRepository adminRepository,
            RolRepository rolRepository, IdiomaRepository idiomaRepository, UsuarioRepository usuarioRepository,
            CategoriaRepository categoriaRepository) {
        this.rolRepository = rolRepository;
        this.idiomaRepository = idiomaRepository;
        this.usuarioRepository = usuarioRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoriaRepository = categoriaRepository;
    }

    /**
     * Este método se ejecuta automáticamente al iniciar la aplicación
     * gracias a que implementamos CommandLineRunner.
     */
    @Override
    public void run(String... args) throws Exception {
        // Definimos los roles que queremos garantizar que existan en la base de datos
        String[] roles = { "ROLE_CLIENTE", "ROLE_COLABORADOR", "ROLE_ADMIN" };

        // Recorremos cada rol de la lista
        for (String nombreRol : roles) {
            // Verificamos si el rol ya existe en la base de datos
            Optional<Rol> rolExistente = rolRepository.findByNombre(nombreRol);

            // Si el rol NO existe, lo creamos y lo guardamos
            if (rolExistente.isEmpty()) {
                Rol rol = new Rol();
                rol.setNombre(nombreRol);
                rolRepository.save(rol);
                System.out.println("Rol creado: " + nombreRol);
            }
        }

        String[][] idiomas = {
                { "Español", "es" },
                { "Inglés", "en" },
                { "Francés", "fr" }
        };

        for (String[] idioma : idiomas) { // El tipo debe ser String[]
            String nombreIdioma = idioma[0]; // El nombre es el primer elemento
            String codigoIdioma = idioma[1]; // El código es el segundo elemento

            // Verificamos si el idioma ya existe en la base de datos
            Optional<Idioma> idiomaExistente = idiomaRepository.findByNombre(nombreIdioma);

            // Si el idioma NO existe, lo creamos y lo guardamos
            if (idiomaExistente.isEmpty()) {
                Idioma nuevoIdioma = new Idioma();
                nuevoIdioma.setNombre(nombreIdioma);
                nuevoIdioma.setCodigo(codigoIdioma); // Asumimos que la entidad Idioma tiene un método setCodigo
                idiomaRepository.save(nuevoIdioma);
                System.out.println("Idioma creado: " + nombreIdioma + " (" + codigoIdioma + ")");
            }
        }

        String[] categorias = { "Playa", "Gastronomía",
                "Historia", "Entretenimiento","Cultura","Vida Nocturna" };

        for (String categoria : categorias) { // El tipo debe ser String[]

            // Verificamos si el idioma ya existe en la base de datos
            Optional<Categoria> categoriaExistente = categoriaRepository.findByNombre(categoria);

            // Si el idioma NO existe, lo creamos y lo guardamos
            if (categoriaExistente.isEmpty()) {
                Categoria nuevoCategoria = new Categoria();
                nuevoCategoria.setNombre(categoria);
                // Generar ruta de imagen basada en el nombre (ej.
                // experiencias_acuaticas_y_playa.jpg)
                String rutaImagen = categoria.toLowerCase()
                        .replace(" ", "_")
                        .replace("ó", "o")
                        .replace("é", "e")
                        .replace("í", "i")
                        .replace("ú", "u")
                        .replace("á", "a")
                        .replace("ñ", "n") + ".png";
                nuevoCategoria.setImagen("/src/main/resources/static/images/categorias/" + rutaImagen);
                categoriaRepository.save(nuevoCategoria);

                System.out.println("Categoría creada: " + categoria + " con imagen: " + nuevoCategoria.getImagen());
            }
        }

        // --- Crear usuario ADMIN por defecto ---
        String emailAdmin = "admin@gmail";

        if (usuarioRepository.findByEmail(emailAdmin).isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setNombre("Administrador");
            usuario.setTelefono("3052135329");
            usuario.setEmail(emailAdmin);
            usuario.setPassword(passwordEncoder.encode("admin123"));

            // Asignar rol ADMIN (asegúrate que ROLE_ADMIN tenga ID = 3 o busca por nombre)
            Rol rolAdmin = rolRepository.findByNombre("ROLE_ADMIN").orElseThrow();
            usuario.setRol(rolAdmin);

            usuarioRepository.save(usuario);

            // --- Crear admin vinculado ---
            Admin admin = new Admin();
            admin.setUsuario(usuario);
            admin.setPorcentajeComision(new BigDecimal("18.00"));

            adminRepository.save(admin);

            System.out.println("Administrador creado con correo: " + emailAdmin);
        } else {
            System.out.println("El administrador ya existe.");
        }
        // Mensaje final de confirma
        System.out.println("Inicialización de roles completada");
    }
}
