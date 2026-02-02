package maineta.eta.config;

// Importaciones necesarias para configuración de seguridad
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import maineta.eta.service.UsuarioService;

@Configuration // Indica que esta clase contiene configuraciones de Spring
@EnableWebSecurity // Habilita la seguridad web de Spring Security
public class SecurityConfig {

        // Inyección del manejador personalizado de login exitoso
        @Autowired
        private maineta.eta.config.CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

        // Servicio que implementa UserDetailsService para cargar usuarios desde base de datos
        @Autowired
        private UsuarioService usuarioService;

        /**
         * Bean que define el codificador de contraseñas.
         * BCrypt es seguro y recomendado para almacenar contraseñas.
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * Bean que mantiene un registro de sesiones activas por usuario.
         */
        @Bean
        public SessionRegistry sessionRegistry() {
                return new SessionRegistryImpl();
        }

        /**
         * Bean que configura el proveedor de autenticación basado en usuarios cargados
         * desde la base de datos (DaoAuthenticationProvider).
         */
        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(usuarioService); // Servicio que carga el usuario
                authProvider.setPasswordEncoder(passwordEncoder()); // Codificador de contraseña
                return authProvider;
        }

        /**
         * Bean que proporciona el AuthenticationManager, necesario para realizar autenticaciones manuales.
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        /**
         * Bean que mapea las autoridades de usuario.
         * En este caso, asegura que todo usuario autenticado tenga también el rol "ROLE_USER".
         */
        @Bean
        public GrantedAuthoritiesMapper userAuthoritiesMapper() {
                return (authorities) -> {
                        Set<GrantedAuthority> mapped = new HashSet<>(authorities);
                        mapped.add(new SimpleGrantedAuthority("ROLE_USER")); // Rol genérico para todos los usuarios
                        return mapped;
                };
        }

        /**
         * Configura toda la seguridad de la aplicación: rutas públicas, roles requeridos, login, logout, sesiones, etc.
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationProvider auth,
                        SessionRegistry sessionRegistry) throws Exception {
                http
                        // Deshabilita CSRF (útil en apps REST o si se maneja manualmente)
                        .csrf(csrf -> csrf.disable())

                        // Configura qué URLs están permitidas sin autenticación y qué roles se requieren
                        .authorizeHttpRequests(requests -> requests
                                .requestMatchers(
                                        "/registro/**",
                                        "/actividad/**",
                                        "/login",
                                        "/css/**",
                                        "/404",
                                        "/terminos-condiciones",
                                        "/403",
                                        "/actividades/**",
                                        "/js/**",
                                        "/registro/**",
                                        "/assets/**",
                                        "/fonts/**",
                                        "/images/**",
                                        "/uploads/**")
                                .permitAll() // Rutas públicas (login, recursos estáticos, páginas de error, etc.)
                                .requestMatchers("/").permitAll() // La raíz es pública, pero puede controlarse desde el controller

                                // Rutas con permisos por rol
                                .requestMatchers("/actividades")
                                .hasAnyAuthority("ROLE_CLIENTE", "ROLE_COLABORADOR")

                                // Solo colaboradores pueden crear (POST) actividades
                                .requestMatchers(HttpMethod.POST, "/actividades")
                                .hasAuthority("ROLE_COLABORADOR")

                                
                                

                                // Secciones exclusivas para clientes
                                .requestMatchers("/cliente/**","/comentarios/**").hasAuthority("ROLE_CLIENTE")
                                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")

                                // Secciones exclusivas para colaboradores
                                .requestMatchers("/colaborador/**", "/colaborador/dashboard", "/actividades/**")
                                .hasAuthority("ROLE_COLABORADOR")

                                // Toda otra solicitud requiere autenticación
                                .anyRequest().authenticated())

                        // Configura el formulario de login
                        .formLogin(form -> form
                                .loginPage("/login") // Página de login personalizada
                                .successHandler(customAuthenticationSuccessHandler) // Manejador de éxito personalizado
                                .permitAll())

                        // Configura el logout
                        .logout(logout -> logout
                                .logoutUrl("/logout") // URL para cerrar sesión
                                .logoutSuccessUrl("/?logout")
                                .invalidateHttpSession(true) // Invalida la sesión
                                .clearAuthentication(true) // Limpia la autenticación
                                .permitAll())

                        // Configura login con OAuth2 (por ejemplo, Google)
                        .oauth2Login(oauth -> oauth
                                .loginPage("/login") // Página de login compartida
                                .userInfoEndpoint(ui -> ui.userAuthoritiesMapper(userAuthoritiesMapper())) // Mapeo de roles
                                .defaultSuccessUrl("/", true)) // Redirección tras login exitoso
                        .exceptionHandling(ex -> ex
                                .accessDeniedPage("/403") // Para errores de autorización (403)
                                // Se configura DENTRO del bloque 'exceptionHandling'
                                .authenticationEntryPoint((request, response, authException) -> {
                                    // Redirigir al usuario que NO está autenticado e intenta acceder a una ruta protegida
                                    String redirectUrl = "/login?registrarse";
                                    response.sendRedirect(redirectUrl);
                                })
                        )
                        // Gestión de sesiones
                        .sessionManagement(sess -> sess
                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Crea sesión solo si es necesaria
                                .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession) // ✅ Opción más limpia y moderna
                                .maximumSessions(1) // Solo se permite una sesión activa por usuario
                                .maxSessionsPreventsLogin(false) // Si hay una sesión activa, expira la anterior en lugar de bloquear
                                .expiredUrl("/login?expired") // Redirección si la sesión expiró
                                .sessionRegistry(sessionRegistry)
                        );

                return http.build(); // Devuelve el filtro de seguridad construido
        }

}

