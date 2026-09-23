package com.Lucca.Projeto1.config;

import com.Lucca.Projeto1.security.ApiSecurityErrorWriter;
import com.Lucca.Projeto1.security.JwtProperties;
import com.Lucca.Projeto1.security.UsuarioAtivoFilter;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;



import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Environment environment;
    private final ApiSecurityErrorWriter errorWriter;
    private final UsuarioRepository usuarioRepository;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    public SecurityConfig(
            Environment environment,
            ApiSecurityErrorWriter errorWriter,
            UsuarioRepository usuarioRepository
    ) {
        this.environment = environment;
        this.errorWriter = errorWriter;
        this.usuarioRepository = usuarioRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                errorWriter.write(
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        "Não autenticado ou token inválido"
                                )
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                errorWriter.write(
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "Usuário autenticado sem permissão"
                                )
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/usuarios/encarregados")
                        .hasAnyRole("ADMIN", "OPERADOR", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/usuarios/encarregados")
                        .hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/materiais", "/materiais/**")
                        .hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(HttpMethod.GET, "/contratos", "/contratos/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/movimentacoes", "/movimentacoes/**")
                        .hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(HttpMethod.GET, "/notas-fiscais", "/notas-fiscais/**")
                        .hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/movimentacoes/*/estorno"
                        )
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/movimentacoes"
                        )
                        .hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/movimentacoes/*/assinatura"
                        )
                        .hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/notas-fiscais",
                                "/notas-fiscais/importar-xml",
                                "/notas-fiscais/*/confirmar"
                        )
                        .hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(HttpMethod.PUT, "/notas-fiscais/*")
                        .hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/materiais")
                        .hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/materiais", "/materiais/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/contratos", "/contratos/**")
                        .hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/movimentacoes", "/movimentacoes/**")
                        .hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/notas-fiscais", "/notas-fiscais/**")
                        .hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/usuarios", "/usuarios/**")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                )
                .addFilterAfter(
                        new UsuarioAtivoFilter(
                                usuarioRepository,
                                errorWriter
                        ),
                        BearerTokenAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return authenticationConverter;
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtProperties.secretKey())
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        return NimbusJwtDecoder
                .withSecretKey(jwtProperties.secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private List<String> allowedOriginPatterns() {
        String origins = environment.getProperty("APP_CORS_ALLOWED_ORIGINS");

        if (origins == null || origins.isBlank()) {
            throw new IllegalStateException(
                    "A variável de ambiente APP_CORS_ALLOWED_ORIGINS deve ser configurada"
            );
        }

        List<String> allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS deve listar origens explícitas ou padrões controlados"
            );
        }

        return allowedOrigins;
    }

    private List<String> allowedOrigins() {
        String origins = environment.getProperty("APP_CORS_ALLOWED_ORIGINS");

        if (origins == null || origins.isBlank()) {
            throw new IllegalStateException(
                    "A variável de ambiente APP_CORS_ALLOWED_ORIGINS deve ser configurada"
            );
        }

        List<String> allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS deve listar origens explícitas"
            );
        }

        return allowedOrigins;
    }
}
