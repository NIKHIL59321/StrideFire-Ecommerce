package com.example.ecommerceProject.config;

import com.example.ecommerceProject.handler.OAuth2SuccessHandler;
import com.example.ecommerceProject.util.JwtFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public CorsConfigurationSource
    corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();

        // ✅ Allow React frontend
        config.setAllowedOrigins(
                Arrays.asList(
                        "http://localhost:5173",
                        "https://stridefire-frontend.vercel.app" // ✅
                ));

        // ✅ Allow all HTTP methods
        config.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS",
                "PATCH"));

        // ✅ Allow all required headers
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"));

        // ✅ Expose Authorization header
        config.setExposedHeaders(
                List.of("Authorization"));

        // ✅ Allow credentials
        config.setAllowCredentials(true);

        // ✅ Cache preflight for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**", config);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .cors(cors->cors
                        .configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED))

                // ✅ Handle 401
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                (request, response, authException) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_UNAUTHORIZED);
                                    response.setContentType("application/json");

                                    Map<String, String> error = new HashMap<>();
                                    error.put("error", "Unauthorized. Please login first");
                                    error.put("status", "401");

                                    response.getWriter().write(
                                            new ObjectMapper()
                                                    .writeValueAsString(error));
                                })

                        // ✅ Handle 403
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_FORBIDDEN);
                                    response.setContentType("application/json");

                                    Map<String, String> error = new HashMap<>();
                                    error.put("error", "Access denied. " + "You do not have permission");
                                    error.put("status", "403");

                                    response.getWriter().write(
                                            new ObjectMapper()
                                                    .writeValueAsString(error));
                                })
                )

                .authorizeHttpRequests(auth -> auth

                        // ✅ Public routes
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/login/oauth2/**").permitAll()

                        // ✅ Public GET products
                        .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // ✅ Admin only
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // ✅ Protected routes
                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/api/payment/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()

                        .anyRequest().authenticated()
                )

                // ✅ OAuth2 Login
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler))

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}