package com.zhiyuan.college.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    /**
     * OpenAPI/Swagger endpoints. They describe every route (including admin routes) and are therefore
     * blocked unless {@code security.expose-api-docs} is explicitly enabled for the environment.
     */
    private static final String[] API_DOC_PATHS = {
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/webjars/swagger-ui/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   ObjectMapper objectMapper,
                                                   @Value("${security.expose-api-docs:false}") boolean exposeApiDocs)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    if (exposeApiDocs) {
                        auth.requestMatchers(API_DOC_PATHS).permitAll();
                    } else {
                        auth.requestMatchers(API_DOC_PATHS).denyAll();
                    }
                    auth.requestMatchers("/api/auth/**", "/api/meta/**",
                                    "/api/universities", "/api/universities/**",
                                    "/api/probability", "/api/probability/**").permitAll()
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers("/api/**").authenticated()
                            // Remaining paths are the bundled single page application (index.html, /assets/**).
                            .anyRequest().permitAll();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeError(HttpServletResponse response,
                            ObjectMapper objectMapper,
                            int status,
                            String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "status", status,
                "message", message
        ));
    }
}
