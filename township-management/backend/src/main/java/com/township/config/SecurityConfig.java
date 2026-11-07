package com.township.config;

import com.township.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("""
                        {
                          "status": 401,
                          "error": "Unauthorized",
                          "message": "Full authentication is required to access this resource",
                          "path": "%s"
                        }
                        """.formatted(request.getRequestURI()));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("""
                        {
                          "status": 403,
                          "error": "Forbidden",
                          "message": "%s",
                          "path": "%s"
                        }
                        """.formatted(accessDeniedException.getMessage() != null ? accessDeniedException.getMessage() : "Access is denied", request.getRequestURI()));
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/towers/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/billing/admin/**").hasAuthority("ROLE_HELP_DESK")
                .requestMatchers("/api/billing/**").authenticated()
                .requestMatchers("/api/payments/admin/**").hasAuthority("ROLE_HELP_DESK")
                .requestMatchers("/api/payments/**").authenticated()
                .requestMatchers("/api/reconciliation/**").hasAuthority("ROLE_HELP_DESK")
                .requestMatchers("/api/complaints/admin/**").hasAuthority("ROLE_HELP_DESK")
                .requestMatchers("/api/complaints/**").authenticated()
                .requestMatchers("/api/visitors/gate/**").hasAnyAuthority("ROLE_GUARD", "ROLE_HELP_DESK")
                .requestMatchers("/api/visitors/admin/**").hasAuthority("ROLE_HELP_DESK")
                .requestMatchers("/api/visitors/**").authenticated()
                .requestMatchers("/api/amenities/admin/**").hasAuthority("ROLE_HELP_DESK")
                .requestMatchers("/api/amenities/**").authenticated()
                .requestMatchers("/api/notices/admin/**").hasAuthority("ROLE_HELP_DESK")
                .requestMatchers("/api/notices/**").authenticated()
                .requestMatchers("/api/committee/**").hasAnyAuthority("ROLE_COMMITTEE", "ROLE_HELP_DESK")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
